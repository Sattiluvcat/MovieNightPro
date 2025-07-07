<template>
  <div class="tags-search-container">
    <div class="tags-search-card">
      <h2 class="header-title">选择分类或语言</h2>
      <div class="selection-section">
        <div class="category-container">
          <h3 class="section-title">
            <span class="icon">🏷️</span>
            分类标签
          </h3>
          <div class="tags-grid">
            <button
                v-for="tag in tags"
                :key="tag"
                @click="toggleTag(tag)"
                :class="{ selected: selectedTags.includes(tag) }"
                class="tag-button"
            >
              {{ tag }}
            </button>
          </div>
        </div>

        <div class="divider"></div>

        <div class="language-container">
          <h3 class="section-title">
            <span class="icon">🌐</span>
            语言选择
          </h3>
          <div class="tags-grid">
            <button
                v-for="language in languages"
                :key="language"
                @click="toggleLanguage(language)"
                :class="{ selected: selectedLanguages.includes(language) }"
                class="tag-button"
            >
              {{ language }}
            </button>
          </div>
        </div>
      </div>

      <div class="action-container">
        <p class="selection-info">
          已选择:
          <span v-if="selectedTags.length > 0">{{ selectedTags.join(', ') }}</span>
          <span v-else>未选择分类</span>
          <span v-if="selectedLanguages.length > 0"> | {{ selectedLanguages.join(', ') }}</span>
        </p>
        <button
            @click="searchByTagsAndLanguages"
            class="search-button"
            :disabled="selectedTags.length === 0 && selectedLanguages.length === 0"
        >
          开始搜索
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { searchByTags } from '@/services/api';
import {mapActions} from "vuex";

export default {
  data() {
    return {
      tags: ['剧情', '喜剧', '冒险', '动画', '奇幻', '爱情', '动作', '科幻', '家庭', '犯罪', '战争', '悬疑', '历史', '惊悚', '古装',
        '传记', '歌舞', '纪录片', '音乐', '短片', '西部', '灾难', '武侠', '运动', '同性', '恐怖', '儿童', '情色'],
      languages: ['英语', '汉语普通话', '日语', '法语', '西班牙语', '德语', '粤语', '俄语', '意大利语', '韩语', '印地语', '阿拉伯语',
        '泰语', '拉丁语', '上海话', '瑞典语', '葡萄牙语', '波斯语', '辛达林语', '重庆话', '美国手语', '丹麦语', '荷兰语', '中国手语',
        '土耳其语', '四川话', '山西话', '沪语', '挪威语', '唐山话', '手语', '克罗地亚语', '科萨语', '潮州话', '武汉话', '毛利语',
        '芬兰语', '匈牙利语', '闽南语', '爱尔兰语', '苏格兰盖尔语', '多斯拉克语', '柏柏尔语', '日本手语', '意第绪语', '希腊语', '冰岛语',
        '晋语', '南非荷兰语', '湖北话', '湖南话', '蒙古语', '捷克语', '爱沙尼亚语', '越南语', '武汉方言', '四川方言', '波尼语', '南京话',
        '昆雅语', '古英语', '古代英语', '泰米尔语', '因纽特语', '塞尔维亚语', '斯瓦希里语', '祖鲁语', '山东方言', '夏威夷语', '乌尔都语',
        '乌克兰语', '索马里语', '贵州方言', '印尼语', '阿姆哈拉语'],
      selectedTags: [],
      selectedLanguages: []
    }
  },
  methods: {
    toggleTag(tag) {
      const index = this.selectedTags.indexOf(tag);
      if (index > -1) {
        this.selectedTags.splice(index, 1);
      } else {
        this.selectedTags.push(tag);
      }
    },
    toggleLanguage(language) {
      const index = this.selectedLanguages.indexOf(language);
      if (index > -1) {
        this.selectedLanguages.splice(index, 1);
      } else {
        this.selectedLanguages.push(language);
      }
    },
    ...mapActions(['updateMovies']),
    async searchByTagsAndLanguages() {
      if (this.selectedTags.length === 0 && this.selectedLanguages.length === 0) {
        alert('至少要选择一个标签噢');
        return;
      }
      try {
        const response = await searchByTags(this.selectedTags, this.selectedLanguages);
        const movies = response.data.data;
        this.updateMovies(movies);
        this.$router.push({ name: 'SearchResults'});
      } catch (error) {
        console.error(error);
      }
    }
  }
}
</script>

<style scoped>
.tags-search-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: white;
  padding: 20px;
}

.tags-search-card {
  width: 90%;
  max-width: 1000px;
  background-color: white;
  border-radius: 15px;
  box-shadow: 0 10px 30px rgba(183, 146, 146, 0.2);
  padding: 30px;
  transition: all 0.3s ease;
}

.tags-search-card:hover {
  box-shadow: 0 15px 40px rgba(183, 146, 146, 0.3);
}

.header-title {
  text-align: center;
  color: #b06767;
  font-size: 32px;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 2px dashed #e0e0e0;
}

.selection-section {
  display: flex;
  gap: 30px;
  margin-bottom: 30px;
}

.category-container, .language-container {
  flex: 1;
}

.section-title {
  color: #b06767;
  font-size: 22px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon {
  font-size: 24px;
}

.tags-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
  padding: 10px;
  border-radius: 10px;
  background-color: white;
  border: 1px solid #f0e6e6;
}

.tag-button {
  padding: 10px 15px;
  font-size: 14px;
  border: 1px solid #e0d0d0;
  border-radius: 25px;
  cursor: pointer;
  background-color: white;
  color: #555;
  transition: all 0.2s ease;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag-button:hover {
  background-color: #fdf5f5;
  transform: translateY(-2px);
  box-shadow: 0 3px 8px rgba(183, 146, 146, 0.2);
}

.tag-button.selected {
  background-color: #b06767;
  color: white;
  border-color: #b06767;
  font-weight: 600;
}

.divider {
  width: 1px;
  background-color: #f0e0e0;
  margin: 0 10px;
}

.action-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px dashed #e0e0e0;
}

.selection-info {
  font-size: 16px;
  color: #666;
  margin-bottom: 20px;
  padding: 12px 20px;
  background-color: #f9f0f0;
  border-radius: 8px;
  width: 100%;
  text-align: center;
}

.selection-info span {
  color: #b06767;
  font-weight: 500;
}

.search-button {
  padding: 15px 40px;
  font-size: 18px;
  background: linear-gradient(135deg, #b79292, #b06767);
  color: white;
  border: none;
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 600;
  letter-spacing: 1px;
  box-shadow: 0 5px 15px rgba(176, 103, 103, 0.3);
  margin-top: 10px;
}

.search-button:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(176, 103, 103, 0.4);
  background: linear-gradient(135deg, #b06767, #883333);
}

.search-button:disabled {
  background: linear-gradient(135deg, #cccccc, #aaaaaa);
  cursor: not-allowed;
  opacity: 0.7;
}

/* 滚动条样式 */
.tags-grid::-webkit-scrollbar {
  width: 8px;
}

.tags-grid::-webkit-scrollbar-track {
  background: #f9f0f0;
  border-radius: 4px;
}

.tags-grid::-webkit-scrollbar-thumb {
  background-color: #d9c2c2;
  border-radius: 4px;
}

.tags-grid::-webkit-scrollbar-thumb:hover {
  background-color: #b06767;
}

@media (max-width: 768px) {
  .selection-section {
    flex-direction: column;
    gap: 20px;
  }

  .divider {
    width: 100%;
    height: 1px;
    margin: 10px 0;
  }

  .tags-grid {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  }
}
</style>