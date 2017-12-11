package com.vertxjava.common;


import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;

import java.util.HashSet;
import java.util.Set;

public class HttpVerticle extends BaseVerticle {

    // 创建http服务
    protected Future<Void> createHttpServer(Router router, String host, int port) {
        Future<HttpServer> httpServerFuture = Future.future();
        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(port, host, httpServerFuture.completer());
        return httpServerFuture.map(r -> null);
    }

    // 启用CORS支持
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

    // 未找到资源
    protected void notFound(RoutingContext context) {
        context.response().setStatusCode(404)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("message", "not_found").encodePrettily());
    }

    // 服务异常，500错误
    protected void serverError(RoutingContext context){
        context.response().setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("message", "server_error").encodePrettily());
    }

}

