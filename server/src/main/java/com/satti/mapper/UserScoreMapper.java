package com.satti.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserScoreMapper {
    @Select("select score from user_score where contact=#{contact} and movie_id=#{id}")
    Double getScoreByContactAndMovieId(String contact, Integer id);

    /**
     * "on duplicate key update" 插入数据时，若记录已存在，则变成 update
     * @param contact
     * @param id
     * @param rating
     */
    @Insert("insert into user_score (contact, movie_id, score) values (#{contact}, #{id}, #{rating}) " +
            "on duplicate key update score = #{rating}")
    void setScore(String contact, Integer id, Double rating);

    @Select("select round(avg(user_score.score),2) from user_score where movie_id=#{id}")
    Double getEvenScoreByMovieId(Integer id);
}
