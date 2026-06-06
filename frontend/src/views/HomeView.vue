<template>
  <section class="dashboard-page" v-loading="loading">
    <el-alert
      v-if="loadError"
      class="dashboard-alert"
      type="warning"
      :closable="false"
      show-icon
      title="接口暂不可用，当前展示本地预览数据"
    />

    <section class="welcome-hero">
      <div class="hero-content">
        <p class="hero-kicker">题云工作台</p>
        <h1>欢迎回来，{{ displayUsername }}</h1>
        <p class="hero-subtitle">今天也来高效管理你的题目、题集和试卷吧</p>
        <el-button type="primary" size="large" @click="router.push('/collections')">
          进入题库
          <el-icon class="button-icon"><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="hero-visual" aria-hidden="true">
        <div class="cloud cloud-one" />
        <div class="cloud cloud-two" />
        <div class="document-card document-main">
          <div class="doc-title" />
          <div class="doc-line wide" />
          <div class="doc-line" />
          <div class="doc-line short" />
        </div>
        <div class="document-card document-small">
          <el-icon><Memo /></el-icon>
        </div>
      </div>
    </section>

    <section class="stats-grid" aria-label="数据概览">
      <article v-for="stat in statsCards" :key="stat.key" class="stat-card">
        <span class="stat-icon" :class="stat.tone">
          <el-icon><component :is="stat.icon" /></el-icon>
        </span>
        <div>
          <p class="stat-label">{{ stat.label }}</p>
          <strong>{{ formatNumber(stat.value) }}</strong>
          <span class="stat-delta">昨日新增 {{ formatNumber(stat.delta) }}</span>
        </div>
      </article>
    </section>

    <section class="dashboard-grid">
      <article class="panel quick-panel">
        <div class="panel-header">
          <div>
            <h2>快捷入口</h2>
            <p>常用工作一键直达</p>
          </div>
        </div>
        <div class="quick-grid">
          <button v-for="entry in quickEntries" :key="entry.path" class="quick-entry" @click="router.push(entry.path)">
            <span class="quick-icon" :class="entry.tone">
              <el-icon><component :is="entry.icon" /></el-icon>
            </span>
            <span>{{ entry.label }}</span>
          </button>
        </div>
      </article>

      <article class="panel announcements-panel">
        <div class="panel-header compact">
          <h2>系统公告</h2>
          <el-icon><Bell /></el-icon>
        </div>
        <ul v-if="announcementRows.length > 0" class="announcement-list">
          <li v-for="announcement in announcementRows" :key="`${announcement.title}-${announcement.date}`">
            <span>{{ announcement.title }}</span>
            <time>{{ formatDate(announcement.date) }}</time>
          </li>
        </ul>
        <el-empty v-else :image-size="84" description="暂无系统公告" />
      </article>
    </section>

    <section class="dashboard-grid bottom-grid">
      <article class="panel recent-panel">
        <div class="panel-header compact">
          <h2>最近使用</h2>
          <el-button text type="primary" @click="router.push('/collections')">
            查看全部
            <el-icon class="button-icon"><ArrowRight /></el-icon>
          </el-button>
        </div>
        <el-table
          v-if="recentRows.length > 0"
          class="recent-table"
          :data="recentRows"
          :show-header="true"
          row-key="id"
        >
          <el-table-column prop="name" label="名称" min-width="220">
            <template #default="{ row }">
              <div class="recent-name">
                <span class="recent-type-dot" :class="typeClass(row.type)" />
                <span>{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag round :type="tagType(row.type)">{{ typeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openRecent(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else :image-size="96" description="暂无最近使用记录" />
      </article>

      <article class="panel activities-panel">
        <div class="panel-header compact">
          <h2>系统动态</h2>
          <el-button :icon="Refresh" circle text @click="loadOverview" />
        </div>
        <ol v-if="activityRows.length > 0" class="activity-list">
          <li v-for="activity in activityRows" :key="`${activity.content}-${activity.occurredAt}`">
            <span class="activity-icon" :class="activityTone(activity.type)">
              <el-icon><component :is="activityIcon(activity.type)" /></el-icon>
            </span>
            <div>
              <span class="activity-user">{{ activity.username || '系统' }}</span>
              <p>{{ activity.content }}</p>
              <time>{{ activity.timeText || formatDateTime(activity.occurredAt) }}</time>
            </div>
          </li>
        </ol>
        <el-empty v-else :image-size="92" description="暂无系统动态" />
      </article>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, markRaw, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import {
  ArrowRight,
  Bell,
  Collection,
  DataAnalysis,
  Document,
  Files,
  FolderOpened,
  MagicStick,
  Memo,
  Notebook,
  Refresh,
  UploadFilled
} from '@element-plus/icons-vue';
import { fetchDashboardOverview } from '../api/dashboard';
import { useAuthStore } from '../stores/auth';
import type {
  DashboardActivityVO,
  DashboardAnnouncementVO,
  DashboardOverviewVO,
  DashboardRecentItemVO
} from '../types/dashboard';

const router = useRouter();
const authStore = useAuthStore();
const { user } = storeToRefs(authStore);
const loading = ref(false);
const loadError = ref(false);
const overview = ref<DashboardOverviewVO | null>(null);

const fallbackOverview: DashboardOverviewVO = {
  welcome: {
    username: user.value?.username || '陈恩泽'
  },
  stats: {
    questionTotal: 1280,
    questionYesterdayDelta: 24,
    collectionTotal: 86,
    collectionYesterdayDelta: 3,
    paperTotal: 42,
    paperYesterdayDelta: 5,
    importTotal: 18,
    importYesterdayDelta: 2
  },
  recentItems: [
    {
      id: 'preview-collection',
      name: '高等数学期末复习题集',
      type: 'COLLECTION',
      updatedAt: '2024-05-20T10:30:00',
      targetPath: '/collections'
    },
    {
      id: 'preview-paper',
      name: '线性代数模拟试卷 A',
      type: 'PAPER',
      updatedAt: '2024-05-19T16:45:00',
      targetPath: '/papers'
    },
    {
      id: 'preview-import',
      name: '计算机网络批量导入',
      type: 'IMPORT',
      updatedAt: '2024-05-18T09:15:00',
      targetPath: '/imports'
    }
  ],
  announcements: [
    { title: '系统将于本周五凌晨进行维护升级', date: '2024-05-15' },
    { title: '新增批量导入模板下载功能', date: '2024-05-10' },
    { title: 'AI 组卷助手上线试用', date: '2024-05-01' }
  ],
  activities: [
    {
      type: 'QUESTION',
      username: '陈恩泽',
      content: '新增题目“数据库索引优化策略”',
      occurredAt: '2024-05-20T11:20:00',
      timeText: '10 分钟前'
    },
    {
      type: 'PAPER',
      username: '陈恩泽',
      content: '生成试卷“操作系统期末模拟卷”',
      occurredAt: '2024-05-20T10:45:00',
      timeText: '45 分钟前'
    },
    {
      type: 'IMPORT',
      username: '陈恩泽',
      content: '完成批量导入 36 道题目',
      occurredAt: '2024-05-20T09:30:00',
      timeText: '2 小时前'
    }
  ]
};

const quickEntries = [
  { label: '题集管理', path: '/collections', icon: markRaw(Collection), tone: 'blue' },
  { label: '组卷管理', path: '/papers', icon: markRaw(Document), tone: 'green' },
  { label: '批量导入', path: '/imports', icon: markRaw(UploadFilled), tone: 'orange' },
  { label: 'Agent演示', path: '/ai-assistant', icon: markRaw(MagicStick), tone: 'purple' }
];

const displayOverview = computed(() => overview.value ?? fallbackOverview);
const displayUsername = computed(() => displayOverview.value.welcome?.username || user.value?.username || '同学');
const recentRows = computed(() => displayOverview.value.recentItems ?? []);
const announcementRows = computed<DashboardAnnouncementVO[]>(() => displayOverview.value.announcements ?? []);
const activityRows = computed<DashboardActivityVO[]>(() => displayOverview.value.activities ?? []);

const statsCards = computed(() => {
  const stats = displayOverview.value.stats;
  return [
    {
      key: 'question',
      label: '题目总数',
      value: stats.questionTotal,
      delta: stats.questionYesterdayDelta,
      icon: markRaw(Notebook),
      tone: 'blue'
    },
    {
      key: 'collection',
      label: '题集总数',
      value: stats.collectionTotal,
      delta: stats.collectionYesterdayDelta,
      icon: markRaw(FolderOpened),
      tone: 'purple'
    },
    {
      key: 'paper',
      label: '试卷总数',
      value: stats.paperTotal,
      delta: stats.paperYesterdayDelta,
      icon: markRaw(Files),
      tone: 'green'
    },
    {
      key: 'import',
      label: '导入记录',
      value: stats.importTotal,
      delta: stats.importYesterdayDelta,
      icon: markRaw(DataAnalysis),
      tone: 'orange'
    }
  ];
});

async function loadOverview() {
  loading.value = true;
  loadError.value = false;
  try {
    overview.value = await fetchDashboardOverview();
  } catch {
    overview.value = fallbackOverview;
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

function formatNumber(value: number) {
  return new Intl.NumberFormat('zh-CN').format(value ?? 0);
}

function formatDate(value: string) {
  if (!value) {
    return '-';
  }
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit' }).format(new Date(value));
}

function formatDateTime(value: string) {
  if (!value) {
    return '-';
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

function typeLabel(type: string) {
  const normalized = type?.toUpperCase();
  if (normalized === 'COLLECTION') {
    return '题集';
  }
  if (normalized === 'PAPER') {
    return '试卷';
  }
  if (normalized === 'IMPORT') {
    return '导入';
  }
  if (normalized === 'QUESTION') {
    return '题目';
  }
  return '记录';
}

function typeClass(type: string) {
  return `type-${type?.toLowerCase() || 'default'}`;
}

function tagType(type: string) {
  const normalized = type?.toUpperCase();
  if (normalized === 'PAPER') {
    return 'success';
  }
  if (normalized === 'IMPORT') {
    return 'warning';
  }
  if (normalized === 'QUESTION') {
    return 'info';
  }
  return 'primary';
}

function activityTone(type: string) {
  const normalized = type?.toUpperCase();
  if (normalized === 'PAPER') {
    return 'green';
  }
  if (normalized === 'IMPORT') {
    return 'orange';
  }
  if (normalized === 'AI') {
    return 'purple';
  }
  return 'blue';
}

function activityIcon(type: string) {
  const normalized = type?.toUpperCase();
  if (normalized === 'PAPER') {
    return markRaw(Document);
  }
  if (normalized === 'IMPORT') {
    return markRaw(UploadFilled);
  }
  if (normalized === 'AI') {
    return markRaw(MagicStick);
  }
  return markRaw(Notebook);
}

function openRecent(row: DashboardRecentItemVO) {
  if (row.targetPath) {
    router.push(row.targetPath);
  }
}

onMounted(loadOverview);
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 100%;
  color: #172033;
}

.dashboard-alert {
  border-radius: 8px;
}

.welcome-hero {
  position: relative;
  display: flex;
  min-height: 238px;
  overflow: hidden;
  border-radius: 8px;
  background:
    radial-gradient(circle at 78% 16%, rgba(255, 255, 255, 0.4) 0 14%, transparent 15%),
    linear-gradient(135deg, #2f80ed 0%, #56ccf2 100%);
  box-shadow: 0 16px 34px rgba(44, 123, 229, 0.18);
}

.hero-content {
  position: relative;
  z-index: 2;
  width: min(56%, 620px);
  padding: 36px 42px;
  color: #fff;
}

.hero-kicker {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 700;
  opacity: 0.86;
}

.hero-content h1 {
  margin: 0;
  font-size: 36px;
  line-height: 1.22;
  font-weight: 800;
  letter-spacing: 0;
}

.hero-subtitle {
  margin: 14px 0 26px;
  font-size: 16px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.86);
}

.hero-content :deep(.el-button) {
  border: 0;
  background: #fff;
  color: #2f73de;
  font-weight: 700;
  box-shadow: 0 10px 18px rgba(31, 76, 148, 0.14);
}

.button-icon {
  margin-left: 6px;
}

.hero-visual {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.cloud {
  position: absolute;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.48);
  filter: drop-shadow(0 16px 28px rgba(27, 85, 160, 0.12));
}

.cloud::before,
.cloud::after {
  position: absolute;
  content: '';
  border-radius: 50%;
  background: inherit;
}

.cloud-one {
  right: 104px;
  top: 56px;
  width: 216px;
  height: 74px;
}

.cloud-one::before {
  left: 34px;
  bottom: 18px;
  width: 82px;
  height: 82px;
}

.cloud-one::after {
  right: 36px;
  bottom: 16px;
  width: 96px;
  height: 96px;
}

.cloud-two {
  right: 32px;
  bottom: 38px;
  width: 166px;
  height: 58px;
  background: rgba(255, 255, 255, 0.34);
}

.cloud-two::before {
  left: 24px;
  bottom: 14px;
  width: 64px;
  height: 64px;
}

.cloud-two::after {
  right: 30px;
  bottom: 12px;
  width: 72px;
  height: 72px;
}

.document-card {
  position: absolute;
  z-index: 1;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 44px rgba(26, 68, 128, 0.18);
}

.document-main {
  right: 166px;
  bottom: 32px;
  width: 154px;
  height: 176px;
  padding: 24px 18px;
  transform: rotate(-6deg);
}

.document-small {
  right: 76px;
  top: 74px;
  display: grid;
  width: 76px;
  height: 88px;
  place-items: center;
  color: #2f80ed;
  font-size: 28px;
  transform: rotate(8deg);
}

.doc-title,
.doc-line {
  border-radius: 999px;
  background: #d9e8ff;
}

.doc-title {
  width: 72px;
  height: 12px;
  margin-bottom: 24px;
  background: #2f80ed;
}

.doc-line {
  width: 74%;
  height: 9px;
  margin-top: 14px;
}

.doc-line.wide {
  width: 100%;
}

.doc-line.short {
  width: 48%;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card,
.panel {
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(34, 52, 86, 0.06);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 118px;
  padding: 22px;
}

.stat-icon,
.quick-icon,
.activity-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  border-radius: 8px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  font-size: 24px;
}

.stat-label {
  margin: 0 0 8px;
  color: #6b778c;
  font-size: 14px;
}

.stat-card strong {
  display: block;
  font-size: 28px;
  line-height: 1.1;
  letter-spacing: 0;
}

.stat-delta {
  display: block;
  margin-top: 8px;
  color: #7b8798;
  font-size: 13px;
}

.blue {
  background: #eaf3ff;
  color: #2f80ed;
}

.purple {
  background: #f1edff;
  color: #7657d8;
}

.green {
  background: #e9f8f0;
  color: #19a765;
}

.orange {
  background: #fff3e4;
  color: #e68a17;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.42fr) minmax(320px, 0.58fr);
  gap: 20px;
}

.bottom-grid {
  align-items: stretch;
}

.panel {
  padding: 22px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-header.compact {
  align-items: center;
}

.panel h2 {
  margin: 0;
  color: #1e2a3b;
  font-size: 18px;
  line-height: 1.35;
  font-weight: 800;
  letter-spacing: 0;
}

.panel-header p {
  margin: 6px 0 0;
  color: #7b8798;
  font-size: 14px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.quick-entry {
  display: flex;
  min-height: 104px;
  cursor: pointer;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  border: 1px solid #edf1f7;
  border-radius: 8px;
  background: #fbfcff;
  color: #27364a;
  font: inherit;
  font-weight: 700;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.quick-entry:hover {
  transform: translateY(-2px);
  border-color: #cfe0ff;
  box-shadow: 0 12px 24px rgba(34, 83, 150, 0.08);
}

.quick-icon {
  width: 44px;
  height: 44px;
  font-size: 22px;
}

.announcement-list,
.activity-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.announcement-list li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding-bottom: 14px;
  border-bottom: 1px solid #eef2f7;
  color: #2d3a4d;
  font-size: 14px;
}

.announcement-list li:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.announcement-list span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.announcement-list time,
.activity-list time {
  color: #8a96a8;
  font-size: 13px;
}

.recent-panel {
  min-width: 0;
}

.recent-table {
  width: 100%;
}

.recent-table :deep(.el-table__header th) {
  color: #7b8798;
  font-weight: 700;
  background: #fbfcff;
}

.recent-table :deep(.el-table__row) {
  height: 58px;
}

.recent-name {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 10px;
  font-weight: 700;
}

.recent-name span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-type-dot {
  width: 9px;
  height: 9px;
  flex: none;
  border-radius: 50%;
  background: #2f80ed;
}

.type-paper {
  background: #19a765;
}

.type-import {
  background: #e68a17;
}

.type-question {
  background: #8a96a8;
}

.activities-panel {
  min-width: 0;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.activity-list li {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 12px;
  align-items: flex-start;
}

.activity-icon {
  width: 38px;
  height: 38px;
  font-size: 18px;
}

.activity-list p {
  margin: 4px 0 6px;
  overflow: hidden;
  color: #2d3a4d;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-user {
  display: block;
  overflow: hidden;
  color: #6b778c;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .stats-grid,
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .welcome-hero {
    min-height: auto;
  }

  .hero-content {
    width: 100%;
    padding: 28px 24px 132px;
  }

  .hero-content h1 {
    font-size: 28px;
  }

  .cloud-one {
    right: 32px;
    top: auto;
    bottom: 36px;
    transform: scale(0.78);
    transform-origin: right bottom;
  }

  .cloud-two,
  .document-small {
    display: none;
  }

  .document-main {
    right: 30px;
    bottom: 18px;
    width: 104px;
    height: 116px;
    padding: 16px 12px;
  }

  .stats-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }

  .stat-card {
    min-height: 104px;
  }

  .panel {
    padding: 18px;
  }

  .announcement-list li {
    grid-template-columns: 1fr;
    gap: 6px;
  }
}
</style>
