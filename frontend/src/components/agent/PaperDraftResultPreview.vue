<template>
  <section class="draft-preview-layout">
    <aside class="draft-side">
      <section class="reason-card">
        <div class="reason-title">
          <span class="reason-icon">
            <el-icon><MagicStick /></el-icon>
          </span>
          <div>
            <h4>AI 组卷理由</h4>
            <p>以下为模型基于需求和约束给出的组卷说明。</p>
          </div>
        </div>
        <div class="reason-body">
          <MathView :content="reason || '暂无组卷理由'" empty-text="暂无组卷理由" />
        </div>
      </section>

      <section class="source-card">
        <div class="card-heading">
          <h4>题集来源</h4>
          <span>{{ sourceNames.length }} 个题集</span>
        </div>
        <div class="source-tags">
          <el-tag v-if="!sourceNames.length" size="small" type="info" effect="plain">未选择</el-tag>
          <el-tag v-for="name in sourceNames" v-else :key="name" size="small" effect="plain">{{ name }}</el-tag>
        </div>
      </section>

      <section class="constraint-card">
        <div class="card-heading">
          <h4>题型与数量</h4>
          <span>目标 {{ expectedTotal }} 题</span>
        </div>
        <div class="constraint-table">
          <div class="constraint-head">
            <span>题型</span>
            <span>题数</span>
            <span>难度范围</span>
          </div>
          <div v-for="item in constraintRows" :key="item.key" class="constraint-row">
            <span>{{ item.label }}</span>
            <span>{{ item.count }} 题</span>
            <span>{{ item.range }}</span>
          </div>
        </div>
      </section>
    </aside>

    <main class="draft-main">
      <section class="draft-card">
        <div class="draft-card-header">
          <div>
            <h4>生成的组卷草稿</h4>
            <p>共 {{ actualTotal }} 题，确认题目内容后可继续调整或重新生成。</p>
          </div>
          <el-tag type="success" effect="light">目标 {{ expectedTotal }} 题</el-tag>
        </div>

        <el-alert
          v-if="shortageMessage"
          class="shortage-alert"
          type="warning"
          :closable="false"
          :title="shortageMessage"
        />
        <el-alert
          v-if="detailErrorMessage"
          class="shortage-alert"
          type="info"
          :closable="false"
          :title="detailErrorMessage"
        />

        <div class="type-summary-strip">
          <article v-for="item in typeSummary" :key="item.key" class="type-summary-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.count }} 题</strong>
          </article>
        </div>

        <div v-if="loadingDetails" class="detail-loading">
          <el-skeleton :rows="5" animated />
        </div>

        <el-empty v-else-if="!groups.length || actualTotal === 0" description="暂无候选题目" />

        <div v-else class="question-groups">
          <section v-for="(group, groupIndex) in visibleGroups" :key="group.typeCode" class="question-group">
            <div class="group-header">
              <div>
                <h5>{{ chineseIndex(groupIndex + 1) }}、{{ group.typeLabel }}</h5>
                <p>目标 {{ group.expectedCount }} 题，已生成 {{ group.questions.length }} 题</p>
              </div>
              <el-button v-if="group.questions.length > previewLimit" link type="primary" @click="toggleGroup(group.typeCode)">
                {{ isGroupExpanded(group.typeCode) ? '收起' : '展开全部' }}
              </el-button>
            </div>

            <div class="question-list">
              <article
                v-for="(question, index) in displayQuestions(group)"
                :key="question.questionId || `${group.typeCode}_${index}`"
                class="question-row"
              >
                <span class="question-index">{{ index + 1 }}.</span>
                <div class="question-stem">
                  <MathView :content="question.stem || question.title || '暂无题干内容'" empty-text="暂无题干内容" />
                </div>
                <span class="question-difficulty">难度 {{ formatDifficulty(question.difficulty) }}</span>
              </article>
            </div>

            <button
              v-if="group.questions.length > previewLimit && !isGroupExpanded(group.typeCode)"
              class="expand-row"
              type="button"
              @click="toggleGroup(group.typeCode)"
            >
              <span>还有 {{ group.questions.length - previewLimit }} 题未展示</span>
              <strong>展开全部</strong>
            </button>
          </section>
        </div>
      </section>
    </main>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue';
import { MagicStick } from '@element-plus/icons-vue';
import MathView from '../MathView.vue';
import type { GeneratePaperDraftBucketConstrain, PaperDraftPreviewGroup } from '../../types/ai';

const props = defineProps<{
  reason: string;
  sourceNames: string[];
  constraints: GeneratePaperDraftBucketConstrain[];
  groups: PaperDraftPreviewGroup[];
  expectedTotal: number;
  actualTotal: number;
  shortageMessage: string;
  loadingDetails: boolean;
  detailErrorMessage: string;
  typeLabel: (code?: string) => string;
}>();

const previewLimit = 3;
const expandedGroups = reactive<Record<string, boolean>>({});

