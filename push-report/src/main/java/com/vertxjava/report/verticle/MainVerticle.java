package com.vertxjava.report.verticle;

import com.vertxjava.common.verticle.BaseVerticle;
import com.vertxjava.report.api.ReportApiVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author Jack
 * @create 2017-12-13 14:27
 **/
public class MainVerticle extends BaseVerticle {
    // log
    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);


    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        deployReportApiVerticle().setHandler(ar -> {
            if (ar.succeeded()) {
                logger.info("Deploy ReportApiVerticle is successful");
            } else {
                logger.error("Deploy ReportApiVerticle is failed,case:" + ar.cause());
            }
        });

    }

    private Future<String> deployReportApiVerticle() {
        return Future.future(f -> vertx.deployVerticle(ReportApiVerticle.class.getName(),
                new DeploymentOptions().setConfig(config()), f.completer()));
    }

    @Override
    public void stop(Future<Void> future) {
        super.stop(future);
    }
}
