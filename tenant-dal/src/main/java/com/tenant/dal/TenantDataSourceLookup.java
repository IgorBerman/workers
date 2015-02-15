package com.tenant.dal;

import java.util.concurrent.ConcurrentMap;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException;
import org.springframework.util.Assert;

import com.google.common.collect.Maps;

public class TenantDataSourceLookup implements DataSourceLookup, DisposableBean {    
    
    private TenantDbParamsTemplateResolver templateResolver;
    
    //properties that can change depending on tenants, they can contain TENANT_ID_PARAM inside which will be replaced at runtime
    private String urlTenantTemplate;
    private String usernameTenantTemplate;
    private String passwordTenantTemplate;
    
    //below properties are same for all tenants
    private String driver;
    private int poolMaximumActiveConnections;
    private int poolMaximumIdleConnections;
    private int poolMaximumCheckoutTime;
    private int poolTimeToWait;
    private String poolPingQuery;
    private int poolPingConnectionsNotUsedFor;
    private boolean poolPingEnabled;
    
    private final ConcurrentMap<String, DataSource> dataSources = Maps.newConcurrentMap();

    @Override
    public DataSource getDataSource(String tenantId) throws DataSourceLookupFailureException {
        Assert.notNull(tenantId, "tenant id must not be null");
        if (!dataSources.containsKey(tenantId)) {
            PooledDataSource ds = createDataSource(tenantId);
            if (dataSources.putIfAbsent(tenantId, ds) != null) {//if there is already ds we want to clean newly created
                ds.forceCloseAll();
            }
        }
        return  dataSources.get(tenantId);
    }

    private PooledDataSource createDataSource(String tenantId) {
        String url = templateResolver.resolveUrl(urlTenantTemplate, tenantId);
        String username = templateResolver.resolveUser(usernameTenantTemplate, tenantId);
        String password = templateResolver.resolvePassword(passwordTenantTemplate, tenantId);
        PooledDataSource ds = new PooledDataSource(driver, url, username, password);
        ds.setPoolMaximumActiveConnections(poolMaximumActiveConnections);
        ds.setPoolMaximumIdleConnections(poolMaximumIdleConnections);
        ds.setPoolMaximumCheckoutTime(poolMaximumCheckoutTime);
        ds.setPoolTimeToWait(poolTimeToWait);
        ds.setPoolPingQuery(poolPingQuery);
        ds.setPoolPingEnabled(poolPingEnabled);
        ds.setPoolPingConnectionsNotUsedFor(poolPingConnectionsNotUsedFor);
        return ds;
    }

    public void setUrlTenantTemplate(String urlTenantTemplate) {
        this.urlTenantTemplate = urlTenantTemplate;
    }

    public void setUsernameTenantTemplate(String usernameTenantTemplate) {
        this.usernameTenantTemplate = usernameTenantTemplate;
    }

    public void setPasswordTenantTemplate(String passwordTenantTemplate) {
        this.passwordTenantTemplate = passwordTenantTemplate;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public void setPoolMaximumActiveConnections(int poolMaximumActiveConnections) {
        this.poolMaximumActiveConnections = poolMaximumActiveConnections;
    }

    public void setPoolMaximumIdleConnections(int poolMaximumIdleConnections) {
        this.poolMaximumIdleConnections = poolMaximumIdleConnections;
    }

    public void setPoolMaximumCheckoutTime(int poolMaximumCheckoutTime) {
        this.poolMaximumCheckoutTime = poolMaximumCheckoutTime;
    }

    public void setPoolTimeToWait(int poolTimeToWait) {
        this.poolTimeToWait = poolTimeToWait;
    }

    public void setPoolPingQuery(String poolPingQuery) {
        this.poolPingQuery = poolPingQuery;
    }

    public void setPoolPingConnectionsNotUsedFor(int poolPingConnectionsNotUsedFor) {
        this.poolPingConnectionsNotUsedFor = poolPingConnectionsNotUsedFor;
    }
    
    public void setTemplateResolver(TenantDbParamsTemplateResolver templateResolver) {
        this.templateResolver = templateResolver;
    }
    
    public void setPoolPingEnabled(boolean poolPingEnabled) {
        this.poolPingEnabled = poolPingEnabled;
    }

    @Override
    public synchronized  void destroy() throws Exception {
        for (DataSource ds : dataSources.values()) {
            ((PooledDataSource)ds).forceCloseAll();//safe since we inserted it
        }
    }
}
