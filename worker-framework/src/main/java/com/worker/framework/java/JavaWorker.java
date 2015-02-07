package com.worker.framework.java;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.worker.framework.api.WorkMessageProcessor;
import com.worker.framework.api.Worker;
import com.worker.shared.WorkMessage;


@Component
public class JavaWorker implements Worker {

    @Autowired(required=false) private List<WorkMessageProcessor> processors;
    private volatile Map<String, WorkMessageProcessor> processorsMap;    

    @PostConstruct
    protected void init() {
        processorsMap = Maps.newHashMap();
        if (processors == null) {
            processors = Lists.newArrayList();
        }
        
        for (WorkMessageProcessor processor : processors) {
            String processorName = processor.getName();
            processorsMap.put(processorName, processor);
        }
    }

    @Override
    public boolean supportsTask(String taskName) {
        return processorsMap.containsKey(taskName);
    }

    @Override
    public List<WorkMessage> execute(WorkMessage input) throws Exception {
        String taskName = input.getTask();

        if (!processorsMap.containsKey(taskName)) {
            throw new RuntimeException("Recieved invalid task: " + taskName + " supported tasks: " + processorsMap.keySet());
        }
        return processorsMap.get(taskName).process(input);
    }

}
