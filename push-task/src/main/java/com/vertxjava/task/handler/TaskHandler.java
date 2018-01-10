package com.vertxjava.task.handler;

import com.vertxjava.task.handler.impl.TaskHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-08 15:15
 **/
public interface TaskHandler extends Handler<RoutingContext> {
    static TaskHandler create(Vertx vertx, JsonObject config) {
        return new TaskHandlerImpl(vertx, config);
    }
}
