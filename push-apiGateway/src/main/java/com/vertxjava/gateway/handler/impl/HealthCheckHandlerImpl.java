package com.vertxjava.gateway.handler.impl;

import com.vertxjava.gateway.handler.HealthCheckHandler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.Status;
import io.vertx.servicediscovery.types.HttpEndpoint;

import java.util.List;
import java.util.Objects;

/**
 * The implementation class of HealthCheckHandler.
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-03 17:26
 **/
public class HealthCheckHandlerImpl implements HealthCheckHandler {

    // log
    private Logger logger = LoggerFactory.getLogger(HealthCheckHandlerImpl.class);
    // The service discovery instance
    private ServiceDiscovery discovery;

    public HealthCheckHandlerImpl(ServiceDiscovery discovery) {
        this.discovery = discovery;
    }

    @Override
    public void handle(Long event) {
        discovery.getRecords(r -> r.getType().equals(HttpEndpoint.TYPE) && !"api-gateway".equals(r.getMetadata().getString("type")), true, ar -> {
            if (ar.succeeded()) {
                List<Record> list = ar.result();
                list.forEach(record -> {
                    ServiceReference reference = discovery.getReference(record);
                    HttpClient client = reference.getAs(HttpClient.class);
                    client.request(HttpMethod.GET, "/health", response -> {
                        if (response.statusCode() == 200) {
                            if (!record.getStatus().equals(Status.UP)) {
                                record.setStatus(Status.UP);
                                discovery.update(record, recordAsyncResult -> {
                                    if (recordAsyncResult.succeeded()) {
                                        logger.info("Update server status success");
                                    } else {
                                        logger.error("Update server status fail,case:" + recordAsyncResult.cause());
                                    }
                                });
                            }
                        } else {
                            if (!record.getStatus().equals(Status.DOWN)) {
                                record.setStatus(Status.DOWN);
                                discovery.update(record, recordAsyncResult -> {
                                    if (recordAsyncResult.succeeded()) {
                                        logger.info("Update server status success");
                                    } else {
                                        logger.error("Update server status fail,case:" + recordAsyncResult.cause());
                                    }
                                });
                            }
                        }
                        // close resource
                        reference.release();
                    }).end();
                });
            } else {
                logger.error("Health check get records fail,case:" + ar.cause());
            }
        });

    }
}
