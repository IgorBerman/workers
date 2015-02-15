package com.worker.framework.java;

import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.worker.framework.api.WorkMessageProcessor;
import com.worker.shared.WorkMessage;
import com.worker.shared.WorkMessageArg;



@RunWith(value = MockitoJUnitRunner.class)
public class JavaWorkerTest {
    private static final String TASK_NAME = "TaskNameProcessor";

    @Mock private WorkMessageProcessor processor;
    @Mock private List<WorkMessageProcessor> processors;
    @Mock private Iterator<WorkMessageProcessor> mockIterator;
    @InjectMocks private JavaWorker javaWorker;

    @Before
    public void init() {
        setupProcessors();
        javaWorker.init();
    }
    
    private void setupProcessors() {
        Mockito.when(processors.iterator()).thenReturn(mockIterator);
        Mockito.when(mockIterator.hasNext()).thenReturn(true, false);
        Mockito.when(mockIterator.next()).thenReturn(processor);
        Mockito.when(processor.getName()).thenReturn(TASK_NAME);
    }

    @Test
    public void testExecute() throws Exception {
        List<WorkMessageArg> args = Lists.newArrayList();
        WorkMessage input = new WorkMessage(TASK_NAME, args);
        List<WorkMessage> triggered = Lists.newArrayList(new WorkMessage("triggered", args));
        Mockito.when(processor.process(input)).thenReturn(triggered);

        List<WorkMessage> actual = javaWorker.execute(input);
        Assert.assertEquals(triggered, actual);
    }
    
    @Test
    public void testSupportsTask() {
        Assert.assertTrue(javaWorker.supportsTask(TASK_NAME));
        Assert.assertFalse(javaWorker.supportsTask("other"));
    }

}
