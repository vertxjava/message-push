package com.vertxjava.mongodb;

import com.vertxjava.mongodb.impl.MongoFClientImpl;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Provide access to postgresql, and use Future to wrap the results
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-09 14:17
 **/
public interface MongoFClient {
    static MongoFClient create(Vertx vertx, JsonObject config) {
        return new MongoFClientImpl(vertx, config);
    }

    /**
     * Save a document in the specified collection
     *
     * @param collection the collection
     * @param document   the document
     * @return Using Future to get the return value
     */
    Future<String> save(String collection, JsonObject document);
}
