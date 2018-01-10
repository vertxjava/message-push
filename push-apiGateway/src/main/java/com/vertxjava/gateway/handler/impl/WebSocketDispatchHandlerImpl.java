package com.vertxjava.gateway.handler.impl;

import com.vertxjava.common.service.EventBusService;
import com.vertxjava.gateway.handler.WebSocketDispatchHandler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.types.HttpEndpoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The implementation class of WebSocketDispatchHandler.
 *
 * @author <a href="http://www.vertxjava.com">Jack</a>
 * @create 2018-01-03 17:26
 **/
public class WebSocketDispatchHandlerImpl implements WebSocketDispatchHandler {

    private static final String MESSAGE_TOPIC = "topic_message_down";
    private Map<String, ServerWebSocket> connectionMap = new HashMap<>();
    private Map<String, Long> connectionTimeMap = new HashMap<>();
    private Map<String, String> pidAndSocketIdMap = new HashMap<>();

    // log
    private Logger logger = LoggerFactory.getLogger(WebSocketDispatchHandlerImpl.class);
    // The service discovery instance
    private ServiceDiscovery discovery;
    // The eventBusService instance
    private EventBusService eventBusService;
    private boolean isCheck = false;

    public WebSocketDispatchHandlerImpl(Vertx vertx, ServiceDiscovery discovery, JsonObject config) {
        this.discovery = discovery;
        eventBusService = EventBusService.create(vertx);
        Map<String, String> kcConfig = new HashMap<>();
        kcConfig.put("bootstrap.servers", config.getJsonObject("kafkaConfig").getString("servers"));
        kcConfig.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kcConfig.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kcConfig.put("group.id", config.getJsonObject("kafkaConfig").getString("groupId"));
        kcConfig.put("enable.auto.commit", "false");
        kcConfig.put("auto.offset.reset", "earliest");
        KafkaConsumer<String, String> kafkaConsumer = KafkaConsumer.create(vertx, kcConfig);
        init(vertx, kafkaConsumer);
    }

    private void init(Vertx vertx, KafkaConsumer<String, String> kafkaConsumer) {
        vertx.setPeriodic(10000, r -> {
            if (isCheck) {
                return;
            }
            isCheck = true;
            connectionTimeMap.entrySet().iterator().forEachRemaining(entry -> {
                String pid = entry.getKey();
                ServerWebSocket socket = this.connectionMap.get(pid);
                socket.close();
                connectionTimeMap.remove(pid);
                connectionTimeMap.remove(pid);
                pidAndSocketIdMap.entrySet().iterator().forEachRemaining(stringEntry -> {
                    if (stringEntry.getValue().equals(pid)) {
                        pidAndSocketIdMap.remove(stringEntry.getKey());
                    }
                });
            });
            isCheck = false;
        });
        kafkaConsumer.subscribe(MESSAGE_TOPIC);
        kafkaConsumer.handler(record -> {
            JsonObject message = new JsonObject(record.value());
            String pid = message.getString("pid");
            ServerWebSocket socket = connectionMap.get(pid);
            if (socket != null)
                socket.writeTextMessage(message.getJsonArray("message").encode());
            kafkaConsumer.commit();
        });
    }

    @Override
    public void handle(ServerWebSocket socket) {
        String socketId = socket.binaryHandlerID();
        String path = socket.path();
        // The "/socket/" length
        if (path.length() <= 8) {
            socket.reject();
            return;
        }
        String serverName = path.substring(8).split("/")[0];
        socket.handler(buffer -> {
            discovery.getRecords(record -> record.getName().equals(serverName) && record.getType().equals(HttpEndpoint.TYPE), ar -> {
                if (ar.succeeded()) {
                    List<Record> list = ar.result();
                    if (list != null && !list.isEmpty()) {
                        // Store socket
                        connectionMap.put(buffer.toJsonObject().getString("pid"), socket);
                        connectionTimeMap.put(buffer.toJsonObject().getString("pid"), System.currentTimeMillis());
                        pidAndSocketIdMap.put(socketId, buffer.toJsonObject().getString("pid"));
                        Record record;
                        if (list.size() == 1) {
                            record = list.get(0);
                        } else {
                            Collections.shuffle(list);
                            record = list.get(0);
                        }
                        String newPath = path.substring(8 + serverName.length());
                        ServiceReference reference = discovery.getReference(record);
                        HttpClient client = reference.getAs(HttpClient.class);
                        HttpClientRequest hcr = client.request(HttpMethod.POST, newPath, response -> {
                            if (response.statusCode() == 200) {
                                logger.info("Http request is successful");
                                eventBusService.publishEventbusLog("api-gateway", "INFO", "Http request is successful");
                            } else {
                                logger.error("Http request is failed!");
                            }
                            reference.release();
                        });
                        hcr.end(buffer);
                    } else {
                        socket.close();
                    }
                } else {
                    logger.error("Get services discovery records fail,case:" + ar.cause());
                    eventBusService.publishEventbusLog("api-gateway", "ERROR", "Get services discovery records,case:" + ar.cause());
                    socket.close();
                }
            });
        });

        // listen socket close
        socket.closeHandler(r -> {
            String pid = pidAndSocketIdMap.get(socketId);
            connectionMap.remove(pid);
            connectionTimeMap.remove(pid);
        });
    }

}
