package com.vertxjava.device.business.verticle;

import com.vertxjava.common.verticle.HttpVerticle;
import com.vertxjava.device.business.handler.DeviceInfoReportHandler;
import com.vertxjava.device.business.handler.DeviceListHandler;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-02 11:56
 **/
public class MainVerticle extends HttpVerticle {
    private static final String MESSAGE_SOURCE_NAME = "device-business-message-source";
    // default http host
    private static final String DEFAULT_HTTP_HOST = "localhost";
    // default http port
    private static final int DEFAULT_HTTP_PORT = 8001;
    // service name
    private static final String DEFAULT_SERVER_NAME = "device";
    private static Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        final Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        // Health check
        router.get("/health").handler(context -> context.response().end());
        router.get("/list").handler(DeviceListHandler.create(vertx,config().getJsonObject("redisConfig")));
        // Stop the verticle
        router.get("/destroy").handler(this::destroy);
        // Enable access to static resources
        router.route("/*").handler(StaticHandler.create());
        // http host
        String httpHost = config().getString("httpHost", DEFAULT_HTTP_HOST);
        // http port
        int httpPort = config().getInteger("httpPort", DEFAULT_HTTP_PORT);
        // server name
        String serverName = config().getString("serverName", DEFAULT_SERVER_NAME);
        // Publish message source service
       createHttpServer(router,httpHost,httpPort)
                .compose(created -> publishHttpEndpoint(serverName,httpHost,httpPort)).setHandler(ar -> {
            if (ar.succeeded()) {
                startFuture.complete();
                vertx.eventBus().consumer("eventbus.device.info", DeviceInfoReportHandler.create(vertx,config().getJsonObject("redisConfig"),config().getJsonObject("mongodbConfig")));
                logger.info("Verticle run success");
            } else {
                startFuture.fail(ar.cause());
                logger.error("Verticle run fail,case:" + ar.cause());
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
