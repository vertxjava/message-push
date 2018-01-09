package com.vertxjava.gateway.verticle;

import com.vertxjava.common.verticle.HttpVerticle;
import com.vertxjava.gateway.handler.WebSocketDispatchHandler;
import com.vertxjava.gateway.handler.HttpDispatchHandler;
import com.vertxjava.gateway.handler.HealthCheckHandler;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.servicediscovery.rest.ServiceDiscoveryRestEndpoint;

/**
 * Api gateway verticle.
 * Implement the following functions:
 * 1. Request forwarding(http and websocket),simple load balancing
 * 2. Service monitoring
 * 3. A simple heartbeat monitoring
 * 4. Authority control(JWT)
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-03 17:26
 **/
public class MainVerticle extends HttpVerticle {

    // The default http host
    private static final String DEFAULT_HTTP_HOST = "localhost";
    // The default http port
    private static final int DEFAULT_HTTP_PORT = 8001;
    // The default server name
    private static final String DEFAULT_SERVER_NAME = "apiGateway";
    // log
    private Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        // The router instance
        Router router = Router.router(vertx);
        // Enable CORS support
        enableCorsSupport(router);
        // Enable fetching data from the body
        router.route().handler(BodyHandler.create());
        // Event bus bridge
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        BridgeOptions options = new BridgeOptions()
                .addOutboundPermitted(new PermittedOptions().setAddress("service.monitor.metrics"))
                .addOutboundPermitted(new PermittedOptions().setAddress("monitor.log"));
        sockJSHandler.bridge(options);
        router.route("/eventbus/*").handler(sockJSHandler);
        // Bind service discovery data to the router
        ServiceDiscoveryRestEndpoint.create(router, discovery, "/discovery");
        // Forwarding the HTTP request, handled by a specific application
        router.route("/api/*").handler(HttpDispatchHandler.create(discovery,vertx));
        // Enable access to static resources
        router.route("/*").handler(StaticHandler.create());
        // The http host
        String httpHost = config().getString("httpHost", DEFAULT_HTTP_HOST);
        // The http port
        int httpPort = config().getInteger("httpPort", DEFAULT_HTTP_PORT);
        // The server name
        String serverName = config().getString("serverName", DEFAULT_SERVER_NAME);
        // Create web socket server and http server
        // Publish http endpoint
        createWebSocketServerAndHttpServer(router, httpHost, httpPort, WebSocketDispatchHandler.create(vertx, discovery)).
                compose(created -> publishHttpEndpoint(serverName, httpHost, httpPort, new JsonObject().put("type", "api-gateway"))).setHandler(ar -> {
            if (ar.succeeded()) {
                startFuture.complete();
                logger.info("Create http server and websocket server is successful,listen on " + httpPort);
                if (config().getBoolean("enableHealthCheck")) {
                    enableHealthCheck();
                }
            } else {
                startFuture.fail(ar.cause());
                logger.info("Create http server and websocket server is failed,the case is : " + ar.cause());
            }
        });
    }

    /**
     * Enable health check
     */
    private void enableHealthCheck() {
        vertx.setPeriodic(5000, HealthCheckHandler.create(discovery));
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        super.stop(stopFuture);
    }

}

