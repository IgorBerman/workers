package com.worker.framework.messagehandlingchain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.worker.framework.tenant.MultiTenantAmqpTemplate;
import com.worker.shared.JoinedTaskSucceded;
import com.worker.shared.WorkMessage;
import com.worker.shared.WorkMessagesJoinState;

@RunWith(MockitoJUnitRunner.class)
public class SubmitJoinMessageTest {
    @Mock private MultiTenantAmqpTemplate messagesTemplate;
    @Mock private WMProcessor delegator;
    @Captor private ArgumentCaptor<String> controlQueueCaptor;
    @Captor private ArgumentCaptor<Object> messageCaptor;
    private SubmitJoinMessage submitter;
    @Before
    public void init() {
        submitter = new SubmitJoinMessage(messagesTemplate, delegator);
    }
    @Test
    public void testWhenNoJoinState() throws Exception {
        WorkMessage input = new WorkMessage();
        List<WorkMessage> triggeredTasks = ImmutableList.of();
        when(delegator.handle(input)).thenReturn(triggeredTasks);
        
        submitter.handle(input);
        
        verify(delegator, times(1)).handle(input);
        verify(messagesTemplate, times(0)).convertAndSend(Mockito.anyString(), Mockito.any(WorkMessage.class));
    }
    
    @Test
    public void testWhenNoJoinStateInParentButThereIsInChildren() throws Exception {
        WorkMessage input = new WorkMessage("parent", null);
        WorkMessage sinkMessage = new WorkMessage();
        String joinId = "joinId";
        WorkMessagesJoinState childrenJoinState = new WorkMessagesJoinState(joinId, sinkMessage);
        WorkMessage e1 = new WorkMessage("child1", null);
        e1.setJoinState(childrenJoinState);
        WorkMessage e2 = new WorkMessage("child2", null);
        e2.setJoinState(childrenJoinState);
        WorkMessage e3 = new WorkMessage("child3", null);
        e3.setJoinState(childrenJoinState);
        List<WorkMessage> triggeredTasks = ImmutableList.of(e1, e2, e3);
        when(delegator.handle(input)).thenReturn(triggeredTasks);
        
        submitter.handle(input);
        
        verify(delegator, times(1)).handle(input);
        verify(messagesTemplate, times(1)).convertAndSend(messageCaptor.capture());
        
        assertTrue(messageCaptor.getValue() instanceof JoinedTaskSucceded);
        JoinedTaskSucceded message = (JoinedTaskSucceded) messageCaptor.getValue();
        
        JoinedTaskSucceded expectedMessage = new JoinedTaskSucceded(joinId, ImmutableSet.<WorkMessage>of(), Sets.newHashSet(triggeredTasks), sinkMessage);
        
        assertEquals(expectedMessage.getJoinDelta(), message.getJoinDelta());
        assertEquals(joinId, message.getJoinId());
        assertEquals(sinkMessage, message.getJoinSinkMessage());
    }
    
    @Test
    public void testWhenJoinStateWithSeveralChildrens() throws Exception {
        WorkMessage input = new WorkMessage("parent", null);
        WorkMessage sinkMessage = new WorkMessage();
        String joinId = "joinId";
        WorkMessagesJoinState joinState = new WorkMessagesJoinState(joinId, sinkMessage);
        input.setJoinState(joinState);
        WorkMessage e1 = new WorkMessage("child1", null);
        WorkMessage e2 = new WorkMessage("child2", null);
        WorkMessage e3 = new WorkMessage("child3", null);
        List<WorkMessage> triggeredTasks = ImmutableList.of(e1, e2, e3);
        when(delegator.handle(input)).thenReturn(triggeredTasks);
        
        submitter.handle(input);
        
        verify(delegator, times(1)).handle(input);
        verify(messagesTemplate, times(1)).convertAndSend(messageCaptor.capture());
        
        assertTrue(messageCaptor.getValue() instanceof JoinedTaskSucceded);
        JoinedTaskSucceded message = (JoinedTaskSucceded) messageCaptor.getValue();
        
        JoinedTaskSucceded expectedMessage = new JoinedTaskSucceded(joinId, ImmutableSet.of(input), Sets.newHashSet(triggeredTasks), sinkMessage);
        
        assertEquals(expectedMessage.getJoinDelta(), message.getJoinDelta());
        assertEquals(joinId, message.getJoinId());
        assertEquals(sinkMessage, message.getJoinSinkMessage());
    }

}
