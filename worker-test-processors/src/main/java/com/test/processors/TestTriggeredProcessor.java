package com.test.processors;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.worker.framework.api.BaseWorkMessageProcessor;
import com.worker.shared.WorkMessage;

@Component
public class TestTriggeredProcessor extends BaseWorkMessageProcessor {
	private static final Logger logger = LoggerFactory.getLogger(TestTriggeredProcessor.class.getName());

    @Override
    public List<WorkMessage> process(WorkMessage input) throws Exception {
    	logger.info("Got " + input.getTask());
        if (input.getArgs() != null && !input.getArgs().isEmpty()) {
            Integer delayInMillis = (Integer)input.getArgs().iterator().next().getValue();
            Thread.sleep(delayInMillis);
        }
        return Lists.newArrayList();
    }
    
}
