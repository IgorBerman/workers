package com.worker.framework.api;

public interface RoutingProperties {
    String getErrorQueueName();

    String getControlQueueName();
    
    String getLowQueueName();
    
    String getQueueName();
}
