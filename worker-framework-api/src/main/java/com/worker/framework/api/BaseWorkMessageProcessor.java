package com.worker.framework.api;

public abstract class BaseWorkMessageProcessor implements WorkMessageProcessor {

    public String getName() {
    	return getClass().getSimpleName();
    }

}
