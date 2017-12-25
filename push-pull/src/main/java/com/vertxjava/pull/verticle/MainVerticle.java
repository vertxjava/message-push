package com.vertxjava.pull.verticle;

import com.vertxjava.common.verticle.BaseVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
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
 * @create 2017-12-12 11:17
 **/
public class MainVerticle extends BaseVerticle {
    // 服务名称
    private static final String DEFAULT_SERVER_NAME = "pull";
    // default http host
    private static final String DEFAULT_HTTP_HOST = "localhost";
    // default http port
    private static final int DEFAULT_HTTP_PORT = 8002;
    // log
    private Logger log = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        // router
        Router router = Router.router(vertx);
        // Enable fetching data from the body
        router.route().handler(BodyHandler.create());
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
        System.out.println(config);
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

        publishWebSocketEndpoint(serverName, httpHost, httpPort).setHandler(r -> {
            if (r.succeeded()) {
                log.info("发布http成功");
            } else {
                log.error("发布http失败");
            }
        });
        vertx.createHttpServer().websocketHandler(websocket -> {
            System.out.println("连接到了");
            websocket.handler(buffer -> {
                System.out.println("收到的数据为："+buffer.toString());
                Buffer buffer1 = Buffer.buffer().appendString("我回复了");
                websocket.writeBinaryMessage(buffer1);
                websocket.close();
            });

        }).requestHandler(router::accept).listen(httpPort,httpHost,ar -> {
            if (ar.succeeded()){
                log.info("create http server success,listen on " + httpPort);
                future.complete();
            }else{
                log.error("create http server fail,case:" + ar.cause());
                future.fail(ar.cause());
            }
        });
        return future;
    }
}
