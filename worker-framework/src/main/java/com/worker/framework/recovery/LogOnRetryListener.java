package com.worker.framework.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

import com.google.common.base.Throwables;

public class LogOnRetryListener implements RetryListener {
    private static final Logger logger = LoggerFactory.getLogger(LogOnRetryListener.class);

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        return true;
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        Throwable rootCause = Throwables.getRootCause(throwable);
        logger.error(context.getRetryCount() + " retry failed due to " + rootCause.getMessage());
    }
}
