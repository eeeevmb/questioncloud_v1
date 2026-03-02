<template>
  <div class="home-container">
    <!-- 顶部导航栏 -->
    <header class="top-nav">
      <div class="brand">
        <div class="logo-icon"></div>
        <span>智能云题库工作台</span>
      </div>
      <div class="user-action">
        <span class="date">{{ currentDate }}</span>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <div class="main-body">
      <!-- 左侧：个人信息卡片 -->
      <aside class="side-panel">
        <div class="profile-card">
          <!-- 默认头像 -->
          <div class="avatar">
            {{ user.username ? user.username.charAt(0).toUpperCase() : '师' }}
          </div>
          <h3 class="username">{{ user.username || '教师用户' }}</h3>
          <p class="role-badge">认证教师</p>
          
          <div class="info-list">
            <div class="info-item">
              <span class="label">用户ID</span>
              <span class="value">{{ user.userId || '---' }}</span>
            </div>
            <div class="info-item">
              <span class="label">账号状态</span>
              <span class="value status-active">● 正常活跃</span>
            </div>
          </div>
        </div>
      </aside>

      <!-- 右侧：内容展示区 -->
      <main class="content-area">
        <div class="welcome-banner">
          <h2>欢迎回来，开始今天的教学工作吧！</h2>
          <p>您已累计登录系统 1 天，保持良好的工作习惯。</p>
        </div>

        <!-- 数据概览 -->
        <div class="stats-grid">
          <div class="stat-card blue">
            <div class="num">0</div>
            <div class="txt">我的题库</div>
          </div>
          <div class="stat-card green">
            <div class="num">0</div>
            <div class="txt">生成试卷</div>
          </div>
          <div class="stat-card purple">
            <div class="num">0</div>
            <div class="txt">班级管理</div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

// 接收父组件传来的用户信息
const props = defineProps(['user']);
const emit = defineEmits(['logout']);

const currentDate = new Date().toLocaleDateString();

const handleLogout = () => {
  if(confirm('确定要退出登录吗？')) {
    emit('logout');
  }
};
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
}

/* 顶部导航 */
.top-nav {
  height: 60px;
  background: white;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
  z-index: 10;
}
.brand { display: flex; align-items: center; font-weight: bold; font-size: 18px; color: #333; }
.logo-icon { width: 24px; height: 24px; background: #1890ff; border-radius: 4px; margin-right: 10px; }
.user-action { display: flex; align-items: center; gap: 20px; }
.date { color: #999; font-size: 14px; }
.logout-btn {
  border: 1px solid #ff4d4f; color: #ff4d4f; background: white;
  padding: 5px 15px; border-radius: 4px; cursor: pointer; transition: all 0.3s;
}
.logout-btn:hover { background: #ff4d4f; color: white; }

/* 主体布局 */
.main-body {
  flex: 1;
  display: flex;
  max-width: 1200px;
  margin: 30px auto;
  width: 100%;
  padding: 0 20px;
  gap: 20px;
}

/* 左侧卡片 */
.side-panel { width: 280px; flex-shrink: 0; }
.profile-card {
  background: white; border-radius: 12px; padding: 30px 20px;
  text-align: center; box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}
.avatar {
  width: 80px; height: 80px; background: #e6f7ff; color: #1890ff;
  font-size: 32px; font-weight: bold; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 15px; border: 2px solid white; box-shadow: 0 4px 10px rgba(24,144,255,0.2);
}
.username { margin: 0; font-size: 20px; color: #333; }
.role-badge { 
  display: inline-block; background: #f6ffed; color: #52c41a; 
  padding: 2px 8px; border-radius: 4px; font-size: 12px; margin-top: 8px; border: 1px solid #b7eb8f;
}
.info-list { margin-top: 30px; border-top: 1px solid #f0f0f0; padding-top: 20px; }
.info-item { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 14px; }
.label { color: #999; }
.value { color: #333; font-family: monospace; }
.status-active { color: #52c41a; }

/* 右侧内容 */
.content-area { flex: 1; }
.welcome-banner {
  background: linear-gradient(135deg, #40a9ff 0%, #096dd9 100%);
  color: white; padding: 30px; border-radius: 12px; margin-bottom: 20px;
}
.welcome-banner h2 { margin: 0 0 10px 0; font-size: 22px; }
.welcome-banner p { margin: 0; opacity: 0.8; font-size: 14px; }

.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.stat-card {
  background: white; padding: 25px; border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03); transition: transform 0.3s;
}
.stat-card:hover { transform: translateY(-5px); }
.stat-card .num { font-size: 32px; font-weight: bold; margin-bottom: 5px; }
.stat-card .txt { color: #888; font-size: 14px; }
.blue .num { color: #020a11; }
.green .num { color: #020a11; }
.purple .num { color: #020a11; }
</style>