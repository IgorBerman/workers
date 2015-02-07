package com.worker.framework.messagehandlingchain;

import java.util.List;

import com.worker.framework.monitoring.ProcessorsFinishInTimeWatchDogAndTimer;
import com.worker.framework.monitoring.ProcessorsFinishInTimeWatchDogAndTimer.StartWithTimerContext;
import com.worker.shared.WorkMessage;

public class WatchdogProcessingTime implements WMProcessor {
    private final ProcessorsFinishInTimeWatchDogAndTimer watchdog;
    private final WMProcessor delegator;
    
    public WatchdogProcessingTime(ProcessorsFinishInTimeWatchDogAndTimer watchdog, WMProcessor delegator) {
        this.watchdog = watchdog;
        this.delegator = delegator;
    }

    @Override
    public List<WorkMessage> handle(WorkMessage input) throws Exception {
        StartWithTimerContext timer = watchdog.startProcessing(input);
        try {
            return delegator.handle(input);
        } finally {
            watchdog.finished(timer);
        }
    }
}