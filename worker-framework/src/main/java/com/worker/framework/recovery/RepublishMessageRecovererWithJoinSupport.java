package com.worker.framework.recovery;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.worker.framework.api.RoutingProperties;
import com.worker.framework.tenant.MultiTenantAmqpTemplate;
import com.worker.shared.JoinedTaskFailed;
import com.worker.shared.WorkMessage;
import com.worker.shared.WorkMessagesJoinState;

/**
 * Handles failures of regular messages and supports messages with join state
 * releases join-failure control message for failed message that is part of "join"(instead of join-success) after all retries used
 * @author igor
 */
public class RepublishMessageRecovererWithJoinSupport extends RepublishMessageRecoverer {
    private static final Logger logger = LoggerFactory.getLogger(RepublishMessageRecovererWithJoinSupport.class);
    private MessageConverter messageConverter;
    private MultiTenantAmqpTemplate controlMessagesTemplate;


    //we want multitenant template to be sure we send tenant id 
    public RepublishMessageRecovererWithJoinSupport(RoutingProperties routingProperties, 
                                                    MultiTenantAmqpTemplate controlMessagesTemplate,
                                                    String errorExchange,
                                                    MultiTenantAmqpTemplate messagesTemplate,
                                                    MessageConverter messageConverter) {
     super(messagesTemplate, errorExchange, routingProperties.getErrorQueueName());
     this.controlMessagesTemplate = controlMessagesTemplate;
     this.messageConverter = messageConverter;
 }


    @Override
    public void recover(Message message, Throwable cause) {
        super.recover(message, cause);
        Object wm = messageConverter.fromMessage(message);
        if (wm instanceof WorkMessage) {
            WorkMessage workMessage = (WorkMessage)wm;
            WorkMessagesJoinState joinState = workMessage.getJoinState();
            if (joinState != null) {
                Set<WorkMessage> failedTasks = ImmutableSet.of(workMessage);
                JoinedTaskFailed joinedTaskFailed = new JoinedTaskFailed(joinState.getJoinId(), failedTasks, joinState.getSinkMessage(),
                                                                         Throwables.getStackTraceAsString(Throwables.getRootCause(cause)));
                logger.warn("publishing to control queue " + joinedTaskFailed + " to decrement join with id " + joinState.getJoinId());
                //no need in routing properties, all control messages are going to control queue
                controlMessagesTemplate.convertAndSend(joinedTaskFailed);
            }
        }
    }
}
