package com.vertxjava.task.service.impl;

import com.vertxjava.common.dataaccess.DataAccessWrapper;
import com.vertxjava.task.service.TaskService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * @author Jack
 * @create 2017-12-18 16:59
 **/
public class TaskServiceImpl extends DataAccessWrapper implements TaskService {

    private static final String SQL_INSERT = "insert into ...";

    public TaskServiceImpl(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    @Override
    public Future<Void> addTask(JsonObject data) {
        return insert(SQL_INSERT);
    }

    @Override
    public Future<Void> sendTask(JsonObject data) {
        Future<Void> future = Future.future();

        return future;
    }

    @Override
    public Future<Void> syncTask() {
        return null;
    }
}
