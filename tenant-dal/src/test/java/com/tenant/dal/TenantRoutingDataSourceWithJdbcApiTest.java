package com.tenant.dal;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.tenant.framework.CurrentTenant;
import com.tenant.framework.NoCurrentTenantException;
import com.tenant.framework.TenantIdsResolver;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/tenant-dal-ctx-test.xml")
@Ignore
public class TenantRoutingDataSourceWithJdbcApiTest extends BaseTenantRoutingDataSourceTest {
    @Inject CurrentTenant currentTenant;
    @Inject SqlSessionFactory sqlSessionFactory;
    @Inject TenantIdsResolver tenantIdsResolver;
    
    
    @Test
    public void testExecuteSqlSingleTenantSqlSessionFactory() throws SQLException {
        currentTenant.set("tenantA");
        runLogic("tenantA", TenantRoutingDataSourceWithJdbcApiTest.class.getSimpleName() + "_testExecuteSqlSingleTenantSqlSessionFactory");
        currentTenant.remove();
    }
    
    @Test(expected=NoCurrentTenantException.class)
    public void testWhenTenantIsAbsent() throws SQLException {
        runLogic("tenantA", TenantRoutingDataSourceWithJdbcApiTest.class.getSimpleName() + "_testExecuteSqlSingleTenantSqlSessionFactory");
    }
    
    @Test
    public void testMultithreaded() throws SQLException {
        super.testMultithreaded(TenantRoutingDataSourceWithJdbcApiTest.class.getSimpleName());
    }
    
    protected void runLogic (String tenantId, String table) throws SQLException {
        SqlSession session = sqlSessionFactory.openSession();
        Connection connection = session.getConnection();
        Statement statement = connection.createStatement();
        statement.executeUpdate("create table " + table + " (test text)");
        statement.executeUpdate("insert into " + table + " (test) values ('" + tenantId + "')");
        for (int i = 0; i < 100; i++) {
            ResultSet resultSet = statement.executeQuery("select * from " + table + "");
            if (resultSet.next()) {
                Assert.assertEquals(tenantId,resultSet.getString(1));
                Assert.assertFalse(resultSet.next());
                resultSet.close();      
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                Assert.fail("expected results");
            }
        }
        session.rollback(true);
        ResultSet resultSet = statement.executeQuery("select * from " + table + "");
        if (resultSet.next()){
            Assert.fail("expected no results");
        }
        statement.executeUpdate("drop table if exists " + table + "");
        session.close();
    }

}
