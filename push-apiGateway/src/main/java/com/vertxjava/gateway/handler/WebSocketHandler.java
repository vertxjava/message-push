package com.vertxjava.gateway.handler;

import com.vertxjava.gateway.handler.impl.WebSocketHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.servicediscovery.ServiceDiscovery;

public interface WebSocketHandler extends Handler<ServerWebSocket> {
    static WebSocketHandler create(ServiceDiscovery discovery){
        return new WebSocketHandlerImpl(discovery);
    }
}
