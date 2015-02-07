package com.tenant.dal.test.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface TestMapper {

    @Update("${sql}")
    void executeUpdate(@Param("sql") String sql);

    @Insert("insert into ${table} (test) values (#{value})")
    void addToTest(@Param("table")String table, @Param("value")String value);

    @Select("select * from  ${table} ")
    List<String> readAllFromTest(@Param("table")String table);

}
