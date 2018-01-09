package com.vertxjava.common.verticle;


import com.vertxjava.common.handler.WebSocketHandler;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;

import java.util.HashSet;
import java.util.Set;

public class HttpVerticle extends BaseVerticle {

    /**
     * Create http server.
     *
     * @param router The router instance
     * @param host   The host
     * @param port   The port
     * @return Asynchronous return void
     */
    protected Future<Void> createHttpServer(Router router, String host, int port) {
        Future<HttpServer> future = Future.future();
        vertx.createHttpServer().requestHandler(router::accept)
                .listen(port, host, future.completer());
        return future.map(r -> null);
    }

    /**
     * Create webSocket server and http server.
     *
     * @param host    The host
     * @param port    The port
     * @param handler The WebSocketHandler instance
     * @return Asynchronous return void
     */
    protected Future<Void> createWebSocketServerAndHttpServer(Router router, String host, int port, WebSocketHandler handler) {
        Future<HttpServer> future = Future.future();
        vertx.createHttpServer().websocketHandler(handler).requestHandler(router::accept)
                .listen(port, host, future.completer());
        return future.map(r -> null);
    }

    /**
     * Enabling CORS support
     *
     * @param router The router instance
     */
    protected void enableCorsSupport(Router router) {
        Set<String> allowHeaders = new HashSet<>();
        allowHeaders.add("Authorization");
        allowHeaders.add("x-requested-with");
        allowHeaders.add("Access-Control-Allow-Origin");
        allowHeaders.add("origin");
        allowHeaders.add("Content-Type");
        allowHeaders.add("accept");
        Set<HttpMethod> allowMethods = new HashSet<>();
        allowMethods.add(HttpMethod.GET);
        allowMethods.add(HttpMethod.POST);
        allowMethods.add(HttpMethod.DELETE);
        allowMethods.add(HttpMethod.PATCH);
        router.route().handler(CorsHandler.create("*")
                .allowedHeaders(allowHeaders)
                .allowedMethods(allowMethods));
    }

}

