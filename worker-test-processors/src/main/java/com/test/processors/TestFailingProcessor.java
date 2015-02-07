package com.test.processors;

import java.util.List;

import org.springframework.stereotype.Component;

import com.worker.framework.api.BaseWorkMessageProcessor;
import com.worker.shared.WorkMessage;

@Component
public class TestFailingProcessor extends BaseWorkMessageProcessor {

    @Override
    public List<WorkMessage> process(WorkMessage input) throws Exception {
        throw new RuntimeException("Failing task!!");
    }
    
}
