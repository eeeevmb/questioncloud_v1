<template>
  <section v-if="detail" class="question-detail-page">
    <main class="question-main">
      <div class="detail-header">
        <div class="title-block">
          <el-button v-if="fromCollectionId" link type="primary" class="back-button" @click="goBack">返回题集</el-button>
          <h1>{{ detail.title }}</h1>
          <div class="meta-row">
            <el-tag effect="light" class="type-tag" :class="typeTagClass">{{ typeLabel }}</el-tag>
            <el-tag effect="plain" class="version-tag">v{{ detail.versionNo }}</el-tag>
            <span class="meta-item">
              难度
              <strong>{{ formatDecimal(detail.difficulty) }}</strong>
              <el-tag effect="light" size="small" class="difficulty-tag" :class="difficultyLevelClass">
                {{ difficultyLabel }}
              </el-tag>
            </span>
            <span class="meta-item">更新于 {{ detail.updatedAt ? formatDate(detail.updatedAt) : '暂无' }}</span>
          </div>
        </div>
        <div class="action-group">
          <el-button type="primary" @click="goToEdit">修改题目</el-button>
          <el-button type="danger" plain @click="confirmDelete">删除题目</el-button>
        </div>
      </div>

      <section class="content-card stem-card">
        <div class="section-title">
          <h2>题干</h2>
        </div>
        <div class="stem-content">
          <MathView :content="detail.stem || ''" empty-text="暂无题干内容" />
        </div>
        <AttachmentGallery v-if="stemAssets.length" :assets="stemAssets" title="题干附件" />
      </section>

      <section v-if="isChoiceType" class="content-card">
        <div class="section-title">
          <h2>选项</h2>
          <span class="section-note">右侧标记正确结果</span>
        </div>
        <div v-if="hasOptions" class="options-list">
          <div v-for="option in detail.options || []" :key="option.key" class="option-row">
            <span class="option-key" :class="{ 'option-key--correct': isCorrectOption(option.key) }">
              {{ option.key }}
            </span>
            <div class="option-content">
              <MathView :content="option.content || ''" empty-text="暂无内容" />
            </div>
            <div class="option-result">
              <el-tag v-if="isCorrectOption(option.key)" type="success" effect="light">正确</el-tag>
              <span v-else>—</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无选项内容" />
      </section>

      <section class="content-card">
        <div class="section-title">
          <h2>{{ answerSectionTitle }}</h2>
        </div>
        <template v-if="isTrueFalseType">
          <el-tag v-if="booleanAnswer" type="success" effect="light">{{ booleanAnswer }}</el-tag>
          <span v-else class="muted">尚未提供答案</span>
        </template>
        <template v-else>
          <MathView :content="detail.answer || ''" empty-text="暂无答案" />
        </template>
      </section>

      <details class="content-card solution-card">
        <summary>
          <span>解析</span>
          <span class="solution-state">{{ detail.solution || solutionAssets.length ? '已填写' : '暂无内容' }}</span>
        </summary>
        <div class="solution-body">
          <MathView :content="detail.solution || ''" empty-text="尚未提供解析" />
          <AttachmentGallery v-if="solutionAssets.length" :assets="solutionAssets" title="解析附件" />
        </div>
      </details>

      <section class="supplement-bar">
        <div v-for="item in supplementItems" :key="item.label" class="supplement-item">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
        <div class="supplement-id">
          <span>题目 ID</span>
          <strong class="mono">{{ shortId(detail.id) }}</strong>
          <el-button link type="primary" @click="copyValue(detail.id, '题目 ID 已复制')">复制</el-button>
        </div>
      </section>
    </main>

    <aside class="stats-sidebar">
      <div class="stats-title">
        <h2>题目统计</h2>
        <p>基于当前题目的学习与曝光记录</p>
      </div>
      <div class="sidebar-metrics">
        <article
          v-for="stat in statCards"
          :key="stat.label"
          class="sidebar-metric-card"
          :class="[`sidebar-metric-card--${stat.tone}`, { 'sidebar-metric-card--featured': stat.featured }]"
        >
          <div class="metric-mainline">
            <span class="metric-icon">
              <el-icon>
                <component :is="stat.icon" />
              </el-icon>
            </span>
            <div class="metric-copy">
              <div class="metric-topline">
                <span class="metric-label">{{ stat.label }}</span>
              </div>
              <div class="metric-value-row">
                <span class="metric-value" :class="{ placeholder: stat.placeholder }">{{ stat.value }}</span>
                <el-tag
                  v-if="stat.badge"
                  effect="light"
                  size="small"
                  class="metric-badge"
                  :class="`metric-badge--${stat.badgeTone ?? stat.tone}`"
                >
                  {{ stat.badge }}
                </el-tag>
              </div>
            </div>
          </div>
          <div
            v-if="stat.progress !== undefined"
            class="metric-progress"
            :class="{ 'metric-progress--empty': stat.placeholder }"
          >
            <span>0</span>
            <div
              class="metric-progress-track"
              role="progressbar"
              :aria-valuenow="stat.progress"
              aria-valuemin="0"
              aria-valuemax="1"
            >
              <i :style="{ width: formatProgressWidth(stat.progress) }" />
            </div>
            <span>1</span>
          </div>
          <p v-if="stat.hint" class="metric-hint">{{ stat.hint }}</p>
        </article>
      </div>
    </aside>
  </section>

  <el-card v-else class="detail-loading">
    <el-skeleton :rows="6" animated />
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import type { Component } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { Aim, Clock, DataLine, Histogram, Odometer, View } from '@element-plus/icons-vue';
import { fetchQuestionDetail, deleteQuestion } from '../api/question';
import type { QuestionDetail } from '../types/question';
import { showSuccess } from '../utils/messages';
import MathView from '../components/MathView.vue';
import AttachmentGallery from '../components/AttachmentGallery.vue';

