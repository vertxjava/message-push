package com.vertxjava.ad.business.service;

import com.vertxjava.ad.business.service.impl.AdServiceImpl;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-09 18:34
 **/
public interface AdService {

    String SERVICE_NAME = "ad.eventbus.address";
    String SERVICE_ADDRESS = "ad.eventbus.service";

    static AdService create(Vertx vertx, JsonObject config) {
        return new AdServiceImpl(vertx, config);
    }

    Future<String> adMatcher(JsonObject pullData);
}
