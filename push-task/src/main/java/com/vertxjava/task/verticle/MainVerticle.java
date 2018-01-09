package com.vertxjava.task.verticle;

import com.vertxjava.common.verticle.HttpVerticle;
import com.vertxjava.task.handler.TaskHandler;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * @author Jack
 * @create 2017-12-18 16:47
 **/
public class MainVerticle extends HttpVerticle {

    // log
    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);
    // service name
    private static final String DEFAULT_SERVER_NAME = "task";
    // default http host
    private static final String DEFAULT_HTTP_HOST = "localhost";
    // default http port
    private static final int DEFAULT_HTTP_PORT = 8004;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        SQLClient client = PostgreSQLClient.createShared(vertx, config().getJsonObject("postgresqlConfig"));
        final Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        // Health check
        router.get("/health").handler(context -> context.response().end());
        router.post("/add").handler(TaskHandler.create(client));
        router.get("/list").handler(TaskHandler.create(client));
        router.get("/pub").handler(TaskHandler.create(client));
        // http host
        String httpHost = config().getString("httpHost", DEFAULT_HTTP_HOST);
        // http port
        int httpPort = config().getInteger("httpPort", DEFAULT_HTTP_PORT);
        // server name
        String serverName = config().getString("serverName", DEFAULT_SERVER_NAME);
        // Create http server and publish http endpoint
        createHttpServer(router, httpHost, httpPort)
                .compose(created -> publishHttpEndpoint(serverName, httpHost, httpPort))
                .setHandler(ar -> {
                    if (ar.succeeded()) {
                        startFuture.complete();
                        logger.info("Create http server is successful,listen on " + httpPort);
                    } else {
                        startFuture.fail(ar.cause());
                        logger.info("Create http server is failed,the case is : " + ar.cause());
                    }
                });

    }

    /**
     * Destroy the verticle and close vertx
     *
     * @param context RoutingContext
     */
    private void destroy(RoutingContext context) {
        context.response().end();
        vertx.undeploy(deploymentID(), ar -> {
            // TODO send log to eventbus
            if (ar.succeeded()) {
                vertx.close();
                System.exit(0);
            } else {

            }
        });
    }

    @Override
    public void stop(Future<Void> future) {
        super.stop(future);
    }
}
