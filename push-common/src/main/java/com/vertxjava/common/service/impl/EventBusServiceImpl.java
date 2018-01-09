package com.vertxjava.common.service.impl;

import com.vertxjava.common.service.EventBusService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-08 14:49
 **/
public class EventBusServiceImpl implements EventBusService {

    private Vertx vertx;

    public EventBusServiceImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void publishEventbusLog(String app, String type, String message) {
        vertx.eventBus().publish("monitor.log",
                new JsonObject().put("type", type)
                        .put("message", message)
                        .put("app", app)
                        .put("date", LocalDateTime.now().toString()).encode());
    }
}
