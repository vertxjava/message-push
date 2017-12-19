package com.vertxjava.pull.verticle;

import com.vertxjava.common.verticle.HttpVerticle;
import com.vertxjava.pull.service.PullService;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceBinder;

/**
 * @author Jack
 * @create 2017-12-12 11:17
 **/
public class PullVerticle extends HttpVerticle {
    // 服务名称
    private static final String SERVER_NAME = "pull";
    // 默认host
    private static final String DEFAULT_HOST = "localhost";
    // 默认port
    private static final Integer DEFAULT_PORT = 8003;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        // 创建实例
        PullService pullService = PullService.create(null);
        ServiceBinder serviceBinder = new ServiceBinder(vertx);
        serviceBinder.setAddress(PullService.SERVICE_ADDRESS);
        serviceBinder.register(PullService.class, pullService);

        final Router router = Router.router(vertx);
        router.get("/v").handler(context -> {
            System.out.println("pull-2");
            System.out.println(Thread.currentThread().getName());
            context.response().end("1.0");
        });

        String host = config().getString("host", DEFAULT_HOST);
        Integer port = config().getInteger("port", DEFAULT_PORT);

        // 组合操作，发布EventBus服务，httpEndpoint服务，创建http服务
        publishEventBusService(PullService.SERVICE_NAME, PullService.SERVICE_ADDRESS, PullService.class)
                .compose(publish -> publishHttpEndpoint(SERVER_NAME, host, port))
                .compose(publish -> createHttpServer(router, host, port))
                .setHandler(startFuture.completer());
    }
}
