package com.vertxjava.ad.business.handler.impl;

import com.vertxjava.ad.business.handler.AdMatherHandler;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

/**
 * @author Jack
 * @create 2017-12-28 15:36
 **/
public class AdMatherHandlerImpl implements AdMatherHandler {
    // ready ad key
    private static final String READY_AD_KEY = "READY_AD";
    private static final String DEVICE_INFO_KEY = "DEVICE_INFO";
    private Logger logger = LoggerFactory.getLogger(AdMatherHandlerImpl.class);
    private RedisClient redisClient;
    private JsonObject context = new JsonObject();

    public AdMatherHandlerImpl(Vertx vertx, JsonObject redisConfig) {
        RedisOptions redisOptions = new RedisOptions()
                .setHost(redisConfig.getString("host"))
                .setPort(redisConfig.getInteger("port"));
        redisClient = RedisClient.create(vertx, redisOptions);
    }

    @Override
    public void handle(Message<JsonObject> message) {

        JsonObject pullData = message.body();
        String pid = pullData.getString("pid");
        Future.<String>future(f -> {
            redisClient.get(createKey(pid), f);
        }).compose((dpc) ->
                Future.<Void>future(f -> {
                    if (dpc == null) {
                        redisClient.set(createKey(pid), new JsonObject().put("pid", pid).encode(), f.completer());
                    } else {
                        f.complete();
                    }
                })
        ).compose(checked ->
                Future.<String>future(f -> {
                    redisClient.get(createKey(pid), f);
                })
        ).compose((dpc) ->
                matchAds(dpc, pid)
        ).setHandler(ar -> {
            if (ar.succeeded()) {
                message.reply(ar.result().encode());
                logger.info("Ad match success");
            } else {
                logger.error("Ad match fail,case:" + ar.cause());
            }
        });
    }

    private Future<Void> setDeviceType(String deviceType) {
        return Future.future(future -> {
            context.put("deviceType", deviceType);
            future.complete();
        });
    }

    private Future<JsonArray> matchAds(String dpc, String pid) {
        Future<JsonArray> future = Future.future();
        JsonArray adCanPush = new JsonArray();
        // Get ads which can push
        Future.<String>future(f -> redisClient.hget(DEVICE_INFO_KEY, pid, f.completer()))
                .compose(info -> setDeviceType(new JsonObject(info).getString("deviceType")))
                .compose(r -> Future.<Long>future(f -> redisClient.llen(READY_AD_KEY, f.completer())))
                .compose(len -> Future.<JsonArray>future(f -> redisClient.lrange(READY_AD_KEY, 0, len, f.completer())))
                .setHandler(ar -> {
                    if (ar.succeeded()) {
                        JsonArray ads = ar.result();
                        JsonObject dpcJson = new JsonObject(dpc);
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
                                if (ad.getString("type").equals(context.getString("deviceType"))) {
                                    adCanPush.add(ad);
                                }
                                dpcJson.put("idx", ad.getLong("idx"));
                            }
                            // Update cache
                            redisClient.set(createKey(pid), dpcJson.encode(), r -> {
                                if (r.succeeded()) {
                                    logger.info("Update device push context cache success");
                                    future.complete(adCanPush);
                                } else {
                                    logger.info("Update device push context cache fail,case:" + r.cause());
                                    future.fail(r.cause());
                                }
                            });
                        } else {
                            future.complete(adCanPush);
                        }
                    } else {
                        logger.error("Get ads fail,case:" + ar.cause());
                        future.fail(ar.cause());
                    }
                });
        return future;
    }

    private String createKey(String pid) {
        return "DEVICE_PUSH_CONTEXT." + pid;
    }
}