interface StatCard {
  label: string;
  value: string;
  placeholder: boolean;
  tone: 'primary' | 'success' | 'warning' | 'neutral';
  icon: Component;
  featured?: boolean;
  hint?: string;
  badge?: string;
  badgeTone?: StatCard['tone'];
  progress?: number;
}

const choiceTypes = ['single-choice', 'multiple-choice'];
const route = useRoute();
const router = useRouter();
const detail = ref<QuestionDetail | null>(null);
const loading = ref(false);

const questionId = computed(() => {
  const id = route.params.questionId;
  return typeof id === 'string' ? id : '';
});
const fromCollectionId = computed(() => {
  const id = route.query.collectionId;
  return typeof id === 'string' ? id : null;
});

const isChoiceType = computed(() => choiceTypes.includes(detail.value?.typeCode ?? ''));
const isTrueFalseType = computed(() => detail.value?.typeCode === 'true-false');
const answerSectionTitle = computed(() => (isTrueFalseType.value ? '答案' : '参考答案'));

const typeLabelMap: Record<string, string> = {
  'single-choice': '单选题',
  'multiple-choice': '多选题',
  'true-false': '判断题',
  'fill-in': '填空题',
  'short-answer': '简答题'
};

const typeLabel = computed(() => {
  if (!detail.value) {
    return '';
  }
  return typeLabelMap[detail.value.typeCode] ?? detail.value.typeCode;
});

const typeTagClass = computed(() => `type-tag--${detail.value?.typeCode ?? 'default'}`);
const difficultyLabel = computed(() => getDifficultyLabel(detail.value?.difficulty));
const difficultyLevelClass = computed(() => `difficulty-tag--${getDifficultyLevel(detail.value?.difficulty)}`);

const supplementItems = computed(() => [
  { label: '知识点', value: '—' },
  { label: '来源', value: fromCollectionId.value ? '当前题集' : '题库' },
  { label: '备注', value: '—' }
]);

const statCards = computed<StatCard[]>(() => {
  if (!detail.value) {
    return [];
  }
  return [
    buildStat('难度', detail.value.difficulty, '未设置', {
      tone: 'primary',
      icon: DataLine,
      featured: true,
      formatter: formatDecimal,
      badge: getDifficultyLabel(detail.value.difficulty),
      badgeTone: difficultyTone(detail.value.difficulty),
      progress: clampRatio(detail.value.difficulty)
    }),
    buildStat('正确率', detail.value.correctRate, '暂无数据', {
      tone: 'success',
      icon: Aim,
      formatter: (v) => `${(v * 100).toFixed(1)}%`,
      hint: '当前题目的正确率',
      progress: clampRatio(detail.value.correctRate)
    }),
    buildStat('曝光系数', detail.value.exposureFactor, '暂无数据', {
      tone: 'neutral',
      icon: View,
      formatter: formatDecimal,
      hint: '用于观察题目被曝光的频次强度'
    }),
    buildStat('尝试次数', detail.value.attempts, '暂无', {
      tone: 'warning',
      icon: Histogram,
      formatter: formatInteger,
      hint: '累计答题或尝试次数'
    }),
    {
      label: '最后曝光',
      value: detail.value.lastExposedAt ? formatDate(detail.value.lastExposedAt) : '尚未曝光',
      placeholder: !detail.value.lastExposedAt,
      tone: 'neutral',
      icon: Clock,
      hint: '最近一次进入曝光链路的时间'
    }
  ];
});

