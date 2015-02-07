package com.tenant.framework;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.tenant.framework.test.TenantDependentService;
@RunWith(SpringJUnit4ClassRunner.class)

@ContextConfiguration(locations = "classpath:/META-INF/spring/tenant-framework-ctx-test.xml")
public class TenantScopeTest {

    @Inject CurrentTenant currentTenant;
    @Inject TenantDependentService service;//we should get here proxy
    
    @Test
    public void test() {
        currentTenant.set("tenantA"); 
        String tenantAGetResult = service.get();
        System.out.println(tenantAGetResult);
        
        currentTenant.set("tenantB"); 
        String tenantBGetResult = service.get();
        System.out.println(tenantBGetResult);
        
        Assert.assertFalse(tenantAGetResult.equals(tenantBGetResult));
    }

}
