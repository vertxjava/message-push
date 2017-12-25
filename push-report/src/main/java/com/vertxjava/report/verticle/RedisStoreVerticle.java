package com.vertxjava.report.verticle;

import com.vertxjava.common.verticle.BaseVerticle;
import com.vertxjava.servicediscovery.types.KCDataSource;
import com.vertxjava.servicediscovery.types.PGDataSource;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import io.vertx.servicediscovery.types.RedisDataSource;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jack
 * @create 2017-12-13 17:45
 **/
public class RedisStoreVerticle extends BaseVerticle {
    // kafka topic
    private static final String TOPIC = "topicReport";
    // kafka group
    private static final String GROUP = "group_redis";
    // 默认kafka服务
    private static final String DEFAULT_KAFKA_SERVER = "127.0.0.1:9092";
    // kafka的消费者服务
    private KafkaConsumer<String, String> consumer;
    private Logger log = LoggerFactory.getLogger(RedisStoreVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();

        KCDataSource.getKCClient(discovery,new JsonObject().put("name","KafkaConsumer_server"),ar -> {
            if (ar.succeeded()){
                consumer = ar.result();
                consumer.subscribe(TOPIC, ar1 -> {
                    if (ar1.succeeded()) {
                        System.out.println("订阅成功");
                    } else {
                        System.out.println("订阅失败");
                    }
                });
                consumer.handler(this::consumer);
            }else{
                System.out.println("获取失败了 kafka");
            }
        });

        // 初始化kafka消费者服务
        /*Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", "192.168.237.128:9092,192.168.237.128:9093");
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("group.id", GROUP);
        config.put("enable.auto.commit", "false");
        config.put("auto.offset.reset", "earliest");
        consumer = KafkaConsumer.create(vertx, config);*/
        // 订阅topic

        startFuture.complete();
    }

    private void consumer(KafkaConsumerRecord<String, String> record) {
        // 重置
        consumer.resume();
        // 将设备数据存入redis中
        JsonObject data = new JsonObject(record.value());
        System.out.println("拿到数据，"+record.value());
        consumer.commit();
        /*RedisDataSource.getRedisClient(discovery,new JsonObject().put("name","redis_server"),ar -> {
            if (ar.succeeded()){
                RedisClient client = ar.result();
                client.set("device_"+data.getString("_id"), data.encode(), ar1 -> {
                    if (ar1.failed()) {
                        log.error("set redis error,case:"+ar1.cause());
                    }else{
                        consumer.commit();
                    }
                });
            }else{
                System.out.println("报错了");
            }
        });*/

    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        consumer.close();
        super.stop(stopFuture);
    }
}
