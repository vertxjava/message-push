package com.vertxjava.servicediscovery.types.impl;

import com.vertxjava.servicediscovery.types.PGDataSource;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.types.AbstractServiceReference;
import io.vertx.servicediscovery.types.impl.MongoDataSourceImpl;

import java.util.Objects;

/**
 * @author Jack
 * @create 2017-12-20 10:13
 **/
public class PGDataSourceImpl implements PGDataSource {
    @Override
    public String name() {
        return PGDataSource.TYPE;
    }

    @Override
    public ServiceReference get(Vertx vertx, ServiceDiscovery discovery, Record record, JsonObject configuration) {
        Objects.requireNonNull(vertx);
        Objects.requireNonNull(record);
        Objects.requireNonNull(discovery);
        return new PGServiceReference(vertx, discovery, record, configuration);
    }

    private class PGServiceReference extends AbstractServiceReference<AsyncSQLClient>{

        private final JsonObject config;

        PGServiceReference(Vertx vertx, ServiceDiscovery discovery, Record record, JsonObject config) {
            super(vertx, discovery, record);
            this.config = config;
        }
        @Override
        protected AsyncSQLClient retrieve() {
            JsonObject result = record().getMetadata().copy();
            result.mergeIn(record().getLocation());
            if (config != null) {
                result.mergeIn(config);
            }
            if (result.getBoolean("shared", false)) {
                return PostgreSQLClient.createShared(vertx, result);
            } else {
                return PostgreSQLClient.createNonShared(vertx, result);
            }
        }

        @Override
        protected void onClose() {
            service.close();
        }
    }
}
