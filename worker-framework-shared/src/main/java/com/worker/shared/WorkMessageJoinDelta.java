package com.worker.shared;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class WorkMessageJoinDelta {
    private static final String SEPARATOR = ":";
    private JoinState state;
    private String messageId;
    
    public WorkMessageJoinDelta() {
        super();
    }
    public WorkMessageJoinDelta(JoinState state, String messageId) {
        super();
        this.state = state;
        this.messageId = messageId;
    }
    public JoinState getState() {
        return state;
    }
    public void setState(JoinState state) {
        this.state = state;
    }
    public String getMessageId() {
        return messageId;
    }
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((messageId == null) ? 0 : messageId.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        WorkMessageJoinDelta other = (WorkMessageJoinDelta) obj;
        if (messageId == null) {
            if (other.messageId != null) return false;
        } else if (!messageId.equals(other.messageId)) return false;
        if (state != other.state) return false;
        return true;
    }
    
    
    @Override
    public String toString() {
        return "WMJD[s=" + state + ", mId=" + messageId + "]";
    }
    public static WorkMessageJoinDelta from(String str) {
        List<String> split = Lists.newArrayList(Splitter.on(SEPARATOR).split(str));
        JoinState state = JoinState.valueOf(split.get(0));
        String messageId = split.get(1);
        return new WorkMessageJoinDelta(state, messageId);
    }
    
    public static String to(WorkMessageJoinDelta state) {
        return Joiner.on(SEPARATOR).join(state.getState(), state.getMessageId());
    }
}
