package com.vertxjava.device.business.handler;

import com.vertxjava.device.business.handler.impl.DeviceInfoReportHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-02 16:53
 **/
public interface DeviceInfoReportHandler extends Handler<Message<JsonObject>> {
    static DeviceInfoReportHandler create(Vertx vertx, JsonObject redisConfig, JsonObject mongodbConfig) {
        return new DeviceInfoReportHandlerImpl(vertx, redisConfig, mongodbConfig);
    }
}
