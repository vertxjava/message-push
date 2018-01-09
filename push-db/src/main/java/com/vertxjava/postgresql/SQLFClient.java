package com.vertxjava.postgresql;

import com.vertxjava.postgresql.impl.SQLFClientImpl;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

import java.util.List;
import java.util.Optional;

/**
 * Provide access to postgresql, and use Future to wrap the results
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-09 13:52
 **/
public interface SQLFClient {

    static SQLFClient create(Vertx vertx, JsonObject config) {
        return new SQLFClientImpl(vertx, config);
    }

    /**
     * Update by sql
     *
     * @param sql the statement to execute
     * @return Using Future to get the return value
     */
    Future<Void> update(String sql);

    /**
     * Update by sql and params
     *
     * @param sql    the statement to execute
     * @param params the arguments to the statement
     * @return Using Future to get the return value
     */
    Future<Void> update(String sql, JsonArray params);

    /**
     * Execute sql
     *
     * @param sql the statement to execute
     * @return Using Future to get the return value
     */
    Future<Void> execute(String sql);

    /**
     * According to the SQL query
     *
     * @param sql the statement to execute
     * @return Using Future to get the return value
     */
    Future<Optional<JsonArray>> query(String sql);

    /**
     * According to the SQL query, and return to List<JsonObject>
     *
     * @param sql the statement to execute
     * @return Using Future to get the return value
     */
    Future<Optional<List<JsonObject>>> queryToList(String sql);

    /**
     * According to the SQL query, and return to Optional<JsonArray>
     *
     * @param sql    the statement to execute
     * @param params the arguments to the statement
     * @return Using Future to get the return value
     */
    Future<Optional<JsonArray>> query(String sql, JsonArray params);

    /**
     * Delete by sql
     *
     * @param sql the statement to execute
     * @return Using Future to get the return value
     */
    Future<Void> delete(String sql);

    /**
     * Delete by sql and params
     *
     * @param sql    the statement to execute
     * @param params the arguments to the statement
     * @return Using Future to get the return value
     */
    Future<Void> delete(String sql, JsonArray params);

    /**
     * Insert by sql
     *
     * @param sql the statement to execute
     * @return Using Future to get the return value
     */
    Future<Void> insert(String sql);

    /**
     * Insert by sql and params
     *
     * @param sql    the statement to execute
     * @param params the arguments to the statement
     * @return Using Future to get the return value
     */
    Future<Void> insert(String sql, JsonArray params);

    /**
     * Number of pages
     *
     * @param page  Current page
     * @param limit Page size
     * @return Query start count
     */
    default int calcPage(int page, int limit) {
        return page <= 0 ? 0 : limit * (page - 1);
    }

    /**
     * Returns a connection that can be used to perform SQL operations on.
     * It's important to remember to close the connection when you are done, so it is returned to the pool.
     *
     * @return Using Future to get the return value
     */
    Future<SQLConnection> getConnection();


}
