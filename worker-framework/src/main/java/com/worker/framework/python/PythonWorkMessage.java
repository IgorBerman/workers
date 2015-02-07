package com.worker.framework.python;

import com.worker.shared.WorkMessage;


public class PythonWorkMessage {
    
    private String tenantId;
    
    private WorkMessage workMessage;

    public PythonWorkMessage() {}
   
    public PythonWorkMessage(String tenantId, WorkMessage input) {
        this.tenantId = tenantId;
        this.workMessage = input;
    }    
    
    public PythonWorkMessage(WorkMessage input) {
        this.workMessage = input;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public WorkMessage getWorkMessage() {
        return workMessage;
    }

    public void setWorkMessage(WorkMessage workMessage) {
        this.workMessage = workMessage;
    }
}
