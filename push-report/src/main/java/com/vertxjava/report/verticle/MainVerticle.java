package com.vertxjava.report.verticle;

import com.vertxjava.common.verticle.BaseVerticle;
import com.vertxjava.report.handler.DeviceInfoHandler;
import com.vertxjava.report.handler.FeedbackHandler;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;

/**
 * @author Jack
 * @create 2017-12-13 14:27
 **/
public class MainVerticle extends BaseVerticle {
    // log
    private static final Logger log = LoggerFactory.getLogger(MainVerticle.class);
    // service name
    private static final String DEFAULT_SERVER_NAME = "report";
    // default http host
    private static final String DEFAULT_HTTP_HOST = "localhost";
    // default http port
    private static final int DEFAULT_HTTP_PORT = 8002;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        final Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/deviceInfo").handler(DeviceInfoHandler.create(vertx));
        router.post("/feedback").handler(FeedbackHandler.create(vertx));
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
            future.compose(cleared -> init(config, router)).setHandler(r -> {
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
        // Enable access to static resources
        router.route("/*").handler(StaticHandler.create());
        // http host
        String httpHost = config.getString("httpHost", DEFAULT_HTTP_HOST);
        // http port
        int httpPort = config.getInteger("httpPort", DEFAULT_HTTP_PORT);
        // server name
        String serverName = config.getString("serverName", DEFAULT_SERVER_NAME);

        createHttpServer(router, httpHost, httpPort)
                .compose(created -> publishHttpEndpoint(serverName, httpHost, httpPort))
                .compose(published -> deployVerticle(new DeviceInfoVerticle(),config))
                .compose(deployed -> deployVerticle(new FeedbackStoreVerticle(),config))
                .setHandler(ar -> {
                    if (ar.succeeded()) {
                        log.info("MainVerticle run success");
                    } else {
                        log.error("MainVerticle run fail,case:"+ar.cause());
                    }
                });
        return future;
    }

    // 部署其他verticle
    private Future<Void> deployVerticle(FeedbackStoreVerticle rsv,JsonObject config) {
        Future<String> future = Future.future();
        vertx.deployVerticle(rsv, new DeploymentOptions().setConfig(config), future.completer());
        return future.map(r -> null);
    }

    // 部署其他verticle
    private Future<Void> deployVerticle(DeviceInfoVerticle msv,JsonObject config) {
        Future<String> future = Future.future();
        vertx.deployVerticle(msv, new DeploymentOptions().setConfig(config), future.completer());
        return future.map(r -> null);
    }


    @Override
    public void stop(Future<Void> stopFuture) {
        super.stop(stopFuture);
    }
}
