package com.vertxjava.gateway.handler.impl;

import com.vertxjava.common.service.EventBusService;
import com.vertxjava.common.service.HttpResponseService;
import com.vertxjava.gateway.handler.HttpDispatchHandler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.types.HttpEndpoint;

import java.util.Collections;
import java.util.List;

/**
 * The implementation class of HttpDispatchHandler.
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-03 17:26
 **/
public class HttpDispatchHandlerImpl implements HttpDispatchHandler {
    // The log
    private Logger logger = LoggerFactory.getLogger(HttpDispatchHandlerImpl.class);
    // The service discovery instance
    private ServiceDiscovery discovery;
    // The http response service instance
    private HttpResponseService httpResponseService;
    // The eventBusService instance
    private EventBusService eventBusService;

    public HttpDispatchHandlerImpl(ServiceDiscovery discovery, Vertx vertx) {
        this.discovery = discovery;
        httpResponseService = HttpResponseService.create();
        eventBusService = EventBusService.create(vertx);
    }

    @Override
    public void handle(RoutingContext context) {
        String path = context.request().uri();
        if (path.length() <= 5) {
            httpResponseService.notFound(context);
            return;
        }
        String serverName = path.substring(5).split("/")[0];
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
                    HttpClientRequest hcr = client.request(context.request().method(), newPath, response -> {
                        response.bodyHandler(body -> {
                            eventBusService.publishEventbusLog("api-gateway", "INFO", "Http request is successful,the data is :" + body.toString());
                            // Get HttpServerResponse
                            HttpServerResponse hsr = context.response().setStatusCode(response.statusCode());
                            // Setting the response request header to the context
                            response.headers().forEach(header -> hsr.putHeader(header.getKey(), header.getValue()));
                            // Response
                            hsr.end(body);
                            // Close resource
                            reference.release();
                        });
                    });
                    // Set headers
                    context.request().headers().forEach(header -> hcr.putHeader(header.getKey(), header.getValue()));
                    // Send request
                    if (context.getBody() == null)
                        hcr.end();
                    else
                        hcr.end(context.getBody());
                } else {
                    httpResponseService.notFound(context);
                }
            } else {
                httpResponseService.serverError(context, ar.cause());
                logger.error("Get services discovery records fail,case:" + ar.cause());
                eventBusService.publishEventbusLog("api-gateway", "ERROR", "Get services discovery records fail,case :" + ar.cause());
            }
        });
    }
}
