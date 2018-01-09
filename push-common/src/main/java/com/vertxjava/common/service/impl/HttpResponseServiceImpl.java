package com.vertxjava.common.service.impl;

import com.vertxjava.common.service.HttpResponseService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * The implementation class of HttpResponseService.
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-05 14:20
 **/
public class HttpResponseServiceImpl implements HttpResponseService {

    @Override
    public void notFound(RoutingContext context) {
        context.response().setStatusCode(404)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("error", "not_found").encodePrettily());
    }

    @Override
    public void serverError(RoutingContext context, Object message) {
        String errorMessage = "";
        if (message instanceof String || message instanceof JsonObject) {
            errorMessage = message.toString();
        } else if (message instanceof Throwable) {
            errorMessage = ((Throwable) message).getMessage();
        }
        context.response().setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("error", "server_error").put("message", errorMessage).encodePrettily());
    }

    @Override
    public void success(RoutingContext context) {
        context.response().end();
    }

    @Override
    public void success(RoutingContext context, Object data) {
        String result = "";
        if (data instanceof JsonArray){
            result = ((JsonArray) data).encodePrettily();
        }
        context.response().end(result);
    }
}
