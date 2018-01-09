package com.vertxjava.common.service;

import com.vertxjava.common.service.impl.HttpResponseServiceImpl;
import io.vertx.ext.web.RoutingContext;

/**
 * Http response.
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-05 14:20
 **/
public interface HttpResponseService {
    /**
     * Create HttpResponseService instance
     *
     * @return The HttpResponseService instance
     */
    static HttpResponseService create() {
        return new HttpResponseServiceImpl();
    }

    /**
     * Not found (404)
     *
     * @param context The RoutingContext instance
     */
    void notFound(RoutingContext context);

    /**
     * Server error (500)
     *
     * @param context The RoutingContext instance
     * @param message The error message
     */
    void serverError(RoutingContext context, Object message);

    /**
     * Success response
     *
     * @param context The RoutingContext instance
     */
    void success(RoutingContext context);

    /**
     * Success response
     *
     * @param context The RoutingContext instance
     * @param data    The response data
     */
    void success(RoutingContext context, Object data);
}