function buildStat(
  label: string,
  value?: number | null,
  fallback = '暂无数据',
  options?: {
    formatter?: (value: number) => string;
    tone?: StatCard['tone'];
    icon?: Component;
    hint?: string;
    featured?: boolean;
    badge?: string;
    badgeTone?: StatCard['tone'];
    progress?: number;
  }
): StatCard {
  if (value === undefined || value === null) {
    return {
      label,
      value: fallback,
      placeholder: true,
      tone: options?.tone ?? 'neutral',
      icon: options?.icon ?? Odometer,
      hint: options?.hint,
      featured: options?.featured,
      badge: options?.badge,
      badgeTone: options?.badgeTone,
      progress: options?.progress
    };
  }
  return {
    label,
    value: options?.formatter ? options.formatter(value) : formatDecimal(value),
    placeholder: false,
    tone: options?.tone ?? 'neutral',
    icon: options?.icon ?? Odometer,
    hint: options?.hint,
    featured: options?.featured,
    badge: options?.badge,
    badgeTone: options?.badgeTone,
    progress: options?.progress
  };
}

const hasOptions = computed(() => Boolean(detail.value?.options?.length));

const correctOptionSet = computed(() => {
  const set = new Set<string>();
  const options = detail.value?.correctOptions;
  if (options?.length) {
    options.forEach((opt) => opt && set.add(opt.trim().toUpperCase()));
    return set;
  }
  const key = detail.value?.answerKey;
  if (!key) {
    return set;
  }
  key.split(/[,，;；\s]+/)
    .filter(Boolean)
    .forEach((segment) => {
      segment.split('').forEach((char) => {
        if (char.trim()) {
          set.add(char.toUpperCase());
        }
      });
    });
  return set;
});

const stemAssets = computed(() => detail.value?.assets?.filter((asset) => asset.section === 'PRO') ?? []);
const solutionAssets = computed(() => detail.value?.assets?.filter((asset) => asset.section === 'SOLU') ?? []);

const booleanAnswer = computed(() => {
  if (detail.value?.answer?.trim()) {
    return detail.value.answer;
  }
  const raw = detail.value?.judgeAnswer ?? detail.value?.answerKey;
  if (!raw) {
    return '';
  }
  const first = raw.trim().toUpperCase();
  if (first === 'T') {
    return '正确';
  }
  if (first === 'F') {
    return '错误';
  }
  return raw;
});

onMounted(loadDetail);

watch(() => route.params.questionId, loadDetail);

async function loadDetail() {
  if (!questionId.value) {
    return;
  }
  loading.value = true;
  try {
    detail.value = await fetchQuestionDetail(questionId.value);
  } finally {
    loading.value = false;
  }
}

function goToEdit() {
  router.push({
    name: 'question-edit',
    params: { questionId: questionId.value },
    query: { collectionId: fromCollectionId.value ?? undefined }
  });
}

function goBack() {
  if (fromCollectionId.value) {
    router.push({
      name: 'collection-questions',
      params: { collectionId: fromCollectionId.value }
    });
  } else {
    router.push('/collections');
  }
}

async function confirmDelete() {
  if (!questionId.value || !detail.value) {
    return;
  }
  let value = '';
  try {
    const result = await ElMessageBox.prompt(`将删除题目「${detail.value.title}」，输入“删除”确认`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      inputPattern: /^删除$/,
      inputErrorMessage: '请输入“删除”'
    });
    value = result.value;
  } catch {
    return;
  }
  if (value !== '删除') {
    return;
  }
  await deleteQuestion(questionId.value);
  showSuccess('题目删除成功');
  goBack();
}

function isCorrectOption(key?: string) {
  if (!key) {
    return false;
  }
  return correctOptionSet.value.has(key.trim().toUpperCase());
}

function copyValue(value: string, message: string) {
  navigator.clipboard.writeText(value).then(() => showSuccess(message));
}

function shortId(value: string) {
  if (value.length <= 12) {
    return value;
  }
  return `${value.slice(0, 6)}…${value.slice(-4)}`;
}

