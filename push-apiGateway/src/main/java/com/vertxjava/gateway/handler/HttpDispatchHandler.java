package com.vertxjava.gateway.handler;

import com.vertxjava.gateway.handler.impl.HttpDispatchHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.ServiceDiscovery;

/**
 * The HTTP request is forwarded,
 * processed by the related application and returned to the processing result.
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-03 17:26
 **/
public interface HttpDispatchHandler extends Handler<RoutingContext> {
    /**
     * Create a HttpDispatchHandler instance by discovery
     *
     * @param discovery The service discovery instance
     * @return The HttpDispatchHandler instance
     */
    static HttpDispatchHandler create(ServiceDiscovery discovery, Vertx vertx) {
        return new HttpDispatchHandlerImpl(discovery,vertx);
    }
}
