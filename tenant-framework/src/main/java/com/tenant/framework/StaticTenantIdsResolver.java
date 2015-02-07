package com.tenant.framework;

import java.util.List;

import com.google.common.collect.Lists;

public class StaticTenantIdsResolver implements TenantIdsResolver {
    private List<String> ids = Lists.newArrayList();
    
    public StaticTenantIdsResolver(List<String> ids) {
        this.ids = ids;
    }
    
    public List<String> getIds() {
        return ids;
    }
}
