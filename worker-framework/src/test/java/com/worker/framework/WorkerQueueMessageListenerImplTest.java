package com.worker.framework;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.worker.framework.messagehandlingchain.WMProcessor;
import com.worker.shared.WorkMessage;
import com.worker.shared.WorkMessageArg;


@RunWith(value = MockitoJUnitRunner.class)
public class WorkerQueueMessageListenerImplTest {
    @Mock private WMProcessor chain;
    private WorkerQueueMessageListenerImpl listener;
    @Before
    public void init() {
        listener = new WorkerQueueMessageListenerImpl(chain);
    }
    @Test
    public void testValidInput() throws Exception {
        List<WorkMessageArg> args = Lists.newArrayList();
        WorkMessage input = new WorkMessage("test", args);
        listener.handleMessage(input);
        verify(chain, times(1)).handle(input);
    }

    @Test(expected  = NullPointerException.class)
    public void testErrorPropagated() throws Exception {
        List<WorkMessageArg> args = Lists.newArrayList();
        WorkMessage input = new WorkMessage("test", args);
        when(chain.handle(input)).thenThrow(new NullPointerException("some error"));
        listener.handleMessage(input);
    }
}