const constraintRows = computed(() =>
  props.constraints.map((item, index) => ({
    key: `${item.typeCode}_${index}`,
    label: props.typeLabel(item.typeCode),
    count: Number(item.count) || 0,
    range: `${formatDifficulty(item.difficultyMin)} ~ ${formatDifficulty(item.difficultyMax)}`
  }))
);

const visibleGroups = computed(() => props.groups.filter((group) => group.expectedCount > 0 || group.questions.length > 0));

const typeSummary = computed(() =>
  visibleGroups.value.map((group) => ({
    key: group.typeCode,
    label: group.typeLabel,
    count: group.questions.length
  }))
);

function isGroupExpanded(typeCode: string) {
  return Boolean(expandedGroups[typeCode]);
}

function toggleGroup(typeCode: string) {
  expandedGroups[typeCode] = !expandedGroups[typeCode];
}

function displayQuestions(group: PaperDraftPreviewGroup) {
  if (isGroupExpanded(group.typeCode)) {
    return group.questions;
  }
  return group.questions.slice(0, previewLimit);
}

function formatDifficulty(value?: number | null) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return '-';
  }
  return numeric.toFixed(2);
}

function chineseIndex(value: number) {
  const map = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十'];
  return map[value - 1] ?? String(value);
}
</script>

<style scoped>
.draft-preview-layout {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(320px, 0.82fr) minmax(0, 1.18fr);
  gap: 14px;
  overflow: hidden;
}

.draft-side,
.draft-main {
  min-height: 0;
  overflow-y: auto;
}

.draft-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reason-card,
.source-card,
.constraint-card,
.draft-card {
  border: 1px solid #e4ebf5;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}

.reason-card,
.source-card,
.constraint-card {
  padding: 18px;
}

.reason-title {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.reason-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: #edf5ff;
  color: var(--el-color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.reason-title h4,
.card-heading h4,
.draft-card-header h4 {
  margin: 0;
  color: #1f2937;
  font-size: 17px;
}

.reason-title p,
.draft-card-header p {
  margin: 5px 0 0;
  color: #7b8495;
  font-size: 12px;
}

.reason-body {
  padding: 13px 14px;
  border-radius: 10px;
  background: #f8fbff;
  color: #4f5d73;
}

.card-heading {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.card-heading span {
  color: #7b8495;
  font-size: 12px;
}

.source-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.constraint-table {
  border: 1px solid #eef2f7;
  border-radius: 10px;
  overflow: hidden;
}

.constraint-head,
.constraint-row {
  display: grid;
  grid-template-columns: 1fr 0.7fr 1fr;
  gap: 10px;
  align-items: center;
}

.constraint-head {
  padding: 9px 12px;
  background: #f8fafd;
  color: #7b8495;
  font-size: 12px;
  font-weight: 600;
}

.constraint-row {
  padding: 10px 12px;
  border-top: 1px solid #eef2f7;
  color: #526075;
  font-size: 13px;
}

.draft-card {
  min-height: 100%;
  padding: 18px;
}

.draft-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid #edf2f8;
}

.shortage-alert {
  margin-top: 12px;
}

.type-summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 14px 0;
}

.type-summary-card {
  padding: 12px 14px;
  border-radius: 10px;
  background: #f7faff;
  border: 1px solid #edf2f8;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.type-summary-card span {
  color: #526075;
  font-size: 13px;
  font-weight: 600;
}

.type-summary-card strong {
  color: #1f2937;
  font-size: 15px;
}

.detail-loading {
  padding: 12px 0;
}

.question-groups {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-group {
  border: 1px solid #edf2f8;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  padding: 13px 14px;
  background: #fbfcff;
  border-bottom: 1px solid #edf2f8;
}

.group-header h5 {
  margin: 0;
  color: #1f2937;
  font-size: 15px;
}

.group-header p {
  margin: 4px 0 0;
  color: #7b8495;
  font-size: 12px;
}

.question-list {
  display: flex;
  flex-direction: column;
}

.question-row {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) 88px;
  gap: 10px;
  align-items: start;
  padding: 10px 14px;
  border-top: 1px solid #f0f3f8;
}

.question-row:first-child {
  border-top: none;
}

.question-index {
  color: #7b8495;
  font-weight: 600;
}

.question-stem {
  min-width: 0;
  color: #293348;
}

.question-stem :deep(.math-view),
.question-stem :deep(.assistant-markdown) {
  font-size: 13px;
  line-height: 1.65;
}

.question-difficulty {
  color: #7b8495;
  font-size: 12px;
  text-align: right;
  white-space: nowrap;
}

.expand-row {
  width: 100%;
  border: none;
  border-top: 1px solid #f0f3f8;
  background: #fbfcff;
  color: var(--el-color-primary);
  padding: 10px 12px;
  display: flex;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
}

@media (max-width: 1024px) {
  .draft-preview-layout {
    grid-template-columns: 1fr;
    overflow-y: auto;
  }

  .draft-side,
  .draft-main {
    overflow: visible;
  }

  .type-summary-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
