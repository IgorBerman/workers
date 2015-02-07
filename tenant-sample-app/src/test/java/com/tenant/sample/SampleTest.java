package com.tenant.sample;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.tenant.framework.CurrentTenant;
import com.tenant.framework.TenantIdsResolver;
import com.tenant.sample.service.SampleService;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/tenant-sample-ctx-test.xml")
public class SampleTest extends AbstractJUnit4SpringContextTests {
    @Inject CurrentTenant currentTenant;
    @Inject SampleService sampleService;
    @Inject TenantIdsResolver tenantIdsResolver;

    @Before
    public void createDBs() {
        List<String> tenantIds = tenantIdsResolver.getIds();
        for (String tenantId : tenantIds) {
            currentTenant.set(tenantId);
            try {
                sampleService.initDB();
            } finally {
                currentTenant.remove();
            }
        }
    }

    @Test
    public void testMultithreaded() throws Throwable {
        int FACTOR = 4;
        List<String> tenantIds = tenantIdsResolver.getIds();
        ExecutorService pool = Executors.newFixedThreadPool(tenantIds.size() * FACTOR);
        List<Future<?>> fs = Lists.newArrayList();
        try {
            for (int i = 0; i < FACTOR; i++) {
                for (final String tenantId : tenantIds) {
                    Future<?> f = pool.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                currentTenant.set(tenantId);
                                tenantServiceCall(tenantId);
                            } catch (SQLException e) {
                                Assert.fail(e.getMessage());
                            } finally {
                                currentTenant.remove();
                            }
                        }
                    });
                    fs.add(f);
                }
            }
            for (Future<?> f : fs) {
                try {
                    f.get();
                } catch (InterruptedException e) {
                    Assert.fail(e.getMessage());
                } catch (ExecutionException e) {
                    throw e.getCause();
                }
            }
        } finally {
            pool.shutdown();
        }
    }

    private void tenantServiceCall(String tenantId) throws SQLException {
        for (int i = 0; i < 30; i++) {
            logger.info("starting " + i + " iteration");
            Sample sample = new Sample(null, Thread.currentThread().getName());
            int id = sampleService.add(sample);
            sample.setId(id);

            List<Sample> samples = sampleService.readAll();
            for (Sample s : samples) {
                Assert.assertTrue(s.getDescription().startsWith(tenantId));
            }

            sample.setDescription(sample.getDescription() + "- updated");
            sampleService.update(sample);
            Optional<Sample> sampleFromDB = sampleService.readById(id);
            Assert.assertTrue(sampleFromDB.isPresent());
            Assert.assertEquals(sample.getDescription(), sampleFromDB.get().getDescription());
            Assert.assertTrue(sample.getDescription().endsWith("- updated"));

            sampleService.remove(id);
            sampleFromDB = sampleService.readById(id);
            Assert.assertFalse(sampleFromDB.isPresent());
        }
    }
}
