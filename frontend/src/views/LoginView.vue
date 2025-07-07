<template>
  <div class="welcome-message">
    <h1>{{ welcomeText }}</h1>
  </div>
  <div class="login">
    <h2>{{ isLoginMode ? '登入' : '注册' }}</h2>
    <form @submit.prevent="submitForm">
      <!-- 登录模式只显示邮箱和密码 -->
      <div v-if="isLoginMode" class="form-group">
        <label for="email">邮箱：</label>
        <input type="email" v-model="email" required>
      </div>
      <div v-if="isLoginMode" class="form-group">
        <label for="password">密码：</label>
        <input type="password" v-model="password" required>
      </div>

      <!-- 注册模式显示所有字段 -->
      <div v-if="!isLoginMode" class="form-group">
        <label for="username">用户名：</label>
        <input type="text" v-model="username" required>
      </div>
      <div v-if="!isLoginMode" class="form-group">
        <label for="title">称呼：</label>
        <input type="text" v-model="title" required>
      </div>
      <div v-if="!isLoginMode" class="form-group">
        <label for="email">邮箱：</label>
        <input type="email" v-model="email" required>
      </div>
      <div v-if="!isLoginMode" class="form-group">
        <label for="password">密码：</label>
        <input type="password" v-model="password" required>
      </div>
    </form>

    <!-- 按钮组 -->
    <div class="button-group">
      <button v-if="isLoginMode" class="submit-button" @click="login">登录</button>
      <button v-if="isLoginMode" class="register-button" @click="switchToRegister">注册</button>
      <button v-if="!isLoginMode" class="submit-button" @click="register">提交注册</button>
      <button v-if="!isLoginMode" class="back-button" @click="switchToLogin">返回登录</button>
    </div>
  </div>
</template>

<script>
import {login, register} from "@/services/api";

export default {
  data() {
    return {
      isLoginMode: true, // 默认为登录模式
      username: '',
      title: '',
      email: '',
      password: '',
      welcomeText: 'Enjoy your movie night!'
    }
  },
  methods: {
    async login() {
      try {
        const response = await login(this.email, this.password)
        if (response.data.code === 1) {
          // 保存完整的用户信息到本地存储
          const userInfo = response.data.data;
          localStorage.setItem('userInfo', JSON.stringify({
            title: userInfo.title, // 确保包含用户ID
            contact: userInfo.contact,
            nickname: userInfo.nickname,
            password: userInfo.password
          }));
          console.log("设置用户信息到本地存储:", userInfo);

          document.cookie = `JSESSIONID=${response.headers['jsessionid']}; path=/`;

          // 不需要延迟，直接跳转
          const redirect = this.$route.query.redirect;
          if (redirect) {
            this.$router.push(redirect);
          } else {
            this.$router.push({ name: 'Home' });
          }
        } else {
          alert('登录失败: ' + response.data.msg)
        }
      } catch (error) {
        console.error(error)
        alert('出错了！请重试~')
      }
    },
    async register() {
      if (!this.username || !this.title || !this.email || !this.password) {
        alert('所有信息都必须填写噢~');
        return;
      }
      try {
        const response = await register(this.username,this.title,this.email,this.password)
        if (response.data.code === 1) {
          alert('注册成功！请登录');
          this.switchToLogin();
          // 清空表单
          this.username = '';
          this.title = '';
          this.email='';
          this.password = '';
        } else {
          alert('注册失败: ' + response.data.msg)
        }
      } catch (error) {
        console.error(error)
        alert('出错了！请重试~')
      }
    },
    switchToRegister() {
      this.isLoginMode = false;
      this.welcomeText = 'Enjoy your movie night!';
    },
    switchToLogin() {
      this.isLoginMode = true;
      this.welcomeText = 'Enjoy your movie night!';
      // 清空表单
      this.username = '';
      this.title = '';
      this.email='';
      this.password = '';
    },
    submitForm() {
      // 处理表单提交（防止回车键提交）
      return false;
    },
    handleKeydown(event) {
      if (event.key === 'Enter') {
        if (this.isLoginMode) {
          this.login();
        } else {
          this.register();
        }
      }
    }
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Roboto:wght@400;700&display=swap');

.welcome-message {
  text-align: center;
  margin-bottom: 20px;
}

.welcome-message h1 {
  margin-bottom: 50px;
  display: inline-block;
  overflow: hidden;
  border-right: .15em solid #b06767;
  white-space: nowrap;
  max-width: 26ch;
  letter-spacing: .15em;
  animation: typing 3.5s steps(40, end), blink-caret .75s step-end infinite;
  font-family: 'Technic';
}

@keyframes typing {
  from { width: 0 }
  to { width: 100% }
}

@keyframes blink-caret {
  from, to { border-color: transparent }
  50% { border-color: #b06767; }
}

.login {
  max-width: 350px;
  margin: 0 auto;
  padding: 20px;
  border: 1px solid #ccc;
  border-radius: 10px;
  background-color: #f9f9f9;
}

h2 {
  text-align: center;
  margin-bottom: 20px;
}

.form-group {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

label {
  flex: 1;
  font-weight: bold;
  text-align: center;
}

input[type="text"],
input[type="email"],
input[type="password"] {
  flex: 2;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  box-sizing: border-box;
}

.button-group {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
}

.submit-button, .register-button, .back-button {
  padding: 10px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}

.submit-button {
  background-color: #b79292;
  color: white;
  flex: 1;
  margin-right: 10px;
}

.submit-button:hover {
  background-color: #883333;
}

.register-button {
  background-color: #ccc;
  color: white;
  flex: 1;
}

.register-button:hover {
  background-color: #aaaaaa;
}

.back-button {
  background-color: #cccccc;
  color: #333333;
  flex: 1;
  margin-left: 10px;
}

.back-button:hover {
  background-color: #aaaaaa;
}
</style>