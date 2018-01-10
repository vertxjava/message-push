package com.vertxjava.device.handler;

import com.vertxjava.device.handler.impl.DeviceInfoReportHandlerImpl;
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
    static DeviceInfoReportHandler create(Vertx vertx, JsonObject config) {
        return new DeviceInfoReportHandlerImpl(vertx, config);
    }
}
