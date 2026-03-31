<template>
  <div v-if="loading" class="loading-wrap">
    <el-skeleton :rows="6" animated />
  </div>
  <div v-else-if="!messages.length" class="empty-wrap">
    <el-empty description="开始你的题库学习">
      <p class="empty-tip">你可以先选题学习并直接讲解，或直接输入问题开始学习交流。</p>
      <div class="quick-prompts">
        <el-button
          v-for="prompt in quickPrompts"
          :key="prompt"
          size="small"
          @click="$emit('apply-quick-prompt', prompt)"
        >
          {{ prompt }}
        </el-button>
      </div>
    </el-empty>
  </div>
  <div v-else class="message-list">
    <article v-for="item in messages" :key="item.id" class="message-row" :class="item.role">
      <div class="message-bubble" :class="{ pending: item.status === 'pending', error: item.status === 'error' }">
        <div class="role">{{ roleLabel(item.role) }}</div>
        <template v-if="item.role === 'assistant'">
          <div v-if="item.status === 'pending'" class="pending-text">AI 助手思考中...</div>
          <AssistantMessageContent v-else :content="item.content" />
        </template>
        <pre v-else class="content">{{ item.content }}</pre>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import AssistantMessageContent from '../AssistantMessageContent.vue';
import type { MessageItem, MessageRole } from '../../types/agent-demo';

defineProps<{
  loading: boolean;
  messages: MessageItem[];
  quickPrompts: string[];
  roleLabel: (role: MessageRole) => string;
}>();

defineEmits<{
  (e: 'apply-quick-prompt', prompt: string): void;
}>();
</script>

<style scoped>
.loading-wrap {
  padding: 10px;
}

.empty-wrap {
  min-height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 10px;
}

.empty-tip {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 980px;
  margin: 0 auto;
}

.message-row {
  display: flex;
}

.message-row.user {
  justify-content: flex-end;
}

.message-row.assistant,
.message-row.system {
  justify-content: flex-start;
}

.message-bubble {
  max-width: min(74ch, 86%);
  border-radius: 14px;
  padding: 11px 13px;
  border: 1px solid #e5eaf2;
  background: #fff;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.06);
}

.message-bubble.pending {
  background: var(--el-fill-color-light);
}

.message-bubble.error {
  border-color: var(--el-color-danger-light-5);
  background: var(--el-color-danger-light-9);
}

.message-row.user .message-bubble {
  background: linear-gradient(120deg, #e8f1ff, #f3f8ff 70%);
  border-color: #bcd7ff;
}

.message-bubble .role {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.message-bubble .content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
}

.message-bubble .pending-text {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}
</style>
