package com.worker.shared;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class WorkMessage {
    private String uuid;
    private String task;    
    private List<WorkMessageArg> args;
    private boolean lowPriority = false;
    private boolean error = false;

    private WorkMessagesJoinState joinState;

    public WorkMessage(String task, List<WorkMessageArg> args) {
        this.uuid = UUID.randomUUID().toString();
        this.task = task;
        this.args = args;
    }

    public WorkMessage() {
        // for json deserialization
    }

    public void setTask(String task) {
        this.task = task;
    }

    public void setArgs(List<WorkMessageArg> args) {
        this.args = args;
    }

    public String getTask() {
        return task;
    }

    public List<WorkMessageArg> getArgs() {
        return args;
    }

    
    public WorkMessagesJoinState getJoinState() {
        return joinState;
    }

    public void setJoinState(WorkMessagesJoinState joinState) {
        this.joinState = joinState;
    }

    @Override
    public String toString() {
        return "WorkMessage["+uuid +", " + task + ", " + args + ", low "+lowPriority+ (joinState == null ? "" : "(j:"+joinState+")") + "]";
    }
    

    public Map<String, Object> getArgsAsMap() {
        Map<String, Object> argsMap = new HashMap<String, Object>();
        if (args != null) {
            for (WorkMessageArg arg : args) {
                argsMap.put(arg.getName(), arg.getValue());
            }
        }
        return argsMap;
    }
    
    public void setLowPriority(boolean lowPriority) {
        this.lowPriority = lowPriority;
    }
    
    public boolean isLowPriority() {
        return lowPriority;
    }
    
    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((args == null) ? 0 : args.hashCode());
        result = prime * result + (error ? 1231 : 1237);
        result = prime * result + ((joinState == null) ? 0 : joinState.hashCode());
        result = prime * result + (lowPriority ? 1231 : 1237);
        result = prime * result + ((task == null) ? 0 : task.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        WorkMessage other = (WorkMessage) obj;
        if (args == null) {
            if (other.args != null) return false;
        } else if (!args.equals(other.args)) return false;
        if (error != other.error) return false;
        if (joinState == null) {
            if (other.joinState != null) return false;
        } else if (!joinState.equals(other.joinState)) return false;
        if (lowPriority != other.lowPriority) return false;
        if (task == null) {
            if (other.task != null) return false;
        } else if (!task.equals(other.task)) return false;
        return true;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String toShortFormat() {
        return "WorkMessage["+uuid +", " + task+"]";
    }
}
