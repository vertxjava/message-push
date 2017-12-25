package com.vertxjava.pull;

import com.vertxjava.pull.verticle.MainVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class App {
    private static Logger logger = LoggerFactory.getLogger(App.class);
    private static String deployId;
    private static Vertx vertx;

    public static void main(String[] args) {
        // 读取配置文件
        JsonObject conf = null;
        try {
            Scanner scanner = new Scanner(new File(args[0])).useDelimiter("\\A");
            conf = new JsonObject(scanner.next());
        } catch (DecodeException e) {
            logger.error("配置文件不是一个标准的json格式");
            return;
        } catch (FileNotFoundException e) {
            logger.error(e.toString());
            return;
        }

        // 配置集群管理器 使用zookeeper作为集群管理器 将使用classpath下的zookeeper.json作为配置文件
        ClusterManager clusterManager = new ZookeeperClusterManager();

        // 配置eventBus
        EventBusOptions eventBusOptions = new EventBusOptions();
        eventBusOptions.setClustered(true)
                .setHost(conf.getString("eventBusHost"))
                .setPort(conf.getInteger("eventBusPort"));

        // 配置Vertx
        VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setClustered(true);
        vertxOptions.setEventBusOptions(eventBusOptions);
        vertxOptions.setClusterManager(clusterManager);
        vertxOptions.setMetricsOptions(new MetricsOptions().setEnabled(true));

        // 配置DeploymentOptions
        DeploymentOptions options = new DeploymentOptions();
        String verticleInstance = conf.getString("verticleInstance");
        if ("auto".equals(verticleInstance)) {
            options.setInstances(vertxOptions.getEventLoopPoolSize());
        } else {
            options.setInstances(Integer.parseInt(verticleInstance));
        }
        options.setConfig(new JsonObject().put("path", conf.getString("verticleConfigFilePath")));

        // 获取集群vertx
        Vertx.clusteredVertx(vertxOptions, ar -> {
            if (ar.succeeded()) {
                // 集群vertx
                vertx = ar.result();
                logger.info("Create vertx cluster success");
                // 部署verticle
                vertx.deployVerticle(MainVerticle.class.getName(), options, res -> {
                    if (res.succeeded()) {
                        logger.info("Deploy verticle success");
                        deployId = res.result();
                    } else {
                        logger.error("Deploy verticle fail, case : " + res.cause());
                    }
                });
            } else {
                logger.error("create vertx cluster fail , case : " + ar.cause());
            }
        });
    }
}
