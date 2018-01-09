package com.vertxjava.task;

import com.vertxjava.task.verticle.MainVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class App {
    // log
    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        // Read the configuration file
        JsonObject appConfig = null;
        JsonObject verticleConfig = null;
        try {
            Scanner scanner = new Scanner(new File(args[0])).useDelimiter("\\A");
            appConfig = new JsonObject(scanner.next());
            scanner = new Scanner(new File(args[1])).useDelimiter("\\A");
            verticleConfig = new JsonObject(scanner.next());
        } catch (DecodeException e) {
            logger.error("The configuration file is not a standard JSON format");
            return;
        } catch (FileNotFoundException e) {
            logger.error(e.toString());
            return;
        }

        /*
         * Cluster manager , using zookeeper as a cluster manager
         * You will use zookeeper.json under classpath as a configuration file
         */
        ClusterManager clusterManager = new ZookeeperClusterManager();

        // Config the eventBus
        EventBusOptions eventBusOptions = new EventBusOptions();
        eventBusOptions.setClustered(true)
                .setHost(appConfig.getString("eventBusHost"))
                .setPort(appConfig.getInteger("eventBusPort"));

        // Config the vert.x
        VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setClustered(true);
        vertxOptions.setEventBusOptions(eventBusOptions);
        vertxOptions.setClusterManager(clusterManager);

        // Config DeploymentOptions
        DeploymentOptions options = new DeploymentOptions();
        String verticleInstance = appConfig.getString("verticleInstance");
        if ("auto".equals(verticleInstance)) {
            options.setInstances(vertxOptions.getEventLoopPoolSize());
        } else {
            options.setInstances(Integer.parseInt(verticleInstance));
        }
        options.setConfig(verticleConfig);

        // Get vertx instance by cluster
        Vertx.clusteredVertx(vertxOptions, ar -> {
            if (ar.succeeded()) {
                Vertx vertx = ar.result();
                logger.info("Create vertx cluster is successful");
                // Deploy verticle
                vertx.deployVerticle(MainVerticle.class.getName(), options, res -> {
                    if (res.succeeded()) {
                        logger.info("Deploy verticle is successful");
                    } else {
                        logger.error("Deploy verticle is failed, case : " + res.cause());
                    }
                });
            } else {
                logger.error("create vertx cluster is failed , case : " + ar.cause());
            }
        });
    }

}
