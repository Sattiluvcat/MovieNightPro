package com.satti.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "movies")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Movie {
    @Id
    private Integer _id; // MongoDB's default primary key

    private String title;
    private String link;
    private String star_mine;    // 自己的评分 有的没评分
    private String release_time;
    private String[] actor_actresses;
    private String director;
    private String duration;
    private String[] tags;
    private String[] languages;
}