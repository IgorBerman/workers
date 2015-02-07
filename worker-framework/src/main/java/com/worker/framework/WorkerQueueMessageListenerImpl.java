package com.worker.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.worker.framework.api.WorkerQueueMessageListener;
import com.worker.framework.messagehandlingchain.WMProcessor;
import com.worker.shared.WorkMessage;


public class WorkerQueueMessageListenerImpl implements WorkerQueueMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(WorkerQueueMessageListenerImpl.class);
    private final WMProcessor chain;
    public WorkerQueueMessageListenerImpl(WMProcessor chain) {
        this.chain = chain;
    }
    @Override
    public void handleMessage(WorkMessage input) throws Exception {
        logger.info("Handling " + input.toShortFormat());
        chain.handle(input);
    }
}
