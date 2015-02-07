package com.worker.framework.service;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.worker.framework.dal.mapper.WorkMessageJoinMapper;
import com.worker.framework.dal.mapper.WorkMessageJoinStatus;
import com.worker.shared.WorkMessageJoinDelta;

@Service
public class WorkMessageJoinServiceImpl implements WorkMessageJoinService {
    private static final Logger logger = LoggerFactory.getLogger(WorkMessageJoinServiceImpl.class);
    private @Inject WorkMessageJoinMapper joinMapper;
    
    @Transactional(rollbackFor=Exception.class)
    public Optional<Date> joinAndGet(String joinId, Set<WorkMessageJoinDelta> joinDelta) throws ConcurrentModificationException {
        Optional<Date> result = Optional.absent();
        WorkMessageJoinStatus joinStatus = joinMapper.readByIdForUpdate(joinId);
        if (joinStatus == null) {
            logger.debug("initializing join state "+ joinId + " " + joinDelta);
            joinStatus = new WorkMessageJoinStatus(joinId, getDBPending(joinDelta));
            try {
                joinMapper.add(joinStatus);//if several threads will try to add join state - only 1 succeed
            } catch (DuplicateKeyException e) {
                throw new ConcurrentModificationException("Unable to add joinStatus, need to retry, error:(" + Throwables.getRootCause(e).getMessage() + ")");
            }
        } else {
            logger.debug("updating join state "+ joinId + " " + joinDelta);
            //Set<WorkMessageJoinDelta> oldDBPendingStates = Sets.newHashSet(joinStatus.getPending());
            Set<WorkMessageJoinDelta> newDBPendingStates = Sets.newHashSet(joinStatus.getPending());          
            newDBPendingStates.addAll(joinDelta);
            boolean released = isEmpty(newDBPendingStates);//if last delta retransmitted we will release once again
            if (released) {
                Date releaseDate = new Date();
                joinStatus.setReleasedTimestamp(releaseDate);
                result = Optional.of(releaseDate);
            }
            joinStatus.setPending(getDBPending(newDBPendingStates));
            joinMapper.update(joinStatus);
        }
        return result;
    }
    
    private WorkMessageJoinDelta[] getDBPending(Set<WorkMessageJoinDelta> joinDelta) {
        return joinDelta.toArray(new WorkMessageJoinDelta[joinDelta.size()]);
    }
    
    private boolean isEmpty(Set<WorkMessageJoinDelta> newDBPendingMessageStates) {
        Set<WorkMessageJoinDelta> sum = Sets.newHashSet(newDBPendingMessageStates);
        for (WorkMessageJoinDelta mstate : newDBPendingMessageStates) {
            //set(-id,+id) = set()
            WorkMessageJoinDelta opposite = new WorkMessageJoinDelta(mstate.getState().opposite(), mstate.getMessageId());
            if (newDBPendingMessageStates.contains(opposite)) {
                sum.remove(mstate);
                sum.remove(opposite);
            }
        }
        return sum.isEmpty();
    }
    
    @Transactional(rollbackFor=Exception.class)
    public void removeById(String joinId) {
        logger.debug("removing join state " + joinId);
        joinMapper.removeById(joinId);
    }
}
