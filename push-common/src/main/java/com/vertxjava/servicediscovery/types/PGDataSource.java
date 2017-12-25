package com.vertxjava.servicediscovery.types;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.spi.ServiceType;

import java.util.Objects;
import java.util.function.Function;

public interface PGDataSource extends ServiceType {

    String TYPE = "postgresql";

    static Record createRecord(String name, JsonObject location, JsonObject metadata){
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

    static void getPGClient(ServiceDiscovery discovery, JsonObject filter,
                            Handler<AsyncResult<AsyncSQLClient>> resultHandler){
        discovery.getRecord(filter, ar -> {
            if (ar.failed() || ar.result() == null) {
                resultHandler.handle(Future.failedFuture("No matching record"));
            } else {
                resultHandler.handle(Future.succeededFuture(discovery.getReference(ar.result()).get()));
            }
        });
    }

    static void getPGClient(ServiceDiscovery discovery, Function<Record, Boolean> filter,
                               Handler<AsyncResult<AsyncSQLClient>> resultHandler) {
        discovery.getRecord(filter, ar -> {
            if (ar.failed() || ar.result() == null) {
                resultHandler.handle(Future.failedFuture("No matching record"));
            } else {
                resultHandler.handle(Future.succeededFuture(discovery.getReference(ar.result()).get()));
            }
        });
    }

    static void getPGClient(ServiceDiscovery discovery, JsonObject filter, JsonObject consumerConfiguration,
                               Handler<AsyncResult<AsyncSQLClient>> resultHandler) {
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
