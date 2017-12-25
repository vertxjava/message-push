package com.vertxjava.servicediscovery.types.impl;

import com.vertxjava.servicediscovery.types.KPDataSource;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.types.AbstractServiceReference;

import java.util.Objects;
import java.util.Properties;

/**
 * @author Jack
 * @create 2017-12-21 17:23
 **/
public class KPDataSourceImpl implements KPDataSource {
    @Override
    public String name() {
        return KPDataSource.TYPE;
    }

    @Override
    public ServiceReference get(Vertx vertx, ServiceDiscovery discovery, Record record, JsonObject configuration) {
        Objects.requireNonNull(vertx);
        Objects.requireNonNull(record);
        Objects.requireNonNull(discovery);
        return new KPServiceReference(vertx, discovery, record, configuration);
    }

    private class KPServiceReference extends AbstractServiceReference<KafkaProducer<String, String>> {

        private final JsonObject config;

        /**
         * Creates a new instance of {@link AbstractServiceReference}.
         *
         * @param vertx     the vert.x instance
         * @param discovery the service discovery instance
         * @param record    the service record
         * @param config    the config
         */
        KPServiceReference(Vertx vertx, ServiceDiscovery discovery, Record record, JsonObject config) {
            super(vertx, discovery, record);
            this.config = config;
        }

        @Override
        protected KafkaProducer<String, String> retrieve() {
            JsonObject result = record().getMetadata().copy();
            result.mergeIn(record().getLocation());
            if (config != null) {
                result.mergeIn(config);
            }

            Properties props = new Properties();
            props.put("bootstrap.servers", result.getString("servers"));
            props.put("acks", "all");
            props.put("retries", "1");
            props.put("batch.size", "1048576");
            props.put("linger.ms", "1");
            props.put("buffer.memory", "33554432");
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            return KafkaProducer.createShared(vertx, "the_kafka",props);
        }
    }
}
