package com.vertxjava.task;

import com.vertxjava.task.verticle.TaskVerticle;
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

public class Launcher {
    private static Logger logger = LoggerFactory.getLogger(Launcher.class);
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
                .setHost(conf.getJsonObject("eventBusConfig").getString("host"))
                .setPort(conf.getJsonObject("eventBusConfig").getInteger("port"));

        // 配置Vertx
        VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setClustered(true);
        vertxOptions.setEventBusOptions(eventBusOptions);
        vertxOptions.setClusterManager(clusterManager);
        vertxOptions.setMetricsOptions(new MetricsOptions().setEnabled(true));

        // 配置DeploymentOptions
        DeploymentOptions options = new DeploymentOptions();
        options.setConfig(conf.getJsonObject("verticleConfig"));
        options.setInstances(vertxOptions.getEventLoopPoolSize());

        // 获取集群vertx
        Vertx.clusteredVertx(vertxOptions, ar -> {
            if (ar.succeeded()) {
                // 集群vertx
                vertx = ar.result();
                logger.info("创建集群vertx成功");
                // 部署verticle
                vertx.deployVerticle(TaskVerticle.class, options, res -> {
                    if (res.succeeded()) {
                        logger.info("部署Verticle成功");
                        deployId = res.result();
                    } else {
                        logger.error("部署Verticle失败 , 原因 : " + res.cause());
                    }
                });
            } else {
                logger.error("创建集群vertx失败 , 原因 : " + ar.cause());
            }
        });
    }
}
