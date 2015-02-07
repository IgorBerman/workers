package com.test.processors;

import java.util.List;

import org.springframework.stereotype.Component;

import com.worker.framework.api.BaseWorkMessageProcessor;
import com.worker.shared.WorkMessage;

@Component
public class TestFailingAfterDelayProcessor extends BaseWorkMessageProcessor {

    @Override
    public List<WorkMessage> process(WorkMessage input) throws Exception {
        Integer delayInMillis = (Integer)input.getArgs().iterator().next().getValue();
        Thread.sleep(delayInMillis);
        throw new RuntimeException("Failing task!! after " + delayInMillis + " millis");
    }
    
}
