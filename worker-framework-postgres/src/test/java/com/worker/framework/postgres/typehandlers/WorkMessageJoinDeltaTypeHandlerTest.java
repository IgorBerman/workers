package com.worker.framework.postgres.typehandlers;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.JdbcType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.worker.shared.JoinState;
import com.worker.shared.WorkMessageJoinDelta;

@RunWith(MockitoJUnitRunner.class)
public class WorkMessageJoinDeltaTypeHandlerTest {
    @Mock PreparedStatement ps;
    @Mock ResultSet rs;
    @Mock Array result; 
    
    private WorkMessageJoinDeltaTypeHandler handler = new WorkMessageJoinDeltaTypeHandler();
    @Test
    public void testSetNonNullParameterNull() throws SQLException {
        int i = 1;
        WorkMessageJoinDelta[] parameter = null;
        JdbcType jdbcType = null;
        handler.setNonNullParameter(ps, i, parameter, jdbcType);
        verify(ps, times(1)).setNull(i, java.sql.Types.VARCHAR);
    }
    
    @Test
    public void testSetNonNullParameter() throws SQLException {
        int i = 1;
        WorkMessageJoinDelta[] parameter = new WorkMessageJoinDelta[] {new WorkMessageJoinDelta(JoinState.STARTED, "1"), new WorkMessageJoinDelta(JoinState.ENDED, "2")};
        JdbcType jdbcType = null;
        handler.setNonNullParameter(ps, i, parameter, jdbcType);
        verify(ps, times(1)).setString(i, "{STARTED:1,ENDED:2}");
    }
    
    @Test
    public void testGetNullableResultNullReturned() throws SQLException {
        when(rs.getArray(1)).thenReturn(null);
        WorkMessageJoinDelta[] nullableResult = handler.getNullableResult(rs, 1);
        assertArrayEquals(new WorkMessageJoinDelta[]{}, nullableResult);
    }
    @Test
    public void testGetNullableResultEmpty() throws SQLException {
        when(rs.getArray(1)).thenReturn(result);
        String[] stringArray= new String[] {};
        when(result.getArray()).thenReturn(stringArray);
        WorkMessageJoinDelta[] nullableResult = handler.getNullableResult(rs, 1);
        assertArrayEquals(new WorkMessageJoinDelta[]{}, nullableResult);
    }
    @Test
    public void testGetNullableResult() throws SQLException {
        when(rs.getArray(1)).thenReturn(result);
        String[] stringArray= new String[] {"STARTED:1", "ENDED:2"};
        when(result.getArray()).thenReturn(stringArray);
        WorkMessageJoinDelta[] nullableResult = handler.getNullableResult(rs, 1);
        assertArrayEquals(new WorkMessageJoinDelta[]{new WorkMessageJoinDelta(JoinState.STARTED, "1"), new WorkMessageJoinDelta(JoinState.ENDED, "2")}, nullableResult);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testGetNullableResultWithIllegalState() throws SQLException {
        when(rs.getArray(1)).thenReturn(result);
        String[] stringArray= new String[] {"STARTE:1"};
        when(result.getArray()).thenReturn(stringArray);
        handler.getNullableResult(rs, 1);
    }

}
