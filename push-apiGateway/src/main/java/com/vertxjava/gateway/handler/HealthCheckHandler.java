package com.vertxjava.gateway.handler;

import com.vertxjava.gateway.handler.impl.HealthCheckHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.servicediscovery.ServiceDiscovery;

/**
 * Health check.
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-03 17:24
 **/
public interface HealthCheckHandler extends Handler<Long> {
    /**
     * Create HealthCheckHandler instance
     *
     * @param serviceDiscovery The server discovery instance
     * @return HealthCheckHandler instance
     */
    static HealthCheckHandler create(ServiceDiscovery serviceDiscovery) {
        return new HealthCheckHandlerImpl(serviceDiscovery);
    }
}
