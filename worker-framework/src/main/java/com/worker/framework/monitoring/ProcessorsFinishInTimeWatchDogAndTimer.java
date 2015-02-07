package com.worker.framework.monitoring;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tenant.framework.CurrentTenant;
import com.worker.framework.WorkerQueueMessageListenerImpl;
import com.worker.shared.WorkMessage;


public class ProcessorsFinishInTimeWatchDogAndTimer extends HealthCheck {
    private MetricRegistry metricsRegistry;
    private final Set<StartWithTimerContext> messagesInProgress =
            Sets.newSetFromMap(Maps.<StartWithTimerContext, Boolean> newConcurrentMap());
    private long threshold;
    private CurrentTenant currentTenant;
    private Long lowPriorityThreshold;

    public ProcessorsFinishInTimeWatchDogAndTimer(Long threshold,
                                                  Long lowPriorityThreshold,
                                                  MetricRegistry metricsRegistry,
                                                  CurrentTenant currentTenant) {
        this.metricsRegistry = metricsRegistry;
        this.currentTenant = currentTenant;
        if (threshold == null) {
            this.threshold = TimeUnit.MINUTES.toMillis(5);
        } else {
            this.threshold = threshold;
        }
        if (lowPriorityThreshold  == null) {
            this.threshold = TimeUnit.MINUTES.toMillis(30);
        } else {
            this.lowPriorityThreshold = lowPriorityThreshold;
        }
    }

    public static class StartWithTimerContext {
        public StartWithTimerContext(long start, Context timerContext, WorkMessage wm, String tenantId) {
            this.start = start;
            this.timerContext = timerContext;
            this.wm = wm;
            this.tenantId = tenantId;
        }

        final private long start;
        final private Timer.Context timerContext;
        final private WorkMessage wm;
        final private String tenantId;
    }

    public StartWithTimerContext startProcessing(WorkMessage wm) {
        final Timer timer =
                metricsRegistry.timer(MetricRegistry.name(WorkerQueueMessageListenerImpl.class, currentTenant.get(),
                                                          wm.getTask()));
        final Timer.Context timerContext = timer.time();
        StartWithTimerContext value = new StartWithTimerContext(System.currentTimeMillis(), timerContext, wm, currentTenant.get());
        messagesInProgress.add(value);
        return value;
    }

    public void finished(StartWithTimerContext startWithTimerContext) {
        startWithTimerContext.timerContext.stop();
        messagesInProgress.remove(startWithTimerContext);
    }

    @Override
    protected Result check() throws Exception {
        long now = System.currentTimeMillis();
        List<String> stuckedMessages = Lists.newArrayList();
        for (StartWithTimerContext startWithTimerContext : messagesInProgress) {
            
            boolean lowPriority = startWithTimerContext.wm.isLowPriority();
            if ((!lowPriority && (now - startWithTimerContext.start > threshold)) ||
                ( lowPriority && (now - startWithTimerContext.start > lowPriorityThreshold)) ) {
                stuckedMessages.add(startWithTimerContext.tenantId + " : " + startWithTimerContext.wm);
            }
        }
        if (!stuckedMessages.isEmpty()) {
            return Result.unhealthy("Following messages are stucked:<br>" + Joiner.on("<br>").join(stuckedMessages));
        }
        return Result.healthy("OK");
    }
}
