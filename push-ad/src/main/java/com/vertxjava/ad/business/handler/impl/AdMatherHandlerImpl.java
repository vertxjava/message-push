package com.vertxjava.ad.business.handler.impl;

import com.vertxjava.ad.business.handler.AdMatherHandler;
import com.vertxjava.redis.RedisFClient;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

import java.util.Properties;

/**
 * @author Jack
 * @create 2017-12-28 15:36
 **/
public class AdMatherHandlerImpl implements AdMatherHandler {
    // ready ad key
    private static final String READY_AD_KEY = "READY_AD";
    private static final String DEVICE_INFO_KEY = "DEVICE_INFO";
    private static final String MESSAGE_TOPIC = "topic_message_down";
    private Logger logger = LoggerFactory.getLogger(AdMatherHandlerImpl.class);
    private RedisFClient redisFClient;
    private JsonObject adContext;
    private KafkaProducer<String, String> kafkaProducer;

    public AdMatherHandlerImpl(Vertx vertx, JsonObject config) {
        redisFClient = RedisFClient.create(vertx, config.getJsonObject("redisConfig"));
        Properties props = new Properties();
        props.put("bootstrap.servers", config.getJsonObject("kafkaConfig").getString("servers"));
        props.put("acks", "all");
        props.put("retries", "1");
        props.put("batch.size", "1048576");
        props.put("linger.ms", "1");
        props.put("buffer.memory", "33554432");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProducer = KafkaProducer.createShared(vertx, config.getJsonObject("kafkaConfig").getString("name"), props);
    }

    @Override
    public void handle(KafkaConsumerRecord<String, String> event) {
        // TODO 这个设计可能有问题，如果第一个用户执行到一半，第二个用户执行new操作，第一个用户的数据是不是不存在了
        adContext = new JsonObject();
        JsonObject pullData = new JsonObject(event.record().value().toString());
        System.out.println("拿到了pu:" + pullData);
        Future<String> future = Future.future();
        final String pid = pullData.getString("pid");
        adContext.put("pid", pid);
        redisFClient.get(createKey(pid))
                .compose((dcp) -> checkDcp(dcp, pid))
                .compose(checked -> redisFClient.get(createKey(pid)))
                .compose((dcp) -> redisFClient.hget(DEVICE_INFO_KEY, pid))
                .compose(this::setDeviceType)
                .compose(seted -> redisFClient.llen(READY_AD_KEY))
                .compose((len) -> redisFClient.lrange(READY_AD_KEY, len))
                .compose(this::adMather).setHandler(ar -> {
            if (ar.succeeded()) {
                // publish message to kafka
                KafkaProducerRecord<String, String> record =
                        KafkaProducerRecord.create(MESSAGE_TOPIC,
                                new JsonObject().put("pid", pid).put("message", ar.result()).encode());
                kafkaProducer.write(record);
                logger.info("Ad match success");
            } else {
                future.fail(ar.cause());
                logger.error("Ad match fail,case:" + ar.cause());
            }

        });
    }

    private Future<Void> checkDcp(String dcp, String pid) {
        return Future.future(f -> {
            if (dcp == null) {
                redisFClient.set(createKey(pid), new JsonObject().put("pid", pid).encode()).setHandler(ar -> {
                    if (ar.succeeded()) {
                        adContext.put("dcp", new JsonObject().put("pid", pid));
                        f.complete();
                    } else {
                        f.fail(ar.cause());
                    }
                });
            } else {
                adContext.put("dcp", new JsonObject(dcp));
                f.complete();
            }
        });
    }

    private Future<Void> setDeviceType(String info) {
        return Future.future(f -> {
            adContext.put("deviceType", new JsonObject(info).getString("deviceType"));
            f.complete();
        });
    }

    private Future<JsonArray> adMather(JsonArray ads) {
        Future<JsonArray> future = Future.future();
        JsonArray adCanPush = new JsonArray();
        JsonObject dpcJson = adContext.getJsonObject("dcp");
        int index = -1;
        // 没有匹配过广告
        Long idx = dpcJson.getLong("idx");
        if (idx == null) {
            index = 0;
        } else {
            for (int i = (ads.size() - 1); i >= 0; i--) {
                JsonObject tmp = new JsonObject(ads.getString(i));
                // 广告都匹配过了
                if (tmp.getLong("idx").compareTo(dpcJson.getLong("idx")) > 0) {
                    index = i;
                } else {
                    break;
                }
            }
        }
        // Begin match
        if (index >= 0) {
            for (int i = index; i < ads.size(); i++) {
                JsonObject ad = new JsonObject(ads.getString(i));
                if (ad.getString("type").equals(adContext.getString("deviceType"))) {
                    adCanPush.add(ad);
                }
                dpcJson.put("idx", ad.getLong("idx"));
            }
            // Update cache
            redisFClient.set(createKey(adContext.getString("pid")), dpcJson.encode()).setHandler(ar -> {
                if (ar.succeeded()) {
                    logger.info("Update device push context cache success");
                    future.complete(adCanPush);
                } else {
                    logger.info("Update device push context cache fail,case:" + ar.cause());
                    future.fail(ar.cause());
                }
            });
        } else {
            future.complete(adCanPush);
        }
        return future;
    }

    private String createKey(String pid) {
        return "DEVICE_PUSH_CONTEXT." + pid;
    }


}

