package com.worker.shared;

import java.util.Collection;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


public class JoinedTaskSucceded extends ControlMessage {
    private String joinId;
    private Set<WorkMessageJoinDelta> joinDelta;    
    private WorkMessage joinSinkMessage;
    
    
    public JoinedTaskSucceded() {
    }

    public JoinedTaskSucceded(String joinId, Set<WorkMessage> finished, Set<WorkMessage> started, WorkMessage joinSinkMessage) {
        this.joinId = joinId;
        this.joinDelta = Sets.newHashSet();
        if (finished != null) {
            Collection<WorkMessageJoinDelta> finishedTasksDelta =
                    Lists.newArrayList(Iterables.transform(finished, new Function<WorkMessage, WorkMessageJoinDelta>() {
                        @Override
                        public WorkMessageJoinDelta apply(WorkMessage input) {
                            return new WorkMessageJoinDelta(JoinState.ENDED, input.getUuid());
                        }}));
            joinDelta.addAll(finishedTasksDelta);
        }
        if (started != null) {
            Collection<WorkMessageJoinDelta> startedTasksDelta =
                    Lists.newArrayList(Iterables.transform(started, new Function<WorkMessage, WorkMessageJoinDelta>() {
                        @Override
                        public WorkMessageJoinDelta apply(WorkMessage input) {
                            return new WorkMessageJoinDelta(JoinState.STARTED, input.getUuid());
                        }}));
            joinDelta.addAll(startedTasksDelta);
        }
        this.joinSinkMessage = joinSinkMessage;
    }

    @Override
    public void accept(ControlMessageVisitor controlMessageVisitor) {
        controlMessageVisitor.visit(this);
    }

    public String getJoinId() {
        return joinId;
    }
    public void setJoinId(String joinId) {
        this.joinId = joinId;
    }
    public WorkMessage getJoinSinkMessage() {
        return joinSinkMessage;
    }
    public void setJoinSinkMessage(WorkMessage joinSinkMessage) {
        this.joinSinkMessage = joinSinkMessage;
    }
    
    public Set<WorkMessageJoinDelta> getJoinDelta() {
        return joinDelta;
    }
    public void setJoinDelta(Set<WorkMessageJoinDelta> joinDelta) {
        this.joinDelta = joinDelta;
    }

    @Override
    public String toString() {
        return "JoinedTaskSucceded [joinId=" + joinId + ", joinDelta=" + joinDelta + ", joinSinkMessage=" +
               joinSinkMessage + "]";
    }
    
    @Override
    public String toShortFormat() {
        return "JoinedSucceded [id=" + joinId + ", sink=" + joinSinkMessage.toShortFormat() + "]";
    }
    
}
