package com.tenant.dal;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.google.common.collect.Maps;
import com.tenant.framework.CurrentTenant;
import com.tenant.framework.NoCurrentTenantException;
import com.tenant.framework.TenantIdsResolver;


public class TenantRoutingDataSource extends AbstractRoutingDataSource {
    private CurrentTenant currentTenant;
    private TenantIdsResolver tenantIdsResolver;

    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = currentTenant.get();
        if (tenantId == null) {
            throw new NoCurrentTenantException(
                    "current tenant id isn't set, unable to proceed. hint: do u want to use " + CurrentTenant.class +
                            ".set method?");
        }
        return tenantId;
    }

    public void setCurrentTenant(CurrentTenant currentTenant) {
        this.currentTenant = currentTenant;
    }

    public void setTenantIdsResolver(TenantIdsResolver tenantIdsResolver) {
        this.tenantIdsResolver = tenantIdsResolver;
    }

    @Override
    public void afterPropertiesSet() {
        List<String> tenantIds = tenantIdsResolver.getIds();
        if (!tenantIds.isEmpty()) {
            currentTenant.set(tenantIds.get(0));
        }
        Map<Object, Object> targetDataSources = Maps.newHashMap();
        for (String tenantId : tenantIds) {
            targetDataSources.put(tenantId, tenantId);// ds lookup will resolve to DataSource
        }
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }
}
