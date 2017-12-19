package com.vertxjava.task.verticle;

import com.vertxjava.common.verticle.HttpVerticle;
import com.vertxjava.task.service.TaskService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Jack
 * @create 2017-12-18 16:47
 **/
public class TaskVerticle extends HttpVerticle {

    // 服务名称
    private static final String SERVER_NAME = "task";
    // 默认host
    private static final String DEFAULT_HOST = "localhost";
    // 默认port
    private static final Integer DEFAULT_PORT = 8004;
    // task service
    private TaskService taskService;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        // get taskService instance
        taskService = TaskService.create(vertx,config());

        final Router router = Router.router(vertx);
        router.post("/add").handler(this::addTask);
        // host
        String host = config().getString("host", DEFAULT_HOST);
        // port
        Integer port = config().getInteger("port", DEFAULT_PORT);

        // 创建http服务并发布httpPoint
        createHttpServer(router, host, port)
                .compose(created -> publishHttpEndpoint(SERVER_NAME, host, port))
                .setHandler(ar -> {
            if (ar.succeeded()) {
                startFuture.complete();
            } else {
                startFuture.fail(ar.cause());
            }
        });

    }

    private void addTask(RoutingContext context){
        JsonObject params = context.getBodyAsJson();

    }
}
