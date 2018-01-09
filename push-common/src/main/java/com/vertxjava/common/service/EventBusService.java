package com.vertxjava.common.service;

import com.vertxjava.common.service.impl.EventBusServiceImpl;
import io.vertx.core.Vertx;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-08 14:47
 **/
public interface EventBusService {

    static EventBusService create(Vertx vertx) {
        return new EventBusServiceImpl(vertx);
    }

    void publishEventbusLog(String app, String type, String message);
}
