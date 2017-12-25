package com.vertxjava.servicediscovery.types;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.spi.ServiceType;
import io.vertx.servicediscovery.types.HttpLocation;

import java.util.Objects;
import java.util.function.Function;


public interface WebSocketEndpoint extends ServiceType {

    String TYPE = "webSocket-endpoint";

    static Record createRecord(String name, String host, int port, String root, JsonObject metadata) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(host);
        if (root == null) {
            root = "/";
        }

        Record record = new Record().setName(name)
                .setType(TYPE)
                .setLocation(new HttpLocation()
                        .setHost(host).setPort(port).setRoot(root).toJson());

        if (metadata != null) {
            record.setMetadata(metadata);
        }

        return record;
    }

    static void getClient(ServiceDiscovery discovery, Function<Record, Boolean> filter, Handler<AsyncResult<HttpClient>>
            resultHandler) {
        discovery.getRecord(filter, ar -> {
            if (ar.failed() || ar.result() == null) {
                resultHandler.handle(Future.failedFuture("No matching record"));
            } else {
                resultHandler.handle(Future.succeededFuture(discovery.<HttpClient>getReference(ar.result()).get()));
            }
        });
    }

}
