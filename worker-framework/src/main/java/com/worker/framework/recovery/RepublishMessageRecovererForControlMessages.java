package com.worker.framework.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;

import com.worker.framework.api.RoutingProperties;
import com.worker.framework.tenant.MultiTenantAmqpTemplate;
import com.worker.shared.JoinedTaskSucceded;
import com.worker.shared.WorkMessage;

/**
 * Handles failures of processing control messages
 * if failed to process, releases sink message after max retries used
 * @author igor
 */
public class RepublishMessageRecovererForControlMessages extends RepublishMessageRecoverer {
    private static final Logger logger = LoggerFactory.getLogger(RepublishMessageRecovererForControlMessages.class);

    private RoutingProperties routingProperties;
    private MessageConverter messageConverter;
    private MultiTenantAmqpTemplate messagesTemplate;

    public RepublishMessageRecovererForControlMessages(RoutingProperties routingProperties, 
                                                       MultiTenantAmqpTemplate controlMessagesTemplate,
                                                       String errorExchange,
                                                       MultiTenantAmqpTemplate messagesTemplate,
                                                       MessageConverter messageConverter) {
        super(controlMessagesTemplate, errorExchange, routingProperties.getErrorQueueName());
        this.routingProperties = routingProperties;
        this.messagesTemplate = messagesTemplate;
        this.messageConverter = messageConverter;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        super.recover(message, cause);
        Object wm = messageConverter.fromMessage(message);
        if (wm instanceof JoinedTaskSucceded) {
            JoinedTaskSucceded joinMessage = (JoinedTaskSucceded)wm;
            WorkMessage joinSinkMessage = joinMessage.getJoinSinkMessage();
            logger.warn("releasing sink message since control message can't be handled max retries times " + joinSinkMessage);
            String destination = joinSinkMessage.isLowPriority() ? routingProperties.getLowQueueName() : routingProperties.getQueueName();
            messagesTemplate.convertAndSend(destination, joinSinkMessage);
        }
    }
}
