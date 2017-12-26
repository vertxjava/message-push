package com.vertxjava.report.handler;

import com.vertxjava.report.handler.impl.DeviceInfoEventBusHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.servicediscovery.ServiceDiscovery;

public interface DeviceInfoEventBusHandler extends Handler<Message<Object>> {
    static DeviceInfoEventBusHandler create(ServiceDiscovery discovery){
        return new DeviceInfoEventBusHandlerImpl(discovery);
    }
}
