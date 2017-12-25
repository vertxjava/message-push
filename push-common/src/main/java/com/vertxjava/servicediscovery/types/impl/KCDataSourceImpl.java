package com.vertxjava.servicediscovery.types.impl;

import com.vertxjava.servicediscovery.types.KCDataSource;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.types.AbstractServiceReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jack
 * @create 2017-12-21 17:23
 **/
public class KCDataSourceImpl implements KCDataSource {
    @Override
    public String name() {
        return KCDataSource.TYPE;
    }

    @Override
    public ServiceReference get(Vertx vertx, ServiceDiscovery discovery, Record record, JsonObject configuration) {
        Objects.requireNonNull(vertx);
        Objects.requireNonNull(record);
        Objects.requireNonNull(discovery);
        return new KCServiceReference(vertx, discovery, record, configuration);
    }

    private class KCServiceReference extends AbstractServiceReference<KafkaConsumer<String, String>> {

        private final JsonObject config;

        /**
         * Creates a new instance of {@link AbstractServiceReference}.
         *
         * @param vertx     the vert.x instance
         * @param discovery the service discovery instance
         * @param record    the service record
         * @param config    the config
         */
        KCServiceReference(Vertx vertx, ServiceDiscovery discovery, Record record,JsonObject config) {
            super(vertx, discovery, record);
            this.config = config;
        }

        @Override
        protected KafkaConsumer<String, String> retrieve() {
            JsonObject result = record().getMetadata().copy();
            result.mergeIn(record().getLocation());
            if (config != null) {
                result.mergeIn(config);
            }
            Map<String, String> kcConfig = new HashMap<>();
            kcConfig.put("bootstrap.servers", result.getString("servers"));
            kcConfig.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kcConfig.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kcConfig.put("group.id", "push");
            kcConfig.put("enable.auto.commit", "false");
            kcConfig.put("auto.offset.reset", "earliest");
            return KafkaConsumer.create(vertx, kcConfig);
        }
    }
}
