package com.vertxjava.report.service;

import com.vertxjava.report.service.impl.MongoServiceImpl;

public interface MongoService {
    static MongoService create(){
        return new MongoServiceImpl();
    }
}
