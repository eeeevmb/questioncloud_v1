<template>
  <el-card shadow="never" class="toolbar-card">
    <div class="workspace-toolbar">
      <div class="toolbar-actions">
        <el-button class="tool-btn" type="primary" :disabled="streaming" @click="$emit('select-questions')">选题学习</el-button>
        <el-button class="tool-btn-secondary" :disabled="streaming" @click="$emit('create-question')">AI 出题</el-button>
        <el-button class="tool-btn-secondary" :disabled="streaming" @click="$emit('generate-paper-draft')">AI 组卷</el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  streaming: boolean;
}>();

defineEmits<{
  (e: 'select-questions'): void;
  (e: 'create-question'): void;
  (e: 'generate-paper-draft'): void;
}>();
</script>

<style scoped>
.toolbar-card {
  border: none;
  border-bottom: 1px solid var(--el-border-color-light);
  border-radius: 0;
}

.toolbar-card :deep(.el-card__body) {
  padding: 8px 12px;
}

.workspace-toolbar {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tool-btn {
  height: 40px;
  padding: 0 22px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
}

.tool-btn-secondary {
  height: 40px;
  padding: 0 18px;
  font-size: 14px;
  border-radius: 10px;
}

@media (max-width: 1024px) {
  .workspace-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .tool-btn,
  .tool-btn-secondary {
    width: 100%;
  }
}
</style>
