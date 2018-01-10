package com.vertxjava.pull.handler;

import com.vertxjava.pull.handler.impl.DeviceOnlineHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.ServiceDiscovery;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-09 18:21
 **/
public interface DeviceOnlineHandler extends Handler<RoutingContext> {
    static DeviceOnlineHandler create(Vertx vertx,JsonObject config) {
        return new DeviceOnlineHandlerImpl(vertx,config);
    }
}
