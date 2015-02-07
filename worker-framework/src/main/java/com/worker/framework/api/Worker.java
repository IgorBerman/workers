package com.worker.framework.api;

import java.util.List;

import com.worker.shared.WorkMessage;


public interface Worker {

    public boolean supportsTask(String taskName);

    public List<WorkMessage> execute(WorkMessage input) throws Exception;

}
