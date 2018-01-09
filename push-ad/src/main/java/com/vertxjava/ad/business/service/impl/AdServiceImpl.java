package com.vertxjava.ad.business.service.impl;

import com.vertxjava.ad.business.handler.impl.AdMatherHandlerImpl;
import com.vertxjava.ad.business.service.AdService;
import com.vertxjava.redis.RedisFClient;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.redis.RedisOptions;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-09 19:26
 **/
public class AdServiceImpl implements AdService {

    // ready ad key
    private static final String READY_AD_KEY = "READY_AD";
    private static final String DEVICE_INFO_KEY = "DEVICE_INFO";
    private Logger logger = LoggerFactory.getLogger(AdMatherHandlerImpl.class);
    private RedisFClient redisFClient;
    private JsonObject adContext = new JsonObject();

    public AdServiceImpl(Vertx vertx, JsonObject config) {
        JsonObject redisConfig = config.getJsonObject("redisConfig");
        RedisOptions redisOptions = new RedisOptions()
                .setHost(redisConfig.getString("host"))
                .setPort(redisConfig.getInteger("port"));
        redisFClient = RedisFClient.create(vertx, redisOptions);
    }

    @Override
    public Future<String> adMatcher(JsonObject pullData) {
        Future<String> future = Future.future();
        final String pid = pullData.getString("pid");
        adContext.put("pid", pid);
        redisFClient.get(createKey(pid))
                .compose((dcp) -> checkDcp(dcp, pid))
                .compose(checked -> redisFClient.get(createKey(pid)))
                .compose((dcp) -> redisFClient.hget(DEVICE_INFO_KEY, pid))
                .compose(this::setDeviceType)
                .compose(seted -> redisFClient.llen(READY_AD_KEY))
                .compose((len) -> redisFClient.lrange(READY_AD_KEY, len))
                .compose(this::adMather).setHandler(ar -> {
            if (ar.succeeded()) {
                future.complete(ar.result().encode());
                logger.info("Ad match success");
            } else {
                future.fail(ar.cause());
                logger.error("Ad match fail,case:" + ar.cause());
            }

        });
        return future;
    }

    private Future<Void> checkDcp(String dcp, String pid) {
        return Future.future(f -> {
            if (dcp == null) {
                redisFClient.set(createKey(pid), new JsonObject().put("pid", pid).encode()).setHandler(ar -> {
                    if (ar.succeeded()) {
                        adContext.put("dcp", new JsonObject().put("pid", pid));
                        f.complete();
                    } else {
                        f.fail(ar.cause());
                    }
                });
            } else {
                adContext.put("dcp", new JsonObject(dcp));
                f.complete();
            }
        });
    }

    private Future<Void> setDeviceType(String info) {
        return Future.future(f -> {
            adContext.put("deviceType", new JsonObject(info).getString("deviceType"));
            f.complete();
        });
    }

    private Future<JsonArray> adMather(JsonArray ads) {
        Future<JsonArray> future = Future.future();
        JsonArray adCanPush = new JsonArray();
        JsonObject dpcJson = adContext.getJsonObject("dcp");
        int index = -1;
        // 没有匹配过广告
        Long idx = dpcJson.getLong("idx");
        if (idx == null) {
            index = 0;
        } else {
            for (int i = (ads.size() - 1); i >= 0; i--) {
                JsonObject tmp = new JsonObject(ads.getString(i));
                // 广告都匹配过了
                if (tmp.getLong("idx").compareTo(dpcJson.getLong("idx")) > 0) {
                    index = i;
                } else {
                    break;
                }
            }
        }
        // Begin match
        if (index >= 0) {
            for (int i = index; i < ads.size(); i++) {
                JsonObject ad = new JsonObject(ads.getString(i));
                if (ad.getString("type").equals(adContext.getString("deviceType"))) {
                    adCanPush.add(ad);
                }
                dpcJson.put("idx", ad.getLong("idx"));
            }
            // Update cache
            redisFClient.set(createKey(adContext.getString("pid")), dpcJson.encode()).setHandler(ar -> {
                if (ar.succeeded()) {
                    logger.info("Update device push context cache success");
                    future.complete(adCanPush);
                } else {
                    logger.info("Update device push context cache fail,case:" + ar.cause());
                    future.fail(ar.cause());
                }
            });
        } else {
            future.complete(adCanPush);
        }
        return future;
    }
    private String createKey(String pid) {
        return "DEVICE_PUSH_CONTEXT." + pid;
    }
}
