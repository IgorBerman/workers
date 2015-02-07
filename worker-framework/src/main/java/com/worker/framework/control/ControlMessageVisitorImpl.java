package com.worker.framework.control;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.worker.framework.internalapi.RoutingProperties;
import com.worker.framework.service.WorkMessageJoinService;
import com.worker.shared.ControlMessage;
import com.worker.shared.ControlMessageVisitor;
import com.worker.shared.FailedControlMessage;
import com.worker.shared.JoinedTaskFailed;
import com.worker.shared.JoinedTaskSucceded;
import com.worker.shared.WorkMessage;
import com.worker.shared.WorkMessageJoinDelta;


@Component
public class ControlMessageVisitorImpl implements ControlMessageVisitor {
    private static final Logger logger = LoggerFactory.getLogger(ControlMessageVisitorImpl.class);
    @Inject private RoutingProperties routingProperties;
    @Inject private AmqpTemplate messagesTemplate;
    @Inject private WorkMessageJoinService joinService;

    public void visit(JoinedTaskSucceded message) {
        // logger.debug(message);
        handleJoinDelta(message);
    }

    public void visit(JoinedTaskFailed message) {
        logger.error("To be joined task failed with error: " + message.getFailureReason());
        handleJoinDelta(message);
    }

    private void handleJoinDelta(JoinedTaskSucceded message) {
        String joinId = message.getJoinId();
        Set<WorkMessageJoinDelta> joinDelta = message.getJoinDelta();
        Optional<Date> releaseDate = Optional.absent();
        try {
            releaseDate = joinService.joinAndGet(joinId, joinDelta);
        } catch (ConcurrentModificationException e) {
            logger.debug("got concurrent insert for join state, retrying " + e.getMessage());
            releaseDate = joinService.joinAndGet(joinId, joinDelta);
        }
        if (releaseDate.isPresent()) {// on re-transmits of message that made a release we will get multiple releases as
                                      // well..
            WorkMessage joinSinkMessage = message.getJoinSinkMessage();
            logger.info("Sink release - all tasks reached join " + joinSinkMessage.toShortFormat());
            logger.debug("Sink release - all tasks reached join " + joinSinkMessage);
            String destQueue =
                    joinSinkMessage.isLowPriority() ? routingProperties.getLowQueueName()
                                                   : routingProperties.getQueueName();
            messagesTemplate.convertAndSend(destQueue, joinSinkMessage);
        }
    }

    @Override
    public void visit(ControlMessage m) {
        throw new IllegalArgumentException(m + " of type ControlMessage which is abstract, expected specific type");
    }

    @Override
    public void visit(FailedControlMessage failedControlMessage) {
        throw new IllegalStateException(failedControlMessage.getFailureReason());
    }
}
