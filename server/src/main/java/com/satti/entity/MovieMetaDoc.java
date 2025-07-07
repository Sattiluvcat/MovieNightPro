package com.satti.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "movie_metadata")
public class MovieMetaDoc {
    @Id
    private String id;

    @Field(type = FieldType.Integer)
    private Integer movieId;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
    private String summary;

    @Field(type = FieldType.Keyword)
    private String officialRating;

    @Field(type = FieldType.Keyword)
    private String coverUrl;

}
