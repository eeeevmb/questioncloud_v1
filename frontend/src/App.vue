<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="brand">题云</div>
      <nav class="nav-links">
        <RouterLink to="/home">首页</RouterLink>
        <RouterLink to="/collections">题集管理</RouterLink>
      </nav>
      <UserBadge />
    </header>
    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router';
import UserBadge from './components/UserBadge.vue';
</script>

<style scoped>
.app-header {
  background-color: #0f172a;
  color: #f8fafc;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  font-weight: 700;
}

.nav-links {
  display: flex;
  gap: 12px;
}

.nav-links a {
  color: #cbd5f5;
  font-weight: 500;
}

.nav-links a.router-link-active {
  color: #fff;
}

</style>
