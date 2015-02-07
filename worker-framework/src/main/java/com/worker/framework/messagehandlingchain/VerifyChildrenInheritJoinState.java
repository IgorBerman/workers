package com.worker.framework.messagehandlingchain;

import java.util.List;

import com.google.common.base.Objects;
import com.worker.shared.WorkMessage;
import com.worker.shared.WorkMessagesJoinState;

public class VerifyChildrenInheritJoinState implements WMProcessor {
    private final WMProcessor delegator;
    
    public VerifyChildrenInheritJoinState(WMProcessor delegator) {
        this.delegator = delegator;
    }

    @Override
    public List<WorkMessage> handle(WorkMessage input) throws Exception {
        List<WorkMessage> triggeredTasks = delegator.handle(input);
        WorkMessagesJoinState joinState = input.getJoinState();
        if (joinState != null) {
            for (WorkMessage task : triggeredTasks) {
                if (task.getJoinState() == null) {
                    task.setJoinState(joinState);
                } else if (!Objects.equal(joinState.getJoinId(), task.getJoinState().getJoinId())) {
                    throw new IllegalStateException("triggered task " + task + " changed join id from " + joinState.getJoinId() + " to " + task.getJoinState().getJoinId());
                } else if (!Objects.equal(joinState.getSinkMessage().getTask(), task.getJoinState().getSinkMessage().getTask())){
                    throw new IllegalStateException("triggered task " + task + " changed sink task from " + joinState.getSinkMessage().getTask() + " to " + task.getJoinState().getSinkMessage().getTask());
                }
            }
        }
        return triggeredTasks;
    }
}