package com.worker.framework.service;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Set;

import com.google.common.base.Optional;
import com.worker.shared.WorkMessageJoinDelta;


public interface WorkMessageJoinService {
    public Optional<Date> joinAndGet(String joinId, Set<WorkMessageJoinDelta> joinDelta) throws ConcurrentModificationException;
    public void removeById(String joinId);
}
