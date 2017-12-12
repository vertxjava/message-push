package com.vertxjava.pull.service.impl;

import com.vertxjava.pull.service.PullService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * @author Jack
 * @create 2017-12-12 16:35
 **/
public class PullServiceImpl implements PullService {
    @Override
    public PullService pull(Handler<AsyncResult<JsonObject>> resultHandler) {
        resultHandler.handle(Future.succeededFuture(new JsonObject().put("name","Jack")));
        return this;
    }
}
