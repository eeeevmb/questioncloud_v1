<template>
  <div v-if="loading" class="loading-wrap">
    <el-skeleton :rows="6" animated />
  </div>
  <div v-else-if="!messages.length" class="empty-wrap">
    <section class="empty-stage" aria-label="AI 学习助手引导">
      <div class="empty-visual" aria-hidden="true">
        <div class="visual-glow"></div>
        <div class="visual-box">
          <el-icon><Reading /></el-icon>
        </div>
        <span class="spark spark-one"></span>
        <span class="spark spark-two"></span>
        <span class="spark spark-three"></span>
      </div>
      <h2>开始你的题库学习</h2>
      <p class="empty-tip">可以先选题学习并直接讲解，也可以输入问题开始交流。</p>
      <div class="quick-prompts">
        <button
          v-for="prompt in quickPrompts"
          :key="prompt.title"
          class="prompt-card"
          :class="`tone-${prompt.tone}`"
          type="button"
          @click="$emit('apply-quick-prompt', prompt.title)"
        >
          <span class="prompt-icon">
            <el-icon v-if="prompt.icon === 'explain'"><School /></el-icon>
            <el-icon v-else-if="prompt.icon === 'summary'"><Notebook /></el-icon>
            <el-icon v-else><EditPen /></el-icon>
          </span>
          <span class="prompt-content">
            <span class="prompt-title">{{ prompt.title }}</span>
            <span class="prompt-desc">{{ prompt.description }}</span>
          </span>
        </button>
      </div>
    </section>
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
import { EditPen, Notebook, Reading, School } from '@element-plus/icons-vue';
import AssistantMessageContent from '../AssistantMessageContent.vue';
import type { MessageItem, MessageRole, QuickPromptItem } from '../../types/agent-demo';

defineProps<{
  loading: boolean;
  messages: MessageItem[];
  quickPrompts: QuickPromptItem[];
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
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 28px 16px;
}

.empty-stage {
  width: min(900px, 100%);
  margin: 0 auto;
  animation: stage-in 0.28s ease-out both;
}

.empty-stage h2 {
  margin: 16px 0 6px;
  font-size: 24px;
  line-height: 1.3;
  color: #1f2937;
}

.empty-visual {
  position: relative;
  width: 180px;
  height: 132px;
  margin: 0 auto;
}

.visual-glow {
  position: absolute;
  inset: 42px 16px 4px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(69, 131, 245, 0.2), rgba(69, 131, 245, 0));
  filter: blur(4px);
}

.visual-box {
  position: absolute;
  left: 50%;
  bottom: 18px;
  width: 96px;
  height: 70px;
  border: 1px solid #cfe0fb;
  border-radius: 18px 18px 12px 12px;
  background: linear-gradient(145deg, #eff6ff, #d9eaff);
  box-shadow: 0 16px 34px rgba(35, 96, 181, 0.16);
  transform: translateX(-50%) rotate(-2deg);
  display: grid;
  place-items: center;
  color: #2f7df4;
}

.visual-box::before,
.visual-box::after {
  content: '';
  position: absolute;
  top: -18px;
  width: 54px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #cfe2ff, #edf5ff);
  box-shadow: 0 8px 18px rgba(35, 96, 181, 0.12);
}

.visual-box::before {
  left: -18px;
  transform: rotate(-16deg);
}

.visual-box::after {
  right: -14px;
  transform: rotate(15deg);
}

.visual-box :deep(.el-icon) {
  position: relative;
  z-index: 1;
  font-size: 30px;
}

.spark {
  position: absolute;
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: #9ac4ff;
  transform: rotate(45deg);
  opacity: 0.86;
}

.spark-one {
  top: 22px;
  left: 28px;
}

.spark-two {
  top: 16px;
  right: 34px;
  width: 6px;
  height: 6px;
}

.spark-three {
  top: 64px;
  right: 10px;
  width: 7px;
  height: 7px;
}

.quick-prompts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  justify-content: center;
  gap: 14px;
  margin: 24px auto 0;
}

.empty-tip {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.prompt-card {
  border: 1px solid #e1e9f4;
  border-radius: 14px;
  padding: 14px 16px;
  min-height: 78px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.06);
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.prompt-card:hover {
  transform: translateY(-2px);
  border-color: var(--prompt-border);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.1);
}

.prompt-card.tone-blue {
  --prompt-bg: #edf5ff;
  --prompt-color: #2f7df4;
  --prompt-border: #b9d5ff;
}

.prompt-card.tone-green {
  --prompt-bg: #e9f8f1;
  --prompt-color: #13a36b;
  --prompt-border: #b8ead5;
}

.prompt-card.tone-orange {
  --prompt-bg: #fff4df;
  --prompt-color: #ed8b22;
  --prompt-border: #f7d6a3;
}

.prompt-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: var(--prompt-bg);
  color: var(--prompt-color);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.prompt-icon :deep(.el-icon) {
  font-size: 22px;
}

.prompt-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.prompt-title {
  font-size: 14px;
  font-weight: 700;
  color: #253044;
}

.prompt-desc {
  font-size: 12px;
  color: #687385;
  line-height: 1.45;
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

@keyframes stage-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 900px) {
  .quick-prompts {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .empty-wrap {
    align-items: flex-start;
    padding: 16px 10px;
  }

  .empty-stage h2 {
    margin-top: 10px;
    font-size: 20px;
  }

  .empty-tip {
    font-size: 13px;
  }

  .empty-visual {
    width: 132px;
    height: 88px;
  }

  .visual-box {
    width: 76px;
    height: 52px;
    bottom: 10px;
    border-radius: 14px 14px 10px 10px;
  }

  .visual-box::before,
  .visual-box::after {
    top: -12px;
    width: 40px;
    height: 24px;
  }

  .visual-box :deep(.el-icon) {
    font-size: 24px;
  }

  .quick-prompts {
    gap: 10px;
    margin-top: 16px;
  }

  .prompt-card {
    min-height: 64px;
    padding: 11px 12px;
  }

  .prompt-icon {
    width: 36px;
    height: 36px;
    border-radius: 10px;
  }

  .prompt-title {
    font-size: 13px;
  }

  .prompt-desc {
    font-size: 11px;
  }
}
</style>
