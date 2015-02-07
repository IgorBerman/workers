package com.tenant.framework;

public interface CurrentTenant {

    String get();

    void set(String tenantId);

    boolean isSet();
    
    void remove();

}