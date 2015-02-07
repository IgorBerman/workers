package com.worker.framework.api;

import com.worker.shared.WorkMessage;

public interface WorkerQueueMessageListener {

    void handleMessage(WorkMessage input) throws Exception;
}
