package com.vertxjava.pull.verticle;

/**
 * @author Jack
 * @create 2017-12-26 16:34
 **/
public class ADMatherVerticle {
    // 从redis中获取广告信息
    // 根据机型匹配广告

    // 广告同步
    // 1 根据广告状态获取新广告
    // 2 发送完成将广告信息置为完成
    // 3 每次pull 只需要上报_id 和 ack 再根据_id在redis或者mongo中查询设备信息

}
