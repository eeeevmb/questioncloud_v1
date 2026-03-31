<template>
  <el-aside class="session-aside" width="300px">
    <div class="aside-header">
      <div class="aside-title-wrap">
        <p class="aside-title">最近会话</p>
        <p class="aside-subtitle">继续你的题库学习</p>
      </div>
      <el-button type="primary" :disabled="streaming || sessionLoading" @click="$emit('create-session')">新会话</el-button>
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
          <button
            class="session-main"
            type="button"
            :disabled="streaming && String(session.sessionId) !== activeSessionId"
            @click="$emit('select-session', String(session.sessionId))"
          >
            <span class="title">{{ getSessionTitle(session, index) }}</span>
            <span class="preview">{{ getSessionPreview(String(session.sessionId)) }}</span>
            <div class="session-meta-line">
              <span class="time">{{ formatSessionTime(session.updatedAt || session.createdAt) }}</span>
            </div>
          </button>
          <el-dropdown trigger="click" @command="$emit('session-command', $event, session)" @click.stop>
            <el-button text class="session-more" @click.stop>...</el-button>
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
  padding: 14px 12px;
  border-bottom: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.aside-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.aside-title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.aside-subtitle {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.session-loading {
  padding: 12px;
}

.session-scroll {
  flex: 1;
  min-height: 0;
  padding: 10px;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.session-item {
  border: 1px solid #e9edf5;
  border-radius: 12px;
  background: #fff;
  display: flex;
  align-items: center;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.session-item:hover {
  border-color: var(--el-color-primary-light-6);
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.06);
}

.session-item.active {
  border-color: var(--el-color-primary);
  background: linear-gradient(120deg, var(--el-color-primary-light-9), #ffffff 75%);
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.14);
}

.session-main {
  flex: 1;
  border: none;
  background: transparent;
  text-align: left;
  padding: 10px 10px 10px 12px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-main .title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-main .preview {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-main .time {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.session-meta-line {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.session-more {
  padding: 6px 9px;
  border-radius: 8px;
  color: var(--el-text-color-secondary);
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
</style>
