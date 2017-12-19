package com.vertxjava.gateway.handler.impl;

import com.vertxjava.gateway.handler.WebSocketHandler;
import com.vertxjava.pull.service.PullService;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;

/**
 * @author Jack
 * @create 2017-12-19 16:57
 **/
public class WebSocketHandlerImpl implements WebSocketHandler {

    private ServiceDiscovery discovery;

    public WebSocketHandlerImpl(ServiceDiscovery discovery){
        this.discovery = discovery;
    }

    @Override
    public void handle(ServerWebSocket socket) {
        String path = socket.path();
        if (!"/socket".equals(path)) {
            // 拒绝服务
            socket.reject();
            return;
        }

        socket.handler(buffer -> {
            EventBusService.getProxy(discovery, PullService.class, ar -> {
                if (ar.succeeded()) {
                    PullService service = ar.result();
                    service.pull(r -> {
                        if (r.succeeded()) {
                            Buffer buffer1 = Buffer.buffer().appendString(r.result().encodePrettily());
                            socket.writeBinaryMessage(buffer1);
                            socket.close();
                        } else {
                            socket.close();
                        }
                        ServiceDiscovery.releaseServiceObject(discovery, service);
                    });
                } else {
                    socket.close();
                }
            });
        });


    }

}
