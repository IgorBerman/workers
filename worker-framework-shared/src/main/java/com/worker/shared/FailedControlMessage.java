package com.worker.shared;

//primary for testing scenarios when handling control message fails
public class FailedControlMessage extends ControlMessage {
    private String failureReason;

    public FailedControlMessage() {
    }
    public FailedControlMessage(String failureReason) {
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
        return "FailedControlMessage [failureReason=" + failureReason + "]";
    }
    @Override
    public String toShortFormat() {
        return toString();
    }
}
