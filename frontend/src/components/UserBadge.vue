<template>
  <div class="user-badge" ref="badgeRef" @click="toggleMenu">
    <img :src="avatarSrc" :alt="displayName" @error="handleImgError" />
    <div class="info">
      <span class="name">{{ displayName }}</span>
      <span class="mail">{{ subtitle }}</span>
    </div>
    <div class="dropdown" v-if="menuVisible" @click.stop>
      <template v-if="authStore.isAuthenticated">
        <p class="title">账户信息</p>
        <p><strong>用户名</strong> {{ authStore.user?.username ?? '未登录' }}</p>
        <p><strong>邮箱</strong> {{ authStore.user?.email ?? '未填写' }}</p>
        <div class="actions">
          <button class="secondary-btn" type="button" @click="goProfile">个人资料</button>
          <button class="danger-btn" type="button" @click="handleLogout" :disabled="logoutLoading">
            {{ logoutLoading ? '退出中...' : '退出登录' }}
          </button>
        </div>
      </template>
      <template v-else>
        <p class="title">未登录</p>
        <p>登录以管理题集和题目。</p>
        <button class="primary-btn" type="button" @click="goLogin">前往登录</button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { logoutUser } from '../api/user';
import { showSuccess } from '../utils/messages';
import defaultAvatar from '../assets/default-avatar.svg';

const authStore = useAuthStore();
const router = useRouter();
const menuVisible = ref(false);
const logoutLoading = ref(false);
const badgeRef = ref<HTMLElement | null>(null);

const avatarSrc = computed(() => {
  const id = authStore.user?.userId;
  if (!id) {
    return defaultAvatar;
  }
  const base = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');
  return `${base}/api/v1/user/avatar/${id}?t=${authStore.avatarVersion}`;
});

const displayName = computed(() => authStore.user?.username ?? '未登录');
const subtitle = computed(() => {
  if (!authStore.isAuthenticated) {
    return '点击登录';
  }
  return authStore.user?.email ?? '点击查看';
});

function toggleMenu() {
  menuVisible.value = !menuVisible.value;
}

function handleImgError(event: Event) {
  (event.target as HTMLImageElement).src = defaultAvatar;
}

async function handleLogout() {
  logoutLoading.value = true;
  try {
    await logoutUser();
    authStore.clear();
    showSuccess('已退出登录');
    router.replace('/auth');
  } catch (error) {
    console.error(error);
  } finally {
    menuVisible.value = false;
    logoutLoading.value = false;
  }
}

function goProfile() {
  menuVisible.value = false;
  router.push('/home');
}

function goLogin() {
  menuVisible.value = false;
  router.push('/auth');
}

function handleClickOutside(event: MouseEvent) {
  const target = event.target as Node;
  if (badgeRef.value && !badgeRef.value.contains(target)) {
    menuVisible.value = false;
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<style scoped>
.user-badge {
  display: flex;
  gap: 10px;
  align-items: center;
  position: relative;
  cursor: pointer;
}

.user-badge img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.name {
  font-weight: 600;
}

.mail {
  font-size: 12px;
  color: #cbd5f5;
}

.dropdown {
  position: absolute;
  top: 50px;
  right: 0;
  background: #fff;
  color: #0f172a;
  min-width: 220px;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.2);
  z-index: 20;
}

.dropdown .title {
  font-weight: 600;
  margin-bottom: 8px;
}

.dropdown p {
  margin: 4px 0;
  font-size: 14px;
}

.dropdown .actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
</style>
