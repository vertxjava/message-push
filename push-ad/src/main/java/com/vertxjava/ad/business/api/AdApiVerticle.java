package com.vertxjava.ad.business.api;

import com.vertxjava.ad.business.handler.ADSyncHandler;
import com.vertxjava.ad.business.handler.AdMatherHandler;
import com.vertxjava.ad.business.verticle.MainVerticle;
import com.vertxjava.common.verticle.HttpVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-09 18:31
 **/
public class AdApiVerticle extends HttpVerticle {

    // Default server name
    private static final String DEFAULT_SERVER_NAME = "ad";
    // Default http host
    private static final String DEFAULT_HTTP_HOST = "localhost";
    // Default http port
    private static final int DEFAULT_HTTP_PORT = 8002;
    // log
    private static Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        // router
        final Router router = Router.router(vertx);
        // Health check
        router.get("/health").handler(context -> context.response().end());
        // http host
        String httpHost = config().getString("httpHost", DEFAULT_HTTP_HOST);
        // http port
        int httpPort = config().getInteger("httpPort", DEFAULT_HTTP_PORT);
        // server name
        String serverName = config().getString("serverName", DEFAULT_SERVER_NAME);
        createHttpServer(router, httpHost, httpPort).compose(created -> publishHttpEndpoint(serverName, httpHost, httpPort)).setHandler(ar -> {
            if (ar.succeeded()) {
                startFuture.complete();
                logger.info("Create http server is successful,listen on " + httpPort);
                vertx.eventBus().consumer("eventbus.ad.business.matcher", AdMatherHandler.create(vertx, config().getJsonObject("redisConfig")));
                // sync ad limit 10s
                vertx.setPeriodic(10000, ADSyncHandler.create(vertx, config().getJsonObject("redisConfig"), config().getJsonObject("postgresqlConfig")));
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
