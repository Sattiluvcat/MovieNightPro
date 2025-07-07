package com.satti.service;

import com.satti.entity.MovieMetaDoc;

public interface CacheService {
    MovieMetaDoc getCachedMovieMeta(Integer movieId);
    void cacheMovieMeta(Integer movieId, String summary, String officialRating, String coverUrl);
}
