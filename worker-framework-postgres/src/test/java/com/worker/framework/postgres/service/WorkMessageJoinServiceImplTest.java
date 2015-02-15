package com.worker.framework.postgres.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.dao.DuplicateKeyException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.worker.framework.postgres.mapper.WorkMessageJoinMapper;
import com.worker.framework.postgres.mapper.WorkMessageJoinStatus;
import com.worker.shared.JoinState;
import com.worker.shared.WorkMessageJoinDelta;

@RunWith(MockitoJUnitRunner.class)
public class WorkMessageJoinServiceImplTest {
    @Mock private WorkMessageJoinMapper joinMapper;
    @InjectMocks private WorkMessageJoinServiceImpl service;
    @Captor private ArgumentCaptor<WorkMessageJoinStatus> joinStatusCaptor;
    
    @Test
    public void testNoStatus() {
        String joinId = "joinId";
        WorkMessageJoinDelta oneStarted = new WorkMessageJoinDelta(JoinState.STARTED, "1");
        Set<WorkMessageJoinDelta> joinDelta = ImmutableSet.of(oneStarted);
        when(joinMapper.readByIdForUpdate(joinId)).thenReturn(null);
        
        Optional<Date> result = service.joinAndGet(joinId, joinDelta);
        
        assertFalse(result.isPresent());
        
        verify(joinMapper, times(1)).add(joinStatusCaptor.capture());
        assertEquals(joinId, joinStatusCaptor.getValue().getId());
        WorkMessageJoinDelta[] expectedPendings = new WorkMessageJoinDelta[]{oneStarted};
        assertArrayEquals(expectedPendings, joinStatusCaptor.getValue().getPending());
        verify(joinMapper, times(0)).update(Mockito.<WorkMessageJoinStatus>any());
    }
    
    @Test(expected=ConcurrentModificationException.class)
    public void testNoStatusWithConcurrentInsert() {
        String joinId = "joinId";
        WorkMessageJoinDelta oneStarted = new WorkMessageJoinDelta(JoinState.STARTED, "1");
        Set<WorkMessageJoinDelta> joinDelta = ImmutableSet.of(oneStarted);
        when(joinMapper.readByIdForUpdate(joinId)).thenReturn(null);
        
        doThrow(new DuplicateKeyException("duplicate key")).when(joinMapper).add(Mockito.<WorkMessageJoinStatus>any());
        
        service.joinAndGet(joinId, joinDelta);
    }
    
    @Test
    public void testWithStatus() {
        String joinId = "joinId";
        WorkMessageJoinDelta oneStarted = new WorkMessageJoinDelta(JoinState.STARTED, "1");
        WorkMessageJoinDelta twoStarted = new WorkMessageJoinDelta(JoinState.STARTED, "2");
        WorkMessageJoinDelta threeEnded = new WorkMessageJoinDelta(JoinState.ENDED, "3");
        WorkMessageJoinDelta[] pendingInDB = new WorkMessageJoinDelta[] {oneStarted,twoStarted,threeEnded};
        WorkMessageJoinStatus joinStatusInDB = new WorkMessageJoinStatus(joinId, pendingInDB);
        when(joinMapper.readByIdForUpdate(joinId)).thenReturn(joinStatusInDB);
        
        
        WorkMessageJoinDelta oneEnded = new WorkMessageJoinDelta(JoinState.ENDED, "1");
        WorkMessageJoinDelta twoEnded = new WorkMessageJoinDelta(JoinState.ENDED, "2");
        WorkMessageJoinDelta threeStarted = new WorkMessageJoinDelta(JoinState.STARTED, "3");
        Set<WorkMessageJoinDelta> delta = ImmutableSet.of(oneEnded, twoEnded, threeStarted);
        Optional<Date> result = service.joinAndGet(joinId, delta);
        assertNotNull(result.get());
        
        
        verify(joinMapper, times(1)).update(joinStatusCaptor.capture());
        verify(joinMapper, times(0)).add(Mockito.any(WorkMessageJoinStatus.class));
        
        assertEquals(joinId, joinStatusCaptor.getValue().getId());
        Set<WorkMessageJoinDelta> newPendingAsDBExpected = ImmutableSet.of(oneStarted, twoStarted, threeEnded, oneEnded, twoEnded, threeStarted);
        Set<WorkMessageJoinDelta> newPendingAsDBActual = Sets.newHashSet(joinStatusCaptor.getValue().getPending());
        assertTrue(Sets.symmetricDifference(newPendingAsDBActual, newPendingAsDBExpected).isEmpty());
    }
    
