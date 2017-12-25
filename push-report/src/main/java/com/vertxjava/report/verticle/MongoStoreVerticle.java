package com.vertxjava.report.verticle;

import com.vertxjava.common.verticle.BaseVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.servicediscovery.types.MongoDataSource;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jack
 * @create 2017-12-13 17:43
 **/
public class MongoStoreVerticle extends BaseVerticle {
    // kafka topic
    private static final String TOPIC = "topicReport";
    // kafka group
    private static final String GROUP = "group_mongo";
    // 默认kafka服务
    private static final String DEFAULT_KAFKA_SERVER = "127.0.0.1:9092";
    // kafka的消费者服务
    private KafkaConsumer<String, String> consumer;
    // mongo服务
    private MongoClient mongoClient;
    private Logger log = LoggerFactory.getLogger(MongoStoreVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
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
        consumer.subscribe(TOPIC, ar -> {
            if (ar.succeeded()) {
                System.out.println("订阅成功");
            } else {
                System.out.println("订阅失败");
            }
        });
        consumer.handler(this::consumer);
        consumer.exceptionHandler(e -> {
            System.out.println("出现异常");
            System.out.println(e.getMessage());
        });
        // 创建mongo客户端
        JsonObject jsonObject = config().getJsonObject("mongodbConfig");
        mongoClient = MongoClient.createShared(vertx,jsonObject);
        startFuture.complete();
    }

    private void consumer(KafkaConsumerRecord<String, String> record) {
        consumer.resume();
        JsonObject data = new JsonObject(record.value());
        consumer.commit();
        //String _id = data.getString("_id");
        /*MongoDataSource.getMongoClient(discovery,new JsonObject().put("name","mongodb_server"),ar -> {
            if (ar.succeeded()){
                MongoClient client = ar.result();
                client.save("device_"+Integer.parseInt(_id)%100,data,ar1 -> {
                    if (ar1.succeeded()){
                        consumer.commit();
                        log.info("没错误");
                    }else{
                        log.error("数据存入mongo报错，case:"+ar1.cause());
                    }
                });
            }else{
                System.out.println("报错了"+ar.cause());
            }
        });*/
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        consumer.close();
        super.stop(stopFuture);
    }
}
