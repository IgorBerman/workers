package com.worker.framework.recovery;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import com.worker.framework.api.RoutingProperties;
import com.worker.framework.tenant.MultiTenantAmqpTemplate;
import com.worker.shared.JoinedTaskFailed;
import com.worker.shared.JoinedTaskSucceded;
import com.worker.shared.WorkMessage;

@RunWith(MockitoJUnitRunner.class)
public class RepublishMessageRecovererForControlMessagesTest {
    @Mock private MessageConverter messageConverter;
    @Mock private MultiTenantAmqpTemplate controlMessagesTemplate;
    @Mock private MultiTenantAmqpTemplate messagesTemplate;
    @Mock private RoutingProperties routingProperties;
    private RepublishMessageRecovererForControlMessages recoverer;
    
    @Before
    public void init() {
        when(routingProperties.getLowQueueName()).thenReturn("low-priority-queue");
        when(routingProperties.getQueueName()).thenReturn("normal-priority-queue");
        recoverer = new RepublishMessageRecovererForControlMessages(routingProperties, controlMessagesTemplate, "", messagesTemplate, messageConverter);
    }
    @Test
    public void testWhenRecoveringNotJoinMessages() {
        MessageProperties messageProperties = new MessageProperties();
        Message message = new Message("control message body".getBytes(), messageProperties);
        when(messageConverter.fromMessage(message)).thenReturn("blalal");
        
        recoverer.recover(message, new NullPointerException("some problem in control message listener"));
        
        verify(messagesTemplate, times(0)).convertAndSend(Mockito.any());
        verify(messagesTemplate, times(0)).convertAndSend(Mockito.anyString(), Mockito.any());
    }
    
    @Test
    public void testWhenRecoveringFailedJoinMessage() {
        MessageProperties messageProperties = new MessageProperties();
        Message message = new Message("control message body".getBytes(), messageProperties);
        WorkMessage joinSinkMessage = new WorkMessage("sink task", null);
        JoinedTaskFailed joinFailed = new JoinedTaskFailed("joinId", null, joinSinkMessage, "some reason");
        when(messageConverter.fromMessage(message)).thenReturn(joinFailed);
        
        recoverer.recover(message, new NullPointerException("some problem in control message listener"));
        
        verify(messagesTemplate, times(1)).convertAndSend("normal-priority-queue", joinSinkMessage);
    }
    
    @Test
    public void testWhenRecoveringFailedJoinMessageWithLowPriority() {
        MessageProperties messageProperties = new MessageProperties();
        Message message = new Message("control message body".getBytes(), messageProperties);
        WorkMessage joinSinkMessage = new WorkMessage("sink task", null);
        joinSinkMessage.setLowPriority(true);
        JoinedTaskFailed joinFailed = new JoinedTaskFailed("joinId", null, joinSinkMessage, "some reason");
        when(messageConverter.fromMessage(message)).thenReturn(joinFailed);
        
        recoverer.recover(message, new NullPointerException("some problem in control message listener"));
        
        verify(messagesTemplate, times(1)).convertAndSend("low-priority-queue", joinSinkMessage);
    }
    
    @Test
    public void testWhenRecoveringSuccessJoinMessage() {
        MessageProperties messageProperties = new MessageProperties();
        Message message = new Message("control message body".getBytes(), messageProperties);
        WorkMessage joinSinkMessage = new WorkMessage("sink task", null);
        JoinedTaskSucceded joinSucceded = new JoinedTaskSucceded("joinId", null, null, joinSinkMessage);
        when(messageConverter.fromMessage(message)).thenReturn(joinSucceded);
        
        recoverer.recover(message, new NullPointerException("some problem in control message listener"));
        
        verify(messagesTemplate, times(1)).convertAndSend("normal-priority-queue", joinSinkMessage);
    }
}
