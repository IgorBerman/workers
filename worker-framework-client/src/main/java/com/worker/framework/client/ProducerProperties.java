package com.worker.framework.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.worker.shared.WorkersConstants;

@Component
public class ProducerProperties {
    
    @Value("${queue.hotsname}") private String queueHostName;
    
    @Value("${queue.password}") private String queuePassword;
    
    @Value("${queue.username}") private String queueUsername;
    
    @Value("${queue.worker.name}") private String queueName;
        
    @Value("${retry.max.interval}") private int retryMaxInterval;
    
    @Value("${retry.multiplier}") private double retryMultiplier;
    
    @Value("${retry.initial.interval}") private int retryInitialInterval;
            
    @Value("${tenant.id.header}") private String tenantIdHeader;

    public String getQueueUsername() {
        return queueUsername;
    }

    public String getQueuePassword() {
        return queuePassword;
    }

    public String getQueueHostName() {
        return queueHostName;
    }

    public long getRetryInitialInterval() {
        return retryInitialInterval;
    }

    public double getRetryMultiplier() {
        return retryMultiplier;
    }

    public long getRetryMaxInterval() {
        return retryMaxInterval;
    }

    public String getQueueName() {
        return queueName;
    }

    public String getTenantIdHeader() {
        return tenantIdHeader;
    }

    public String getControlQueueName() {
        return WorkersConstants.DEFAULT_CONTROL_QUEUE_PREFIX + queueName;
    }
    
    public String getLowQueueName() {
        return WorkersConstants.DEFAUL_LOW_QUEUE_PREFIX + queueName;
    }
}
