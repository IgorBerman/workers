package com.worker.framework.api;

public interface WorkerProperties extends RoutingProperties {
    
    String getPidDir();

    String getQueueHostName();

    String getQueuePassword();

    String getQueueUsername();    
    
    int getConcurrentConsumers();

    int getRetryMaxInterval();

    double getRetryMultiplier();

    int getRetryInitialInterval();

    int getRetryMaxAttempts();
    
    int getControlRetryMaxAttempts();

    String getTenantIdHeader();

    String getPythonLogPathPrefix();

    String getPythonBinPath();

    boolean isPythonLogsCleanup();

    String getDatabaseHost();

    String getDatabaseName();

    String getDatabaseUsername();

    String getDatabasePassword();

    String getDatabaseDialect();

    String getGraphiteServer();

    Integer getGraphitePort();

    Integer getAdminPort();

    Long getProcessorsTimeThreshold();

    Long getLowPriorityProcessorsTimeThreshold();
    
    String getErrorRoutingKeyPrefix();

    int getConcurrentLowConsumers();

	String getPythonCodeDir();

	String getPythonSubprocessMain();

}
