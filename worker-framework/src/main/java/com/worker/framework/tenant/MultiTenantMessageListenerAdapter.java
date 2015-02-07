package com.worker.framework.tenant;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.JsonMessageConverter;

import com.rabbitmq.client.Channel;
import com.tenant.framework.CurrentTenant;

public class MultiTenantMessageListenerAdapter extends MessageListenerAdapter {
    
    private CurrentTenant currentTenant;
    private String tenantIdHeader;
    
    public MultiTenantMessageListenerAdapter(Object listener, 
                                             JsonMessageConverter jsonMessageConverter,
                                             CurrentTenant currentTenant,
                                             String tenantIdHeader) {
        super(listener, jsonMessageConverter);
        this.currentTenant = currentTenant;
        this.tenantIdHeader = tenantIdHeader;
    }

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        setTenant(message);
        super.onMessage(message, channel);
    }
    
    @Override
    public void onMessage(Message message) {
        setTenant(message);
        super.onMessage(message);
    }

    private void setTenant(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        if (!headers.containsKey(tenantIdHeader)) {
            throw new RuntimeException("Missing tenant id from message header");
        }
        String tenantId = (String) headers.get(tenantIdHeader);
        currentTenant.set(tenantId);
        MDC.put("tenantId", tenantId);        
    }
    
}
