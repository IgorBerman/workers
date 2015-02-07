package com.tenant.framework;



public class CurrentTenantThreadLocal implements CurrentTenant {

    private static ThreadLocal<String> currentTenant = new ThreadLocal<String>();

    @Override
    public String get() {
        return currentTenant.get();
    }

    @Override
    public boolean isSet() {
        return currentTenant.get() != null;
    }

    @Override
    public void set(String tenantId) {
        currentTenant.set(tenantId);
    }

    @Override
    public void remove() {
        currentTenant.remove();
    }

}