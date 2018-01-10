package com.vertxjava.ad.business.handler.impl;

import com.vertxjava.ad.business.handler.ADSyncHandler;
import com.vertxjava.postgresql.SQLFClient;
import com.vertxjava.redis.RedisFClient;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jack
 * @create 2017-12-28 10:15
 **/
public class ADSyncHandlerImpl implements ADSyncHandler {

    private static final String READY_AD_KEY = "READY_AD";
    private static final String SQL_FIND_AD = "select * from ad where state = 1 order by idx desc";
    private Logger logger = LoggerFactory.getLogger(ADSyncHandlerImpl.class);
    private RedisFClient redisFClient;
    private SQLFClient sqlfClient;

    public ADSyncHandlerImpl(Vertx vertx, JsonObject config) {
        redisFClient = RedisFClient.create(vertx, config.getJsonObject("redisConfig"));
        sqlfClient = SQLFClient.create(vertx, config.getJsonObject("postgresqlConfig"));
    }

    @Override
    public void handle(Long event) {
        // 获取可以发送的广告
        sqlfClient.query(SQL_FIND_AD).setHandler(ar -> {
            if (ar.succeeded()) {
                if (ar.result().isPresent()) {
                    JsonArray ads = ar.result().get();
                    redisFClient.del(READY_AD_KEY).compose(deled -> {
                        List<String> adList = new ArrayList<>();
                        ads.forEach(ad -> adList.add(((JsonObject) ad).encode()));
                        return redisFClient.lpushMany(READY_AD_KEY, adList);
                    }).setHandler(r -> {
                        if (r.succeeded()) {
                            logger.info("Ad cache success");
                        } else {
                            logger.error("Ad cache fail,case:" + r.cause());
                        }
                    });
                }
            } else {
                logger.error("Ad cache fail,case:" + ar.cause());
            }
        });
    }
}
