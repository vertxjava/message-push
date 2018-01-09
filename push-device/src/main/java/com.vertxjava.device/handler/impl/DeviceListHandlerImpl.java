package com.vertxjava.device.business.handler.impl;

import com.vertxjava.common.service.HttpResponseService;
import com.vertxjava.device.business.handler.DeviceListHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-03 10:54
 **/
public class DeviceListHandlerImpl implements DeviceListHandler {

    private static final String DEVICE_INFO_KEY = "DEVICE_INFO";
    private RedisClient redisClient;
    private HttpResponseService httpResponseService;

    public DeviceListHandlerImpl(Vertx vertx, JsonObject redisConfig) {
        RedisOptions redisOptions = new RedisOptions()
                .setHost(redisConfig.getString("host"))
                .setPort(redisConfig.getInteger("port"));
        redisClient = RedisClient.create(vertx, redisOptions);
        httpResponseService = HttpResponseService.create();
    }

    @Override
    public void handle(RoutingContext context) {
        redisClient.hgetall(DEVICE_INFO_KEY, ar -> {
            if (ar.succeeded()) {
                JsonArray result = new JsonArray();
                ar.result().iterator().forEachRemaining(r -> {
                    result.add(new JsonObject(r.getValue().toString()));
                });
                context.response().end(result.encodePrettily());
            } else {
                httpResponseService.serverError(context,ar.cause());
            }
        });
    }
}
