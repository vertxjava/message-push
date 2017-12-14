package com.vertxjava.report.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jack
 * @create 2017-12-13 17:45
 **/
public class RedisStoreVerticle extends AbstractVerticle {
    // kafka的消费者服务
    private KafkaConsumer<Object, Object> consumer;
    // kafka topic
    private static final String TOPIC = "topicReport";
    // kafka group
    private static final String GROUP = "group_redis";
    // 默认kafka服务
    private static final String DEFAULT_KAFKA_SERVER = "127.0.0.1:9092";

    @Override
    public void start(Future<Void> startFuture) throws Exception {
// 初始化kafka消费者服务
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", "192.168.237.128:9092,192.168.237.128:9093");
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("group.id", GROUP);
        config.put("enable.auto.commit", "false");
        config.put("auto.offset.reset", "earliest");
        consumer = KafkaConsumer.create(vertx, config);
        // 订阅topic
        consumer.subscribe(TOPIC,ar -> {
            if (ar.succeeded()){
                System.out.println("订阅成功");
            }else{
                System.out.println("订阅失败");
            }
        });
        consumer.handler(this::consumer);
        startFuture.complete();
    }

    private void consumer(KafkaConsumerRecord<Object,Object> record){
        consumer.resume();
        System.out.println("消费成功redis");
        System.out.println(record.value());
        consumer.commit();
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        consumer.close();
        super.stop(stopFuture);
    }
}
