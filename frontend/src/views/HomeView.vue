<template>
  <el-card v-if="user">
    <div class="top-row">
      <div class="hero">
        <el-avatar :size="72" :src="profileAvatar" @error="handleAvatarError" />
        <div>
          <h2>欢迎回来，{{ user.username }}</h2>
          <p>下面是您的基础资料。</p>
        </div>
      </div>
      <el-button :loading="loading" @click="handleLogout">退出登录</el-button>
    </div>

    <el-descriptions :column="2" border>
      <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
      <el-descriptions-item label="邮箱">{{ user.email || '未填写' }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ user.phone || '未填写' }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ formatStatus(user.status) }}</el-descriptions-item>
      <el-descriptions-item label="用户 ID">{{ user.userId }}</el-descriptions-item>
    </el-descriptions>

    <div class="actions">
      <el-button type="primary" @click="router.push('/collections')">进入题集管理</el-button>
      <el-button @click="router.push('/avatar/edit')">更换头像</el-button>
      <el-button @click="openResetPasswordDialog">修改密码</el-button>
    </div>
  </el-card>

  <el-empty v-else description="尚未登录，登录后即可管理题集与题目。">
    <el-button type="primary" @click="router.push('/auth')">前往登录</el-button>
  </el-empty>

  <ResetPasswordDialog
    v-model="resetPasswordDialogVisible"
    :initial-email="user?.email ?? ''"
    :email-readonly="true"
    @success="handleResetPasswordSuccess"
  />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import ResetPasswordDialog from '../components/ResetPasswordDialog.vue';
import { useAuthStore } from '../stores/auth';
import { logoutUser } from '../api/user';
import { showInfo, showSuccess } from '../utils/messages';
import defaultAvatar from '../assets/default-avatar.svg';

const router = useRouter();
const authStore = useAuthStore();
const { user } = storeToRefs(authStore);
const loading = ref(false);
const resetPasswordDialogVisible = ref(false);

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

function openResetPasswordDialog() {
  if (!user.value?.email) {
    showInfo('当前账号未绑定邮箱，暂时无法通过邮箱验证码修改密码');
    return;
  }
  resetPasswordDialogVisible.value = true;
}

async function handleResetPasswordSuccess() {
  loading.value = true;
  try {
    await logoutUser();
    authStore.clear();
    showSuccess('密码已更新，请重新登录');
    router.replace('/auth');
  } finally {
    loading.value = false;
  }
}

async function handleLogout() {
  loading.value = true;
  try {
    await logoutUser();
    authStore.clear();
    showSuccess('已退出登录');
    router.replace('/auth');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.top-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.hero {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hero h2 {
  margin: 0;
}

.hero p {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
}

.actions {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 768px) {
  .top-row {
    flex-direction: column;
  }
}
</style>
