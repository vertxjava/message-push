package com.vertxjava.postgresql.impl;

import com.vertxjava.postgresql.SQLFClient;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-09 13:52
 **/
public class SQLFClientImpl implements SQLFClient {

    private SQLClient sqlClient;

    public SQLFClientImpl(Vertx vertx, JsonObject config) {
        sqlClient = PostgreSQLClient.createShared(vertx, config);
    }

    @Override
    public Future<Void> update(String sql) {
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

    @Override
    public Future<Void> update(String sql, JsonArray params) {
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

    @Override
    public Future<Void> execute(String sql) {
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

    @Override
    public Future<Optional<JsonArray>> query(String sql) {
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

    @Override
    public Future<Optional<List<JsonObject>>> queryToList(String sql) {
        return getConnection().compose(connection -> {
            Future<Optional<List<JsonObject>>> future = Future.future();
            connection.query(sql, ar -> {
                List<JsonObject> list = ar.result().getRows();
                if (list.isEmpty()) {
                    future.complete(Optional.empty());
                } else {
                    List<JsonObject> result = new ArrayList<JsonObject>();
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

    @Override
    public Future<Optional<JsonArray>> query(String sql, JsonArray params) {
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

    @Override
    public Future<Void> delete(String sql) {
        return update(sql);
    }

    @Override
    public Future<Void> delete(String sql, JsonArray params) {
        return update(sql, params);
    }

    @Override
    public Future<Void> insert(String sql) {
        return update(sql);
    }

    @Override
    public Future<Void> insert(String sql, JsonArray params) {
        return update(sql, params);
    }

    @Override
    public Future<SQLConnection> getConnection() {
        return Future.future(f -> sqlClient.getConnection(f.completer()));
    }
}
