package com.vertxjava.task.handler.impl;

import com.vertxjava.common.service.HttpResponseService;
import com.vertxjava.postgresql.SQLFClient;
import com.vertxjava.task.handler.TaskHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-08 15:16
 **/
public class TaskHandlerImpl implements TaskHandler {

    private static final String SQL_ADD = "insert into ad (id,title,type) values (?,?,?)";
    private static final String SQL_LIST = "select * from ad";
    private static final String SQL_PUB = "update ad set state = 1 where id = ?";
    private SQLFClient sqlfClient;
    private HttpResponseService service;

    public TaskHandlerImpl(Vertx vertx, JsonObject config) {
        sqlfClient = SQLFClient.create(vertx, config.getJsonObject("postgresqlConfig"));
        service = HttpResponseService.create();
    }

    @Override
    public void handle(RoutingContext context) {
        String action = context.request().uri();
        if (action.contains("?")) {
            action = action.substring(0, action.indexOf("?"));
        }
        if (action.equals("/add")) {
            JsonObject params = context.getBodyAsJson();
            sqlfClient.insert(SQL_ADD, new JsonArray()
                    .add(UUID.randomUUID().toString())
                    .add(params.getString("title"))
                    .add(params.getString("type"))).setHandler(ar -> {
                if (ar.succeeded()) {
                    service.success(context);
                } else {
                    service.serverError(context, ar.cause());
                }
            });
        } else if (action.equals("/list")) {
            sqlfClient.query(SQL_LIST).setHandler(ar -> {
                if (ar.succeeded()) {
                    JsonArray result = new JsonArray();
                    if (ar.result().isPresent()) {
                        result = ar.result().get();
                    }
                    service.success(context, result);
                } else {
                    service.serverError(context, ar.cause());
                }
            });
        } else if (action.equals("/pub")) {
            String id = context.request().getParam("id");
            sqlfClient.update(SQL_PUB, new JsonArray().add(id)).setHandler(ar -> {
                if (ar.succeeded()) {
                    service.success(context);
                } else {
                    service.serverError(context, ar.cause());
                }
            });
        }
    }
}
