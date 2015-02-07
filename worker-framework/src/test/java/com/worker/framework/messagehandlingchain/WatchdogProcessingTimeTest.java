package com.worker.framework.messagehandlingchain;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.worker.framework.monitoring.ProcessorsFinishInTimeWatchDogAndTimer;
import com.worker.framework.monitoring.ProcessorsFinishInTimeWatchDogAndTimer.StartWithTimerContext;
import com.worker.shared.WorkMessage;

@RunWith(MockitoJUnitRunner.class)
public class WatchdogProcessingTimeTest {
    @Mock private ProcessorsFinishInTimeWatchDogAndTimer watchdog;
    @Mock private WMProcessor delegator;
    @InjectMocks private WatchdogProcessingTime watchdogMessage;
   
    @Test
    public void testRegisterAtSuccess() throws Exception {
        WorkMessage input = new WorkMessage();
        StartWithTimerContext timer = new StartWithTimerContext(1, null, input, "jenkins");
        when(watchdog.startProcessing(input)).thenReturn(timer);
        
        watchdogMessage.handle(input);
        
        verify(delegator, times(1)).handle(input);
        verify(watchdog, times(1)).startProcessing(input);
        verify(watchdog, times(1)).finished(timer);
    }
    
    @Test(expected=RuntimeException.class)
    public void testRegisterAtFailure() throws Exception {
        WorkMessage input = new WorkMessage();
        StartWithTimerContext timer = new StartWithTimerContext(1, null, input, "jenkins");
        when(watchdog.startProcessing(input)).thenReturn(timer);
        when(delegator.handle(input)).thenThrow(new RuntimeException("unsupported"));
        try {
            watchdogMessage.handle(input);
        } finally {
            verify(delegator, times(1)).handle(input);
            verify(watchdog, times(1)).startProcessing(input);
            verify(watchdog, times(1)).finished(timer);
        }
    }

}
