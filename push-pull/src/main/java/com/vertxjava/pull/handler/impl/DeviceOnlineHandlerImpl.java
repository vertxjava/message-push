package com.vertxjava.pull.handler.impl;

import com.vertxjava.ad.business.service.AdService;
import com.vertxjava.pull.handler.DeviceOnlineHandler;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-09 18:23
 **/
public class DeviceOnlineHandlerImpl implements DeviceOnlineHandler {

    private ServiceDiscovery discovery;

    public DeviceOnlineHandlerImpl(ServiceDiscovery discovery) {
        this.discovery = discovery;
    }

    @Override
    public void handle(RoutingContext context) {
        getAdService().compose(adService ->
                adService.adMatcher(context.getBodyAsJson())
        ).setHandler(ar -> {
            if (ar.succeeded()){

            }else{

            }
        });
    }

    private Future<AdService> getAdService() {
        return Future.future(f -> EventBusService.getProxy(discovery, AdService.class, f.completer()));
    }
}
