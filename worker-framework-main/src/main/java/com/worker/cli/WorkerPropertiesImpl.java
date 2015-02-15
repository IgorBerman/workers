package com.worker.cli;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.worker.framework.api.WorkerProperties;
import com.worker.shared.WorkersConstants;


@Component
public class WorkerPropertiesImpl implements WorkerProperties {
    @Value("${database.host}") private String databaseHost;

    @Value("${database.name}") private String databaseName;

    @Value("${database.username}") private String databaseUsername;

    @Value("${database.password}") private String databasePassword;

    @Value("${database.dialect}") private String databaseDialect;

    @Value("${queue.hotsname}") private String queueHostName;

    @Value("${queue.password}") private String queuePassword;

    @Value("${queue.username}") private String queueUsername;

    @Value("${queue.worker.name}") private String queueName;

    @Value("${concurent.consumers}") private Integer concurrentConsumers;
    
    @Value("${concurent.lowConsumers}") private Integer concurrentLowConsumers;

    @Value("${retry.max.interval}") private int retryMaxInterval;

    @Value("${retry.multiplier}") private double retryMultiplier;

    @Value("${retry.initial.interval}") private int retryInitialInterval;

    @Value("${retry.max.attempts}") private int retryMaxAttempts;
    
    @Value("${controlRetry.max.attempts}") private int controlRetryMaxAttempts;

    @Value("${pid.dir}") private String pidDir;

    @Value("${tenant.id.header}") private String tenantIdHeader;

    @Value("${python.log.pathprefix}") private String pythonLogPathPrefix;

    @Value("${python.bin.path}") private String pythonBinPath;
    
    @Value("${python.logs.cleanup}") private boolean pythonLogsCleanup;

    @Value("${graphite.server}") private String graphiteServer;

    @Value("${graphite.port}") private Integer graphitePort;
    
    @Value("${admin.port}") private Integer adminPort;
    
    @Value("${processors.time.threshold}") private Long processorsTimeThreshold;
    
    @Value("${lowPriority.processors.time.threshold}") private Long lowPriorityProcessorsTimeThreshold;

    @Value("${python.code.dir}")private String pythonCodeDir;

    @Value("${python.subprocess.main}") private String pythonSubprocessMain;

    @Override
    public String getPidDir() {
        return pidDir;
    }
    @Override
    public String getQueueHostName() {
        return queueHostName;
    }
    @Override
    public String getQueuePassword() {
        return queuePassword;
    }
    @Override
    public String getQueueUsername() {
        return queueUsername;
    }
    @Override
    public String getQueueName() {
        return queueName;
    }
    
    @Override
    public String getErrorQueueName() {
        return WorkersConstants.DEFAULT_ERROR_QUEUE_PREFIX + queueName; 
    }
    
    @Override
    public String getControlQueueName() {
        return WorkersConstants.DEFAULT_CONTROL_QUEUE_PREFIX + queueName;
    }

    @Override
    public int getConcurrentConsumers() {
        return concurrentConsumers==null ? Runtime.getRuntime().availableProcessors()*2/3 : concurrentConsumers;
    }
    
    @Override
    public int getConcurrentLowConsumers() {
        return concurrentLowConsumers==null ? Runtime.getRuntime().availableProcessors()*1/3 : concurrentLowConsumers;
    }

    @Override
    public int getRetryMaxInterval() {
        return retryMaxInterval;
    }
    @Override
    public double getRetryMultiplier() {
        return retryMultiplier;
    }
    @Override
    public int getRetryInitialInterval() {
        return retryInitialInterval;
    }
    @Override
    public int getRetryMaxAttempts() {
        return retryMaxAttempts;
    }
    @Override
    public String getTenantIdHeader() {
        return tenantIdHeader;
    }

    @Override
    public String getPythonLogPathPrefix() {
        return pythonLogPathPrefix;
    }

    @Override
    public String getPythonBinPath() {
        return pythonBinPath;
    }

    @Override
    public boolean isPythonLogsCleanup() {
        return pythonLogsCleanup;
    }

    @Override
    public String getDatabaseHost() {
        return databaseHost;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public String getDatabaseUsername() {
        return databaseUsername;
    }

    @Override
    public String getDatabasePassword() {
        return databasePassword;
    }

    @Override
    public String getDatabaseDialect() {
        return databaseDialect;
    }

    @Override
    public String getGraphiteServer() {
        return graphiteServer;
    }

    @Override
    public Integer getGraphitePort() {
        return graphitePort;
    }

    @Override
    public Integer getAdminPort() {
        return adminPort;
    }

    @Override
    public Long getProcessorsTimeThreshold() {
        return processorsTimeThreshold;
    }
    @Override
    public Long getLowPriorityProcessorsTimeThreshold() {
        return lowPriorityProcessorsTimeThreshold;
    }
    
    @Override
    public String getErrorRoutingKeyPrefix() {
        return WorkersConstants.DEFAULT_ERROR_QUEUE_PREFIX;
    }
    
    @Override
    public int getControlRetryMaxAttempts() {
        return controlRetryMaxAttempts;
    }
    @Override
    public String getLowQueueName() {
        return WorkersConstants.DEFAUL_LOW_QUEUE_PREFIX + queueName;
    }
	@Override
	public String getPythonCodeDir() {
		return pythonCodeDir;
	}
	@Override
	public String getPythonSubprocessMain() {
		return pythonSubprocessMain;
	}    
    
}
