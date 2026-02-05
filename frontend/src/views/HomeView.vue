<template>
  <section class="card" v-if="user">
    <header class="section-header">
      <div class="hero">
        <img :src="profileAvatar" alt="用户头像" @error="handleAvatarError" />
        <div>
          <h2>欢迎回来，{{ user.username }}</h2>
          <p>下面是您的基本资料。</p>
        </div>
      </div>
      <button class="secondary-btn" @click="handleLogout" :disabled="loading">
        {{ loading ? '退出中...' : '退出登录' }}
      </button>
    </header>
    <div class="info-grid">
      <div><strong>用户名</strong><span>{{ user.username }}</span></div>
      <div><strong>邮箱</strong><span>{{ user.email || '未填写' }}</span></div>
      <div><strong>手机号</strong><span>{{ user.phone || '未填写' }}</span></div>
      <div><strong>状态</strong><span>{{ formatStatus(user.status) }}</span></div>
    </div>
    <div class="actions">
      <RouterLink to="/collections" class="primary-btn">进入题集管理</RouterLink>
      <RouterLink to="/avatar/edit" class="secondary-btn">更换头像</RouterLink>
    </div>
    <details class="debug">
      <summary>调试信息</summary>
      <p>用户 ID：{{ user.userId }}</p>
    </details>
  </section>
  <section class="card empty" v-else>
    <h2>尚未登录</h2>
    <p>登录后即可管理题集与题目。</p>
    <RouterLink to="/auth" class="primary-btn">前往登录</RouterLink>
  </section>
</template>

<script setup lang="ts">
import { RouterLink, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { computed, ref } from 'vue';
import { useAuthStore } from '../stores/auth';
import { logoutUser } from '../api/user';
import { showSuccess } from '../utils/messages';
import defaultAvatar from '../assets/default-avatar.svg';

const router = useRouter();
const authStore = useAuthStore();
const { user } = storeToRefs(authStore);
const loading = ref(false);

function formatStatus(status: number) {
  if (status === 1) {
    return '正常';
  }
  if (status === 0) {
    return '未激活';
  }
  return `状态 ${status}`;
}

const profileAvatar = computed(() => {
  if (!user.value?.userId) {
    return defaultAvatar;
  }
  const base = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');
  return `${base}/api/v1/user/avatar/${user.value.userId}?t=${authStore.avatarVersion}`;
});

function handleAvatarError(event: Event) {
  (event.target as HTMLImageElement).src = defaultAvatar;
}

async function handleLogout() {
  loading.value = true;
  try {
    await logoutUser();
    authStore.clear();
    showSuccess('已退出登录');
    router.replace('/auth');
  } catch (error) {
    // 统一拦截
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.hero {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hero img {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #e2e8f0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.info-grid div {
  background: #f1f5f9;
  border-radius: 10px;
  padding: 12px;
}

.info-grid strong {
  font-size: 12px;
  color: #475569;
}

.info-grid span {
  display: block;
  margin-top: 6px;
}

.actions {
  margin-top: 18px;
  display: flex;
  gap: 12px;
}

.debug {
  margin-top: 16px;
  font-size: 13px;
}

.empty {
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
