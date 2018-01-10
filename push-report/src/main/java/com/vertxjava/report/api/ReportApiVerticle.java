package com.vertxjava.report.api;

import com.vertxjava.common.verticle.HttpVerticle;
import com.vertxjava.report.handler.DeviceInfoHandler;
import com.vertxjava.report.handler.FeedbackHandler;
import com.vertxjava.report.verticle.MainVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-10 16:44
 **/
public class ReportApiVerticle extends HttpVerticle {

    // log
    private static final Logger logger = LoggerFactory.getLogger(ReportApiVerticle.class);
    // service name
    private static final String DEFAULT_SERVER_NAME = "report";
    // default http host
    private static final String DEFAULT_HTTP_HOST = "localhost";
    // default http port
    private static final int DEFAULT_HTTP_PORT = 8002;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        final Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        // Health check
        router.get("/health").handler(context -> context.response().end());
        router.post("/deviceInfoReport").handler(DeviceInfoHandler.create(vertx));
        router.post("/feedbackInfoReport").handler(FeedbackHandler.create(vertx));
        // Enable access to static resources
        router.route("/*").handler(StaticHandler.create());
        // http host
        String httpHost = config().getString("httpHost", DEFAULT_HTTP_HOST);
        // http port
        int httpPort = config().getInteger("httpPort", DEFAULT_HTTP_PORT);
        // server name
        String serverName = config().getString("serverName", DEFAULT_SERVER_NAME);
        // Create http server and publish http endpoint
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
        super.stop(future);
    }
}
