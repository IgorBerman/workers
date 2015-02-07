package com.worker.shared;

public interface ControlMessageVisitor {

    void visit(JoinedTaskSucceded taskSucceded);
    void visit(JoinedTaskFailed taskSucceded);
    void visit(FailedControlMessage failedControlMessage);
    void visit(ControlMessage taskSucceded);
}
