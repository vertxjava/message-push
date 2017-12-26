package com.vertxjava.gateway.verticle;

import com.vertxjava.common.verticle.BaseVerticle;
import com.vertxjava.gateway.handler.DispatchHandler;
import com.vertxjava.gateway.handler.WebSocketHandler;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.dropwizard.MetricsService;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.rest.ServiceDiscoveryRestEndpoint;

public class MainVerticle extends BaseVerticle {

    // default http host
    private static final String DEFAULT_HTTP_HOST = "localhost";
    // default http port
    private static final int DEFAULT_HTTP_PORT = 8001;
    // log
    private Logger log = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        // router
        Router router = Router.router(vertx);
        enableCorsSupport(router);
        // Enable fetching data from the body
        router.route().handler(BodyHandler.create());
        // metrics monitor
        MetricsService service = MetricsService.create(vertx);
        // event bus bridge
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        BridgeOptions options = new BridgeOptions()
                .addOutboundPermitted(new PermittedOptions().setAddress("monitor_metrics"))
                .addOutboundPermitted(new PermittedOptions().setAddress("events.log"));
        sockJSHandler.bridge(options);
        router.route("/eventbus/*").handler(sockJSHandler);
        vertx.setPeriodic(5000, t -> {
            JsonObject metrics = service.getMetricsSnapshot(vertx);
            vertx.eventBus().publish("monitor_metrics", metrics);
        });
        // vert.x config
        ConfigStoreOptions fileStore = new ConfigStoreOptions().setType("file").setConfig(config());
        ConfigRetrieverOptions crOptions = new ConfigRetrieverOptions()
                .addStore(fileStore).setScanPeriod(5000);
        ConfigRetriever retriever = ConfigRetriever.create(vertx, crOptions);
        // listen config change
        retriever.listen(change -> {
            JsonObject config = change.getNewConfiguration();
            // clear che records
            Future<Void> future = Future.future();
            stop(future);
            future.compose(cleared -> init(config,router)).setHandler(r -> {
                if (r.succeeded()) {
                    log.info("Redeployment success");
                } else {
                    log.error("Redeployment fail,case:" + r.cause());
                }
            });
        });
        // get config
        retriever.getConfig(ar -> {
            init(ar.result(), router).setHandler(r -> {
                if (r.succeeded()) {
                    startFuture.complete();
                } else {
                    startFuture.fail(r.cause());
                }
            });
        });

        // 心跳监测 并动态修改service discovery的UP和DOWN
        // jwt auth

    }

    private Future<Void> init(JsonObject config, Router router) {
        Future<Void> future = Future.future();
        /*
         * get serviceDiscovery instance, using zookeeper as back end storage.
         *
         * setAnnounceAddress():Every time a services provider is published or withdrawn,
         * an event is fired on the event bus. This event contains the record that has been modified.
         * We can get this information through eventBus.
         *
         * setName():set the name of a services discovery.
         */
        discovery = ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions()
                .setBackendConfiguration(config.getJsonObject("serviceDiscovery"))
                .setAnnounceAddress("vertx.discovery.announce").setName("discovery"));
        // Bind service discovery data to the router
        ServiceDiscoveryRestEndpoint.create(router, discovery, "/discovery");
        router.route("/api/*").handler(DispatchHandler.create(discovery));
        // Enable access to static resources
        router.route("/*").handler(StaticHandler.create());
        // http host
        String httpHost = config.getString("httpHost", DEFAULT_HTTP_HOST);
        // http port
        int httpPort = config.getInteger("httpPort", DEFAULT_HTTP_PORT);
        vertx.createHttpServer().websocketHandler(WebSocketHandler.create(discovery)).requestHandler(router::accept).listen(httpPort, httpHost, ar -> {
            if (ar.succeeded()) {
                log.info("create http server success,listen on " + httpPort);
                // 发布网关服务
                publishApiGateway(httpHost, httpPort)
                        .compose(published -> publishRedisDataSource(config.getJsonObject("redis")))
                        .compose(published -> publishMongodbDataSource(config.getJsonObject("mongodb")))
                        .compose(published -> publishPGDataSource(config.getJsonObject("postgresql")))
                        .compose(published -> publishKCDataSource(config.getJsonObject("kafkaConsumer")))
                        .compose(published -> publishKPDataSource(config.getJsonObject("kafkaProduce")))
                        .setHandler(r -> {
                            if (r.succeeded()) {
                                log.info("Service publish success");
                                future.complete();
                            } else {
                                log.error("service publish fail,case:" + r.cause());
                            }
                        });
            } else {
                log.error("create http server fail,case:" + ar.cause());
                future.fail(ar.cause());
            }
        });
        return future;
    }

    @Override
    public void stop(Future<Void> stopFuture) {

        super.stop(stopFuture);
    }
}

