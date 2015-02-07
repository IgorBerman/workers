package com.worker.shared;

import java.util.HashSet;
import java.util.Set;

public class JoinedTaskFailed extends JoinedTaskSucceded {
    private String failureReason;

    public JoinedTaskFailed() {
        super();
    }
    public JoinedTaskFailed(String joinId, Set<WorkMessage> failed, WorkMessage joinSinkMessage, String failureReason) {
        super(joinId, failed, new HashSet<WorkMessage>(), joinSinkMessage);
        this.failureReason = failureReason;
    }
    @Override
    public void accept(ControlMessageVisitor controlMessageVisitor) {
        controlMessageVisitor.visit(this);
    }
    public String getFailureReason() {
        return failureReason;
    }
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    @Override
    public String toString() {
        return "JoinedTaskFailed [failureReason=" + failureReason + ", getJoinId()=" + getJoinId() +
               ", getJoinSinkMessage()=" + getJoinSinkMessage() + ", getJoinDelta()=" + getJoinDelta() + "]";
    }
}
