package com.vertxjava.pull.api;

import com.vertxjava.common.verticle.HttpVerticle;
import com.vertxjava.pull.handler.DeviceOnlineHandler;
import com.vertxjava.pull.handler.DisplayHandler;
import com.vertxjava.pull.verticle.MainVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

/**
 * Client pull message service
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-09 14:49
 **/
public class PullApiVerticle extends HttpVerticle {

    // Default server name
    private static final String DEFAULT_SERVER_NAME = "pull";
    // Default http host
    private static final String DEFAULT_HTTP_HOST = "localhost";
    // Default http port
    private static final int DEFAULT_HTTP_PORT = 8002;
    // log
    private Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        // router
        final Router router = Router.router(vertx);
        // Health check
        router.get("/health").handler(context -> context.response().end());
        // Pull message
        router.get("/deviceOnline").handler(DeviceOnlineHandler.create());
        // Display data report
        router.post("/display").handler(DisplayHandler.create());
        // http host
        String httpHost = config().getString("httpHost", DEFAULT_HTTP_HOST);
        // http port
        int httpPort = config().getInteger("httpPort", DEFAULT_HTTP_PORT);
        // server name
        String serverName = config().getString("serverName", DEFAULT_SERVER_NAME);
        // Create websocket server and publish websocket-endpoint service
        createHttpServer(router, httpHost, httpPort).compose(created -> publishHttpEndpoint(serverName, httpHost, httpPort)).setHandler(ar -> {
            if (ar.succeeded()) {
                startFuture.complete();
                logger.info("Create http server is successful,listen on " + httpPort);
            } else {
                startFuture.fail(ar.cause());
                logger.info("Create http server is failed,the case is : " + ar.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> future) {
        System.out.println("执行了apiVerticle的stop");
        super.stop(future);
    }

}
