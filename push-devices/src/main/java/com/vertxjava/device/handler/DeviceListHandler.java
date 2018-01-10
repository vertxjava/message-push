package com.vertxjava.device.handler;

import com.vertxjava.device.handler.impl.DeviceListHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-03 10:53
 **/
public interface DeviceListHandler extends Handler<RoutingContext> {
    static DeviceListHandler create(Vertx vertx, JsonObject redisConfig) {
        return new DeviceListHandlerImpl(vertx, redisConfig);
    }
}
