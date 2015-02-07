package com.tenant.framework.test;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import com.tenant.framework.TenantScope;

@Service
@Scope(value=TenantScope.NAME, proxyMode=ScopedProxyMode.INTERFACES)
public class TenantDependentServiceImpl implements TenantDependentService {

    @Override
    public String get() {
        return toString();
    }

}
