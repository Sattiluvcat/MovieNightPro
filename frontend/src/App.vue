<template>
  <nav>
<!--    <router-link to="/">Home</router-link> |-->
<!--    <router-link to="/about">About</router-link>-->
  </nav>
  <router-view/>
</template>

<script>
export default {
  mounted() {
    // 监听未处理的 Promise 错误
    window.addEventListener('unhandledrejection', event => {
      if (event.reason.response && event.reason.response.status === 403) {
        this.$router.push({ name: 'Login' });
        event.preventDefault(); // 阻止默认行为
      }
    });
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
}

nav {
  padding: 30px;
}

nav a {
  font-weight: bold;
  color: #2c3e50;
}

nav a.router-link-exact-active {
  color: #42b983;
}
</style>
