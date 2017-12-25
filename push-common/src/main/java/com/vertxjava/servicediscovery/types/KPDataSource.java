package com.vertxjava.servicediscovery.types;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.spi.ServiceType;

import java.util.Objects;
import java.util.function.Function;

public interface KPDataSource extends ServiceType {

    String TYPE = "kafkaProducer";

    static Record createRecord(String name, JsonObject location, JsonObject metadata) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(location);

        Record record = new Record().setName(name)
                .setType(TYPE)
                .setLocation(location);

        if (metadata != null) {
            record.setMetadata(metadata);
        }

        return record;
    }

    static void getKPClient(ServiceDiscovery discovery, JsonObject filter,
                            Handler<AsyncResult<KafkaProducer<String, String>>> resultHandler) {
        discovery.getRecord(filter, ar -> {
            if (ar.failed() || ar.result() == null) {
                resultHandler.handle(Future.failedFuture("No matching record"));
            } else {
                resultHandler.handle(Future.succeededFuture(discovery.getReference(ar.result()).get()));
            }
        });
    }

    static void getKPClient(ServiceDiscovery discovery, Function<Record, Boolean> filter,
                            Handler<AsyncResult<KafkaProducer<String, String>>> resultHandler) {
        discovery.getRecord(filter, ar -> {
            if (ar.failed() || ar.result() == null) {
                resultHandler.handle(Future.failedFuture("No matching record"));
            } else {
                resultHandler.handle(Future.succeededFuture(discovery.getReference(ar.result()).get()));
            }
        });
    }

    static void getKPClient(ServiceDiscovery discovery, JsonObject filter, JsonObject consumerConfiguration,
                            Handler<AsyncResult<KafkaProducer<String, String>>> resultHandler) {
        discovery.getRecord(filter, ar -> {
            if (ar.failed() || ar.result() == null) {
                resultHandler.handle(Future.failedFuture("No matching record"));
            } else {
                resultHandler.handle(Future.succeededFuture(
                        discovery.getReferenceWithConfiguration(ar.result(), consumerConfiguration).get()));
            }
        });
    }
}
