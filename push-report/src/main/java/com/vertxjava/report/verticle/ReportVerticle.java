package com.vertxjava.report.verticle;

import com.vertxjava.common.verticle.HttpVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Jack
 * @create 2017-12-13 14:27
 **/
public class ReportVerticle extends HttpVerticle {

    // 服务名称
    private static final String SERVER_NAME = "server-report-http";
    // 默认host
    private static final String DEFAULT_HOST = "localhost";
    // 默认port
    private static final Integer DEFAULT_PORT = 8004;
    // 默认kafka服务
    private static final String DEFAULT_KAFKA_SERVER = "127.0.0.1:9092";
    // api name
    private static final String API_NAME = "report";
    // kafka topic
    private static final String TOPIC = "topicReport";
    // kafka的发布者服务
    private KafkaProducer<Object, Object> kafkaProducer;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        final Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/report").handler(this::dataReport);
        // 创建kafka服务
        // 初始化kafka生产者服务
        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.237.128:9092,192.168.237.128:9093");
        props.put("acks", "all");
        props.put("retries", "1");
        props.put("batch.size", "1048576");
        props.put("linger.ms", "1");
        props.put("buffer.memory", "33554432");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProducer = io.vertx.kafka.client.producer.KafkaProducer.createShared(vertx, "the_Kafka", props);
        // host
        String host = config().getString("host", DEFAULT_HOST);
        // port
        Integer port = config().getInteger("port", DEFAULT_PORT);
        // 创建http服务 并发布httpEndpoint
        createHttpServer(router, host, port)
                .compose(created -> publishHttpEndpoint(SERVER_NAME, API_NAME, host, port))
                .compose(published -> deployVerticle(new MongoStoreVerticle())
                .compose(deployed -> deployVerticle(new RedisStoreVerticle()))).setHandler(ar -> {
            if (ar.succeeded()) {
                System.out.println("部署成功");
                startFuture.complete();
            } else {
                startFuture.fail(ar.cause());
                System.out.println("部署失败");
            }
        });

    }

    private void dataReport(RoutingContext context) {
        JsonObject params = context.getBodyAsJson();
        KafkaProducerRecord<Object, Object> record = KafkaProducerRecord.create(TOPIC, params.encodePrettily());
        kafkaProducer.write(record, done -> {
            if (done.succeeded()) {
                RecordMetadata recordMetadata = done.result();
                System.out.println("发送");
                System.out.println(recordMetadata.getTopic());
                context.response().end();
            } else {
                context.response().setStatusCode(500).end();
            }
        });
    }

    // 部署其他verticle
    private Future<Void> deployVerticle(RedisStoreVerticle rsv){
        Future<String> future = Future.future();
        vertx.deployVerticle(rsv,new DeploymentOptions().setConfig(config()),future.completer());
        return future.map(r -> null);
    }
    // 部署其他verticle
    private Future<Void> deployVerticle(MongoStoreVerticle msv){
        Future<String> future = Future.future();
        vertx.deployVerticle(msv,new DeploymentOptions().setConfig(config()),future.completer());
        return future.map(r -> null);
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        kafkaProducer.close();
        super.stop(stopFuture);
    }
}