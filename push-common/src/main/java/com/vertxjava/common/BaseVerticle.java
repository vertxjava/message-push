package com.vertxjava.common;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
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
    protected Set<Record> records = new ConcurrentHashSet<Record>();
    // kafka的消费者服务
    protected KafkaConsumer<String, String> kafkaConsumer;
    // kafka的发布者服务
    protected KafkaProducer<String, String> kafkaProducer;

    private Logger log = LoggerFactory.getLogger(BaseVerticle.class);

    @Override
    public void start() throws Exception {
        // 初始化服务发现，使用zookeeper保存服务记录
        discovery = ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions()
                .setBackendConfiguration(config().getJsonObject("serviceDiscovery")));

        // 初始化kafka消费者服务
        Map<String, String> configConsumer = new HashMap<>();
        configConsumer.put("bootstrap.servers", "127.0.0.1:9092");
        configConsumer.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        configConsumer.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        configConsumer.put("group.id", "my_group");
        configConsumer.put("auto.offset.reset", "earliest");
        configConsumer.put("enable.auto.commit", "false");
        kafkaConsumer = KafkaConsumer.create(vertx, configConsumer);

        // 初始化kafka生产者服务
        Map<String, String> configProducer = new HashMap<>();
        configProducer.put("bootstrap.servers", "127.0.0.1:9092");
        configProducer.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        configProducer.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        configProducer.put("acks", "1");
        kafkaProducer = KafkaProducer.create(vertx, configProducer);

    }

    // 发布api gateway服务
    protected Future<Void> publishApiGateway(String host, int port) {
        Record record = HttpEndpoint.createRecord("api-gateway", true, host, port, "/", null)
                .setType("api-gateway");
        return publish(record);
    }

    // 发布http服务
    protected Future<Void> publishHttpEndpoint(String name, String host, int port) {
        Record record = HttpEndpoint.createRecord(name, host, port, "/",
                new JsonObject().put("api.name", config().getString("api.name", "")));
        return publish(record);
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
    protected Future<List<Record>> getAllHttpEndpoints(){
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
