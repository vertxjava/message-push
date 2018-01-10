package com.vertxjava.redis;

import com.vertxjava.redis.impl.RedisFClientImpl;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * Provide access to redis, and use Future to wrap the results
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-09 11:28
 **/
public interface RedisFClient {

    static RedisFClient create(Vertx vertx, JsonObject config) {
        return new RedisFClientImpl(vertx, config);
    }

    /**
     * Close the client
     *
     * @return Using Future to get the return value
     */
    Future<Void> close();

    /**
     * Set the string value of a key
     *
     * @param key   Key of which value to set
     * @param value New value for the key
     * @return Using Future to get the return value
     */
    Future<Void> set(String key, String value);

    /**
     * Set the string value of a hash field
     *
     * @param key   Key string
     * @param field Field name
     * @param value New value
     * @return Using Future to get the return value
     */
    Future<Long> hset(String key, String field, String value);

    Future<String> hget(String key, String field);

    Future<Long> llen(String key);

    Future<JsonArray> lrange(String key, long to);

    Future<Long> del(String key);

    Future<Long> lpush(String key, String value);

    Future<Long> lpushMany(String key, List<String> value);

    /**
     * Get the value of a key
     *
     * @param key Key string
     * @return Using Future to get the return value
     */
    Future<String> get(String key);

}
