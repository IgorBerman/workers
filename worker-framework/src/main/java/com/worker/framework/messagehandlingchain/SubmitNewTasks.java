package com.worker.framework.messagehandlingchain;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.worker.framework.internalapi.RoutingProperties;
import com.worker.framework.tenant.MultiTenantAmqpTemplate;
import com.worker.shared.WorkMessage;

public class SubmitNewTasks implements WMProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SubmitNewTasks.class);
    private final RoutingProperties routingProperties;
    private final MultiTenantAmqpTemplate workerMessagesTemplate;
    private final WMProcessor delegator;
    
    //we want multitenant template to be sure we send tenant id 
    public SubmitNewTasks(RoutingProperties routingProperties, MultiTenantAmqpTemplate workerMessagesTemplate, WMProcessor delegator) {
        this.routingProperties = routingProperties;
        this.workerMessagesTemplate = workerMessagesTemplate;
        this.delegator = delegator;
    }
    @Override
    public List<WorkMessage> handle(WorkMessage input) throws Exception {
            List<WorkMessage> triggeredTasks = delegator.handle(input);
            submitTriggeredTasks(input, triggeredTasks);
            return triggeredTasks;
    }
    private void submitTriggeredTasks(WorkMessage input, List<WorkMessage> triggeredTasks) {
        if (!triggeredTasks.isEmpty()) {
            logger.info(input.getTask() + " triggered " + triggeredTasks.size() + " tasks");
        }
        for (WorkMessage task : triggeredTasks) {
            String destination = getDestination(task);
            workerMessagesTemplate.convertAndSend(destination, task);
        }
    }
    private String getDestination(WorkMessage task) {
        if (task.isError()) {
            return routingProperties.getErrorQueueName();
        }
        if (task.isLowPriority()) {
            return routingProperties.getLowQueueName();
        }
        return routingProperties.getQueueName();
    }
}