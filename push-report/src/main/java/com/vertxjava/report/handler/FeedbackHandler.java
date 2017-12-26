package com.vertxjava.report.handler;

import com.vertxjava.report.handler.impl.FeedbackHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public interface FeedbackHandler extends Handler<RoutingContext> {
    static FeedbackHandler create(Vertx vertx){
        return new FeedbackHandlerImpl(vertx);
    }
}
