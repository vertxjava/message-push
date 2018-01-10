package com.vertxjava.device.verticle;

import com.vertxjava.common.verticle.HttpVerticle;
import com.vertxjava.device.api.DeviceApiVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-02 11:56
 **/
public class MainVerticle extends HttpVerticle {
    // log
    private static Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        deployDeviceApiVerticle().setHandler(ar -> {
            if (ar.succeeded()) {
                logger.info("Deploy DeviceApiVerticle is successful");
            } else {
                logger.error("Deploy DeviceApiVerticle is failed,case:" + ar.cause());
            }
        });
    }

    private Future<String> deployDeviceApiVerticle() {
        return Future.future(f -> vertx.deployVerticle(DeviceApiVerticle.class.getName(),
                new DeploymentOptions().setConfig(config()), f.completer()));
    }

    @Override
    public void stop(Future<Void> future) {
        super.stop(future);
    }
}
