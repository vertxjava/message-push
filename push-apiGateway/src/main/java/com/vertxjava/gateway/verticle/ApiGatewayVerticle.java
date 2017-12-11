package com.vertxjava.gateway.verticle;

import com.vertxjava.common.HttpVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;

import java.util.List;
import java.util.Optional;

public class ApiGatewayVerticle extends HttpVerticle {

    // 默认host
    private static final String DEFAULT_HOST = "localhost";
    // 默认port
    private static final Integer DEFAULT_PORT = 8001;
    private Logger log = LoggerFactory.getLogger(ApiGatewayVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        // host
        String host = config().getString("host", DEFAULT_HOST);
        // 端口
        int port = config().getInteger("port", DEFAULT_PORT);
        // 路由
        Router router = Router.router(vertx);
        // 支持从body中获取数据
        router.route().handler(BodyHandler.create());
        // 将/api/请求进行转发
        router.route("/api/*").handler(this::dispatch);
        // 支持静态资源的访问
        router.route("/*").handler(StaticHandler.create());
        // 创建http监听
        vertx.createHttpServer().requestHandler(router::accept).listen(port, host, ar -> {
            if (ar.succeeded()) {
                // 发布网关服务
                publishApiGateway(host, port);
                log.info("API Gateway 发布成功，监听端口为：" + port);
            } else {
                log.info("API Gateway 发布失败，原因：" + ar.cause());
                startFuture.fail(ar.cause());
            }
        });

    }

    private void dispatch(RoutingContext context) {
        // 获取所有http端点服务
        getAllHttpEndpoints().setHandler(ar -> {
            if (ar.succeeded()) {
                List<Record> records = ar.result();
                // 获取请求绝对路径
                String path = context.request().uri();
                // 5 : /api/的长度，拦截非法请求
                if (path.length() <= 5) {
                    notFound(context);
                    return;
                }
                // API名称
                String apiName = path.substring(5).split("/")[0];
                // findAny() 方法实现简单的负载均衡
                Optional<Record> record = records.stream().filter(rec -> apiName.equals(rec.getMetadata().getString("api.name"))).findAny();
                if (record.isPresent()) {
                    String newPath = path.substring(5 + apiName.length());
                    // 根据record获取HttpClient
                    HttpClient client = discovery.getReference(record.get()).get();
                    HttpClientRequest hcr = client.request(context.request().method(), newPath, response -> {
                        response.bodyHandler(body -> {
                            // 获取HttpServerResponse
                            HttpServerResponse hsr = context.response().setStatusCode(response.statusCode());
                            // 将响应请求头设置到context中
                            response.headers().forEach(header -> hsr.putHeader(header.getKey(), header.getValue()));
                            // 返回响应
                            hsr.end(body);
                            // 关闭资源
                            ServiceDiscovery.releaseServiceObject(discovery, client);
                        });
                    });
                    // 设置请求头
                    context.request().headers().forEach(header -> hcr.putHeader(header.getKey(), header.getValue()));
                    // 发送请求
                    if (context.getBody() == null)
                        hcr.end();
                    else
                        hcr.end(context.getBody());
                } else {
                    notFound(context);
                }
            } else {
                serverError(context);
                log.error("获取http端点服务失败，原因：" + ar.cause());
            }
        });
    }

}

