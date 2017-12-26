package com.vertxjava.report.handler.impl;

import com.vertxjava.report.handler.FeedbackHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Jack
 * @create 2017-12-26 14:49
 **/
public class FeedbackHandlerImpl implements FeedbackHandler {

    private Vertx vertx;

    public FeedbackHandlerImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void handle(RoutingContext context) {
        JsonObject data = context.getBodyAsJson();
        vertx.eventBus().publish("feedback",data);
        context.response().end();
    }
}
