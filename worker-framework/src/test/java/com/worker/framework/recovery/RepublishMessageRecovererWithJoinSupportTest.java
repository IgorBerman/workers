package com.worker.framework.recovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import com.google.common.collect.ImmutableSet;
import com.worker.framework.internalapi.RoutingProperties;
import com.worker.framework.tenant.MultiTenantAmqpTemplate;
import com.worker.shared.JoinedTaskFailed;
import com.worker.shared.WorkMessage;
import com.worker.shared.WorkMessagesJoinState;

@RunWith(MockitoJUnitRunner.class)
public class RepublishMessageRecovererWithJoinSupportTest {
    @Mock private MultiTenantAmqpTemplate controlMessageTemplate;
    @Mock private MultiTenantAmqpTemplate messageTemplate;
    @Mock private MessageConverter messageConverter;
    @Mock private RoutingProperties routingProperties; 
    @Captor private ArgumentCaptor<Object> messageCaptor;
    @Captor private ArgumentCaptor <String> queueNameCaptor;
    
    
    private RepublishMessageRecovererWithJoinSupport recoverer;
    
    @Before
    public void init() {
        recoverer = new RepublishMessageRecovererWithJoinSupport(routingProperties, controlMessageTemplate, "DEFAULT_EXCHANGE", messageTemplate, messageConverter);
    }
    
    @Test
    public void testWhenControlMessageFailed() {
        MessageProperties messageProperties = new MessageProperties();
        Message message = new Message("body".getBytes(), messageProperties);
        
        
        when(messageConverter.fromMessage(message)).thenReturn(new JoinedTaskFailed());
        recoverer.recover(message, new NullPointerException("bug"));
        verify(controlMessageTemplate, times(0)).convertAndSend(Mockito.any());
        verify(controlMessageTemplate, times(0)).convertAndSend(Mockito.anyString(), Mockito.any());
    }
    
    @Test
    public void testWhenWorkerMessageWithoutJoinStateFailed() {
        MessageProperties messageProperties = new MessageProperties();
        Message message = new Message("body".getBytes(), messageProperties);
        
        
        when(messageConverter.fromMessage(message)).thenReturn(new WorkMessage());
        recoverer.recover(message, new NullPointerException("bug"));
        verify(controlMessageTemplate, times(0)).convertAndSend(Mockito.any());
        verify(controlMessageTemplate, times(0)).convertAndSend(Mockito.anyString(), Mockito.any());
    }
    
    @Test
    public void testWhenWorkerMessageWithJoinStateFailed() {
        MessageProperties messageProperties = new MessageProperties();
        Message message = new Message("body".getBytes(), messageProperties);
        
        
        WorkMessage workMessage = new WorkMessage();
        String joinId = "join id";
        WorkMessage sinkMessage = new WorkMessage();
        workMessage.setJoinState(new WorkMessagesJoinState(joinId, sinkMessage));
        when(messageConverter.fromMessage(message)).thenReturn(workMessage);
        recoverer.recover(message, new NullPointerException("bug"));
        verify(controlMessageTemplate, times(1)).convertAndSend(messageCaptor.capture());
        verify(controlMessageTemplate, times(0)).convertAndSend(Mockito.anyString(), Mockito.any());
        assertTrue(messageCaptor.getValue() instanceof JoinedTaskFailed);
        JoinedTaskFailed failedMessage = (JoinedTaskFailed) messageCaptor.getValue();
        JoinedTaskFailed expectedFailedMessage = new JoinedTaskFailed(joinId, ImmutableSet.of(workMessage), sinkMessage, "some reason");
        assertEquals(joinId, failedMessage.getJoinId());
        assertEquals(expectedFailedMessage.getJoinDelta(), failedMessage.getJoinDelta());
        assertEquals(sinkMessage, failedMessage.getJoinSinkMessage());
    }

}
