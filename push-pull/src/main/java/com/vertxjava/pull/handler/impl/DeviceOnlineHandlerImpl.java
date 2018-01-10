package com.vertxjava.pull.handler.impl;

import com.vertxjava.pull.handler.DeviceOnlineHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

import java.util.Properties;

/**
 * .
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-09 18:23
 **/
public class DeviceOnlineHandlerImpl implements DeviceOnlineHandler {

    private static final String MESSAGE_TOPIC = "topic_message_up";
    private KafkaProducer<String, String> kafkaProducer;
    private Logger logger = LoggerFactory.getLogger(DeviceOnlineHandlerImpl.class);

    public DeviceOnlineHandlerImpl(Vertx vertx, JsonObject config) {
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
    public void handle(RoutingContext context) {
        JsonObject pullData = context.getBodyAsJson();
        // publish message to kafka
        KafkaProducerRecord<String, String> record =
                KafkaProducerRecord.create(MESSAGE_TOPIC, pullData.encode());
        kafkaProducer.write(record, r -> {
            if (r.succeeded()) {
                logger.info("Pub message to kafka is successful");
                context.response().end();
            } else {
                logger.info("Pub message to kafka is failed,case:" + r.cause());
                context.response().setStatusCode(500).end();
            }
        });
    }

}
