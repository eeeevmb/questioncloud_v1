<template>
  <el-dropdown trigger="click" class="user-badge">
    <span class="badge-trigger">
      <el-avatar :src="avatarSrc" :alt="displayName" @error="handleImgError" />
      <span class="meta">
        <span class="name">{{ displayName }}</span>
        <span class="mail">{{ subtitle }}</span>
      </span>
    </span>
    <template #dropdown>
      <el-dropdown-menu v-if="authStore.isAuthenticated">
        <el-dropdown-item disabled>用户名：{{ authStore.user?.username ?? '未登录' }}</el-dropdown-item>
        <el-dropdown-item disabled>邮箱：{{ authStore.user?.email ?? '未填写' }}</el-dropdown-item>
        <el-dropdown-item divided @click="goProfile">个人资料</el-dropdown-item>
        <el-dropdown-item :disabled="logoutLoading" @click="handleLogout">退出登录</el-dropdown-item>
      </el-dropdown-menu>
      <el-dropdown-menu v-else>
        <el-dropdown-item disabled>未登录</el-dropdown-item>
        <el-dropdown-item @click="goLogin">前往登录</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { logoutUser } from '../api/user';
import { showSuccess } from '../utils/messages';
import defaultAvatar from '../assets/default-avatar.svg';

const authStore = useAuthStore();
const router = useRouter();
const logoutLoading = ref(false);

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
    logoutLoading.value = false;
  }
}

function goProfile() {
  router.push('/home');
}

function goLogin() {
  router.push('/auth');
}
</script>

<style scoped>
.user-badge {
  line-height: 1;
}

.badge-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.name {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.mail {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

@media (max-width: 1080px) {
  .mail {
    display: none;
  }
}
</style>
