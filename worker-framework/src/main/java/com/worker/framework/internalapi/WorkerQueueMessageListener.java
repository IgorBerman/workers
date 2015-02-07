package com.worker.framework.internalapi;

import com.worker.shared.WorkMessage;

public interface WorkerQueueMessageListener {

    void handleMessage(WorkMessage input) throws Exception;
}
