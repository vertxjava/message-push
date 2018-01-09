package com.vertxjava.gateway.handler;

import com.vertxjava.common.handler.WebSocketHandler;
import com.vertxjava.gateway.handler.impl.WebSocketDispatchHandlerImpl;
import io.vertx.core.Vertx;
import io.vertx.servicediscovery.ServiceDiscovery;

/**
 * The web socket request is forwarded,
 * processed by the related application and returned to the processing result.
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-03 17:26
 **/
public interface WebSocketDispatchHandler extends WebSocketHandler {
    /**
     * Create WebSocketDispatchHandler instance.
     *
     * @param vertx     The vertx instance
     * @param discovery The service discovery instance
     * @return WebSocketDispatchHandler instance
     */
    static WebSocketDispatchHandler create(Vertx vertx, ServiceDiscovery discovery) {
        return new WebSocketDispatchHandlerImpl(vertx, discovery);
    }
}
