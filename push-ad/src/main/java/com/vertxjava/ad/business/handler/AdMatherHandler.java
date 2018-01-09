package com.vertxjava.ad.business.handler;

import com.vertxjava.ad.business.handler.impl.AdMatherHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public interface AdMatherHandler extends Handler<Message<JsonObject>> {
    static AdMatherHandler create(Vertx vertx, JsonObject redisConfig) {
        return new AdMatherHandlerImpl(vertx, redisConfig);
    }
}
