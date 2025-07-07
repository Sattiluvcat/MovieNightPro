package com.satti.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieSmallRow {
    // 显示在排行榜与每日推荐上的格式
    private String title;
    private String[] tags; // 标签
    private Integer _id; // 电影ID
}
