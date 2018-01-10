package com.vertxjava.ad.business.handler;

import com.vertxjava.ad.business.handler.impl.AdMatherHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;

public interface AdMatherHandler extends Handler<KafkaConsumerRecord<String, String>> {
    static AdMatherHandler create(Vertx vertx, JsonObject redisConfig) {
        return new AdMatherHandlerImpl(vertx, redisConfig);
    }
}
