package com.worker.framework.dal.mapper;

import java.util.Date;

import com.worker.shared.WorkMessageJoinDelta;



public class WorkMessageJoinStatus {

    private String id;
    private WorkMessageJoinDelta[] pending;
    private Date releasedTimestamp;

    public WorkMessageJoinStatus() {}
    
    public WorkMessageJoinStatus(String id, WorkMessageJoinDelta[] pending) {
        this.id = id;
        this.pending = pending;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public WorkMessageJoinDelta[] getPending() {
        return pending;
    }

    public void setPending(WorkMessageJoinDelta[] pending) {
        this.pending = pending;
    }

    public Date getReleasedTimestamp() {
        return releasedTimestamp;
    }

    public void setReleasedTimestamp(Date releasedTimestamp) {
        this.releasedTimestamp = releasedTimestamp;
    }
    
}
