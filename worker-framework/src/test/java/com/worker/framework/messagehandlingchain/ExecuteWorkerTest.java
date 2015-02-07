package com.worker.framework.messagehandlingchain;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.worker.framework.api.Worker;
import com.worker.shared.WorkMessage;
import com.worker.shared.WorkMessageArg;

@RunWith(MockitoJUnitRunner.class)
public class ExecuteWorkerTest {
    private static final String TASK_NAME = "task";
    private static final String OTHER_NAME = "other";

    @Mock private Worker worker;
    @Mock private List<Worker> workers;
    @Mock private Iterator<Worker> mockIterator;
    @InjectMocks ExecuteWorker executeWorker;
    
    @Before
    public void init() {
        setupWorkers();
    }

    private void setupWorkers() {
        when(workers.iterator()).thenReturn(mockIterator);
        when(mockIterator.hasNext()).thenReturn(true, false);
        when(mockIterator.next()).thenReturn(worker);
        when(worker.supportsTask(TASK_NAME)).thenReturn(true);
        when(worker.supportsTask(OTHER_NAME)).thenReturn(false);
    }
    
    @Test
    public void testSuccess() throws Exception {
        List<WorkMessageArg> args = Lists.newArrayList();
        WorkMessage input = new WorkMessage(TASK_NAME, args);
        WorkMessage triggered = new WorkMessage(OTHER_NAME, args);
        ImmutableList<WorkMessage> triggeredTasks = ImmutableList.of(triggered);
        when(worker.execute(input)).thenReturn(triggeredTasks);
        assertEquals(triggeredTasks, executeWorker.handle(input));
        
    }
    
    @Test(expected=NullPointerException.class)
    public void testFailurePropogated() throws Exception {
        List<WorkMessageArg> args = Lists.newArrayList();
        WorkMessage input = new WorkMessage(TASK_NAME, args);
        when(worker.execute(input)).thenThrow(new NullPointerException("bug in code"));        
        executeWorker.handle(input);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNoProcessorFound() throws Exception {
        List<WorkMessageArg> args = Lists.newArrayList();
        WorkMessage input = new WorkMessage(OTHER_NAME, args);
        List<WorkMessage> triggeredTasks = Lists.newArrayList();
        when(worker.execute(input)).thenReturn(triggeredTasks);
        executeWorker.handle(input);
    }

}
