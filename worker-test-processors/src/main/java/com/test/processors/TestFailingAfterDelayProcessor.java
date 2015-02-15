package com.test.processors;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.worker.framework.api.BaseWorkMessageProcessor;
import com.worker.shared.WorkMessage;

@Component
public class TestFailingAfterDelayProcessor extends BaseWorkMessageProcessor {
	private static final Logger logger = LoggerFactory.getLogger(TestFailingAfterDelayProcessor.class.getName());
    @Override
    public List<WorkMessage> process(WorkMessage input) throws Exception {
    	logger.info("Got " + input.getTask());
        Integer delayInMillis = (Integer)input.getArgs().iterator().next().getValue();
        Thread.sleep(delayInMillis);
        throw new RuntimeException("Failing task!! after " + delayInMillis + " millis");
        
    }
    
}