function getDifficultyLabel(value?: number | null) {
  const level = getDifficultyLevel(value);
  if (level === 'easy') {
    return '简单';
  }
  if (level === 'hard') {
    return '困难';
  }
  if (level === 'unset') {
    return '未设置';
  }
  return '中等';
}

function difficultyTone(value?: number | null): StatCard['tone'] {
  const level = getDifficultyLevel(value);
  if (level === 'easy') {
    return 'success';
  }
  if (level === 'hard') {
    return 'warning';
  }
  return 'primary';
}

function getDifficultyLevel(value?: number | null) {
  if (value === undefined || value === null) {
    return 'unset';
  }
  if (value < 0.34) {
    return 'easy';
  }
  if (value >= 0.67) {
    return 'hard';
  }
  return 'medium';
}

function formatDate(date: string) {
  return new Date(date).toLocaleString();
}

function formatDecimal(value?: number | null) {
  if (value === undefined || value === null) {
    return '未设置';
  }
  return value.toFixed(2);
}

function formatInteger(value?: number | null) {
  if (value === undefined || value === null) {
    return '暂无';
  }
  return Math.trunc(value).toLocaleString();
}

function clampRatio(value?: number | null) {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return 0;
  }
  return Math.max(0, Math.min(1, value));
}

function formatProgressWidth(value?: number) {
  return `${Math.round(clampRatio(value) * 100)}%`;
}
</script>

<style scoped>
.question-detail-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  align-items: start;
  gap: 20px;
  padding: 4px 0 18px;
}

.question-detail-page :deep(.el-button),
.question-detail-page :deep(.el-tag) {
  border-radius: 6px;
}

.question-main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 16px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  padding: 4px 2px 2px;
}

.title-block {
  min-width: 0;
}

.back-button {
  margin: 0 0 12px -8px;
  color: #475569;
  font-weight: 600;
}

.title-block h1 {
  margin: 0;
  color: #172033;
  font-size: 34px;
  line-height: 1.2;
  font-weight: 750;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  color: #64748b;
  font-size: 14px;
}

.meta-item strong {
  color: #334155;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.action-group {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.type-tag,
.version-tag,
.difficulty-tag {
  border: 1px solid transparent;
  font-weight: 650;
}

.type-tag--single-choice {
  color: #2563eb;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.type-tag--multiple-choice {
  color: #4f46e5;
  background: #eef2ff;
  border-color: #c7d2fe;
}

.type-tag--true-false {
  color: #047857;
  background: #ecfdf5;
  border-color: #bbf7d0;
}

.type-tag--fill-in {
  color: #b45309;
  background: #fffbeb;
  border-color: #fde68a;
}

.type-tag--short-answer {
  color: #be123c;
  background: #fff1f2;
  border-color: #fecdd3;
}

.type-tag--default,
.version-tag {
  color: #475569;
  background: #f8fafc;
  border-color: #e2e8f0;
}

.difficulty-tag--easy {
  color: #047857;
  background: #ecfdf5;
  border-color: #bbf7d0;
}

.difficulty-tag--medium {
  color: #b45309;
  background: #fffbeb;
  border-color: #fde68a;
}

.difficulty-tag--hard {
  color: #be123c;
  background: #fff1f2;
  border-color: #fecdd3;
}

.difficulty-tag--unset {
  color: #64748b;
  background: #f8fafc;
  border-color: #e2e8f0;
}

.content-card,
.supplement-bar,
.sidebar-metric-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.04);
}

.content-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px 20px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 26px;
}

.section-title h2,
.stats-title h2 {
  margin: 0;
  color: #1f2937;
  font-size: 17px;
  line-height: 1.3;
  font-weight: 750;
}

.section-note,
.stats-title p,
.metric-hint,
.muted {
  color: #64748b;
}

.section-note {
  font-size: 13px;
}

.stem-card {
  padding: 20px 22px;
}

.stem-content {
  color: #111827;
  font-size: 18px;
  line-height: 1.8;
}

.options-list {
  overflow: hidden;
  border: 1px solid #edf1f7;
  border-radius: 8px;
}

.option-row {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) 86px;
  align-items: center;
  min-height: 58px;
  gap: 14px;
  padding: 12px 16px;
  border-bottom: 1px solid #edf1f7;
  background: #ffffff;
}

.option-row:last-child {
  border-bottom: 0;
}

.option-row:hover {
  background: #f8fbff;
}

.option-key {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid #dbe3ef;
  border-radius: 50%;
  color: #475569;
  background: #ffffff;
  font-weight: 700;
}

