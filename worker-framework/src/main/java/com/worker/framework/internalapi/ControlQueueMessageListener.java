package com.worker.framework.internalapi;

import com.worker.shared.ControlMessage;

public interface ControlQueueMessageListener {

    void handleMessage(ControlMessage input) throws Exception;
}
