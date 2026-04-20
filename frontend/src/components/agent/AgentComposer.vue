<template>
  <footer class="composer-wrap">
    <el-card shadow="never" class="composer-card">
      <div class="composer-head">
        <span class="composer-title">学习提问</span>
      </div>
      <el-input
        :model-value="modelValue"
        type="textarea"
        :rows="2"
        resize="none"
        :disabled="!activeSessionId || streaming"
        placeholder="输入问题，直接开始学习交流"
        @update:model-value="$emit('update:modelValue', $event)"
        @keydown.enter.exact.prevent="$emit('send')"
      />
      <div class="composer-footer">
        <div class="action-group">
          <el-button type="primary" :loading="streaming" :disabled="!canSend" @click="$emit('send')">
            <el-icon v-if="!streaming"><Promotion /></el-icon>
            <span>发送</span>
          </el-button>
        </div>
      </div>
    </el-card>
  </footer>
</template>

<script setup lang="ts">
import { Promotion } from '@element-plus/icons-vue';

defineProps<{
  modelValue: string;
  activeSessionId: string;
  streaming: boolean;
  canSend: boolean;
}>();

defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'send'): void;
  (e: 'abort'): void;
}>();
</script>

<style scoped>
.composer-wrap {
  border-top: 1px solid var(--el-border-color-light);
  padding: 10px 12px 12px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), #ffffff);
  backdrop-filter: blur(6px);
}

.composer-card {
  border: 1px solid #d9e3f0;
  border-radius: 14px;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
}

.composer-card :deep(.el-card__body) {
  padding: 10px 12px;
}

.composer-card :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  background: transparent;
  padding: 2px 0;
  font-size: 14px;
  line-height: 1.7;
}

.composer-head {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.composer-title {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.composer-footer {
  margin-top: 6px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.action-group {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

@media (max-width: 1024px) {
  .composer-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
