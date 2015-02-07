package com.worker.framework.control;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Throwables;
import com.worker.framework.internalapi.ControlQueueMessageListener;
import com.worker.shared.ControlMessage;
import com.worker.shared.ControlMessageVisitor;


@Component
public class ControlQueueMessageListenerImpl implements ControlQueueMessageListener {
    private Logger logger = LoggerFactory.getLogger(ControlQueueMessageListenerImpl.class);
    private @Inject ControlMessageVisitor visitor;

    @Override
    public void handleMessage(ControlMessage message) throws Exception {
        logger.info("Handling control message : " + message.toShortFormat());
        try {
            message.accept(visitor);
        } catch (Exception e) {
            logger.error("failure in handling control message: " + message, Throwables.getRootCause(e));            
            throw e;
        }
    }
}
