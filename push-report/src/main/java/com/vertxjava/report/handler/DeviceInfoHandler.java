package com.vertxjava.report.handler;

import com.vertxjava.report.handler.impl.DeviceInfoHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Jack
 * @create 2017-12-26 14:28
 **/
public interface DeviceInfoHandler extends Handler<RoutingContext> {
    static DeviceInfoHandler create(Vertx vertx) {
        return new DeviceInfoHandlerImpl(vertx);
    }
}
