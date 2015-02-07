package com.worker.shared;

public class WorkMessagesJoinState {
    private String joinId;
    private WorkMessage sinkMessage;

    public WorkMessagesJoinState() {
        // for json deserialization
    }

    public WorkMessagesJoinState(String joinId, WorkMessage sinkMessage) {
        this.joinId = joinId;
        this.sinkMessage = sinkMessage;
    }

    public String getJoinId() {
        return joinId;
    }

    public void setJoinId(String joinId) {
        this.joinId = joinId;
    }

    public WorkMessage getSinkMessage() {
        return sinkMessage;
    }

    public void setSinkMessage(WorkMessage sinkMessage) {
        this.sinkMessage = sinkMessage;
    }

    @Override
    public String toString() {
        return "WorkMessagesJoinState [joinId=" + joinId + ", sinkMessage=" + sinkMessage + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((joinId == null) ? 0 : joinId.hashCode());
        result = prime * result + ((sinkMessage == null) ? 0 : sinkMessage.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        WorkMessagesJoinState other = (WorkMessagesJoinState) obj;
        if (joinId == null) {
            if (other.joinId != null) return false;
        } else if (!joinId.equals(other.joinId)) return false;
        if (sinkMessage == null) {
            if (other.sinkMessage != null) return false;
        } else if (!sinkMessage.equals(other.sinkMessage)) return false;
        return true;
    }
}
