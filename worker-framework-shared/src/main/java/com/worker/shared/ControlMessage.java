package com.worker.shared;

import org.codehaus.jackson.annotate.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="@type")
public abstract class ControlMessage {
    //see visitor pattern
    //very important to implement this method in every subclass
    public abstract void accept(ControlMessageVisitor controlMessageVisitor);
    public abstract String toShortFormat();
}
