package com.worker.framework.api;

public interface JoinStateIdGenerator {

    public String generateUniqueId(String taskName);

    public String generateUniqueId(String taskName, String middle);

}
