<template>
  <el-card shadow="never" class="toolbar-card">
    <div class="workspace-toolbar">
      <div class="toolbar-actions">
        <el-button class="tool-btn is-primary" type="primary" :disabled="streaming" @click="$emit('select-questions')">
          <el-icon><Reading /></el-icon>
          <span>选题学习</span>
        </el-button>
        <el-button class="tool-btn" :disabled="streaming" @click="$emit('create-question')">
          <el-icon><MagicStick /></el-icon>
          <span>AI 出题</span>
        </el-button>
        <el-button class="tool-btn" :disabled="streaming" @click="$emit('generate-paper-draft')">
          <el-icon><Grid /></el-icon>
          <span>AI 组卷</span>
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { Grid, MagicStick, Reading } from '@element-plus/icons-vue';

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
  padding: 12px 18px;
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
  gap: 12px;
  flex-wrap: wrap;
}

.tool-btn {
  height: 46px;
  min-width: 132px;
  padding: 0 22px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
  border-color: #d8e1ee;
  color: #253044;
  background: #fff;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.tool-btn:hover {
  transform: translateY(-1px);
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 8px 18px rgba(36, 99, 235, 0.1);
}

.tool-btn :deep(.el-icon) {
  font-size: 18px;
}

.tool-btn.is-primary {
  min-width: 148px;
  color: #fff;
  border-color: var(--el-color-primary);
  background: linear-gradient(135deg, #2f7df4, #1f6eea);
  box-shadow: 0 10px 20px rgba(47, 125, 244, 0.2);
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

  .tool-btn {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .toolbar-card :deep(.el-card__body) {
    padding: 10px 12px;
  }

  .toolbar-actions {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }

  .tool-btn,
  .tool-btn.is-primary {
    width: 100%;
    min-width: 0;
    height: 44px;
    padding: 0 8px;
    font-size: 14px;
  }

  .tool-btn :deep(.el-icon) {
    font-size: 16px;
  }
}
</style>
