package com.vertxjava.pull.handler;

import com.vertxjava.pull.handler.impl.DisplayHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-09 18:21
 **/
public interface DisplayHandler extends Handler<RoutingContext> {
    static DisplayHandler create(){
        return new DisplayHandlerImpl();
    }
}
