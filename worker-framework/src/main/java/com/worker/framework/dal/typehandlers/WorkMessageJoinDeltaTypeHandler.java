package com.worker.framework.dal.typehandlers;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.worker.shared.WorkMessageJoinDelta;


@MappedTypes(WorkMessageJoinDelta[].class)
public class WorkMessageJoinDeltaTypeHandler extends BaseTypeHandler<WorkMessageJoinDelta[]> {

    private static final WorkMessageJoinDelta[] EMPTY = new WorkMessageJoinDelta[] {};

    public WorkMessageJoinDeltaTypeHandler() {}

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, WorkMessageJoinDelta[] parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, java.sql.Types.VARCHAR);
        } else {
            Iterable<String> states = Iterables.transform(Lists.newArrayList(parameter), new Function<WorkMessageJoinDelta, String>() {
                @Override
                public String apply(WorkMessageJoinDelta input) {
                    return WorkMessageJoinDelta.to(input);
                }
                
            });
            ps.setString(i, "{" + Joiner.on(",").join(states) + "}");
        }
    }

    @Override
    public WorkMessageJoinDelta[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Array array = rs.getArray(columnName);
        if (array == null) {
            return EMPTY;
        }
        return deserialize((String[]) array.getArray());
    }

    private WorkMessageJoinDelta[] deserialize(String[] array) {
        Iterable<WorkMessageJoinDelta> states = Iterables.transform(Lists.newArrayList(array), new Function<String, WorkMessageJoinDelta>() {
            @Override
            public WorkMessageJoinDelta apply(String input) {
                return WorkMessageJoinDelta.from(input);
            }
            
        });
        List<WorkMessageJoinDelta> list = Lists.newArrayList(states);
        return list.toArray(new WorkMessageJoinDelta[list.size()]);
    }

    @Override
    public WorkMessageJoinDelta[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Array array = rs.getArray(columnIndex);
        if (array == null) {
            return EMPTY;
        }
        return deserialize((String[]) array.getArray());
    }

    @Override
    public WorkMessageJoinDelta[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Array array = cs.getArray(columnIndex);
        if (array == null) {
            return EMPTY;
        }
        return deserialize((String[]) array.getArray());
    }
}
