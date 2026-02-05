<template>
  <section class="card auth-card">
    <header>
      <h1>题云</h1>
      <p>注册或登录后即可管理题集和题目。</p>
    </header>
    <div class="auth-content">
      <div class="pane">
        <h2>登录</h2>
        <form @submit.prevent="handleLogin">
          <label>
            用户名 / 邮箱
            <input v-model="loginForm.account" placeholder="用户名或邮箱" required />
          </label>
          <label>
            密码
            <input v-model="loginForm.password" type="password" placeholder="密码" required />
          </label>
          <button class="primary-btn" type="submit" :disabled="loginLoading">
            {{ loginLoading ? '登录中...' : '登录' }}
          </button>
        </form>
      </div>
      <div class="pane">
        <h2>注册</h2>
        <form @submit.prevent="handleRegister">
          <label>
            用户名
            <input v-model="registerForm.username" placeholder="3-16 位用户名" required />
          </label>
          <label>
            邮箱
            <input v-model="registerForm.email" type="email" placeholder="name@example.com" required />
          </label>
          <label>
            密码
            <input v-model="registerForm.password" type="password" placeholder="至少 8 位" required />
          </label>
          <button class="secondary-btn" type="submit" :disabled="registerLoading">
            {{ registerLoading ? '注册中...' : '注册' }}
          </button>
        </form>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { loginUser, registerUser } from '../api/user';
import { useAuthStore } from '../stores/auth';
import { showSuccess } from '../utils/messages';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const loginForm = reactive({
  account: '',
  password: ''
});
const registerForm = reactive({
  username: '',
  email: '',
  password: ''
});

const loginLoading = ref(false);
const registerLoading = ref(false);

async function handleLogin() {
  if (!loginForm.account || !loginForm.password) {
    return;
  }
  loginLoading.value = true;
  try {
    await loginUser({ ...loginForm });
    await authStore.refreshUser();
    showSuccess('登录成功');
    const redirect = (route.query.redirect as string) || '/home';
    router.replace(redirect);
  } catch (error) {
    // 错误信息由 axios 拦截器提示
  } finally {
    loginLoading.value = false;
  }
}

async function handleRegister() {
  if (!registerForm.username || !registerForm.email || !registerForm.password) {
    return;
  }
  registerLoading.value = true;
  try {
    await registerUser({ ...registerForm });
    showSuccess('注册成功，请使用账号登录');
  } catch (error) {
    // 已统一提示
  } finally {
    registerLoading.value = false;
  }
}
</script>

<style scoped>
.auth-card {
  max-width: 960px;
  margin: 40px auto;
}

.auth-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.pane form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
