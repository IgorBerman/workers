package com.tenant.framework;


public class TenantDbParamsTemplateResolverImpl implements TenantDbParamsTemplateResolver {

    private static final String TENANT_ID_PARAM = "$[tenantId]";

    @Override
    public String resolveUrl(String urlTemplate, String tenantId) {
        return replaceDefault(urlTemplate, tenantId);
    }

    @Override
    public String resolveUser(String userTemplate, String tenantId) {
        return replaceDefault(userTemplate, tenantId);
    }

    @Override
    public String resolvePassword(String passwordTemplate, String tenantId) {
        return replaceDefault(passwordTemplate, tenantId);
    }

    @Override
    public String resolveDbName(String dbNameTemplate, String tenantId) {
        return replaceDefault(dbNameTemplate, tenantId);
    }

    private String replaceDefault(String template, String tenantId) {
        return template.replace(TENANT_ID_PARAM, tenantId);
    }

}
