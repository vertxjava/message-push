package com.vertxjava.ad.business.verticle;

import com.vertxjava.ad.business.api.AdApiVerticle;
import com.vertxjava.ad.business.handler.ADSyncHandler;
import com.vertxjava.common.verticle.BaseVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author Jack
 * @create 2017-12-29 11:42
 **/
public class MainVerticle extends BaseVerticle {

    // log
    private static Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        deployAdApiVerticle().setHandler(ar -> {
            if (ar.succeeded()) {
                logger.info("Deploy AdApiVerticle is successful");
            } else {
                logger.error("Deploy AdApiVerticle is failed,case:" + ar.cause());
            }
        });
    }

    private Future<String> deployAdApiVerticle() {
        return Future.future(f -> vertx.deployVerticle(AdApiVerticle.class.getName(),
                new DeploymentOptions().setConfig(config()), f.completer()));
    }


    @Override
    public void stop(Future<Void> future) {
        super.stop(future);
    }
}
