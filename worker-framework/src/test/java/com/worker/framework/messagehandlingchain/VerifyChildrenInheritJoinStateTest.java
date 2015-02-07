package com.worker.framework.messagehandlingchain;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.worker.shared.WorkMessage;
import com.worker.shared.WorkMessagesJoinState;

@RunWith(MockitoJUnitRunner.class)
public class VerifyChildrenInheritJoinStateTest {
    @Mock private WMProcessor delegator;
    @InjectMocks private VerifyChildrenInheritJoinState verifier;
    
    @Test
    public void testNoJoinState() throws Exception {
        WorkMessage input = new WorkMessage();
        
        verifier.handle(input);
        
        verify(delegator, times(1)).handle(input);
    }
    
    @Test
    public void testWithJoinStateAndChildrenForgot() throws Exception {
        WorkMessage input = new WorkMessage();
        WorkMessagesJoinState joinState = new WorkMessagesJoinState();
        input.setJoinState(joinState);
        List<WorkMessage> children = ImmutableList.of(new WorkMessage(), new WorkMessage());
        when(delegator.handle(input)).thenReturn(children);
        
        verifier.handle(input);
        
        verify(delegator, times(1)).handle(input);
        assertEquals(joinState, children.get(0).getJoinState());
        assertEquals(joinState, children.get(1).getJoinState());
    }
    
    @Test
    public void testWithJoinStateAndChildrenDidntForgot() throws Exception {
        WorkMessage input = new WorkMessage();
        WorkMessagesJoinState joinState = new WorkMessagesJoinState("1", new WorkMessage());
        input.setJoinState(joinState);
        WorkMessage firstChild = new WorkMessage();
        firstChild.setJoinState(joinState);
        WorkMessage secondChild = new WorkMessage();
        secondChild.setJoinState(joinState);
        List<WorkMessage> children = ImmutableList.of(firstChild, secondChild);
        when(delegator.handle(input)).thenReturn(children);
        
        verifier.handle(input);
        
        verify(delegator, times(1)).handle(input);
        assertEquals(joinState, children.get(0).getJoinState());
        assertEquals(joinState, children.get(1).getJoinState());
    }
    
    @Test(expected=IllegalStateException.class)
    public void testWithJoinStateAndChildrenDidntForgotButChangedJoinId() throws Exception {
        WorkMessage input = new WorkMessage();
        WorkMessagesJoinState joinState = new WorkMessagesJoinState("1", null);
        input.setJoinState(joinState);
        WorkMessage firstChild = new WorkMessage();
        firstChild.setJoinState(new WorkMessagesJoinState("2", null));
        List<WorkMessage> children = ImmutableList.of(firstChild);
        when(delegator.handle(input)).thenReturn(children);
        
        verifier.handle(input);
        
        verify(delegator, times(1)).handle(input);
        assertEquals(joinState, children.get(0).getJoinState());
        assertEquals(joinState, children.get(1).getJoinState());
    }
    
    @Test(expected=IllegalStateException.class)
    public void testWithJoinStateAndChildrenDidntForgotButChangedSinkTaskId() throws Exception {
        WorkMessage input = new WorkMessage();
        WorkMessagesJoinState joinState = new WorkMessagesJoinState("1", new WorkMessage("task2", null));
        input.setJoinState(joinState);
        WorkMessage firstChild = new WorkMessage();
        firstChild.setJoinState(new WorkMessagesJoinState("1", new WorkMessage("task1", null)));
        List<WorkMessage> children = ImmutableList.of(firstChild);
        when(delegator.handle(input)).thenReturn(children);
        
        verifier.handle(input);
        
        verify(delegator, times(1)).handle(input);
        assertEquals(joinState, children.get(0).getJoinState());
    }
    
    @Test
    public void testParentWithoutJoinStateAndChildrenHave() throws Exception {
        WorkMessage input = new WorkMessage();
        WorkMessagesJoinState joinState = new WorkMessagesJoinState("1", new WorkMessage("task2", null));
        WorkMessage firstChild = new WorkMessage();
        firstChild.setJoinState(joinState);
        List<WorkMessage> children = ImmutableList.of(firstChild);
        when(delegator.handle(input)).thenReturn(children);
        
        verifier.handle(input);
        
        verify(delegator, times(1)).handle(input);
        assertEquals(joinState, children.get(0).getJoinState());
    }

}
