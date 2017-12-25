package com.vertxjava.pull.service;

import com.vertxjava.pull.service.impl.PullServiceImpl;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;


/**
 * @author Jack
 * @create 2017-12-12 16:31
 **/
@ProxyGen
public interface PullService {
    // 服务名称
    String SERVICE_NAME = "pull-eb-services";
    // 服务地址
    String SERVICE_ADDRESS = "service_pull";

    static PullService create(RedisClient redisClient) {
        return new PullServiceImpl(redisClient);
    }

    /**
     * 拉取消息
     *
     * @param resultHandler 异步处理结果
     * @return PullService
     */
    @Fluent
    PullService pull(Handler<AsyncResult<JsonObject>> resultHandler);

}
