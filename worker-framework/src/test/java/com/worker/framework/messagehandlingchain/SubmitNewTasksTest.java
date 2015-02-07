package com.worker.framework.messagehandlingchain;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.worker.framework.api.RoutingProperties;
import com.worker.framework.tenant.MultiTenantAmqpTemplate;
import com.worker.shared.WorkMessage;

@RunWith(MockitoJUnitRunner.class)
public class SubmitNewTasksTest {
    @Mock private MultiTenantAmqpTemplate workerMessagesTemplate;
    @Mock private WMProcessor delegator;
    @Mock private RoutingProperties routingProperties;
    @Captor private ArgumentCaptor<Object> messageCaptor;
    
    @InjectMocks private SubmitNewTasks submitter;
    
    @Before
    public void init() {
       when(routingProperties.getLowQueueName()).thenReturn("low-priority-queue");
       when(routingProperties.getQueueName()).thenReturn("normal-priority-queue");
       when(routingProperties.getErrorQueueName()).thenReturn("error-queue");
    }

    @Test
    public void testWhenNoChildren() throws Exception {
        WorkMessage input = new WorkMessage();
        submitter.handle(input);
        verify(delegator, times(1)).handle(input);
        verify(workerMessagesTemplate, times(0)).convertAndSend(Mockito.any());
        verify(workerMessagesTemplate, times(0)).convertAndSend(Mockito.anyString(), Mockito.any());
    }
    
    @Test
    public void testWhenChildren() throws Exception {
        WorkMessage input = new WorkMessage();
        List<WorkMessage> children = ImmutableList.of(new WorkMessage(), new WorkMessage());
        when(delegator.handle(input)).thenReturn(children);
        submitter.handle(input);
        verify(delegator, times(1)).handle(input);
        verify(workerMessagesTemplate, times(children.size())).convertAndSend(eq("normal-priority-queue"), messageCaptor.capture());
        assertEquals(messageCaptor.getAllValues(), children);
    }
    
    @Test
    public void testWhenChildrenAtLowPriority() throws Exception {
        WorkMessage input = new WorkMessage("input",null);
        WorkMessage child1 = new WorkMessage("child1", null);
        child1.setLowPriority(true);
        WorkMessage child2 = new WorkMessage("child2", null);
        List<WorkMessage> children = ImmutableList.of(child1, child2);
        when(delegator.handle(input)).thenReturn(children);
        submitter.handle(input);
        verify(delegator, times(1)).handle(input);
        verify(workerMessagesTemplate).convertAndSend("low-priority-queue", child1);
        verify(workerMessagesTemplate).convertAndSend("normal-priority-queue", child2);
    }
    
    @Test
    public void testWhenOneChildIsError() throws Exception {
        WorkMessage input = new WorkMessage("input",null);
        WorkMessage child1 = new WorkMessage("child1", null);
        child1.setError(true);
        WorkMessage child2 = new WorkMessage("child2", null);
        List<WorkMessage> children = ImmutableList.of(child1, child2);
        when(delegator.handle(input)).thenReturn(children);
        submitter.handle(input);
        verify(delegator, times(1)).handle(input);
        verify(workerMessagesTemplate).convertAndSend("error-queue", child1);
        verify(workerMessagesTemplate).convertAndSend("normal-priority-queue", child2);
    }

}
