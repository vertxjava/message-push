package com.vertxjava.common.dataaccess;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLConnection;

import java.util.List;
import java.util.Optional;

/**
 * postgresql access
 *
 * @author jack
 * @create 2017-09-01 14:06
 **/
public class DataAccessWrapper {

    private final AsyncSQLClient client;
    private Logger logger = LoggerFactory.getLogger(DataAccessWrapper.class);

    public DataAccessWrapper(Vertx vertx, JsonObject config) {
        this.client = PostgreSQLClient.createShared(vertx, config);
    }

    protected Future<Void> update(String sql) {
        return getConnection().compose(connection -> {
            Future<Void> future = Future.future();
            connection.update(sql, ar -> {
                if (ar.succeeded())
                    future.complete();
                else
                    future.fail(ar.cause());
                connection.close();
            });
            return future;
        });
    }

    protected Future<Void> update(String sql, JsonArray params) {
        return getConnection().compose(connection -> {
            Future<Void> future = Future.future();
            connection.updateWithParams(sql, params, ar -> {
                if (ar.succeeded())
                    future.complete();
                else
                    future.fail(ar.cause());
                connection.close();
            });
            return future;
        });
    }

    protected Future<Void> execute(String sql) {
        return getConnection().compose(connection -> {
            Future<Void> future = Future.future();
            connection.execute(sql, ar -> {
                if (ar.succeeded())
                    future.complete();
                else
                    future.fail(ar.cause());
                connection.close();
            });
            return future;
        });
    }

    protected Future<Optional<JsonArray>> query(String sql) {
        return getConnection().compose(connection -> {
            Future<Optional<JsonArray>> future = Future.future();
            connection.query(sql, ar -> {
                List<JsonObject> list = ar.result().getRows();
                if (list.isEmpty()) {
                    future.complete(Optional.empty());
                } else {
                    JsonArray result = new JsonArray();
                    list.forEach(jo -> {
                        if (jo.containsKey("info")) {
                            result.add(new JsonObject(jo.getString("info")));
                        } else {
                            result.add(jo);
                        }
                    });
                    future.complete(Optional.of(result));
                }
                connection.close();
            });
            return future;
        });
    }

    protected Future<Optional<JsonArray>> query(String sql, JsonArray params) {
        return getConnection().compose(connection -> {
            Future<Optional<JsonArray>> future = Future.future();
            connection.queryWithParams(sql, params, ar -> {
                List<JsonObject> list = ar.result().getRows();
                if (list.isEmpty()) {
                    future.complete(Optional.empty());
                } else {
                    JsonArray result = new JsonArray();
                    list.forEach(jo -> {
                        if (jo.containsKey("info")) {
                            result.add(new JsonObject(jo.getString("info")));
                        } else {
                            result.add(jo);
                        }
                    });
                    future.complete(Optional.of(result));
                }
                connection.close();
            });
            return future;
        });
    }

    protected Future<Void> delete(String sql) {
        return update(sql);
    }

    protected Future<Void> delete(String sql, JsonArray params) {
        return update(sql, params);
    }

    protected Future<Void> insert(String sql) {
        return update(sql);
    }

    protected Future<Void> insert(String sql, JsonArray params) {
        return update(sql, params);
    }

    protected int calcPage(int page, int limit) {
        if (page <= 0)
            return 0;
        return limit * (page - 1);
    }

    /**
     * 异步获取SQLConnection
     *
     * @return SQLConnection
     */
    private Future<SQLConnection> getConnection() {
        Future<SQLConnection> future = Future.future();
        client.getConnection(future.completer());
        return future;
    }


}

