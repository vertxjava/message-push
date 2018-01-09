package com.vertxjava.pull.handler;

import com.vertxjava.pull.handler.impl.DeviceOnlineHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-09 18:21
 **/
public interface DeviceOnlineHandler extends Handler<RoutingContext> {
    static DeviceOnlineHandler create(){
        return new DeviceOnlineHandlerImpl();
    }
}
