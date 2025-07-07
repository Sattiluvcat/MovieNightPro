package com.satti.service.serviceImpl;

import com.satti.entity.MovieMetaDoc;
import com.satti.service.CacheService;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

/**
 * 设置定时任务从豆瓣拉取电影数据存储到es里
 */
@Service
public class CacheServiceImpl implements CacheService {
    private static final String INDEX_NAME = "movie_metadata";
    private final ElasticsearchOperations esOperations;

    public CacheServiceImpl(ElasticsearchOperations esOperations) {
        this.esOperations = esOperations;
    }

    public void cacheMovieMeta(Integer movieId, String summary, String officialRating, String coverUrl){
        MovieMetaDoc doc = new MovieMetaDoc();
        doc.setMovieId(movieId);
        doc.setSummary(summary);
        doc.setCoverUrl(coverUrl);
        doc.setOfficialRating(officialRating);
        IndexQuery query=new IndexQueryBuilder()
                .withId(movieId.toString())
                .withObject(doc)
                .build();
        esOperations.index(query, IndexCoordinates.of(INDEX_NAME));
    }

    public MovieMetaDoc getCachedMovieMeta(Integer movieId){
        return esOperations.get(
                movieId.toString(),
                MovieMetaDoc.class,
                IndexCoordinates.of(INDEX_NAME)
        );
    }

}
