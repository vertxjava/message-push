package com.vertxjava.report.handler.impl;

import com.vertxjava.report.handler.DeviceInfoEventBusHandler;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.redis.RedisClient;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.MongoDataSource;
import io.vertx.servicediscovery.types.RedisDataSource;

/**
 * @author Jack
 * @create 2017-12-26 15:04
 **/
public class DeviceInfoEventBusHandlerImpl implements DeviceInfoEventBusHandler {

    private Logger log = LoggerFactory.getLogger(DeviceInfoEventBusHandlerImpl.class);
    private ServiceDiscovery discovery;

    public DeviceInfoEventBusHandlerImpl(ServiceDiscovery discovery) {
        this.discovery = discovery;
    }

    @Override
    public void handle(Message<Object> event) {
        JsonObject data = (JsonObject) event.body();
        saveToMongodb(data).compose(saved -> saveToRedis(data)).setHandler(ar -> {
            if (ar.succeeded()){
                log.info("Data save success");
            }else{
                log.error("Data save fail,case:"+ar.cause());
            }
        });
    }


    private Future<Void> saveToMongodb(JsonObject data) {
        Future<Void> future = Future.future();
        // 将device_info信息存入mongodb
        MongoDataSource.getMongoClient(discovery, new JsonObject().put("name", "mongodbServer"), ar -> {
            if (ar.succeeded()) {
                MongoClient client = ar.result();
                client.save("device_info", data, res -> {
                    if (res.succeeded()) {
                        future.complete();
                        log.info("Data save to mongodb success,id = " + res.result());
                    } else {
                        future.fail(res.cause());
                        log.error("Data save to mongodb fail,case:" + res.cause());
                    }
                });
            } else {
                future.fail(ar.cause());
                log.error("Get mongoClient from service disvovery fail,case:" + ar.cause());
            }
        });
        return future;
    }

    private Future<Void> saveToRedis(JsonObject data) {
        Future<Void> future = Future.future();
        RedisDataSource.getRedisClient(discovery, new JsonObject().put("name", "redisServer"), ar -> {
            if (ar.succeeded()) {
                RedisClient client = ar.result();
                client.set(data.getString("_id"), data.encode(), res -> {
                    if (res.succeeded()) {
                        future.complete();
                        log.info("Data save to redis success");
                    } else {
                        future.fail(res.cause());
                        log.error("Data save to redis fail,case:"+res.cause());
                    }
                });
            } else {
                future.fail(ar.cause());
                log.error("Get RedisClient from service discovery fail,case:" + ar.cause());
            }
        });
        return future;
    }
}
