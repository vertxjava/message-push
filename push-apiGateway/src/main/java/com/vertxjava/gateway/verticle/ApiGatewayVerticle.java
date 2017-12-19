package com.vertxjava.gateway.verticle;

import com.vertxjava.common.verticle.HttpVerticle;
import com.vertxjava.gateway.handler.DispatchHandler;
import com.vertxjava.gateway.handler.WebSocketHandler;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class ApiGatewayVerticle extends HttpVerticle {

    // 默认host
    private static final String DEFAULT_HOST = "localhost";
    // 默认port
    private static final Integer DEFAULT_PORT = 8001;
    // redis 默认host
    private static final String DEFAULT_HOST_REDIS = "localhost";
    // redis 默认port
    private static final Integer DEFAULT_PORT_REDIS = 6379;
    // mongodb 默认host
    private static final String DEFAULT_HOST_MONGODB = "localhost";
    // mongodb 默认port
    private static final Integer DEFAULT_PORT_MONGODB = 27017;
    // redis data source name
    private static final String DEFAULT_REDIS_DATA_SOURCE_NAME = "redisServer";
    // mongodb data source name
    private static final String DEFAULT_MONGODB_DATA_SOURCE_NAME = "mongodbServer";
    // log
    private Logger log = LoggerFactory.getLogger(ApiGatewayVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        // host
        String host = config().getString("host", DEFAULT_HOST);
        // 端口
        int port = config().getInteger("port", DEFAULT_PORT);
        // redis host
        String redisHost = config().getJsonObject("redisDataSource").getString("host", DEFAULT_HOST_REDIS);
        int redisPort = config().getJsonObject("redisDataSource").getInteger("port", DEFAULT_PORT_REDIS);
        String redisName = config().getJsonObject("redisDataSource").getString("name", DEFAULT_REDIS_DATA_SOURCE_NAME);


        String mongodbHost = config().getJsonObject("mongodbDataSource").getString("host", DEFAULT_HOST_MONGODB);
        int mongodbPort = config().getJsonObject("mongodbDataSource").getInteger("port", DEFAULT_PORT_MONGODB);
        String mongodbName = config().getJsonObject("mongodbDataSource").getString("name", DEFAULT_MONGODB_DATA_SOURCE_NAME);

        // 路由
        Router router = Router.router(vertx);
        // 支持从body中获取数据
        router.route().handler(BodyHandler.create());
        // 将/api/请求进行转发
        router.route("/api/*").handler(DispatchHandler.create(discovery));
        // 支持静态资源的访问
        router.route("/*").handler(StaticHandler.create());
        // 处理异常

        router.route().failureHandler(context -> {

        });

        // 创建
        vertx.createHttpServer().websocketHandler(WebSocketHandler.create(discovery)).requestHandler(router::accept).listen(port, host, ar -> {
            if (ar.succeeded()) {
                log.info("create http server success,listen on " + port);
                // 发布网关服务
                publishApiGateway(host, port)
                        .compose(published -> publishRedisDataSource(redisName, redisHost, redisPort))
                        .compose(published -> publishMongoDataSource(mongodbName,mongodbHost,mongodbPort,"push"))
                        .setHandler(r -> {
                            if (r.succeeded()) {
                                log.info("ssssss");
                            } else {
                                log.error("eeeee");
                            }
                        });
            } else {
                log.error("create http server fail,case:" + ar.cause());
                startFuture.fail(ar.cause());
            }
        });

    }
}

