package com.vertxjava.task.service;

import com.vertxjava.task.service.impl.TaskServiceImpl;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public interface TaskService {

    static TaskService create(Vertx vertx, JsonObject config) {
        return new TaskServiceImpl(vertx,config);
    }

    Future<Void> addTask(JsonObject data);
    Future<Void> sendTask(JsonObject data);

    /**
     * 同步广告
     * @return Future<Void>
     */
    Future<Void> syncTask();
}
