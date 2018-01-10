package com.vertxjava.pull.verticle;

import com.vertxjava.common.verticle.BaseVerticle;
import com.vertxjava.common.verticle.HttpVerticle;
import com.vertxjava.pull.api.PullApiVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author Jack
 * @create 2017-12-12 11:17
 **/
public class MainVerticle extends BaseVerticle {

    // log
    private Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        deployPullApiVerticle().setHandler(ar -> {
            if (ar.succeeded()) {
                logger.info("Deploy PullApiVerticle is successful");
            } else {
                logger.error("Deploy PullApiVerticle is failed,case:" + ar.cause());
            }
        });
    }

    private Future<String> deployPullApiVerticle() {
        return Future.future(f -> vertx.deployVerticle(PullApiVerticle.class.getName(),
                new DeploymentOptions().setConfig(config()), f.completer()));
    }

    @Override
    public void stop(Future<Void> future) {
        super.stop(future);
    }
}
