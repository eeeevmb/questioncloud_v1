<template>
  <div class="app-shell">
    <el-container>
      <el-header class="app-header">
        <div class="brand">题云</div>
        <el-menu class="top-menu" mode="horizontal" :ellipsis="false" :default-active="activePath" @select="handleMenuSelect">
          <el-menu-item index="/home">
            <el-icon class="nav-icon"><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/collections">
            <el-icon class="nav-icon"><FolderOpened /></el-icon>
            <span>题集管理</span>
          </el-menu-item>
          <el-menu-item index="/papers">
            <el-icon class="nav-icon"><School /></el-icon>
            <span>组卷管理</span>
          </el-menu-item>
          <el-menu-item index="/imports">
            <el-icon class="nav-icon"><CirclePlusFilled /></el-icon>
            <span>批量导入</span>
          </el-menu-item>
          <el-menu-item index="/ai-assistant">
            <el-icon class="nav-icon"><MagicStick /></el-icon>
            <span>Agent演示</span>
          </el-menu-item>
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
import { CirclePlusFilled, FolderOpened, HomeFilled, MagicStick, School } from '@element-plus/icons-vue';
import UserBadge from './components/UserBadge.vue';

const route = useRoute();
const router = useRouter();

const activePath = computed(() => {
  if (route.path.startsWith('/collections')) {
    return '/collections';
  }
  if (route.path.startsWith('/papers')) {
    return '/papers';
  }
  if (route.path.startsWith('/ai-assistant') || route.path.startsWith('/agent-demo')) {
    return '/ai-assistant';
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
  --el-menu-active-color: var(--el-color-primary);
  --el-menu-hover-bg-color: #f3f8ff;
}

.top-menu :deep(.el-menu-item) {
  white-space: nowrap;
  gap: 8px;
  padding: 0 22px;
  font-size: 14px;
  font-weight: 600;
  color: #283243;
  transition: color 0.18s ease, background-color 0.18s ease;
}

.top-menu :deep(.el-menu-item:hover) {
  color: var(--el-color-primary);
}

.top-menu :deep(.el-menu-item.is-active) {
  color: var(--el-color-primary);
  background: linear-gradient(180deg, rgba(47, 125, 244, 0.08), rgba(255, 255, 255, 0));
}

.nav-icon {
  width: 26px;
  height: 26px;
  margin-right: 0;
  border-radius: 9px;
  background: #eef6ff;
  color: #2f7df4;
  box-shadow: inset 0 -1px 0 rgba(47, 125, 244, 0.08);
}

.nav-icon :deep(svg) {
  width: 17px;
  height: 17px;
}

.top-menu :deep(.el-menu-item.is-active) .nav-icon {
  background: #e3efff;
  color: var(--el-color-primary);
  box-shadow: 0 6px 14px rgba(47, 125, 244, 0.16);
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
