package com.vertxjava.redis.impl;

import com.vertxjava.redis.RedisFClient;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

import java.util.List;

/**
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-09 11:34
 **/
public class RedisFClientImpl implements RedisFClient {

    private RedisClient client;

    public RedisFClientImpl(Vertx vertx, JsonObject config) {
        RedisOptions redisOptions = new RedisOptions()
                .setHost(config.getString("host"))
                .setPort(config.getInteger("port"));
        client = RedisClient.create(vertx, redisOptions);
    }

    @Override
    public Future<Void> close() {
        return Future.future(f -> client.close(f.completer()));
    }

    @Override
    public Future<Void> set(String key, String value) {
        return Future.future(f -> client.set(key, value, f.completer()));
    }

    @Override
    public Future<Long> hset(String key, String field, String value) {
        return Future.future(f -> client.hset(key, field, value, f.completer()));
    }

    @Override
    public Future<String> hget(String key, String field) {
        return Future.future(f -> client.hget(key, field, f.completer()));
    }

    @Override
    public Future<Long> llen(String key) {
        return Future.future(f -> client.llen(key, f.completer()));
    }

    @Override
    public Future<JsonArray> lrange(String key, long to) {
        return Future.future(f -> client.lrange(key, 0, to, f.completer()));
    }

    @Override
    public Future<Long> del(String key) {
        return Future.future(f -> client.del(key, f.completer()));
    }

    @Override
    public Future<Long> lpush(String key, String value) {
        return Future.future(f -> client.lpush(key, value, f.completer()));
    }

    @Override
    public Future<Long> lpushMany(String key, List<String> value) {
        return Future.future(f -> client.lpushMany(key, value, f.completer()));
    }


    @Override
    public Future<String> get(String key) {
        return Future.future(f -> client.get(key, f.completer()));
    }
}
