package com.vertxjava.ad.business.handler;


import com.vertxjava.ad.business.handler.impl.ADSyncHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public interface ADSyncHandler extends Handler<Long> {
    static ADSyncHandler create(Vertx vertx, JsonObject config) {
        return new ADSyncHandlerImpl(vertx, config);
    }
}
