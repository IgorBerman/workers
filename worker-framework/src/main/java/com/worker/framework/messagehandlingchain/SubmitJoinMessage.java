package com.worker.framework.messagehandlingchain;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.worker.framework.tenant.MultiTenantAmqpTemplate;
import com.worker.shared.JoinedTaskSucceded;
import com.worker.shared.WorkMessage;
import com.worker.shared.WorkMessagesJoinState;


public class SubmitJoinMessage implements WMProcessor {
    private final MultiTenantAmqpTemplate controlMessagesTemplate;
    private final WMProcessor delegator;

    // we want multitenant template to be sure we send tenant id
    public SubmitJoinMessage(MultiTenantAmqpTemplate controlMessagesTemplate, WMProcessor delegator) {
        this.controlMessagesTemplate = controlMessagesTemplate;
        this.delegator = delegator;
    }

    @Override
    public List<WorkMessage> handle(WorkMessage input) throws Exception {
        List<WorkMessage> triggeredTasks = delegator.handle(input);
        Set<WorkMessage> finishedTasks = Sets.newHashSet();
        Set<WorkMessage> startedTasks = Sets.newHashSet();
        if (input.getJoinState() != null) {
            finishedTasks.add(input);
            startedTasks.addAll(triggeredTasks);//if parent has choin state it means children also have join state(even if not specified)
        } else {//sometimes only children will have join state
            for (WorkMessage tt : triggeredTasks) {
                if (tt.getJoinState() != null) {
                    startedTasks.add(tt);
                }
            }
        }
        if (!finishedTasks.isEmpty()) {
            WorkMessagesJoinState workMessagesJoinState = input.getJoinState();
            JoinedTaskSucceded joinMessage =
                    new JoinedTaskSucceded(workMessagesJoinState.getJoinId(), finishedTasks, startedTasks,
                                           workMessagesJoinState.getSinkMessage());
            controlMessagesTemplate.convertAndSend(joinMessage);
        } else if (!startedTasks.isEmpty()) {//when only children have join state
            WorkMessagesJoinState workMessagesJoinState = startedTasks.iterator().next().getJoinState();
            JoinedTaskSucceded joinMessage =
                    new JoinedTaskSucceded(workMessagesJoinState.getJoinId(), finishedTasks, startedTasks,
                                           workMessagesJoinState.getSinkMessage());
            controlMessagesTemplate.convertAndSend(joinMessage);
        }
        
        return triggeredTasks;
    }
}
