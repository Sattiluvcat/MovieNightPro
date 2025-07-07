<template>
  <div class="movie-detail-container">
    <div class="beautify-element">
      <hr class="beautify-line" />
      <p class="beautify-text">Welcome to the Movie Detail Page!🤩</p>
      <hr class="beautify-line" />
    </div>

    <div v-if="movie" class="movie-detail">
      <div class="detail-card">
        <div class="top-section">
          <div class="left-column">
            <img referrerpolicy="no-referrer" :src="cover_url" alt="Movie Cover" class="movie-cover" />
          </div>

          <div class="middle-column">
            <div class="info-card">
              <h1 class="movie-title">{{ movie.title }}</h1>
              <div class="info-row">
                <div class="info-item">
                  <span class="info-label">导演:</span>
                  <span class="info-value">{{ movie.director }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">时长:</span>
                  <span class="info-value">{{ movie.duration }}</span>
                </div>
              </div>
              <div class="info-item">
                <span class="info-label">演员:</span>
                <span class="info-value">{{ movie.actor_actresses.join(', ') }}</span>
              </div>
            </div>
          </div>

          <div class="right-column">
            <div class="rating-card">
              <div class="rating-item">
                <span class="rating-label">豆瓣评分:</span>
                <span class="rating-value">{{ score_official }}</span>
              </div>
              <div class="rating-item">
                <span class="rating-label">网站评分:</span>
                <span class="rating-value">{{ movie.star_mine }} / 10</span>
              </div>
              <div class="rating-item">
                <span class="rating-label">平均评分:</span>
                <span class="rating-value">{{ avgRating.toFixed(1) }} / 10</span>
              </div>
              <div class="user-rating">
                <span class="rating-label">我的评分:</span>
                <div class="rating-control">
                  <div class="stars">
                    <span
                        v-for="star in 10"
                        :key="star"
                        class="star"
                        :class="{ 'active': star <= userRating }"
                        @click="rateMovie(star)"
                    >
                      ★
                    </span>
                  </div>
                  <span v-if="userRating > 0" class="current-rating">{{ userRating }}/10</span>
                </div>
              </div>
              <button @click="watchTogether" class="watch-together-button">一起看</button>
            </div>
          </div>
        </div>

        <div class="bottom-section">
          <!-- 修改后的标签和语言区域 -->
          <div class="meta-row">
            <div class="meta-item tags-container">
              <span class="meta-label">标签：</span>
              <span class="tag-bubble" v-for="tag in movie.tags" :key="tag">
                {{ tag }}
              </span>
            </div>
            <div class="meta-item languages-container">
              <span class="meta-label">语言：</span>
              <span class="language-bubble" v-for="language in movie.languages" :key="language">
                {{ language }}
              </span>
            </div>
          </div>

          <div class="summary-card">
            <h3 class="summary-title">简介</h3>
            <p class="movie-summary preserve-whitespace">{{ summary }}</p>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="loading-message">
      加载中 请稍候😣
    </div>

    <div class="comment-section">
      <input
          v-model="comment"
          @keydown="handleKeydown"
          placeholder="看过了？请跟Satti分享评论😸（按下Enter就会发送噢）"
          class="comment-input"
      />
      <button @click="sendComment" class="send-comment-button">发送评论</button>
    </div>
  </div>
</template>

<script>
import {getMovieDetail, watchTogether, sendComment, rateMovie} from '@/services/api';

export default {
  props: ['_id'],
  data() {
    return {
      movie: null,
      cover_url: '',
      score_official: '',
      summary: '',
      comment: '',
      userRating: 0,   // 当前用户评分
      avgRating: 0     // 平均评分
    }
  },
  async created() {
    await this.fetchMovieDetail();
  },
  methods: {
    async fetchMovieDetail() {
      try {
        const response = await getMovieDetail(this._id);
        if (response && response.data) {
          this.movie = response.data.data.movie;
          this.cover_url = response.data.data.cover_url;
          this.score_official = response.data.data.score_official;
          this.summary = response.data.data.summary;

          // 设置评分数据
          this.avgRating = response.data.data.score_even || 0;
          this.userRating = response.data.data.score_user || 0;
        }
      } catch (error) {
        console.error('抓取电影内容时出错了:', error);
      }
    },
    async rateMovie(rating) {
      // 保存本地状态
      this.userRating = rating;

      try {
        // 调用评分API
        const response = await rateMovie(this._id, rating.toString());

        if (response.data.code === 0) {
          alert('评分失败: ' + response.data.msg);
          this.userRating = 0; // 重置评分显示
        } else {
          // 重新获取电影详情更新平均分
          await this.fetchMovieDetail();
        }
      } catch (error) {
        console.error('评分出错:', error);
        this.userRating = 0;
      }
    },
    async watchTogether() {
      try {
        const response = await watchTogether(this._id);
        console.log('一起看 响应:', response);
        if (response.data.code === 0)
          alert('请登录后再发送噢');
        else
          alert('已向Satti发送一起看邀请');
      } catch (error) {
        console.error('一起看功能出错了:', error);
      }
    },
    async sendComment() {
      try {
        const response = await sendComment(this._id, this.comment);
        console.log('发送评论 响应:', response);
        if (response.data.code === '0')
          alert('请登录后再发送噢');
        else
          alert('已向Satti发送电影评论');
        this.comment = ''; // Clear the input after sending the comment
      } catch (error) {
        console.error('发送评论功能出错了:', error);
      }
    },
    handleKeydown(event) {
      if (event.key === 'Enter') {
        this.sendComment();
      }
    }
  }
}
</script>

<style scoped>
.user-rating {
  margin-bottom: 25px;
}

.rating-control {
  display: flex;
  align-items: center;
}

.stars {
  display: inline-flex;
}

.star {
  font-size: 24px;
  color: #ccc;
  cursor: pointer;
  transition: color 0.2s;
  margin-right: 2px;
}

.star:hover,
.star.active {
  color: #ffc107;
}

.current-rating {
  margin-left: 10px;
  font-size: 16px;
  font-weight: bold;
  color: #b06767;
}

.movie-detail-container {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(135deg, #f9f5f5, #f0e8e8);
  padding-top: 90px;
  padding-bottom: 80px;
}

.beautify-element {
  position: fixed;
  top: 0;
  width: 100%;
  text-align: center;
  background: linear-gradient(to right, #b06767, #b79292);
  padding: 15px 0;
  box-shadow: 0 4px 15px rgba(176, 103, 103, 0.3);
  z-index: 1000;
}

.beautify-line {
  width: 80%;
  margin: 0 auto;
  border: none;
  border-top: 1px solid rgba(255, 255, 255, 0.5);
}

.beautify-text {
  font-size: 28px;
  font-weight: bold;
  color: white;
  overflow: hidden;
  white-space: nowrap;
  max-width: 29ch;
  animation: typing 3.5s steps(30, end), blink-caret .75s step-end infinite;
  text-align: center;
  margin: 15px auto;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

@keyframes typing {
  from { width: 0 }
  to { width: 100% }
}

@keyframes blink-caret {
  from, to { border-color: transparent }
  50% { border-color: white; }
}

.movie-detail {
  display: flex;
  justify-content: center;
  padding: 0 20px;
}

.detail-card {
  width: 90%;
  max-width: 1200px;
  background: white;
  border-radius: 15px;
  box-shadow: 0 10px 30px rgba(183, 146, 146, 0.2);
  overflow: hidden;
  padding: 30px;
}

.top-section {
  display: flex;
  flex-wrap: wrap;
  gap: 30px;
  margin-bottom: 30px;
}

.left-column {
  flex: 0 0 250px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.movie-cover {
  width: 100%;
  height: 350px;
  object-fit: cover;
  border-radius: 10px;
  box-shadow: 0 8px 20px rgba(176, 103, 103, 0.3);
  transition: transform 0.3s ease;
}

.movie-cover:hover {
  transform: scale(1.03);
}

.middle-column {
  flex: 1;
  min-width: 300px;
}

.right-column {
  flex: 0 0 280px;
}

.info-card {
  background-color: #fcf5f5;
  border-radius: 10px;
  padding: 20px;
  height: 100%;
}

.movie-title {
  font-size: 32px;
  font-weight: bold;
  color: #b06767;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px dashed #e0d0d0;
}

.info-item {
  margin-bottom: 15px;
  display: flex;
  align-items: flex-start;
}

.info-label {
  font-weight: bold;
  color: #b06767;
  min-width: 60px;
  margin-right: 10px;
}

.info-value {
  color: #555;
  line-height: 1.6;
}

.rating-card {
  background-color: #fcf5f5;
  border-radius: 10px;
  padding: 20px;
  height: 100%;
}

.rating-item {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.rating-label {
  font-weight: bold;
  color: #b06767;
  min-width: 80px;
  margin-right: 10px;
}

.rating-value {
  font-size: 20px;
  font-weight: bold;
  color: #555;
}

.user-rating {
  display: flex;
  align-items: center;
  margin-bottom: 25px;
}

.watch-together-button {
  padding: 12px 30px;
  font-size: 16px;
  background: linear-gradient(to right, #b79292, #b06767);
  color: white;
  border: none;
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: bold;
  box-shadow: 0 4px 10px rgba(183, 146, 146, 0.3);
  width: 100%;
}

.watch-together-button:hover {
  background: linear-gradient(to right, #b06767, #883333);
  transform: translateY(-3px);
  box-shadow: 0 6px 15px rgba(176, 103, 103, 0.4);
}

.bottom-section {
  background-color: #fcf5f5;
  border-radius: 10px;
  padding: 25px;
}

/* 修改后的标签和语言样式 */
.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 25px;
  background: white;
  border-radius: 10px;
  padding: 15px;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.05);
}

.meta-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 300px;
}

.meta-label {
  font-weight: bold;
  color: #b06767;
  font-size: 16px;
  min-width: 50px;
}

.tag-bubble, .language-bubble {
  padding: 8px 15px;
  border-radius: 20px;
  font-size: 14px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.tag-bubble {
  background-color: #b06767;
  color: white;
}

.language-bubble {
  background-color: #b79292;
  color: white;
}

.summary-card {
  background-color: white;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.05);
}

.summary-title {
  color: #b06767;
  font-size: 22px;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #e0d0d0;
}

.movie-summary {
  color: #555;
  line-height: 1.8;
  font-size: 16px;
  text-align: center;
}

.preserve-whitespace {
  white-space: pre-wrap;
  text-align: center;
}

.loading-message {
  text-align: center;
  font-size: 20px;
  color: #b06767;
  padding: 50px;
  background-color: #fcf5f5;
  border-radius: 10px;
  margin: 30px auto;
  max-width: 500px;
}

.comment-section {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  padding: 15px;
  background: linear-gradient(to right, #b06767, #b79292);
  box-shadow: 0 -4px 15px rgba(176, 103, 103, 0.3);
  z-index: 1000;
}

.comment-input {
  flex: 1;
  padding: 12px 20px;
  font-size: 16px;
  border: none;
  border-radius: 30px;
  margin-right: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.send-comment-button {
  padding: 12px 30px;
  font-size: 16px;
  background: white;
  color: #b06767;
  border: none;
  border-radius: 30px;
  cursor: pointer;
  font-weight: bold;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.send-comment-button:hover {
  background: #f0f0f0;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

@media (max-width: 900px) {
  .top-section {
    flex-direction: column;
  }

  .left-column {
    flex: 0 0 auto;
    margin-bottom: 20px;
  }

  .movie-cover {
    max-width: 250px;
  }

  .right-column {
    flex: 0 0 auto;
  }

  .detail-card {
    padding: 20px;
  }

  .meta-item {
    min-width: 100%;
  }
}

@media (max-width: 600px) {
  .beautify-text {
    font-size: 20px;
    max-width: 22ch;
  }

  .comment-section {
    flex-direction: column;
    gap: 10px;
  }

  .comment-input {
    margin-right: 0;
    margin-bottom: 10px;
    width: 100%;
  }

  .send-comment-button {
    width: 100%;
  }
}
</style>