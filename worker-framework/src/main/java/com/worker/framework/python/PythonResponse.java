package com.worker.framework.python;

import java.util.List;

import com.worker.shared.WorkMessage;


public class PythonResponse {
    enum Command {
        ACK,
        ERROR
    }
    private Command command;
    private String msg;
    private List<WorkMessage> triggeredTasks;
    public Command getCommand() {
        return command;
    }
    public void setCommand(Command command) {
        this.command = command;
    }
    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
    public List<WorkMessage> getTriggeredTasks() {
        return triggeredTasks;
    }
    public void setTriggeredTasks(List<WorkMessage> triggeredTasks) {
        this.triggeredTasks = triggeredTasks;
    }
    
    
}
