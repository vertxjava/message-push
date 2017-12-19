package com.vertxjava.pull.service.impl;

import com.vertxjava.pull.service.PullService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;



/**
 * @author Jack
 * @create 2017-12-12 16:35
 **/
public class PullServiceImpl implements PullService {

    // redis client
    private RedisClient redisClient;
    // 订阅的key
    private static final String SUBSCRIBE_KEY = "SUBSCRIBE_KEY_";

    public PullServiceImpl(RedisClient redisClient){
        this.redisClient = redisClient;
    }

    @Override
    public PullService pull( Handler<AsyncResult<JsonObject>> resultHandler) {
        // 获取设备上线数据
        JsonObject data = new JsonObject();
        // 根据设备type获取是否有广告信息
        // 将设备信息存入redis中
        redisClient.set(SUBSCRIBE_KEY,data.getString("_id"),ar -> {
            if (ar.succeeded()){

            }else{

            }
        });
        resultHandler.handle(Future.succeededFuture(new JsonObject().put("name", "Jack")));
        return this;
    }
}
