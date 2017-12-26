package com.vertxjava.report.handler.impl;

import com.vertxjava.report.handler.DeviceInfoHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Jack
 * @create 2017-12-26 14:30
 **/
public class DeviceInfoHandlerImpl implements DeviceInfoHandler {

    private Vertx vertx;

    public DeviceInfoHandlerImpl(Vertx vertx){
        this.vertx = vertx;
    }

    @Override
    public void handle(RoutingContext context) {
        JsonObject data = context.getBodyAsJson();
        vertx.eventBus().publish("device_info",data);
        context.response().end();
    }
}
