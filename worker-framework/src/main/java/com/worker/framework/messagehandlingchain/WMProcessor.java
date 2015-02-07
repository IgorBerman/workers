package com.worker.framework.messagehandlingchain;

import java.util.List;

import com.worker.shared.WorkMessage;

public interface WMProcessor {
    List<WorkMessage> handle(WorkMessage input) throws Exception;
}