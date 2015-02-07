package com.worker.framework.api;

import java.util.List;

import com.worker.shared.WorkMessage;


public interface WorkMessageProcessor {

    public List<WorkMessage> process(WorkMessage input) throws Exception;
    
    public String getName();

}
