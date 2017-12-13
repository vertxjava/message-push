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
    private KafkaConsumer<String, JsonObject> kafkaConsumer;
    // kafka topic
    private static final String TOPIC = "topicReport";
    // kafka group
    private static final String GROUP = "group_redis";
    // 默认kafka服务
    private static final String DEFAULT_KAFKA_SERVER = "127.0.0.1:9092";

    @Override
    public void start(Future<Void> startFuture) throws Exception {
// 初始化kafka消费者服务
        Map<String, String> configConsumer = new HashMap<>();
        configConsumer.put("bootstrap.servers", config().getString("kafkaServer",DEFAULT_KAFKA_SERVER));
        configConsumer.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        configConsumer.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        configConsumer.put("group.id", GROUP);
        configConsumer.put("auto.offset.reset", "earliest");
        configConsumer.put("enable.auto.commit", "false");
        kafkaConsumer = KafkaConsumer.create(vertx, configConsumer);
        // 订阅topic
        kafkaConsumer.subscribe(TOPIC,ar -> {
            if (ar.succeeded()){
                System.out.println("订阅成功");
            }else{
                System.out.println("订阅失败");
            }
        });
        kafkaConsumer.handler(this::consumer);
        startFuture.complete();
    }

    private void consumer(KafkaConsumerRecord<String,JsonObject> record){
        System.out.println("消费成功");
        System.out.println(record.value());
    }
}