.option-key--correct {
  color: #15803d;
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.option-content {
  min-width: 0;
  color: #1f2937;
}

.option-result {
  display: flex;
  justify-content: center;
  color: #94a3b8;
}

.solution-card {
  padding: 0;
}

.solution-card summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px;
  cursor: pointer;
  list-style: none;
  color: #1f2937;
  font-size: 17px;
  font-weight: 750;
}

.solution-card summary::-webkit-details-marker {
  display: none;
}

.solution-card summary::after {
  content: '展开';
  color: #2563eb;
  font-size: 13px;
  font-weight: 650;
}

.solution-card[open] summary {
  border-bottom: 1px solid #edf1f7;
}

.solution-card[open] summary::after {
  content: '收起';
}

.solution-state {
  margin-left: auto;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.solution-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px 20px 20px;
}

.supplement-bar {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) auto;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
}

.supplement-item,
.supplement-id {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.supplement-item span,
.supplement-id span {
  color: #64748b;
  font-size: 13px;
}

.supplement-item strong,
.supplement-id strong {
  overflow: hidden;
  color: #334155;
  font-size: 14px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mono {
  font-family: Menlo, Consolas, monospace;
}

.stats-sidebar {
  position: fixed;
  top: 80px;
  right: max(20px, calc((100vw - 1240px) / 2 + 20px));
  display: flex;
  min-width: 0;
  width: 360px;
  max-height: calc(100vh - 100px);
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-bottom: 2px;
  scrollbar-gutter: stable;
  z-index: 2;
}

.stats-title {
  padding: 6px 2px 0;
}

.stats-title p {
  margin: 6px 0 0;
  font-size: 13px;
}

.sidebar-metrics {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sidebar-metric-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 126px;
  padding: 16px 18px 18px;
}

.metric-mainline {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.metric-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 48px;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  color: #64748b;
  background: #f1f5f9;
}

.metric-icon :deep(.el-icon) {
  font-size: 24px;
}

.sidebar-metric-card--primary .metric-icon {
  color: #2563eb;
  background: #dbeafe;
}

.sidebar-metric-card--success .metric-icon {
  color: #16a34a;
  background: #dcfce7;
}

.sidebar-metric-card--warning .metric-icon {
  color: #f97316;
  background: #ffedd5;
}

.metric-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 8px;
}

.metric-topline {
  display: flex;
  align-items: center;
  gap: 8px;
}

.metric-label {
  color: #475569;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0;
}

.metric-value-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.metric-value {
  color: #111827;
  font-size: 30px;
  line-height: 1.1;
  font-weight: 750;
  font-variant-numeric: tabular-nums;
}

.sidebar-metric-card--featured .metric-value {
  font-size: 32px;
}

.metric-value.placeholder {
  color: #94a3b8;
  font-size: 24px;
}

.metric-badge {
  flex: 0 0 auto;
  border: 1px solid transparent;
  font-weight: 650;
}

.metric-badge--primary,
.metric-badge--neutral {
  color: #475569;
  background: #f8fafc;
  border-color: #e2e8f0;
}

.metric-badge--success {
  color: #047857;
  background: #ecfdf5;
  border-color: #bbf7d0;
}

.metric-badge--warning {
  color: #b45309;
  background: #fffbeb;
  border-color: #fde68a;
}

.metric-progress {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  color: #64748b;
  font-size: 12px;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}

.metric-progress-track {
  position: relative;
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #edf2f7;
}

.metric-progress-track i {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: inherit;
  background: #2563eb;
}

.sidebar-metric-card--success .metric-progress-track i {
  background: #16a34a;
}

.metric-progress--empty .metric-progress-track i {
  background: #cbd5e1;
}

.metric-hint {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
}

.detail-loading {
  border-radius: 8px;
}

@media (max-width: 1080px) {
  .question-detail-page {
    grid-template-columns: 1fr;
  }

  .stats-sidebar {
    position: static;
    max-height: none;
    overflow: visible;
  }

  .sidebar-metrics {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .question-detail-page {
    gap: 16px;
  }

  .detail-header {
    flex-direction: column;
  }

  .title-block h1 {
    font-size: 28px;
  }

  .action-group {
    width: 100%;
    justify-content: flex-start;
  }

  .option-row {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .option-result {
    grid-column: 2;
    justify-content: flex-start;
  }

  .supplement-bar,
  .sidebar-metrics {
    grid-template-columns: 1fr;
  }

  .supplement-bar {
    align-items: flex-start;
  }
}
</style>
