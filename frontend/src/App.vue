<template>
  <div class="app-shell">
    <el-container>
      <el-header class="app-header">
        <div class="brand">题云</div>
        <el-menu class="top-menu" mode="horizontal" :ellipsis="false" :default-active="activePath" @select="handleMenuSelect">
          <el-menu-item index="/home">首页</el-menu-item>
          <el-menu-item index="/collections">题集管理</el-menu-item>
          <el-menu-item index="/imports">批量导入</el-menu-item>
          <el-menu-item index="/agent-demo">Agent演示</el-menu-item>
        </el-menu>
        <UserBadge class="user-area" />
      </el-header>
      <el-main class="app-main">
        <RouterView />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { RouterView, useRoute, useRouter } from 'vue-router';
import UserBadge from './components/UserBadge.vue';

const route = useRoute();
const router = useRouter();

const activePath = computed(() => {
  if (route.path.startsWith('/collections')) {
    return '/collections';
  }
  if (route.path.startsWith('/agent-demo')) {
    return '/agent-demo';
  }
  if (route.path.startsWith('/imports')) {
    return '/imports';
  }
  return '/home';
});

function handleMenuSelect(index: string) {
  if (index !== route.path) {
    void router.push(index);
  }
}
</script>

<style scoped>
.app-shell {
  height: 100%;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

.app-shell > .el-container {
  flex: 1;
  min-height: 0;
}

.app-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 20px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: #fff;
  flex-shrink: 0;
}

.brand {
  min-width: 48px;
  font-size: 18px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.top-menu {
  flex: 1;
  min-width: 0;
  border-bottom: none;
  overflow-x: auto;
  overflow-y: hidden;
}

.top-menu :deep(.el-menu-item) {
  white-space: nowrap;
}

.user-area {
  flex-shrink: 0;
}

@media (max-width: 920px) {
  .app-header {
    gap: 10px;
    padding: 0 12px;
  }
}
</style>
