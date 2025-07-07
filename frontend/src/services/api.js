import axios from 'axios';
import router from "@/router";
axios.defaults.withCredentials = true;

const API_URL = 'http://localhost:8081/user/movie';

// 创建 axios 实例
const apiClient = axios.create({
    baseURL: API_URL,
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
        'X-Requested-With': 'XMLHttpRequest'
    }
});

// 添加响应拦截器
apiClient.interceptors.response.use(
    response => {
        // 正常响应直接返回
        return response;
    },
    error => {
        if (error.response) {
            switch (error.response.status) {
                case 403:
                    // 跳转到登录页面
                    alert('没有权限访问该资源，请登录后重试噢');
                    router.push({ name: 'Login' });
                    break;
                case 401:
                    // 显示未授权提示
                    alert('登录已过期，请重新登录');
                    router.push({ name: 'Login' });
                    break;
                default:
                    console.error('请求错误:', error);
            }
        }
        return Promise.reject(error);
    }
);

export const login = (contact, password) => {
    return apiClient.post(`${API_URL}/login`, { contact, password });
}

export const register = (nickname, title, contact, password) => {
    return apiClient.post(`${API_URL}/register`, { nickname, title, contact, password });
}

export const logout = () => {
    return apiClient.post(`${API_URL}/logout`);
}

export const searchMovies = (keyword) => {
    return apiClient.get(`${API_URL}/search`, { params: { keyword } });
}

export const fuzzySearchMovies = (keyword) => {
    return apiClient.get(`${API_URL}/fuzzySearch`, { params: { keyword } });
}

export const getHomePage = () => {
    return apiClient.get(`${API_URL}/home`);
}

export const searchByTags = (tags, languages) => {
    return apiClient.get(`${API_URL}/categorySearch`, {
        params: {
            tags: tags.join(','),
            languages: languages.join(',')
        }
    });
}

export const getMovieDetail = (_id) => {
    return apiClient.get(`${API_URL}/${_id}`);
}

export const watchTogether = (_id) => {
    return apiClient.post(`${API_URL}/${_id}/watch-together`);
}

export const sendComment = (_id, comment) => {
    return apiClient.post(`${API_URL}/${_id}/comment`, { comment });
}

export const rateMovie = (_id, rating) => {
    return apiClient.post(`${API_URL}/${_id}/rate`, { rating });
}

export const getSuggestedMovies = () => {
    return apiClient.get(`${API_URL}/suggest`);
}

export const getAllMovies = () => {
    return apiClient.get(`${API_URL}/find-all`);
}