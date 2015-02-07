package com.worker.framework.service;

import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;
import com.worker.framework.api.JoinStateIdGenerator;

@Service
public class JoinStateIdGeneratorImpl implements JoinStateIdGenerator {

    final private String seperator = "_";

    @Override
    public String generateUniqueId(String taskName, String middle) {
        return Joiner.on(seperator).join(taskName, middle, System.currentTimeMillis());
    }

    @Override
    public String generateUniqueId(String taskName) {
        return Joiner.on(seperator).join(taskName, System.currentTimeMillis());
    }
}
