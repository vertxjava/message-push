package com.vertxjava.gateway.handler.impl;

import com.vertxjava.gateway.handler.WebSocketHandler;
import com.vertxjava.servicediscovery.types.WebSocketEndpoint;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;

import java.util.Collections;
import java.util.List;

/**
 * @author Jack
 * @create 2017-12-19 16:57
 **/
public class WebSocketHandlerImpl implements WebSocketHandler {

    private ServiceDiscovery discovery;
    private Logger log = LoggerFactory.getLogger(WebSocketHandlerImpl.class);

    public WebSocketHandlerImpl(ServiceDiscovery discovery) {
        this.discovery = discovery;
    }

    @Override
    public void handle(ServerWebSocket socket) {
        String path = socket.path();
        if (path.length() <= 8) {
            socket.reject();
            return;
        }
        String apiName = path.substring(8).split("/")[0];
        socket.handler(buffer -> {
            discovery.getRecords(record -> record.getName().equals(apiName) && record.getType().equals(WebSocketEndpoint.TYPE), ar -> {
                if (ar.succeeded()) {
                    List<Record> list = ar.result();
                    Record record = null;
                    if (list != null && !list.isEmpty()) {
                        if (list.size() == 1) {
                            record = list.get(0);
                        } else {
                            Collections.shuffle(list);
                            record = list.get(0);
                        }
                        String newPath = path.substring(8 + apiName.length());
                        ServiceReference reference = discovery.getReference(record);
                        HttpClient client = reference.getAs(HttpClient.class);
                        client.websocket(newPath,websocket -> {
                            websocket.write(buffer);
                            websocket.handler(socket::write);
                        });
                    } else {
                        socket.reject();
                    }
                } else {
                    log.error("get services discovery records fail,case:" + ar.cause());
                    socket.reject();
                }
            });
        });
    }

}
