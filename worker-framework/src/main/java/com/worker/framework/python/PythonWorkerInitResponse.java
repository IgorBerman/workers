package com.worker.framework.python;

import java.util.List;

import com.google.common.collect.Lists;


public class PythonWorkerInitResponse {

    private Number pid;
    private List<String> supportedTasks = Lists.newArrayList();
    private String msg;
    
    public PythonWorkerInitResponse() {
        
    }

    public PythonWorkerInitResponse(Number pid) {
        this.pid = pid;
    }

    public Number getPid() {
        return pid;
    }
    public void setPid(Number pid) {
        this.pid = pid;
    }

    public List<String> getSupportedTasks() {
        return supportedTasks;
    }

    public void setSupportedTasks(List<String> supportedTasks) {
        this.supportedTasks = supportedTasks;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
    
}
