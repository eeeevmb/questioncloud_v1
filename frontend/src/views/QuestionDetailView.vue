<template>
  <section class="card detail-card" v-if="detail">
    <header class="detail-header">
      <div class="title-block">
        <button class="link-btn" type="button" v-if="fromCollectionId" @click="goBack">
          ← 返回题集
        </button>
        <h1>{{ detail.title }}</h1>
        <p class="subtitle">{{ typeLabel }} · 当前版本 {{ detail.versionNo }}</p>
        <details class="meta-details">
          <summary>更多信息</summary>
          <ul>
            <li>
              题目 ID
              <span>{{ detail.id }}</span>
              <button type="button" @click="copyValue(detail.id, '题目ID已复制')">复制</button>
            </li>
            <li>
              当前版本 ID
              <span>{{ detail.currentVersionId }}</span>
              <button type="button" @click="copyValue(detail.currentVersionId, '版本ID已复制')">复制</button>
            </li>
            <li>
              题型编码
              <span>{{ detail.typeCode }}</span>
              <button type="button" @click="copyValue(detail.typeCode, '题型编码已复制')">复制</button>
            </li>
          </ul>
        </details>
      </div>
      <div class="action-group">
        <button class="primary-btn" type="button" @click="goToEdit">
          修改并生成新版本
        </button>
        <button class="danger-link" type="button" @click="confirmDelete">
          删除题目
        </button>
      </div>
    </header>

    <section class="stats-section">
      <div class="stat-card" v-for="stat in statCards" :key="stat.label">
        <p class="label">{{ stat.label }}</p>
        <p class="value" :class="{ placeholder: stat.placeholder }">{{ stat.value }}</p>
        <p class="hint" v-if="stat.hint">{{ stat.hint }}</p>
      </div>
    </section>

    <section class="content-section">
      <h3>题干</h3>
      <MathView :content="detail.stem || ''" empty-text="暂无题干内容" />
      <AttachmentGallery v-if="stemAssets.length" :assets="stemAssets" title="题干附件" />
    </section>

    <section v-if="isChoiceType" class="content-section">
      <h3>选项</h3>
      <template v-if="hasOptions">
        <table class="options-table">
          <thead>
            <tr>
              <th>选项</th>
              <th>内容</th>
              <th>结果</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="option in detail.options" :key="option.key">
              <td class="option-key">{{ option.key }}</td>
              <td class="option-content">
                <MathView :content="option.content || ''" empty-text="暂无内容" />
              </td>
              <td class="result-cell">
                <span v-if="isCorrectOption(option.key)" class="badge success">正确</span>
              </td>
            </tr>
          </tbody>
        </table>
      </template>
      <p v-else class="muted">
        该题为选择题，但详情接口未返回 options，请后端在 QuestionDetailVO 增加 options 映射。
      </p>
    </section>

    <section class="content-section">
      <h3>{{ answerSectionTitle }}</h3>
      <template v-if="isTrueFalseType">
        <p class="badge success" v-if="booleanAnswer">{{ booleanAnswer }}</p>
        <p class="muted" v-else>尚未提供答案</p>
      </template>
      <template v-else>
        <MathView :content="detail.answer || ''" empty-text="暂无答案" />
      </template>
    </section>

    <section class="content-section">
      <h3>解析</h3>
      <MathView :content="detail.solution || ''" empty-text="尚未提供解析" />
      <AttachmentGallery v-if="solutionAssets.length" :assets="solutionAssets" title="解析附件" />
    </section>
  </section>
  <div v-else class="card">加载中...</div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchQuestionDetail, deleteQuestion } from '../api/question';
import type { QuestionDetail } from '../types/question';
import { showSuccess } from '../utils/messages';
import MathView from '../components/MathView.vue';
import AttachmentGallery from '../components/AttachmentGallery.vue';

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

const statCards = computed(() => {
  if (!detail.value) {
    return [];
  }
  return [
    buildStat('难度', detail.value.difficulty, '未设置'),
    buildStat('正确率', detail.value.correctRate, '暂无数据', (v) => `${(v * 100).toFixed(1)}%`),
    buildStat('曝光系数', detail.value.exposureFactor, '暂无数据'),
    buildStat('尝试次数', detail.value.attempts, '暂无'),
    {
      label: '更新时间',
      value: detail.value.updatedAt ? formatDate(detail.value.updatedAt) : '暂无',
      placeholder: !detail.value.updatedAt
    },
    {
      label: '最后曝光',
      value: detail.value.lastExposedAt ? formatDate(detail.value.lastExposedAt) : '尚未曝光',
      placeholder: !detail.value.lastExposedAt
    }
  ];
});

function buildStat(label: string, value?: number | null, fallback = '暂无数据', formatter?: (value: number) => string) {
  if (value === undefined || value === null) {
    return { label, value: fallback, placeholder: true };
  }
  return { label, value: formatter ? formatter(value) : value.toFixed(2), placeholder: false };
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
  const input = window.prompt(`将删除题目「${detail.value.title}」，输入“删除”确认`);
  if (input !== '删除') {
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

function formatDate(date: string) {
  return new Date(date).toLocaleString();
}
</script>

<style scoped>
.detail-card {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.title-block {
  flex: 1;
}

.subtitle {
  margin: 4px 0;
  color: #475569;
}

.meta-details {
  margin-top: 8px;
}

.meta-details ul {
  list-style: none;
  padding: 0;
  margin: 8px 0 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.meta-details li {
  font-size: 13px;
  color: #475569;
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.meta-details span {
  font-family: 'SFMono-Regular', Menlo, Consolas, monospace;
}

.meta-details button {
  border: 1px solid #94a3b8;
  background: transparent;
  padding: 2px 8px;
  border-radius: 6px;
}

.link-btn {
  background: none;
  border: none;
  padding: 0;
  color: #2563eb;
  cursor: pointer;
  margin-bottom: 8px;
}

.action-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.danger-link {
  background: none;
  border: none;
  color: #dc2626;
  text-decoration: underline;
  font-weight: 600;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.stat-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
}

.stat-card .label {
  font-size: 13px;
  color: #475569;
  margin-bottom: 6px;
}

.stat-card .value {
  font-size: 20px;
  font-weight: 600;
}

.stat-card .value.placeholder {
  color: #94a3b8;
}

.content-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.options-table {
  width: 100%;
  border-collapse: collapse;
}

.options-table th,
.options-table td {
  border: 1px solid #e2e8f0;
  padding: 8px;
  vertical-align: top;
}

.options-table .option-key {
  width: 80px;
  font-weight: 600;
  text-align: center;
}

.option-content :deep(.math-view) {
  background: transparent;
  padding: 0;
}

.result-cell {
  width: 120px;
  text-align: center;
}

.badge {
  display: inline-flex;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.badge.success {
  background: rgba(34, 197, 94, 0.15);
  color: #047857;
}

.muted {
  color: #94a3b8;
  font-size: 14px;
}
</style>
