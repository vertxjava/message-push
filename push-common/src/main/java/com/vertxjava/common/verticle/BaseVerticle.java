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
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.servicediscovery.types.HttpEndpoint;

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

    // ServiceDiscovery instance 
    protected ServiceDiscovery discovery;
    // log
    private Logger logger = LoggerFactory.getLogger(BaseVerticle.class);
    /*
     * Services records,When a component is destroyed,
     * it is used to clear the record from the services discovery.
     */
    private Set<Record> records = new ConcurrentHashSet<Record>();

    @Override
    public void start() throws Exception {
        // Get serviceDiscovery instance, using redis as back end storage.
        discovery = ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions()
                .setBackendConfiguration(config().getJsonObject("serviceDiscovery"))
                .setAnnounceAddress("vertx.discovery.announce").setName("discovery"));
    }

    /**
     * Publish a httpEndPoint services.
     *
     * @param name The service name
     * @param host The host
     * @param port The port
     * @return Asynchronous return void
     */
    protected Future<Void> publishHttpEndpoint(String name, String host, int port) {
        return publishHttpEndpoint(name, host, port, null);
    }

    /**
     * Publish a httpEndPoint services by type and metadata.
     *
     * @param name     The service name
     * @param host     The host
     * @param port     The port
     * @param metadata The metadata
     * @return Asynchronous return void
     */
    protected Future<Void> publishHttpEndpoint(String name, String host, int port, JsonObject metadata) {
        Record record = HttpEndpoint.createRecord(name, host, port, "/" + name).setType(HttpEndpoint.TYPE).setMetadata(metadata);
        return publish(record);
    }

    /**
     *
     * @param name The name
     * @param address The address
     * @param serviceClass Class
     * @return Asynchronous return void
     */
    protected Future<Void> publishEventBusService(String name, String address, Class serviceClass) {
        Record record = EventBusService.createRecord(name, address, serviceClass);
        return publish(record);
    }

    /**
     * Publish record
     *
     * @param record Service discovery record
     * @return Asynchronous return void
     */
    private Future<Void> publish(Record record) {
        return Future.future(future -> {
            if (discovery == null) {
                try {
                    start();
                } catch (Exception e) {
                    throw new IllegalStateException("Cannot create discovery instance");
                }
            }
            discovery.getRecord(r -> record.getName().equals(r.getName()), r -> {
                if (r.succeeded()) {
                    // Exclude duplication of service
                    if (r.result() == null) {
                        discovery.publish(record, ar -> {
                            if (ar.succeeded()) {
                                future.complete();
                                records.add(record);
                                logger.info("Service <" + ar.result().getName() + "> published");
                            } else {
                                logger.error("Service <" + ar.result().getName() + "> publish failed,case:" + ar.cause());
                                future.fail(ar.cause());
                            }
                        });
                    } else {
                        future.complete();
                    }
                }
            });

        });
    }

    /**
     * Get httpEndPoint record by service name.
     * Simple load balancing is implemented.
     *
     * @param name The service name
     * @return Asynchronous return record
     */
    protected Future<Record> getHttpEndPoint(String name) {
        return Future.future(future -> {
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
        });
    }

    /**
     * Get all records,include a service with a state of DOWN
     *
     * @return Asynchronous return List<Record>
     */
    protected Future<List<Record>> getAllRecord() {
        return Future.future(future -> {
            discovery.getRecords(Objects::nonNull, true, ar -> {
                if (ar.succeeded()) {
                    future.complete(ar.result());
                } else {
                    future.fail(ar.cause());
                }
            });
        });
    }

    /**
     * Stop verticle
     *
     * @param future Future<Void>
     */
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
