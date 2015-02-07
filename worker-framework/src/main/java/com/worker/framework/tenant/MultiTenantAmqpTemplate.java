package com.worker.framework.tenant;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

import com.tenant.framework.CurrentTenant;

public class MultiTenantAmqpTemplate extends RabbitTemplate {

    private CurrentTenant currentTenant;
    private String defaultRoutingKey;
    private String tenantIdHeader;
    private MessageConverter messageConverter;
    
    public MultiTenantAmqpTemplate(ConnectionFactory connectionFactory, 
                                   CurrentTenant currentTenant, 
                                   MessageConverter messageConverter,
                                   String defaultRoutingKey,
                                   String tenantIdHeader) {
        super(connectionFactory);
        this.currentTenant = currentTenant;
        this.messageConverter = messageConverter;
        this.defaultRoutingKey = defaultRoutingKey;
        this.tenantIdHeader = tenantIdHeader;
    }
    
    @Override
    public void convertAndSend(Object workMessage) throws AmqpException {
        Message message = createMessageWithTenantId(workMessage);
        super.convertAndSend(defaultRoutingKey, message);
    }
    
    @Override
    public void convertAndSend(String routingKey, Object workMessage) throws AmqpException {
        Message message = createMessageWithTenantId(workMessage);
        super.convertAndSend(routingKey, message);
    }

    private Message createMessageWithTenantId(Object workMessage) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader(tenantIdHeader, currentTenant.get());
        return messageConverter.toMessage(workMessage, messageProperties);
    }
    
}
