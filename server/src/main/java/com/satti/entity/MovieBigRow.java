package com.satti.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 缩略信息
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieBigRow {
    public final static Integer[] SUGGEST = {385, 172, 292, 192, 171};
    private String title;
    private String star_mine;    // 自己的评分 有的没评分
    private String release_time;
    private String summary=null;
    private String[] tags;
    private String[] actor_actresses;
    private String[] languages;
    private Integer _id;

    public MovieBigRow(MovieBigRow movieBigRow) {
        this.title = movieBigRow.getTitle();
        this.star_mine = movieBigRow.getStar_mine();
        this.release_time = movieBigRow.getRelease_time();
        this.tags = movieBigRow.getTags();
        this.actor_actresses = movieBigRow.getActor_actresses();
        this.languages = movieBigRow.getLanguages();
        this._id=movieBigRow.get_id();
        this.summary=movieBigRow.getSummary();
    }
}
