package com.vertxjava.report.service;

import com.vertxjava.report.service.impl.RedisServiceImpl;

public interface RedisService {
    static RedisService create(){
        return new RedisServiceImpl();
    }
}
