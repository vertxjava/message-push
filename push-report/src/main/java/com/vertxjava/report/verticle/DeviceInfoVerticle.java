package com.vertxjava.report.verticle;

import com.vertxjava.common.verticle.BaseVerticle;
import com.vertxjava.report.handler.DeviceInfoEventBusHandler;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;

/**
 * @author Jack
 * @create 2017-12-13 17:43
 **/
public class DeviceInfoVerticle extends BaseVerticle {

    private Logger log = LoggerFactory.getLogger(DeviceInfoVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        discovery = ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions()
                .setBackendConfiguration(config().getJsonObject("serviceDiscovery"))
                .setAnnounceAddress("vertx.discovery.announce").setName("discovery"));
        vertx.eventBus().consumer("device_info", DeviceInfoEventBusHandler.create(discovery));
        startFuture.complete();
    }


    @Override
    public void stop(Future<Void> stopFuture) {
        super.stop(stopFuture);
    }
}
