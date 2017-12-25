package com.vertxjava.common.verticle;

import com.vertxjava.servicediscovery.types.KCDataSource;
import com.vertxjava.servicediscovery.types.KPDataSource;
import com.vertxjava.servicediscovery.types.PGDataSource;
import com.vertxjava.servicediscovery.types.WebSocketEndpoint;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.*;

import java.util.*;

/**
 * The base class of all verticle.
 * Some common components are encapsulated.
 *
 * @author Jack
 * @create 2017-12-08 11:03
 * @blog http://www.vertxjava.com
 **/
public class BaseVerticle extends AbstractVerticle {

    /*
     * ServiceDiscovery
     */
    protected ServiceDiscovery discovery;

    /*
     * services records,When a component is destroyed,
     * it is used to clear the record from the services discovery.
     */
    private Set<Record> records = new ConcurrentHashSet<Record>();

    /*
     * log
     */
    private Logger log = LoggerFactory.getLogger(BaseVerticle.class);

    /**
     * publish apiGateway services
     *
     * @param host
     * @param port
     * @return
     */
    protected Future<Void> publishApiGateway(String host, int port) {
        Record record = HttpEndpoint.createRecord("ApiGateway", true, host, port, "/", null).setType("api-gateway");
        return publish(record);
    }

    /*
     * Publish a httpEndPoint services
     */
    protected Future<Void> publishHttpEndpoint(String name, String host, int port) {
        Record record = HttpEndpoint.createRecord(name, host, port, "/" + name);
        return publish(record);
    }

    protected Future<Void> publishWebSocketEndpoint(String name, String host, int port) {
        Record record = WebSocketEndpoint.createRecord(name, host, port, "/" + name,null);
        return publish(record);
    }

    // 发布eventBus服务
    protected Future<Void> publishEventBusService(String name, String address, Class serviceClass) {
        Record record = EventBusService.createRecord(name, address, serviceClass);
        return publish(record);
    }

    protected Future<Void> publishMessageSource(String name, String address) {
        Record record = MessageSource.createRecord(name, address);
        return publish(record);
    }

    protected Future<Void> publishRedisDataSource(JsonObject config) {
        Record record = RedisDataSource.createRecord(config.getString("name"), config, null);
        return publish(record);
    }

    protected Future<Void> publishMongodbDataSource(JsonObject config) {
        Record record = MongoDataSource.createRecord(config.getString("name"), config, null);
        return publish(record);
    }

    protected Future<Void> publishPGDataSource(JsonObject config) {
        Record record = PGDataSource.createRecord(config.getString("name"), config, null);
        return publish(record);
    }

    protected Future<Void> publishKCDataSource(JsonObject config) {
        Record record = KCDataSource.createRecord(config.getString("name"), config, null);
        return publish(record);
    }

    protected Future<Void> publishKPDataSource(JsonObject config) {
        Record record = KPDataSource.createRecord(config.getString("name"), config, null);
        return publish(record);
    }

    protected Future<Void> createHttpServer(Router router, String host, int port) {
        Future<HttpServer> httpServerFuture = Future.future();
        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(port, host, httpServerFuture.completer());
        return httpServerFuture.map(r -> null);
    }

    // 启用CORS支持
    protected void enableCorsSupport(Router router) {
        Set<String> allowHeaders = new HashSet<>();
        allowHeaders.add("Authorization");
        allowHeaders.add("x-requested-with");
        allowHeaders.add("Access-Control-Allow-Origin");
        allowHeaders.add("origin");
        allowHeaders.add("Content-Type");
        allowHeaders.add("accept");
        Set<HttpMethod> allowMethods = new HashSet<>();
        allowMethods.add(HttpMethod.GET);
        allowMethods.add(HttpMethod.POST);
        allowMethods.add(HttpMethod.DELETE);
        allowMethods.add(HttpMethod.PATCH);
        router.route().handler(CorsHandler.create("*")
                .allowedHeaders(allowHeaders)
                .allowedMethods(allowMethods));
    }

    /**
     * publish record
     *
     * @param record Service discovery record
     * @return Asynchronous return
     */
    private Future<Void> publish(Record record) {
        Future<Void> future = Future.future();
        if (discovery == null) {
            try {
                start();
            } catch (Exception e) {
                throw new IllegalStateException("Cannot create discovery services");
            }
        }
        discovery.publish(record, ar -> {
            if (ar.succeeded()) {
                records.add(record);
                log.info("Service <" + ar.result().getName() + "> published");
                future.complete();
            } else {
                future.fail(ar.cause());
                log.error("Service <" + ar.result().getName() + "> publish failed,case:" + ar.cause());
            }
        });
        return future;
    }



    // 根据服务名称获取httpPoint服务，多个服务实现简单的负载均衡（随机）
    protected Future<Record> getHttpEndPointByName(String name) {
        Future<Record> future = Future.future();
        discovery.getRecords(r -> r.getName().equals(name) && r.getType().equals(HttpEndpoint.TYPE), ar -> {
            if (ar.succeeded()) {
                List<Record> list = ar.result();
                if (list != null && !list.isEmpty()) {
                    if (list.size() == 1)
                        future.complete(list.get(0));
                    else
                        Collections.shuffle(list);
                    future.complete(list.get(0));
                } else {
                    future.complete(null);
                }
            } else {
                future.fail(ar.cause());
            }
        });
        return future;
    }

    // 销毁discovery服务
    @Override
    public void stop(Future<Void> future) {
        if (records.isEmpty()) {
            discovery.close();
            future.complete();
        } else {
            List<Future> futures = new ArrayList<>();
            records.forEach(record -> {
                Future<Void> cleanupFuture = Future.future();
                futures.add(cleanupFuture);
                discovery.unpublish(record.getRegistration(), cleanupFuture.completer());
            });
            CompositeFuture.all(futures).setHandler(ar -> {
                if (ar.succeeded()) {
                    records.clear();
                    future.complete();
                } else {
                    future.fail(ar.cause());
                }
                discovery.close();
            });
        }

    }
}
