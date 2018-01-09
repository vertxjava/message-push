package com.vertxjava.mongodb.impl;

import com.vertxjava.mongodb.MongoFClient;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-09 14:18
 **/
public class MongoFClientImpl implements MongoFClient {

    private MongoClient mongoClient;

    public MongoFClientImpl(Vertx vertx, JsonObject config) {
        mongoClient = MongoClient.createShared(vertx, config);
    }

    @Override
    public Future<String> save(String collection, JsonObject document) {
        return Future.future(f -> mongoClient.save(collection, document, f.completer()));
    }
}
