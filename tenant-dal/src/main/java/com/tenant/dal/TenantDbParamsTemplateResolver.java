package com.tenant.dal;

public interface TenantDbParamsTemplateResolver {

    public String resolveUrl(String urlTemplate, String tenantId);

    public String resolveUser(String userTemplate, String tenantId);

    public String resolvePassword(String passwordTemplate, String tenantId);

    public String resolveDbName(String dbNameTemplate, String tenantId);

}
