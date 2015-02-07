package com.tenant.dal;

import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.tenant.dal.test.mapper.TestMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/tenant-dal-ctx-test.xml")
@Ignore
public class TenantRoutingDataSourceWithSessionTest extends BaseTenantRoutingDataSourceTest {
    @Inject SqlSessionFactory sqlSessionFactory;
    
    
    @Test
    public void testExecuteSqlSingleTenantSqlSessionFactory() throws SQLException {
        currentTenant.set("tenantA");
        runLogic("tenantA", TenantRoutingDataSourceWithSessionTest.class.getSimpleName() + "_testExecuteSqlSingleTenantSqlSessionFactory");
        currentTenant.remove();
    }
    
    @Test(expected=PersistenceException.class)
    public void testWhenTenantIsAbsent() throws SQLException {
        runLogic("tenantA", TenantRoutingDataSourceWithSessionTest.class.getSimpleName() + "_testExecuteSqlSingleTenantSqlSessionFactory");
    }
    @Test
    public void testMultithreaded() throws SQLException {
       super.testMultithreaded(TenantRoutingDataSourceWithSessionTest.class.getSimpleName());
    }
    
    @Override
    protected void runLogic(String tenantId, String table) throws SQLException {
  
        SqlSession session = sqlSessionFactory.openSession();
        TestMapper mapper = session.getMapper(TestMapper.class);
        mapper.executeUpdate("create table " + table + " (test text)");
        mapper.addToTest(table, tenantId);
        for (int i = 0; i < 100; i++) {            
            List<String> results = mapper.readAllFromTest(table);
            Assert.assertEquals(1,results.size());
            Assert.assertEquals(tenantId,results.get(0));
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        session.rollback(true);
        List<String> results = mapper.readAllFromTest(table);
        Assert.assertEquals(0,results.size());
        session.close();
        
    }

}
