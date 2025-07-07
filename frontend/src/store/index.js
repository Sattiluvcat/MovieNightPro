import { createStore } from 'vuex';

export default createStore({
  state: {
    movies: []
  },
  mutations: {
    setMovies(state, movies) {
      state.movies = movies;
    }
  },
  actions: {
    updateMovies({ commit }, movies) {
      commit('setMovies', movies);
    }
  },
  getters: {
    getMovies: (state) => state.movies
  }
});