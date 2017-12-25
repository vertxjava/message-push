package com.vertxjava.gateway.handler.impl;

import com.vertxjava.common.http.HttpResponse;
import com.vertxjava.gateway.handler.DispatchHandler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.types.HttpEndpoint;

import java.util.Collections;
import java.util.List;

/**
 * @author Jack
 * @create 2017-12-19 16:39
 **/
public class DispatchHandlerImpl extends HttpResponse implements DispatchHandler {

    private ServiceDiscovery discovery;
    private Logger log = LoggerFactory.getLogger(DispatchHandlerImpl.class);

    public DispatchHandlerImpl(ServiceDiscovery discovery) {
        this.discovery = discovery;
    }

    @Override
    public void handle(RoutingContext context) {
        String path = context.request().uri();
        if (path.length() <= 5) {
            notFound(context);
            return;
        }
        String apiName = path.substring(5).split("/")[0];
        discovery.getRecords(record -> record.getName().equals(apiName) && record.getType().equals(HttpEndpoint.TYPE), ar -> {
            if (ar.succeeded()) {
                List<Record> list = ar.result();
                Record record = null;
                if (list != null && !list.isEmpty()) {
                    if (list.size() == 1) {
                        record = list.get(0);
                    } else {
                        Collections.shuffle(list);
                        record = list.get(0);
                    }
                    String newPath = path.substring(5 + apiName.length());
                    ServiceReference reference = discovery.getReference(record);
                    HttpClient client = reference.getAs(HttpClient.class);
                    // webSocket
                    System.out.println(context.request().path());
                    if (context.request().path().equals("/pull")){
                        ServerWebSocket webSocket = context.request().upgrade();
                        webSocket.handler(buffer -> {
                            System.out.println("客户端发来的数据为:"+buffer.toString());
                            System.out.println("请求后端");
                            client.websocket(newPath,websocket -> {
                                websocket.write(buffer);
                                websocket.handler(replay -> {
                                    System.out.println("收到回复:"+replay.toString());
                                    webSocket.write(replay);
                                    webSocket.close();
                                });
                            });
                        });

                    }else{// http
                        HttpClientRequest hcr = client.request(context.request().method(), newPath, response -> {
                            response.bodyHandler(body -> {
                                // 获取HttpServerResponse
                                HttpServerResponse hsr = context.response().setStatusCode(response.statusCode());
                                // 将响应请求头设置到context中
                                response.headers().forEach(header -> hsr.putHeader(header.getKey(), header.getValue()));
                                // 返回响应
                                hsr.end(body);
                                // 关闭资源
                                reference.release();
                            });
                        });
                        // 设置请求头
                        context.request().headers().forEach(header -> hcr.putHeader(header.getKey(), header.getValue()));
                        // 发送请求
                        if (context.getBody() == null)
                            hcr.end();
                        else
                            hcr.end(context.getBody());
                    }
                } else {
                    notFound(context);
                }
            } else {
                serverError(context);
                log.error("get services discovery records fail,case:" + ar.cause());
            }
        });
    }
}
