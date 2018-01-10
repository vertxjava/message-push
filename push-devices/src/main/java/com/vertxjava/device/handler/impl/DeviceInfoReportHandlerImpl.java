package com.vertxjava.device.handler.impl;

import com.vertxjava.device.handler.DeviceInfoReportHandler;
import com.vertxjava.mongodb.MongoFClient;
import com.vertxjava.redis.RedisFClient;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-02 16:54
 **/
public class DeviceInfoReportHandlerImpl implements DeviceInfoReportHandler {

    private static final String DEVICE_INFO_KEY = "DEVICE_INFO";
    private Logger logger = LoggerFactory.getLogger(DeviceInfoReportHandlerImpl.class);

    private MongoFClient mongoFClient;
    private RedisFClient redisFClient;

    public DeviceInfoReportHandlerImpl(Vertx vertx, JsonObject config) {
        redisFClient = RedisFClient.create(vertx, config.getJsonObject("redisConfig"));
        mongoFClient = MongoFClient.create(vertx, config.getJsonObject("mongodbConfig"));
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject data = message.body();
        mongoFClient.save("device_info", data)
                .compose(saved -> redisFClient.hset(DEVICE_INFO_KEY, data.getString("_id"), data.encode()))
                .setHandler(ar -> {
                    if (ar.succeeded()) {
                        logger.info("Save device info success");
                    } else {
                        logger.error("Save device info fail,case:" + ar.cause());
                    }
                });
    }
}
