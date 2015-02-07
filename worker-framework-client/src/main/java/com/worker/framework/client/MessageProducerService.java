package com.worker.framework.client;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;
import com.worker.shared.JoinedTaskSucceded;
import com.worker.shared.WorkMessage;
import com.worker.shared.WorkMessagesJoinState;


@Service
public class MessageProducerService {
    private final Logger logger = LoggerFactory.getLogger(MessageProducerService.class);
    @Inject private ProducerProperties properties;
    @Inject private AmqpTemplate template;
    @Inject private MessageConverter converter;
    
    public void submit(String project, WorkMessage workMessage) throws IOException {
        submitWithSink(project, workMessage, null);
    }

    public void submitWithSink(String project, WorkMessage workMessage, WorkMessage sinkMessage) throws IOException {
        if (sinkMessage != null) {
            sinkMessage.setLowPriority(workMessage.isLowPriority());
            String joinId = sinkMessage.getTask() + System.currentTimeMillis();
            workMessage.setJoinState(new WorkMessagesJoinState(joinId, sinkMessage));
            Set<WorkMessage> started = ImmutableSet.of(workMessage);
            Set<WorkMessage> finished = ImmutableSet.of();
            JoinedTaskSucceded virtualSuccess = new JoinedTaskSucceded(joinId, finished, started, sinkMessage);
            Message message = buildMessageWithTenantIdHeader(project, virtualSuccess);
            logger.info("submitting " + message + " into " + properties.getControlQueueName());
            template.convertAndSend(properties.getControlQueueName(), message);
        }
        Message message = buildMessageWithTenantIdHeader(project, workMessage);
        String destQueueName = workMessage.isLowPriority() ? properties.getLowQueueName() : properties.getQueueName();
        logger.info("submitting " + message + " into " + destQueueName);
        template.convertAndSend(destQueueName, message);
    }

    private Message buildMessageWithTenantIdHeader(String project, Object o) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader(properties.getTenantIdHeader(), project);
        Message message = converter.toMessage(o, messageProperties);
        return message;
    }
}
