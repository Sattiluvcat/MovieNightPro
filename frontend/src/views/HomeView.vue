<template>
  <div class="home">
    <div class="user-info">
      <span>Enjoy your movie night, {{ userTitle+" "+userName+ " 😺" }}</span>
      <button @click="logout" class="logout-button">登出</button>
    </div>
    <div class="typing-effect">
      <p class="typing-text">Select one for movie night</p>
    </div>
    <div class="search-container">
      <input
          type="text"
          v-model="searchKeyword"
          @keydown="handleKeydown"
          placeholder="输入电影名 / 导演 / 演员精确搜索️🙂 或者 输入电影描述模糊搜索😶‍🌫"
          class="search-box"
      />
      <button @click="searchByKeyword" class="search-button">精确搜索</button>
      <button @click="fuzzySearch" class="fuzzy-search-button">模糊搜索</button>
    </div>
    <div class="category-search-container">
      <button @click="fetchAllMovies" class="view-all-button">查看全部电影😎</button>
      <button @click="goToTagsSearch" class="category-search-button">通过标签搜索😎</button>
    </div>

    <!-- 三列布局容器 -->
    <div class="three-column-container">
      <!-- 左侧：热度榜 -->
      <div class="side-column">
        <h3 class="column-title">热度榜🔥</h3>
        <div
            v-for="(movie, index) in topMovies"
            :key="movie._id"
            class="side-item"
            @click="goToMovieDetail(movie._id)"
        >
          <div class="side-content">
            <span class="side-index">{{ index + 1 }}.</span>
            <span class="side-title">{{ movie.title }}</span>
          </div>
          <span class="side-tags">{{ movie.tags.join(', ') }}</span>
        </div>
      </div>

      <!-- 中间：推荐电影（最宽） -->
      <div class="recommendation-column">
        <div class="recommendation-header">
          <p class="header-text">推荐电影</p>
          <hr class="header-line">
        </div>
        <div
            v-for="movie in suggestMovies"
            :key="movie._id"
            class="movie-item"
            @click="goToMovieDetail(movie._id)"
        >
          <div class="movie-header">
            <h2 class="movie-title">{{ movie.title }}</h2>
            <p class="movie-score">评分: <span class="score-value">{{ movie.star_mine }}</span> / 10</p>
          </div>
          <div class="movie-details">
            <div class="left-column">
              <p class="movie-actors"><span class="detail-label">演员:</span> {{ movie.actor_actresses.join(', ') }}</p>
              <p class="movie-tags"><span class="detail-label">标签:</span> {{ movie.tags.join(', ') }}</p>
              <p class="movie-languages"><span class="detail-label">语言:</span> {{ movie.languages.join(', ') }}</p>
            </div>
            <div class="right-column">
              <p class="movie-release"><span class="detail-label">上映时间:</span> {{ movie.release_time }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：每日推荐 -->
      <div class="side-column">
        <h3 class="column-title">个性推荐💕</h3>
        <div
            v-for="movie in dailyMovies"
            :key="movie._id"
            class="side-item"
            @click="goToMovieDetail(movie._id)"
        >
          <div class="side-content">
            <span class="side-title">{{ movie.title }}</span>
          </div>
          <span class="side-tags">{{ movie.tags.join(', ') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  getSuggestedMovies,
  searchMovies,
  getAllMovies,
  logout,
  fuzzySearchMovies,
  getHomePage
} from "@/services/api";
import { mapActions } from "vuex";

export default {
  data() {
    return {
      userTitle: '',
      userName: '',
      searchKeyword: '',
      topMovies: [],      // 左侧热度榜数据
      suggestMovies: [],  // 中间推荐电影数据
      dailyMovies: []     // 右侧每日推荐数据
    }
  },
  methods: {
    ...mapActions(['updateMovies']),
    async logout() {
      try {
        await logout();
        this.$router.push({name: 'Login'});
      } catch (error) {
        console.error('登出失败:', error);
      }
    },
    async searchByKeyword() {
      if (!this.searchKeyword) {
        alert('请输入搜索关键词');
        return;
      }
      try {
        const response = await searchMovies(this.searchKeyword);
        const movies = response.data.data;
        this.updateMovies(movies);
        this.$router.push({name: 'SearchResults'});
      } catch (error) {
        console.error(error);
      }
    },
    async fuzzySearch() {
      if (!this.searchKeyword) {
        alert('请输入搜索关键词');
        return;
      }
      try{
        const response=await fuzzySearchMovies(this.searchKeyword);
        const movies = response.data.data;
        this.updateMovies(movies);
        this.$router.push({name: 'SearchResults'});
      }catch(error){
        console.error(error);
      }
    },
    async fetchAllMovies() {
      try {
        const response = await getAllMovies();
        const movies = response.data.data;
        await this.updateMovies(movies);
        this.$router.push({name: 'SearchResults'});
      } catch (error) {
        console.error(error);
      }
    },
    goToTagsSearch() {
      this.$router.push({name: 'TagsSearch'});
    },
    async fetchHomeData() {
      try {
        // 获取主页数据（包含10个电影）
        const response = await getHomePage();
        if (response.data.code === 1 && response.data.data) {
          const homeData = response.data.data;

          // 分割数据：前5个是热度榜，后5个是每日推荐
          this.topMovies = homeData.slice(0, 5);
          this.dailyMovies = homeData.slice(5, 10);
        } else {
          console.error('获取主页数据失败:', response.data.msg);
        }
      } catch (error) {
        console.error('获取主页数据失败', error);
      }
    },
    async fetchSuggestedMovies() {
      try {
        const response = await getSuggestedMovies();
        if (response.data.code === 1 && response.data.data) {
          this.suggestMovies = response.data.data;
        } else {
          console.error('获取推荐电影失败:', response.data.msg);
        }
      } catch (error) {
        console.error(error);
      }
    },
    goToMovieDetail(_id) {
      this.$router.push({name: 'MovieDetail', params: {_id}});
    },
    handleKeydown(event) {
      if (event.key === 'Enter') {
        if (confirm('请选择搜索方式：\n\n确定 - 精确搜索\n取消 - 模糊搜索')) {
          this.searchByKeyword();
        } else {
          this.fuzzySearch();
        }
      }
    }
  },
  async created() {
    // 从本地存储获取用户信息
    const userInfo = JSON.parse(localStorage.getItem('userInfo'));
    if (userInfo) {
      this.userTitle = userInfo.title;
      this.userName = userInfo.nickname;
    }

    // 然后按顺序加载数据
    await this.fetchHomeData(); // 这会帮助建立会话
    await this.fetchSuggestedMovies(); // 在会话建立后调用
  }
}
</script>

<style scoped>
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
}

.user-info {
  position: absolute;
  top: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.logout-button {
  padding: 5px 10px;
  background-color: #b79292;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}

.logout-button:hover {
  background-color: #883333;
}


.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 0;
  padding-top: 0;
}

.typing-effect {
  text-align: center;
  margin-top: 0;
  padding-top: 0;
}

.typing-text {
  font-size: 30px;
  font-weight: bold;
  font-family: 'Technic';
  overflow: hidden;
  white-space: nowrap;
  border-right: .15em solid #b06767;
  max-width: 22.5ch;
  animation: typing 3.5s steps(40, end), blink-caret .75s step-end infinite;
  text-align: center;
  margin-bottom: 30px;
}

@keyframes typing {
  from { width: 0 }
  to { width: 100% }
}

@keyframes blink-caret {
  from, to { border-color: transparent }
  50% { border-color: #b06767; }
}

.search-container {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  gap: 10px; /* 添加按钮间距 */
}

.search-box {
  width: 500px; /* 稍微减小宽度以容纳新按钮 */
  padding: 10px;
  font-size: 16px;
}

.search-button, .fuzzy-search-button {
  padding: 10px 20px;
  font-size: 16px;
  background-color: #b79292;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  white-space: nowrap; /* 防止按钮文字换行 */
}

.search-button:hover, .fuzzy-search-button:hover {
  background-color: #883333;
}

.search-button:hover, .fuzzy-search-button:hover {
  background-color: #883333;
}

.fuzzy-search-button {
  background-color: #b79292; /* 紫色调 */
}

.fuzzy-search-button:hover {
  background-color: #883333;
}

.category-search-container {
  display: flex;
  align-items: center;
  margin-top: 20px;
}

.view-all-button {
  padding: 20px 30px;
  font-size: 20px;
  background-color: #b79292;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  margin-right: 40px;
}

.view-all-button:hover {
  background-color: #883333;
}

.category-search-button {
  padding: 20px 30px;
  font-size: 20px;
  background-color: #b79292;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}

.category-search-button:hover {
  background-color: #883333;
}

.recommendation-header {
  text-align: center;
  margin-top: 20px;
}

.header-text {
  font-size: 26px;
  font-weight: bold;
  margin-bottom: 10px;
}

.header-line {
  width: 80%;
  margin: 0 auto;
  border: none;
  border-top: 2px solid #b79292;
}

.three-column-container {
  display: flex;
  width: 100%;
  margin-top: 30px;
  gap: 20px;
  align-items: flex-start;
}

.side-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0; /* 移除内边距 */
}

/* 项目样式 */
.side-item {
  padding: 12px 15px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  width: 100%;
  max-width: 280px;
  text-align: center;
  background-color: #f9f9f9;
  margin-bottom: 10px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
  border: 1px solid #e0e0e0; /* 添加边框 */
}

.side-item:hover {
  background-color: #fffbfb; /* 更温暖的悬停背景色 */
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(183, 146, 146, 0.2); /* 使用主题色的阴影 */
  border-color: #b79292; /* 悬停时边框颜色变化 */
}

.side-content {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 5px;
  width: 100%;
}

.side-index {
  font-weight: bold;
  margin-right: 8px;
  color: #b06767; /* 更深的主题色 */
  font-size: 16px; /* 稍大字号 */
}

.side-title {
  font-size: 15px;
  font-weight: 600; /* 加粗 */
  text-align: center;
  flex-grow: 1;
  color: #333; /* 深色文字 */
}

.side-tags {
  font-size: 13px;
  color: #b06767; /* 使用主题色 */
  display: block;
  text-align: center;
  padding-top: 5px;
  font-style: italic; /* 斜体 */
}

/* 中间列样式 */
.recommendation-column {
  flex: 2;
  margin: 0 10px; /* 添加左右边距 */
}

.recommendation-header {
  margin-bottom: 20px;
}

.movie-item {
  margin: 15px 0;
  padding: 15px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.08);
  border: 1px solid #eee;
  transition: all 0.3s ease;
}

.movie-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(183, 146, 146, 0.25);
  border-color: #b79292;
}

.movie-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  border-bottom: 1px dashed #e0e0e0;
  padding-bottom: 10px;
}

.movie-title {
  font-size: 20px;
  font-weight: bold;
  color: #b06767; /* 使用主题色 */
}

.movie-score {
  font-size: 16px;
  font-weight: bold;
}

.score-value {
  color: #b06767; /* 评分值使用主题色 */
  font-size: 18px;
}

.detail-label {
  font-weight: bold;
  color: #b06767; /* 标签使用主题色 */
}

.movie-details {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #555;
}

.left-column, .right-column {
  display: flex;
  flex-direction: column;
}

/* 列标题样式 */
.column-title {
  text-align: center;
  margin-bottom: 15px;
  color: #b06767; /* 更深的主题色 */
  font-size: 22px;
  font-weight: bold;
  padding: 8px 15px;
  background-color: white; /* 浅背景色 */
  border-radius: 8px;
  width: 100%;
  max-width: 280px;
}

.movie-actors,
.movie-tags,
.movie-languages {
  font-size: 14px;
  margin-bottom: 5px;
  text-align: left;
}

.movie-release {
  font-size: 14px;
  margin-bottom: 5px;
}
</style>