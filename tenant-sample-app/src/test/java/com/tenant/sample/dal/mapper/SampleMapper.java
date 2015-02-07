package com.tenant.sample.dal.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.tenant.sample.Sample;

public interface SampleMapper {

    @Update("${sql}")
    void executeUpdate(@Param("sql") String sql);
    
    @Select("select * from sample where id = #{id}")
    Sample readById(@Param("id")int id);

    @Select("select * from sample")
    List<Sample> readAll();

    @Delete("delete from sample where id = #{id}")
    void remove(@Param("id")int id);

    @Update("update sample set description = #{o.description} where id = #{o.id}")
    void update(@Param("o") Sample sample);

    @Insert("insert into sample (description) values (#{o.description})")
    @Options(keyProperty="o.id", useGeneratedKeys=true)
    void add(@Param("o") Sample sample);

}
