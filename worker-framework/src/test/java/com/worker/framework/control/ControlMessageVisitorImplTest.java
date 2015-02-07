package com.worker.framework.control;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.core.AmqpTemplate;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.worker.framework.api.RoutingProperties;
import com.worker.framework.service.WorkMessageJoinService;
import com.worker.shared.JoinState;
import com.worker.shared.JoinedTaskFailed;
import com.worker.shared.JoinedTaskSucceded;
import com.worker.shared.WorkMessage;
import com.worker.shared.WorkMessageJoinDelta;

@RunWith(MockitoJUnitRunner.class)
public class ControlMessageVisitorImplTest {
    @Mock private AmqpTemplate messageTemplate;
    @Mock private WorkMessageJoinService joinService;
    @Mock private RoutingProperties routingProperties;
    @InjectMocks ControlMessageVisitorImpl visitor;
    
    @Before 
    public void init() {
        when(routingProperties.getLowQueueName()).thenReturn("low-priority-queue");
        when(routingProperties.getQueueName()).thenReturn("normal-priority-queue");
    }
    
    @Test
    public void testConcurrentInitializationOfJoinState() {
        String joinId = "join id";
        Set<WorkMessageJoinDelta> joinDelta = ImmutableSet.of(new WorkMessageJoinDelta(JoinState.STARTED, "1"));
        WorkMessage sinkMessage = new WorkMessage();
        JoinedTaskSucceded message = new JoinedTaskSucceded(joinId, null, null, sinkMessage);
        message.setJoinDelta(joinDelta);
        Optional<Date>  result = Optional.of(new Date());
        when(joinService.joinAndGet(joinId, joinDelta)).thenThrow(new ConcurrentModificationException("conc inserts")).thenReturn(result);
        
        visitor.visit(message);
        
        verify(messageTemplate, times(1)).convertAndSend("normal-priority-queue", sinkMessage);
    }
    
    @Test
    public void testNotAllJoinedYet() {
        String joinId = "join id";
        Set<WorkMessageJoinDelta> joinDelta = ImmutableSet.of(new WorkMessageJoinDelta(JoinState.STARTED, "1"));
        JoinedTaskSucceded message = new JoinedTaskSucceded(joinId, null, null, new WorkMessage());
        message.setJoinDelta(joinDelta);
        Optional<Date>  result = Optional.absent();
        when(joinService.joinAndGet(joinId, joinDelta)).thenReturn(result);
        visitor.visit(message);
        verify(messageTemplate, times(0)).convertAndSend(Mockito.any());
        //verify(messageTemplate, times(0)).convertAndSend(Mockito.anyString(), Mockito.any());
    }
    
    @Test
    public void testAllJoined() {
        String joinId = "join id";
        WorkMessage sinkMessage = new WorkMessage();
        Set<WorkMessageJoinDelta> joinDelta = ImmutableSet.of(new WorkMessageJoinDelta(JoinState.STARTED, "1"));
        JoinedTaskSucceded message = new JoinedTaskSucceded(joinId, null, null, sinkMessage);
        message.setJoinDelta(joinDelta);
        Optional<Date>  result = Optional.of(new Date());
        when(joinService.joinAndGet(joinId, joinDelta)).thenReturn(result);
        visitor.visit(message);
        verify(messageTemplate, times(1)).convertAndSend("normal-priority-queue", sinkMessage);
    }
    
    @Test
    public void testAllJoinedWhenSinkMessageIsLowPriority() {
        String joinId = "join id";
        WorkMessage sinkMessage = new WorkMessage();
        sinkMessage.setLowPriority(true);
        Set<WorkMessageJoinDelta> joinDelta = ImmutableSet.of(new WorkMessageJoinDelta(JoinState.STARTED, "1"));
        JoinedTaskSucceded message = new JoinedTaskSucceded(joinId, null, null, sinkMessage);
        message.setJoinDelta(joinDelta);
        Optional<Date>  result = Optional.of(new Date());
        when(joinService.joinAndGet(joinId, joinDelta)).thenReturn(result);
        visitor.visit(message);
        verify(messageTemplate, times(1)).convertAndSend("low-priority-queue", sinkMessage);
    }
    
    @Test
    public void testFailure() {
        String joinId = "join id";
        Set<WorkMessageJoinDelta> joinDelta = ImmutableSet.of(new WorkMessageJoinDelta(JoinState.STARTED, "1"));
        JoinedTaskFailed message = new JoinedTaskFailed(joinId, null, new WorkMessage(), "error");
        message.setJoinDelta(joinDelta);
        Optional<Date>  result = Optional.absent();
        when(joinService.joinAndGet(joinId, joinDelta)).thenReturn(result);
        visitor.visit(message);
        verify(messageTemplate, times(0)).convertAndSend(Mockito.any());
        //verify(messageTemplate, times(0)).convertAndSend(Mockito.anyString(), Mockito.any());
    }

}
