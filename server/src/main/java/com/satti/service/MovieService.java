package com.satti.service;

import com.satti.entity.MovieDTO;
import com.satti.entity.MovieBigRow;
import com.satti.entity.MovieSmallRow;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MovieService {

    MovieDTO getMovieDetails(Integer id, HttpSession session);

    List<MovieBigRow> searchMovies(String keyword);

    List<MovieBigRow> categorySearch(String[] tags, String[] languages);

//    String watchTogetherWithTitle(Integer id);
//
//    String commentWithTitle(Integer id, String comment);

    String getTitle(Integer id);

    List<MovieBigRow> getSuggestedMovies();

    List<MovieBigRow> getAllMovies();

    boolean watchTogetherWithTitle(Integer id, HttpSession session);

    boolean commentWithTitle(Integer id, String comment, HttpSession session);

    CompletableFuture<List<MovieSmallRow>> loadHomeAsync(HttpSession session);

    List<MovieBigRow> searchDescriptions(String keyword);
}
