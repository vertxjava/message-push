package com.vertxjava.ad.business.handler.impl;

import com.vertxjava.ad.business.handler.ADSyncHandler;
import com.vertxjava.common.access.PostgresqlDataAccess;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jack
 * @create 2017-12-28 10:15
 **/
public class ADSyncHandlerImpl implements ADSyncHandler {

    private static final String READY_AD_KEY = "READY_AD";
    private Logger logger = LoggerFactory.getLogger(ADSyncHandlerImpl.class);
    private RedisClient redisClient;
    private PostgresqlDataAccess pgDataAccess;


    public ADSyncHandlerImpl(Vertx vertx, JsonObject redisConfig, JsonObject postgresqlConfig) {
        RedisOptions redisOptions = new RedisOptions()
                .setHost(redisConfig.getString("host"))
                .setPort(redisConfig.getInteger("port"));
        redisClient = RedisClient.create(vertx, redisOptions);
        SQLClient sqlClient = PostgreSQLClient.createShared(vertx, postgresqlConfig);
        pgDataAccess = PostgresqlDataAccess.create(sqlClient);
    }

    @Override
    public void handle(Long event) {
        // 获取可以发送的广告
        pgDataAccess.query("select * from ad where state = 1 order by idx desc").compose(ads -> {
            return Future.future(future -> {
                if (ads.isPresent()) {
                    Future.<Long>future(f -> redisClient.del(READY_AD_KEY, f)).compose(r -> Future.future(f1 -> {
                        List<Future> list = new ArrayList<Future>();
                        ads.get().forEach(ad -> list.add(Future.<Long>future(f2 -> redisClient.lpush(READY_AD_KEY, ((JsonObject) ad).encode(), f2))));
                        CompositeFuture.all(list).setHandler(ar -> future.completer());
                    }));
                } else {
                    future.complete();
                }
            });
        }).setHandler(ar -> {
            if (ar.succeeded()) {
                logger.info("Ad cache success");
            } else {
                logger.error("Ad cache fail,case:" + ar.cause());
            }
        });
    }
}
