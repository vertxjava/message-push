package com.vertxjava.common.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.types.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
     * service records,When a component is destroyed,
     * it is used to clear the record from the service discovery.
     */
    private Set<Record> records = new ConcurrentHashSet<Record>();

    /*
     * log
     */
    private Logger log = LoggerFactory.getLogger(BaseVerticle.class);

    @Override
    public void start() throws Exception {
        /*
         * get serviceDiscovery instance, using zookeeper as back end storage.
         *
         * setAnnounceAddress():Every time a service provider is published or withdrawn,
         * an event is fired on the event bus. This event contains the record that has been modified.
         * We can get this information through eventBus.
         *
         * setName():set the name of a service discovery.
         */
        discovery = ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions()
                .setBackendConfiguration(config().getJsonObject("serviceDiscovery"))
                .setAnnounceAddress("vertxjava.discovery.announce")
                .setName("vertxjava-message"));
    }

    /**
     * publish apiGateway service
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
     * Publish a httpEndPoint service
     */
    protected Future<Void> publishHttpEndpoint(String name, String host, int port) {
        Record record = HttpEndpoint.createRecord(name, host, port, "/" + name);
        return publish(record);
    }

    // 发布eventBus服务
    protected Future<Void> publishEventBusService(String name, String address, Class serviceClass) {
        Record record = EventBusService.createRecord(name, address, serviceClass);
        return publish(record);
    }

    /*protected <T> Future<T> getProxyService(Class<T> clazz) {
        Future<T> future = Future.future();
        EventBusService.getProxy(discovery, clazz, future.completer());
        return future;
    }*/

    protected Future<Void> publishMessageSource(String name, String address) {
        Record record = MessageSource.createRecord(name, address);
        return publish(record);
    }

    protected Future<Void> publishRedisDataSource(String name,String host,int port){
        Record record = RedisDataSource.createRecord(name,new JsonObject().put("host",host).put("port",port),null);
        return publish(record);
    }

    protected Future<Void> publishMongoDataSource(String name,String host,int port,String dbName){
        Record record = MongoDataSource.createRecord(name,new JsonObject().put("host",host).put("port",port).put("db_name",dbName),null);
        return publish(record);
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
                throw new IllegalStateException("Cannot create discovery service");
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
    public void stop(Future<Void> future) throws Exception {
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
                    future.complete();
                } else {
                    future.fail(ar.cause());
                }
                discovery.close();
            });
        }

    }
}
