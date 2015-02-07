package com.worker.framework.python;

public class PythonWorkerConnectionUrl {

    private String tenantId;
    private String dbUrl;

    public PythonWorkerConnectionUrl() {
        super();
    }

    public PythonWorkerConnectionUrl(String tenantId,
                                     String driver,
                                     String host,
                                     String dbname,
                                     String username,
                                     String password) {
        super();
        this.tenantId = tenantId;
        this.dbUrl = String.format("%s://%s:%s@%s/%s", driver, username, password, host, dbname);
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getDbUrl() {
        return dbUrl;
    }

}
