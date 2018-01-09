package com.vertxjava.device.business.handler.impl;

import com.vertxjava.device.business.handler.DeviceInfoReportHandler;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-02 16:54
 **/
public class DeviceInfoReportHandlerImpl implements DeviceInfoReportHandler {

    private static final String DEVICE_INFO_KEY = "DEVICE_INFO";
    private Logger logger = LoggerFactory.getLogger(DeviceInfoReportHandlerImpl.class);

    private MongoClient mongoClient;
    private RedisClient redisClient;

    public DeviceInfoReportHandlerImpl(Vertx vertx, JsonObject redisConfig, JsonObject mongoConfig) {
        RedisOptions redisOptions = new RedisOptions()
                .setHost(redisConfig.getString("host"))
                .setPort(redisConfig.getInteger("port"));
        redisClient = RedisClient.create(vertx, redisOptions);
        mongoClient = MongoClient.createShared(vertx, mongoConfig);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject data = message.body();
        Future.<String>future(future -> mongoClient.save("device_info", data, future.completer()))
                .compose(saved -> Future.<Long>future(future -> redisClient.hset(DEVICE_INFO_KEY, data.getString("_id"), data.encode(), future.completer()))).setHandler(ar -> {
            if (ar.succeeded()) {
                logger.info("Save device info success");
            } else {
                logger.error("Save device info fail,case:" + ar.cause());
            }
        });

    }
}
