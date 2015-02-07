package com.test.processors;

import java.util.List;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.worker.framework.api.BaseWorkMessageProcessor;
import com.worker.shared.WorkMessage;

@Component
public class TestTriggeredProcessor extends BaseWorkMessageProcessor {

    @Override
    public List<WorkMessage> process(WorkMessage input) throws Exception {
        if (input.getArgs() != null && !input.getArgs().isEmpty()) {
            Integer delayInMillis = (Integer)input.getArgs().iterator().next().getValue();
            Thread.sleep(delayInMillis);
        }
        return Lists.newArrayList();
    }
    
}
