package com.worker.framework.dal.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface WorkMessageJoinMapper {

    @Select("select * from work_message_join_status where id = #{id} for update")
    WorkMessageJoinStatus readByIdForUpdate(@Param("id")String id);
    
    @Insert("insert into work_message_join_status (id, pending, released_timestamp) values (#{joinStatus.id}, #{joinStatus.pending}, #{joinStatus.releasedTimestamp, jdbcType=TIMESTAMP})")
    void add(@Param("joinStatus")WorkMessageJoinStatus joinStatus);
    
    @Update("update work_message_join_status set pending = #{joinStatus.pending}, released_timestamp = #{joinStatus.releasedTimestamp, jdbcType=TIMESTAMP} where id = #{joinStatus.id}")
    void update(@Param("joinStatus")WorkMessageJoinStatus joinStatus);
    
    @Delete("delete from work_message_join_status where id = #{id}")
    void removeById(@Param("id")String id);

}
