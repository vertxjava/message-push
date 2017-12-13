package com.vertxjava.common.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.servicediscovery.types.HttpEndpoint;

import java.util.*;

/**
 * Verticle的基类，封装了一些公共组件
 *
 * @author Jack
 * @create 2017-12-08 11:03
 **/
public class BaseVerticle extends AbstractVerticle {
    // 服务发现
    protected ServiceDiscovery discovery;
    // 服务记录，用于在销毁Verticle时，将服务记录从服务发现中移除
    private Set<Record> records = new ConcurrentHashSet<Record>();

    private Logger log = LoggerFactory.getLogger(BaseVerticle.class);

    @Override
    public void start() throws Exception {
        // 初始化服务发现，使用zookeeper保存服务记录
        discovery = ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions()
                .setBackendConfiguration(config().getJsonObject("serviceDiscovery")));
    }

    // 发布api gateway服务
    protected Future<Void> publishApiGateway(String host, int port) {
        Record record = HttpEndpoint.createRecord("api-gateway", true, host, port, "/", null)
                .setType("api-gateway");
        return publish(record);
    }

    // 发布http服务
    protected Future<Void> publishHttpEndpoint(String name, String apiName, String host, int port) {
        Record record = HttpEndpoint.createRecord(name, host, port, "/",
                new JsonObject().put("api.name", apiName));
        return publish(record);
    }

    // 发布eventBus服务
    protected Future<Void> publishEventBusService(String name, String address, Class serviceClass) {
        Record record = EventBusService.createRecord(name, address, serviceClass);
        return publish(record);
    }

    // 获取代理服务
    protected <T> Future<T> getProxyService(Class<T> clazz) {
        Future<T> future = Future.future();
        EventBusService.getProxy(discovery, clazz, future.completer());
        return future;
    }

    // 发布服务
    private Future<Void> publish(Record record) {
        Future<Void> future = Future.future();
        if (discovery == null) {
            try {
                start();
            } catch (Exception e) {
                throw new IllegalStateException("Cannot create discovery service");
            }
        }
        // 发布服务
        discovery.publish(record, ar -> {
            if (ar.succeeded()) {
                records.add(record);
                log.info("Service <" + ar.result().getName() + "> published");
                future.complete();
            } else {
                future.fail(ar.cause());
                log.error("Service <" + ar.result().getName() + "> publish failed,case:" + ar.cause());
            }
        });
        return future;
    }

    // 获取所有http端点服务
    protected Future<List<Record>> getAllHttpEndpoints() {
        Future<List<Record>> future = Future.future();
        discovery.getRecords(record -> record.getType().equals(HttpEndpoint.TYPE),
                future.completer());
        return future;
    }

    // 销毁discovery服务
    @Override
    public void stop(Future<Void> future) throws Exception {
        // In current design, the publisher is responsible for removing the service
        List<Future> futures = new ArrayList<>();
        records.forEach(record -> {
            Future<Void> cleanupFuture = Future.future();
            futures.add(cleanupFuture);
            discovery.unpublish(record.getRegistration(), cleanupFuture.completer());
        });

        if (futures.isEmpty()) {
            discovery.close();
            future.complete();
        } else {
            CompositeFuture.all(futures)
                    .setHandler(ar -> {
                        discovery.close();
                        if (ar.failed()) {
                            future.fail(ar.cause());
                        } else {
                            future.complete();
                        }
                    });
        }
    }
}
