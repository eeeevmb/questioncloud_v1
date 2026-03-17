<template>
  <el-card class="auth-card">
    <template #header>
      <div class="header">
        <h1>题云</h1>
        <p>注册或登录后即可管理题集和题目。</p>
      </div>
    </template>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="登录" name="login">
        <el-form label-position="top" @submit.prevent="handleLogin">
          <el-form-item label="用户名 / 邮箱">
            <el-input v-model="loginForm.account" placeholder="用户名或邮箱" clearable />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="loginForm.password" type="password" placeholder="密码" show-password />
          </el-form-item>
          <el-button type="primary" :loading="loginLoading" @click="handleLogin">登录</el-button>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="注册" name="register">
        <el-form label-position="top" @submit.prevent="handleRegister">
          <el-form-item label="用户名">
            <el-input v-model="registerForm.username" placeholder="3-16 位用户名" clearable />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="registerForm.email" type="email" placeholder="name@example.com" clearable />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="registerForm.password" type="password" placeholder="至少 8 位" show-password />
          </el-form-item>
          <el-button type="success" :loading="registerLoading" @click="handleRegister">注册</el-button>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </el-card>
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

const activeTab = ref<'login' | 'register'>('login');
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
    activeTab.value = 'login';
  } catch (error) {
    // 已统一提示
  } finally {
    registerLoading.value = false;
  }
}
</script>

<style scoped>
.auth-card {
  max-width: 560px;
  margin: 48px auto;
}

.header h1 {
  margin: 0;
}

.header p {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
}
</style>
