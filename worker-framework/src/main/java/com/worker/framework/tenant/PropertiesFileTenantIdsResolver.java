package com.worker.framework.tenant;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.collect.Lists;
import com.tenant.framework.TenantIdsResolver;

public class PropertiesFileTenantIdsResolver implements TenantIdsResolver {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesFileTenantIdsResolver.class);
    @Value("${tenant.id.list}") private String[] tenantsProp;
    private List<String> tenants;
    
    @PostConstruct
    protected void init() {
        tenants = Lists.newArrayList(tenantsProp);
        logger.info("Following tenants are deployed: " + tenants);
    }
    
    @Override
    public List<String> getIds() {
        return tenants;
    }

}
