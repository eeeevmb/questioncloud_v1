<template>
  <el-aside class="session-aside" width="300px">
    <div class="aside-header">
      <div class="aside-heading">
        <span class="aside-icon">
          <el-icon><Service /></el-icon>
        </span>
        <div class="aside-title-wrap">
          <p class="aside-title">最近会话</p>
          <p class="aside-subtitle">继续与 AI 助手追问、复盘和练习</p>
        </div>
      </div>
      <el-button type="primary" :icon="Plus" :disabled="streaming || sessionLoading" @click="$emit('create-session')">
        新建会话
      </el-button>
    </div>
    <div v-if="sessionLoading" class="session-loading">
      <el-skeleton :rows="6" animated />
    </div>
    <el-scrollbar v-else class="session-scroll">
      <el-empty v-if="!sessions.length" description="暂无会话，点击上方创建" />
      <div v-else class="session-list">
        <div
          v-for="(session, index) in sessions"
          :key="session.sessionId"
          class="session-item"
          :class="{ active: String(session.sessionId) === activeSessionId }"
        >
          <span class="session-icon">
            <el-icon><ChatDotRound /></el-icon>
          </span>
          <button
            class="session-main"
            type="button"
            :disabled="streaming && String(session.sessionId) !== activeSessionId"
            @click="$emit('select-session', String(session.sessionId))"
          >
            <span class="title">{{ getSessionTitle(session, index) }}</span>
            <span class="preview">
              <el-icon><ChatLineRound /></el-icon>
              <span>{{ getSessionPreview(String(session.sessionId)) }}</span>
            </span>
            <div class="session-meta-line">
              <span class="time">
                <el-icon><Clock /></el-icon>
                <span>{{ formatSessionTime(session.updatedAt || session.createdAt) }}</span>
              </span>
            </div>
          </button>
          <el-dropdown trigger="click" @command="$emit('session-command', $event, session)" @click.stop>
            <el-button text class="session-more" aria-label="会话操作" @click.stop>
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename">重命名</el-dropdown-item>
                <el-dropdown-item command="delete">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-scrollbar>
  </el-aside>
</template>

<script setup lang="ts">
import { ChatDotRound, ChatLineRound, Clock, MoreFilled, Plus, Service } from '@element-plus/icons-vue';
import type { AgentSessionVO } from '../../api/agent';

defineProps<{
  sessions: AgentSessionVO[];
  activeSessionId: string;
  sessionLoading: boolean;
  streaming: boolean;
  getSessionTitle: (session: AgentSessionVO, index: number) => string;
  getSessionPreview: (sessionId: string) => string;
  formatSessionTime: (value?: string) => string;
}>();

defineEmits<{
  (e: 'create-session'): void;
  (e: 'select-session', sessionId: string): void;
  (e: 'session-command', command: string | number | object, session: AgentSessionVO): void;
}>();
</script>

<style scoped>
.session-aside {
  min-height: 0;
  border-right: 1px solid var(--el-border-color-light);
  background: #f7f9fc;
  display: flex;
  flex-direction: column;
}

.aside-header {
  padding: 16px 14px;
  border-bottom: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: linear-gradient(180deg, #fbfdff 0%, #f5f8fd 100%);
}

.aside-heading {
  display: flex;
  align-items: center;
  gap: 10px;
}

.aside-icon {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  background: #eaf3ff;
  color: var(--el-color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.aside-icon :deep(.el-icon) {
  font-size: 18px;
}

.aside-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.aside-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
}

.aside-subtitle {
  margin: 0;
  font-size: 12px;
  line-height: 1.45;
  color: var(--el-text-color-secondary);
}

.aside-header :deep(.el-button) {
  height: 34px;
  font-weight: 600;
  border-radius: 10px;
}

.session-loading {
  padding: 12px;
}

.session-scroll {
  flex: 1;
  min-height: 0;
  padding: 12px;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.session-item {
  border: 1px solid #e9edf5;
  border-radius: 12px;
  background: #fff;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 8px 9px 10px;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.session-item:hover {
  border-color: var(--el-color-primary-light-6);
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.06);
}

.session-item.active {
  border-color: var(--el-color-primary);
  background: linear-gradient(120deg, #edf5ff, #ffffff 78%);
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.14);
}

.session-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: #edf5ff;
  color: var(--el-color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.session-item.active .session-icon {
  background: var(--el-color-primary);
  color: #fff;
}

.session-icon :deep(.el-icon) {
  font-size: 17px;
}

.session-main {
  flex: 1;
  border: none;
  background: transparent;
  text-align: left;
  padding: 0;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-main .title {
  font-size: 13px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-main .preview {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-main .time {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: var(--el-text-color-secondary);
}

.session-main .preview :deep(.el-icon),
.session-main .time :deep(.el-icon) {
  font-size: 12px;
  flex: 0 0 auto;
}

.session-meta-line {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.session-more {
  width: 28px;
  height: 28px;
  padding: 0;
  border-radius: 8px;
  color: var(--el-text-color-secondary);
}

.session-more :deep(.el-icon) {
  font-size: 16px;
}

.session-more:hover {
  background: var(--el-fill-color-light);
}

@media (max-width: 1024px) {
  .session-aside {
    width: 100% !important;
    max-height: 280px;
    border-right: none;
    border-bottom: 1px solid var(--el-border-color-light);
  }
}

@media (max-width: 640px) {
  .session-aside {
    max-height: 226px;
  }

  .aside-header {
    padding: 12px;
    gap: 8px;
  }

  .session-scroll {
    padding: 10px 12px;
  }

  .session-item {
    padding: 9px 8px 9px 10px;
  }
}
</style>
