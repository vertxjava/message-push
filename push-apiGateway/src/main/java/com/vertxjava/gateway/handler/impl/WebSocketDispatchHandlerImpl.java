package com.vertxjava.gateway.handler.impl;

import com.vertxjava.common.service.EventBusService;
import com.vertxjava.gateway.handler.WebSocketDispatchHandler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.types.HttpEndpoint;

import java.util.Collections;
import java.util.List;

/**
 * The implementation class of WebSocketDispatchHandler.
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-03 17:26
 **/
public class WebSocketDispatchHandlerImpl implements WebSocketDispatchHandler {

    // log
    private Logger logger = LoggerFactory.getLogger(WebSocketDispatchHandlerImpl.class);
    // The vertx instance
    private Vertx vertx;
    // The service discovery instance
    private ServiceDiscovery discovery;
    // The eventBusService instance
    private EventBusService eventBusService;

    public WebSocketDispatchHandlerImpl(Vertx vertx, ServiceDiscovery discovery) {
        this.vertx = vertx;
        this.discovery = discovery;
        eventBusService = EventBusService.create(vertx);
    }

    @Override
    public void handle(ServerWebSocket socket) {
        String path = socket.path();
        // The "/socket/" length
        if (path.length() <= 8) {
            socket.reject();
            return;
        }
        String serverName = path.substring(8).split("/")[0];
        String apiName = path.substring(8).split("/")[1];
        socket.handler(buffer -> {
            discovery.getRecords(record -> record.getName().equals(serverName) && record.getType().equals(HttpEndpoint.TYPE), ar -> {
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
                        String newPath = path.substring(5 + serverName.length());

                        ServiceReference reference = discovery.getReference(record);
                        HttpClient client = reference.getAs(HttpClient.class);
                        HttpClientRequest hcr = client.request(apiName.equals("deviceOnline") ? HttpMethod.GET : HttpMethod.POST, newPath, response -> {
                            response.bodyHandler(body -> {
                                eventBusService.publishEventbusLog("api-gateway", "INFO", "Http request is successful,the data is :" + body.toString());
                                socket.write(body);
                                // Close resource
                                reference.release();
                            });
                        });
                        hcr.end(buffer);
                    } else {
                        socket.close();
                    }
                } else {
                    logger.error("Get services discovery records fail,case:" + ar.cause());
                    eventBusService.publishEventbusLog("api-gateway", "ERROR", "Get services discovery records,case:" + ar.cause());
                    socket.close();
                }
            });
        });
    }

}
