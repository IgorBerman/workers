package com.tenant.dal;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.junit.Assert;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.google.common.collect.Lists;
import com.tenant.framework.CurrentTenant;
import com.tenant.framework.TenantIdsResolver;

abstract class BaseTenantRoutingDataSourceTest extends AbstractJUnit4SpringContextTests {
    @Inject CurrentTenant currentTenant;
    @Inject TenantIdsResolver tenantIdsResolver;
    protected void testMultithreaded(final String name) throws SQLException {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        List<Future<?>> fs = Lists.newArrayList();
        try {
            for (final String tenantId : tenantIdsResolver.getIds()) {
                fs.add(pool.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            currentTenant.set(tenantId);
                            runLogic(tenantId, name + "_testMultithreaded");
                            currentTenant.remove();
                        } catch (SQLException e) {
                            Assert.fail(e.getMessage());
                            
                        }
                    }
                }));
            }
            for (Future<?> f : fs) {
                try {
                    f.get();
                } catch (InterruptedException e) {
                    Assert.fail(e.getMessage());
                } catch (ExecutionException e) {
                    Assert.fail(e.getMessage());
                }
            }
        } finally {
            pool.shutdown();
        }
    }
    abstract void runLogic(String tenantId, String table) throws SQLException;

}
