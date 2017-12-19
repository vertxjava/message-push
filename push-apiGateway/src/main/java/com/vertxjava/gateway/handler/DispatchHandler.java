package com.vertxjava.gateway.handler;

import com.vertxjava.gateway.handler.impl.DispatchHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.ServiceDiscovery;

/**
 * @author Jack
 * @create 2017-12-19 16:37
 **/
public interface DispatchHandler extends Handler<RoutingContext> {
    static DispatchHandler create(ServiceDiscovery discovery){
        return new DispatchHandlerImpl(discovery);
    }
}
