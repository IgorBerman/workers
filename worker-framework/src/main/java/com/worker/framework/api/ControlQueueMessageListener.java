package com.worker.framework.api;

import com.worker.shared.ControlMessage;

public interface ControlQueueMessageListener {

    void handleMessage(ControlMessage input) throws Exception;
}
