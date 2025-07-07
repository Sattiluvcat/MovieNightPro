<template>
  <div class="search-results">
    <p class="header-message">这里是所有搜索结果😁</p>
    <div v-if="movies && movies.length > 0">
      <p class="results-info">本页为第 {{  movies.length === 0 ? 0 :(currentPage-1)*10+1 }} - {{  movies.length === 0 ? 0 :(currentPage-1)*10+paginatedMovies.length }} 条记录，共找到 {{ movies.length }} 条记录😁</p>
      <div v-for="movie in paginatedMovies" :key="movie._id" class="movie-item" @click="goToMovieDetail(movie._id)">
        <div class="movie-header">
          <h2 class="movie-title">{{ movie.title }}</h2>
          <p class="movie-score">评分: <span class="score-value">{{ movie.star_mine }}</span> / 10</p>
        </div>
        <div class="movie-details">
          <!-- 新增：显示简介 -->
          <div v-if="movie.summary" class="summary-container">
            <div class="actor-release-row">
              <p class="movie-actors"><b class="detail-label">演员:</b> {{ movie.actor_actresses.join(', ') }}</p>
              <p class="movie-release"><b class="detail-label">上映时间:</b> {{ movie.release_time }}</p>
            </div>
            <p class="movie-summary"><b class="detail-label">简介:</b> {{ truncateSummary(movie.summary, 200) }}</p>
          </div>

          <!-- 原有信息（当没有简介时显示） -->
          <template v-else>
            <div class="left-column">
              <p class="movie-actors"><b class="detail-label">演员:</b> {{ movie.actor_actresses.join(', ') }}</p>
              <p class="movie-tags"><b class="detail-label">标签:</b> {{ movie.tags.join(', ') }}</p>
              <p class="movie-languages"><b class="detail-label">语言:</b> {{ movie.languages.join(', ') }}</p>
            </div>
            <div class="right-column">
              <p class="movie-release"><b class="detail-label">上映时间:</b> {{ movie.release_time }}</p>
            </div>
          </template>
        </div>
      </div>
    </div>
    <div v-else class="no-results">
      没有相关电影噢，请重试😢
    </div>
    <div class="pagination">
      <button @click="prevPage" :disabled="currentPage === 1">上一页</button>
      <span v-for="page in paginationPages" :key="page">
        <button v-if="page !== '...'" @click="goToPage(page)" :class="{ active: currentPage === page }">{{ page }}</button>
        <span v-else>...</span>
      </span>
      <button @click="nextPage" :disabled="currentPage === totalPages">下一页</button>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';

export default {
  data() {
    return {
      currentPage: 1,
      itemsPerPage: 10 // Each page shows 10 movies
    };
  },
  computed: {
    ...mapGetters(['getMovies']),
    movies() {
      return this.getMovies;
    },
    totalPages() {
      return Math.ceil(this.movies.length / this.itemsPerPage);
    },
    paginatedMovies() {
      const start = (this.currentPage - 1) * this.itemsPerPage;
      const end = start + this.itemsPerPage;
      return this.movies.slice(start, end);
    },
    paginationPages() {
      const pages = [];
      if (this.totalPages <= 5) {
        for (let i = 1; i <= this.totalPages; i++) {
          pages.push(i);
        }
      } else {
        if (this.currentPage > 3) {
          pages.push(1);
          pages.push('...');
        }
        for (let i = Math.max(1, this.currentPage - 2); i <= Math.min(this.totalPages, this.currentPage + 2); i++) {
          pages.push(i);
        }
        if (this.currentPage < this.totalPages - 2) {
          pages.push('...');
          pages.push(this.totalPages);
        }
      }
      return pages;
    }
  },
  methods: {
    goToMovieDetail(_id) {
      this.$router.push({ name: 'MovieDetail', params: { _id } });
    },
    truncateSummary(summary, maxLength) {
      if (!summary) return '';
      return summary.length > maxLength
          ? summary.substring(0, maxLength) + '...'
          : summary;
    },
    nextPage() {
      if (this.currentPage < this.totalPages) {
        this.currentPage++;
      }
    },
    prevPage() {
      if (this.currentPage > 1) {
        this.currentPage--;
      }
    },
    goToPage(page) {
      if (page !== '...') {
        this.currentPage = page;
      }
    }
  }
}
</script>

<style scoped>
.search-results {
  text-align: center;
  margin-top: 20px;
  padding: 0 20px;
}

.header-message {
  font-size: 26px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #b06767;
}

.results-info {
  font-size: 16px;
  margin-bottom: 30px;
  color: #666;
}

.actor-release-row {
  display: flex;
  justify-content: space-between;
  width: 100%;
  margin-bottom: 5px;
}

/* 新增：简介容器样式 */
.summary-container {
  width: 100%;
}

.movie-release {
  font-size: 16px;
  margin-bottom: 5px;
  text-align: right; /* 保持右对齐 */
}

.movie-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin: 20px auto;
  padding: 20px;
  width: 80%;
  background-color: #fff;
  border-radius: 10px;
  cursor: pointer;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
  border: 1px solid #eee;
  transition: all 0.3s ease;
}

.movie-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px rgba(183, 146, 146, 0.3);
  border-color: #b79292;
}

.movie-header {
  display: flex;
  justify-content: space-between;
  width: 100%;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px dashed #e0e0e0;
}

.movie-title {
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 2px;
  color: #b06767;
}

.movie-score {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 0;
}

.score-value {
  color: #b06767;
  font-size: 22px;
}

.movie-details {
  display: flex;
  justify-content: space-between;
  width: 100%;
  margin-top: 10px;
}

/* 新增：简介容器样式 */
.summary-container {
  width: 100%;
}

.movie-summary {
  font-size: 16px;
  line-height: 1.6;
  text-align: left;
  color: #555;
  margin-top: 10px;
  display: -webkit-box;
  line-clamp: 3; /* 限制显示3行 */
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.left-column,
.right-column {
  display: flex;
  flex-direction: column;
}

.movie-tags,
.movie-languages {
  font-size: 16px;
  margin-bottom: 8px;
  text-align: left;
  color: #555;
}

.movie-actors {
  font-size: 16px;
  margin-bottom: 8px;
  text-align: left;
  color: #555;
}

.movie-release {
  font-size: 16px;
  margin-bottom: 8px;
  color: #555;
}

.detail-label {
  color: #b06767;
  font-weight: 600;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 40px;
  margin-bottom: 30px;
}

.pagination button {
  padding: 10px 20px;
  font-size: 16px;
  margin: 0 10px;
  background-color: #b79292;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 80px;
}

.pagination button:hover:not(:disabled) {
  background-color: #883333;
  transform: translateY(-2px);
}

.pagination button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.pagination button.active {
  background-color: #883333;
  font-weight: bold;
}

.no-results {
  font-size: 18px;
  color: #b06767;
  margin: 40px 0;
  padding: 20px;
  background-color: #fcf5f5;
  border-radius: 10px;
  display: inline-block;
}
</style>