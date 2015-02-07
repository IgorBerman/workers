package com.tenant.sample.service;

import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import com.tenant.framework.CurrentTenant;
import com.tenant.framework.TenantScope;

@Service
@Scope(value=TenantScope.NAME,proxyMode=ScopedProxyMode.INTERFACES)
public class ConfigurationServiceImpl implements ConfigurationService {

    @Inject CurrentTenant currentTenant;
    
    @Override
    public String getProperty(String name) {
        //hash should be different between scopes, i.e. different instance for different scopes
        return currentTenant.get() + "." + name + "." + this.hashCode();
        
    }

}
