package com.tenant.framework;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import com.google.common.base.Strings;

public class TenantScope implements Scope {
    private static final Logger logger = LoggerFactory.getLogger(TenantScope.class);
    public static final String NAME = "tenant";

    private CurrentTenant currentTenant;

    //it's safe to read from it concurrently, however we don't want that same bean will be created twice
    private ConcurrentHashMap<String, ConcurrentMap<String, Object>> tenantBeanCache = new ConcurrentHashMap<String, ConcurrentMap<String, Object>>(50);

    /**
     * Return the object with the given name from the underlying scope, creating
     * it if not found in the underlying storage mechanism.
     */
    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        String tenantId = currentTenant.get();
        if (Strings.isNullOrEmpty(tenantId)) {
            throw new NoCurrentTenantException("tenant is not set, unable to proceed");
        }
        ConcurrentMap<String, Object> beanCache = getBeanCacheForTenant(tenantId);
        if (!beanCache.containsKey(name)) {
            synchronized (beanCache) {
                if (!beanCache.containsKey(name)) {
                    beanCache.put(name, objectFactory.getObject());
                }
            }
        }

        return beanCache.get(name);
    }

    private ConcurrentMap<String, Object> getBeanCacheForTenant(String tenantId) {
        if (!tenantBeanCache.containsKey(tenantId)) {
            synchronized (tenantBeanCache) {
                if (!tenantBeanCache.containsKey(tenantId)) {
                    tenantBeanCache.put(tenantId, new ConcurrentHashMap<String, Object>(10));
                }
            }
        }

        return tenantBeanCache.get(tenantId);
    }

    @Override
    public String getConversationId() {
        return currentTenant.get();
    }

    /**
     * Register a callback to be executed on destruction of the specified object
     * in the scope (or at destruction of the entire scope, if the scope does
     * not destroy individual objects but rather only terminates in its
     * entirety).
     */
    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        logger.warn("registerDestructionCallback of " + this.getClass().getSimpleName() + " isn't supported");
    }

    /**
     * Remove the object with the given name from the underlying scope.
     */
    @Override
    public Object remove(String name) {
        String tenantId = currentTenant.get();
        if (Strings.isNullOrEmpty(tenantId)) {
            throw new NoCurrentTenantException("tenant is not set, unable to proceed");
        }
        Map<String, Object> beanCache = getBeanCacheForTenant(tenantId);
        return beanCache.remove(name);
    }

    /**
     * Resolve the contextual object for the given key, if any.
     */
    @Override
    public Object resolveContextualObject(String key) {
        throw new UnsupportedOperationException("resolveContextualObject " + key + " is not implemented");
    }

    public void setCurrentTenant(CurrentTenant currentTenant) {
        this.currentTenant = currentTenant;
    }
}