package com.worker.framework.internalapi;

public interface RoutingProperties {
    String getErrorQueueName();

    String getControlQueueName();
    
    String getLowQueueName();
    
    String getQueueName();
}
