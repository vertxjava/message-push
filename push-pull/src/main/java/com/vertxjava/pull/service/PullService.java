package com.vertxjava.pull.service;

import com.vertxjava.pull.service.impl.PullServiceImpl;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * @author Jack
 * @create 2017-12-12 16:31
 **/
@ProxyGen
public interface PullService {
    // 服务名称
    String SERVICE_NAME = "pull-eb-service";
    // 服务地址
    String SERVICE_ADDRESS = "service_pull";

    static PullService create(){
        return new PullServiceImpl();
    }

    /**
     * 拉取消息
     * @param resultHandler 异步处理结果
     * @return PullService
     */
    @Fluent
    PullService pull(Handler<AsyncResult<JsonObject>> resultHandler);

}
