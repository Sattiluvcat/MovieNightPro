package com.satti.mapper;

import com.satti.entity.UserInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("select nickname from user_info where contact = #{contact}")
    String isEmailRegistered(String contact);

    @Insert("insert into user_info (nickname, password, contact, title) " +
            "values (#{nickname}, #{password}, #{contact}, #{title})")
    void registerUser(String nickname, String password, String contact, String title);

    @Select("select password from user_info where contact=#{contact}")
    String checkInUser(String contact);

    @Select("select * from user_info where contact=#{contact}")
    UserInfo getInfoByContact(String contact);
}