    @Test
    public void testWithStatusSecondRelease() {
        String joinId = "joinId";
        WorkMessageJoinDelta oneStarted = new WorkMessageJoinDelta(JoinState.STARTED, "1");
        WorkMessageJoinDelta twoStarted = new WorkMessageJoinDelta(JoinState.STARTED, "2");
        WorkMessageJoinDelta threeEnded = new WorkMessageJoinDelta(JoinState.ENDED, "3");
        WorkMessageJoinDelta oneEnded = new WorkMessageJoinDelta(JoinState.ENDED, "1");
        WorkMessageJoinDelta twoEnded = new WorkMessageJoinDelta(JoinState.ENDED, "2");
        WorkMessageJoinDelta threeStarted = new WorkMessageJoinDelta(JoinState.STARTED, "3");
        WorkMessageJoinDelta[] pendingInDB = new WorkMessageJoinDelta[] {oneStarted, twoStarted, threeEnded, oneEnded, twoEnded, threeStarted};
        WorkMessageJoinStatus joinStatusInDB = new WorkMessageJoinStatus(joinId, pendingInDB);
        when(joinMapper.readByIdForUpdate(joinId)).thenReturn(joinStatusInDB);
        
        
        Set<WorkMessageJoinDelta> delta = ImmutableSet.of(oneEnded, twoEnded, threeStarted);
        Optional<Date> result = service.joinAndGet(joinId, delta);
        assertNotNull(result.get());
        
        
        verify(joinMapper, times(1)).update(joinStatusCaptor.capture());
        verify(joinMapper, times(0)).add(Mockito.any(WorkMessageJoinStatus.class));
        
        assertEquals(joinId, joinStatusCaptor.getValue().getId());
        Set<WorkMessageJoinDelta> newPendingAsDBExpected = ImmutableSet.of(oneStarted, twoStarted, threeEnded, oneEnded, twoEnded, threeStarted);
        Set<WorkMessageJoinDelta> newPendingAsDBActual = Sets.newHashSet(joinStatusCaptor.getValue().getPending());
        assertTrue(Sets.symmetricDifference(newPendingAsDBActual, newPendingAsDBExpected).isEmpty());
    }
    
    @Test
    public void testPendingUpdate2ChildrenAdded() {
        String joinId = "joinId";
        WorkMessageJoinDelta oneStarted = new WorkMessageJoinDelta(JoinState.STARTED, "1");
        WorkMessageJoinDelta[] pendingInDB = new WorkMessageJoinDelta[] {oneStarted};
        WorkMessageJoinStatus joinStatusInDB = new WorkMessageJoinStatus(joinId, pendingInDB);
        when(joinMapper.readByIdForUpdate(joinId)).thenReturn(joinStatusInDB);

        
        WorkMessageJoinDelta twoStarted = new WorkMessageJoinDelta(JoinState.STARTED, "2");
        WorkMessageJoinDelta threeStarted = new WorkMessageJoinDelta(JoinState.STARTED, "3");
        Set<WorkMessageJoinDelta> delta = ImmutableSet.of(twoStarted, threeStarted);
        Optional<Date> result = service.joinAndGet(joinId, delta);
        assertFalse(result.isPresent());
        
        verify(joinMapper, times(1)).update(joinStatusCaptor.capture());
        verify(joinMapper, times(0)).add(Mockito.any(WorkMessageJoinStatus.class));
        
        assertEquals(joinId, joinStatusCaptor.getValue().getId());
        Set<WorkMessageJoinDelta> newPendingAsDBExpected = ImmutableSet.of(oneStarted, twoStarted, threeStarted);
        Set<WorkMessageJoinDelta> newPendingAsDBActual = Sets.newHashSet(joinStatusCaptor.getValue().getPending());
        assertTrue(Sets.symmetricDifference(newPendingAsDBActual, newPendingAsDBExpected).isEmpty());
    }
    
   
    @Test
    public void testPendingUpdate2ChildrenFinishedWithoutNotifyingParentFinished() {
        String joinId = "joinId";
        WorkMessageJoinDelta oneStarted = new WorkMessageJoinDelta(JoinState.STARTED, "1");
        WorkMessageJoinDelta twoStarted = new WorkMessageJoinDelta(JoinState.STARTED, "2");
        WorkMessageJoinDelta threeStarted = new WorkMessageJoinDelta(JoinState.STARTED, "3");
        WorkMessageJoinDelta[] pendingInDB = new WorkMessageJoinDelta[] {oneStarted, twoStarted, threeStarted};
        WorkMessageJoinStatus joinStatusInDB = new WorkMessageJoinStatus(joinId, pendingInDB);
        when(joinMapper.readByIdForUpdate(joinId)).thenReturn(joinStatusInDB);

        
        WorkMessageJoinDelta twoEnded = new WorkMessageJoinDelta(JoinState.ENDED, "2");
        WorkMessageJoinDelta threeEnded = new WorkMessageJoinDelta(JoinState.ENDED, "3");
        Set<WorkMessageJoinDelta> delta = ImmutableSet.of(twoEnded, threeEnded);
        Optional<Date> result = service.joinAndGet(joinId, delta);
        assertFalse(result.isPresent());
        
        verify(joinMapper, times(1)).update(joinStatusCaptor.capture());
        verify(joinMapper, times(0)).add(Mockito.any(WorkMessageJoinStatus.class));
        
        assertEquals(joinId, joinStatusCaptor.getValue().getId());
        Set<WorkMessageJoinDelta> newPendingAsDBExpected = ImmutableSet.of(oneStarted, twoStarted, threeStarted, twoEnded, threeEnded);
        Set<WorkMessageJoinDelta> newPendingAsDBActual = Sets.newHashSet(joinStatusCaptor.getValue().getPending());
        assertTrue(Sets.symmetricDifference(newPendingAsDBActual, newPendingAsDBExpected).isEmpty());
    }
    
    @Test
    public void testRemoveById() {
        String joinId = "joinId";
        service.removeById(joinId);
        verify(joinMapper, times(1)).removeById(joinId);
    }

}
