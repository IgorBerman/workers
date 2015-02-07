package com.tenant.sample.service;

import java.util.List;

import com.google.common.base.Optional;
import com.tenant.sample.Sample;


public interface SampleService {
    int add(Sample sample);
    void update(Sample sample);
    void remove(int id);
    
    List<Sample> readAll();
    Optional<Sample> readById(int id);
    
    void initDB();
}
