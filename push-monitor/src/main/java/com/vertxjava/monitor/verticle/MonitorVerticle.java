package com.vertxjava.monitor.verticle;

import com.vertxjava.common.BaseVerticle;
import com.vertxjava.common.HttpVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.dropwizard.MetricsService;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.servicediscovery.rest.ServiceDiscoveryRestEndpoint;

/**
 * @author Jack
 * @create 2017-12-11 15:21
 **/
public class MonitorVerticle extends HttpVerticle{

    private static final String DEFAULT_HOST = "localhost";
    private static final Integer DEFAULT_PORT = 9000;
    private static final Integer DEFAULT_INTERVAL = 5000;

    @Override
    public void start() throws Exception {
        super.start();
        Router router = Router.router(vertx);
        enableCorsSupport(router);
        MetricsService service = MetricsService.create(vertx);

        // event bus bridge
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        BridgeOptions options = new BridgeOptions()
                .addOutboundPermitted(new PermittedOptions().setAddress("monitor_metrics"))
                .addOutboundPermitted(new PermittedOptions().setAddress("events.log"));

        sockJSHandler.bridge(options);
        router.route("/eventbus/*").handler(sockJSHandler);
        // 将discovery中的数据绑定到router上
        ServiceDiscoveryRestEndpoint.create(router, discovery);

        router.route("/*").handler(StaticHandler.create());

        int port = config().getInteger("port", DEFAULT_PORT);
        String host = config().getString("host", DEFAULT_HOST);
        int metricsInterval = config().getInteger("interval", DEFAULT_INTERVAL);

        createHttpServer(router,host,port);

        vertx.setPeriodic(metricsInterval,t -> {
            JsonObject metrics = service.getMetricsSnapshot(vertx);
            vertx.eventBus().publish("monitor_metrics", metrics);
        });
    }
}
