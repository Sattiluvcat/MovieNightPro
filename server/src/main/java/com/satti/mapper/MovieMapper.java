package com.satti.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MovieMapper {

    @Update("update view_times set times = times + 1 where id = #{id}")
    void addTimes(Integer id);

    @Select("select id from view_times order by times desc limit #{count}")
    List<Integer> getMostView(Integer count);

    @Select("select times from view_times where id = #{id}")
    Integer getViewTimes(Integer id);
}
