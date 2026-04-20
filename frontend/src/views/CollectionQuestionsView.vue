<template>
  <section class="collection-questions-page">
    <div class="hero-card">
      <div class="hero-copy">
        <p class="eyebrow">题集管理</p>
        <h2 v-if="collectionName">题集「{{ collectionName }}」</h2>
        <h2 v-else>题集内容</h2>
        <p>题集是题目的集合，支持按题型、难度和更新时间检索与维护。</p>
      </div>
      <div class="hero-meta">
        <el-button type="primary" @click="goToCreate">添加题目</el-button>
      </div>
    </div>

    <el-card class="filter-card" shadow="never">
      <div class="card-title-row">
        <div>
          <h3>检索条件</h3>
          <p>支持按题型、难度和关键字快速定位题目。</p>
        </div>
      </div>

      <el-form label-position="top" class="filter-form">
        <div class="filter-section">
          <div class="filter-section-head">
            <span class="filter-section-label">基础筛选</span>
            <span class="filter-section-desc">先定位题目范围，再决定排序方式。</span>
          </div>
          <div class="filter-grid filter-grid--primary">
            <el-form-item label="关键词">
              <el-input v-model="filters.keyword" placeholder="标题关键字" clearable />
            </el-form-item>
            <el-form-item label="题型">
              <el-select v-model="filters.typeCode" placeholder="全部题型" clearable>
                <el-option label="全部" value="" />
                <el-option v-for="type in questionTypes" :key="type.code" :label="type.label" :value="type.code" />
              </el-select>
            </el-form-item>
            <el-form-item label="排序字段">
              <el-select v-model="filters.sortField">
                <el-option label="更新时间" value="updatedAt" />
                <el-option label="创建时间" value="createdAt" />
                <el-option label="难度" value="difficulty" />
                <el-option label="正确率" value="correctRate" />
              </el-select>
            </el-form-item>
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-section-head">
            <span class="filter-section-label">排序与范围</span>
            <span class="filter-section-desc">用于收窄结果或调整默认浏览顺序。</span>
          </div>
          <div class="filter-grid filter-grid--secondary">
            <el-form-item label="难度下限">
              <el-input-number v-model="filters.levelMin" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" />
            </el-form-item>
            <el-form-item label="难度上限">
              <el-input-number v-model="filters.levelMax" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" />
            </el-form-item>
            <el-form-item label="排序方向">
              <el-select v-model="filters.sortDirection">
                <el-option label="降序" value="DESC" />
                <el-option label="升序" value="ASC" />
              </el-select>
            </el-form-item>
          </div>
        </div>

        <div class="filter-footer">
          <el-alert v-if="levelError" :title="levelError" type="error" :closable="false" class="level-error" />
          <div class="filter-actions">
            <el-button type="primary" :loading="loading" :disabled="Boolean(levelError)" @click="loadQuestions">应用筛选</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="page?.records ?? []" row-key="id" stripe>
        <el-table-column prop="title" label="题目" min-width="260">
          <template #default="{ row }">
            <span class="title-text" :title="row.title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="typeCode" label="题型" width="104" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag
              effect="light"
              class="type-tag"
              size="small"
              :class="getTypeTagClass(row.typeCode)"
            >
              {{ getTypeLabel(row.typeCode) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="versionNo" label="版本" width="92" align="center" header-align="center">
          <template #default="{ row }">
            <span class="version-chip">v{{ row.versionNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度" width="132" align="center" header-align="center">
          <template #default="{ row }">
            <span v-if="row.difficulty !== undefined && row.difficulty !== null" class="difficulty-compact">
              <span class="difficulty-value">{{ formatDifficultyValue(row.difficulty) }}</span>
              <span class="difficulty-separator">/</span>
              <span class="difficulty-label" :class="getDifficultyTone(row.difficulty)">
                {{ getDifficultyLabel(row.difficulty) }}
              </span>
            </span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="correctRate" label="正确率" width="104" align="center" header-align="center">
          <template #default="{ row }">{{ formatPercent(row.correctRate) }}</template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="132" align="center" header-align="center">
          <template #default="{ row }">
            <span :title="formatFullDate(row.updatedAt)">{{ formatDate(row.updatedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="72" align="center" header-align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" class="table-action" @click="openDetail(row.id)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && page && !page.records.length" description="暂无题目" />

      <div class="pagination-wrap" v-if="page">
        <div class="pagination-summary">
          <span class="result-count">共 {{ page.total ?? 0 }} 题</span>
          <span class="page-summary">{{ pageSummary }}</span>
        </div>
        <el-pagination
          v-model:current-page="filters.pageNum"
          v-model:page-size="filters.pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="page.total"
          :hide-on-single-page="false"
          :disabled="loading"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchCollectionQuestions, fetchCollections } from '../api/collection';
import type { QuestionSummaryPage } from '../types/question';
import type { CollectionView } from '../types/collection';

const route = useRoute();
const router = useRouter();

const filters = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  sortField: 'updatedAt',
  sortDirection: 'DESC' as 'ASC' | 'DESC',
  typeCode: '',
  levelMin: undefined as number | undefined,
  levelMax: undefined as number | undefined
});

const page = ref<QuestionSummaryPage | null>(null);
const loading = ref(false);
const collectionName = ref('');
const nameLoading = ref(false);

const questionTypes = [
  { code: 'single-choice', label: '单选题' },
  { code: 'multiple-choice', label: '多选题' },
  { code: 'true-false', label: '判断题' },
  { code: 'fill-in', label: '填空题' },
  { code: 'short-answer', label: '简答题' }
];
const typeLabelMap = Object.fromEntries(questionTypes.map((item) => [item.code, item.label]));

const collectionId = computed(() => {
  const id = route.params.collectionId;
  return typeof id === 'string' ? id : '';
});

const levelError = computed(() => {
  if (
    filters.levelMin !== undefined &&
    filters.levelMax !== undefined &&
    filters.levelMin !== null &&
    filters.levelMax !== null &&
    filters.levelMin > filters.levelMax
  ) {
    return '难度下限不能大于上限';
  }
  return '';
});

const pageSummary = computed(() => {
  const currentPage = page.value;
  if (!currentPage) {
    return '';
  }
  const total = Number.isFinite(currentPage.total) ? currentPage.total : 0;
  const pageSize = currentPage.pageSize || filters.pageSize || 10;
  const pageNum = currentPage.pageNum || filters.pageNum || 1;
  const pages = Math.max(currentPage.pages || Math.ceil(total / Math.max(pageSize, 1)) || 1, 1);
  return `第 ${Math.min(pageNum, pages)} / ${pages} 页`;
});

watch(() => route.params.collectionId, () => {
  filters.pageNum = 1;
  loadQuestions();
  resolveCollectionName();
}, { immediate: true });

watch(
  () => route.query.name,
  (name) => {
    if (typeof name === 'string') {
      collectionName.value = name;
    }
  },
  { immediate: true }
);

async function loadQuestions() {
  if (!collectionId.value) {
    return;
  }
  loading.value = true;
  try {
    const response = await fetchCollectionQuestions(collectionId.value, {
      pageNum: filters.pageNum,
      pageSize: filters.pageSize,
      keyword: filters.keyword || undefined,
      sortField: filters.sortField,
      sortDirection: filters.sortDirection,
      typeCode: filters.typeCode || undefined,
      levelMin: filters.levelMin ?? undefined,
      levelMax: filters.levelMax ?? undefined
    });
    page.value = normalizeQuestionPage(response);
    filters.pageNum = page.value.pageNum || filters.pageNum;
    filters.pageSize = page.value.pageSize || filters.pageSize;
  } catch (error) {
    // 统一拦截
  } finally {
    loading.value = false;
  }
}

function normalizeInteger(value: unknown, fallback: number, min = 0) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return fallback;
  }
  const normalized = Math.trunc(parsed);
  return normalized < min ? min : normalized;
}

function normalizeQuestionPage(raw: unknown): QuestionSummaryPage {
  const payload = raw && typeof raw === 'object' ? (raw as Record<string, unknown>) : {};
  const recordsRaw = payload.records ?? payload.list ?? payload.items ?? [];
  const records = Array.isArray(recordsRaw) ? recordsRaw : [];
  const pageSize = normalizeInteger(payload.pageSize ?? payload.size, filters.pageSize || 10, 1);
  const total = normalizeInteger(payload.total, records.length, 0);
  const pageNum = normalizeInteger(payload.pageNum ?? payload.current ?? payload.page, filters.pageNum || 1, 1);
  const pages = normalizeInteger(payload.pages ?? payload.pageCount, Math.max(Math.ceil(total / pageSize), 1), 1);
  return {
    records: records as QuestionSummaryPage['records'],
    total,
    pageNum,
    pageSize,
    pages,
    hasPrevious: Boolean(payload.hasPrevious ?? pageNum > 1),
    hasNext: Boolean(payload.hasNext ?? pageNum < pages)
  };
}

function onPageChange(pageNum: number) {
  if (loading.value) {
    return;
  }
  filters.pageNum = pageNum;
  void loadQuestions();
}

function onSizeChange(pageSize: number) {
  if (loading.value) {
    return;
  }
  filters.pageSize = pageSize;
  filters.pageNum = 1;
  void loadQuestions();
}

function resetFilters() {
  filters.keyword = '';
  filters.typeCode = '';
  filters.sortField = 'updatedAt';
  filters.sortDirection = 'DESC';
  filters.levelMin = undefined;
  filters.levelMax = undefined;
  filters.pageNum = 1;
  loadQuestions();
}

function openDetail(questionId: string) {
  router.push({
    name: 'question-detail',
    params: { questionId },
    query: { collectionId: collectionId.value }
  });
}

function goToCreate() {
  const query = collectionName.value ? { name: collectionName.value } : {};
  router.push({ name: 'create-question', params: { collectionId: collectionId.value }, query });
}

function getTypeLabel(code: string) {
  return typeLabelMap[code] ?? code;
}

function getTypeTagClass(code: string) {
  return `type-tag--${code}`;
}

function formatPercent(value?: number | null) {
  if (value === undefined || value === null) {
    return '—';
  }
  return `${(value * 100).toFixed(1)}%`;
}

function formatDifficultyValue(value: number) {
  return value.toFixed(2);
}

function getDifficultyLabel(value: number) {
  if (value < 0.34) {
    return '简单';
  }
  if (value < 0.67) {
    return '中等';
  }
  return '困难';
}

function getDifficultyTone(value: number) {
  if (value < 0.34) {
    return 'difficulty-label--easy';
  }
  if (value < 0.67) {
    return 'difficulty-label--medium';
  }
  return 'difficulty-label--hard';
}

function formatDate(date: string) {
  const parsed = new Date(date);
  if (Number.isNaN(parsed.getTime())) {
    return '—';
  }
  const includeYear = parsed.getFullYear() !== new Date().getFullYear();
  return new Intl.DateTimeFormat('zh-CN', {
    ...(includeYear ? { year: '2-digit' as const } : {}),
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(parsed);
}

function formatFullDate(date: string) {
  const parsed = new Date(date);
  if (Number.isNaN(parsed.getTime())) {
    return '';
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(parsed);
}

async function resolveCollectionName() {
  if (!collectionId.value || typeof route.query.name === 'string') {
    return;
  }
  nameLoading.value = true;
  try {
    const allCollections: CollectionView[] = await fetchCollections();
    const match = allCollections.find((item) => item.collectionId === collectionId.value);
    collectionName.value = match?.name ?? '';
  } finally {
    nameLoading.value = false;
  }
}
</script>

<style scoped>

.collection-questions-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero-card,
.filter-card,
.table-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 18px;
  background: #fff;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  padding: 20px 24px;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.hero-copy h2 {
  margin: 0;
  font-size: 26px;
  line-height: 1.2;
}

.hero-copy p {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.filter-card {
  padding: 20px 22px 14px;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.card-title-row h3 {
  margin: 0;
  font-size: 16px;
}

.card-title-row p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
}

.filter-form {
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 1080px;
  margin-inline: auto;
}

.filter-grid {
  display: grid;
  gap: 14px 18px;
}

.filter-grid--primary {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.filter-grid--secondary {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.filter-section {
  padding: 16px 0 0;
  border-top: 1px solid var(--el-border-color-lighter);
}

.filter-section:first-child {
  padding-top: 0;
  border-top: 0;
}

.filter-section-head {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  margin-bottom: 12px;
}

.filter-section-label {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.filter-section-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.level-error {
  flex: 1 1 320px;
  margin-bottom: 0;
}

.filter-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  flex-shrink: 0;
}

.filter-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding-top: 4px;
  flex-wrap: wrap;
}

.filter-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-card :deep(.el-select),
.filter-card :deep(.el-input-number) {
  width: 100%;
}

.table-card {
  padding: 6px 0 0;
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

.table-card :deep(.el-table) {
  --el-table-border-color: transparent;
}

.table-card :deep(.el-table th.el-table__cell) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
}

.table-card :deep(.el-table .el-table__cell) {
  padding: 10px 12px;
}

.table-card :deep(.el-table .cell) {
  padding: 0 4px;
  line-height: 1.4;
}

.table-card :deep(.el-table th .cell) {
  white-space: nowrap;
}

.title-text {
  display: block;
  width: 100%;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.version-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid #e6e9ef;
  background: #f8fafc;
  color: #667085;
  font-size: 12px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  white-space: nowrap;
}

.pagination-wrap {
  margin-top: 0;
  padding: 12px 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pagination-summary {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
}

.result-count {
  font-size: 13px;
  color: #334155;
  font-weight: 600;
  white-space: nowrap;
}

.page-summary {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.type-tag {
  border: 1px solid transparent;
  font-weight: 600;
}

.table-card :deep(.el-tag--small) {
  padding: 0 8px;
  height: 22px;
  line-height: 20px;
}

.type-tag--single-choice {
  color: #4f77c8;
  background: #f4f7ff;
  border-color: #d8e2f6;
}

.type-tag--multiple-choice {
  color: #8a67c2;
  background: #f7f4ff;
  border-color: #e1d7f3;
}

.type-tag--true-false {
  color: #5f8f7c;
  background: #f4fbf8;
  border-color: #d6ebe1;
}

.type-tag--fill-in {
  color: #b88443;
  background: #fff9f2;
  border-color: #f0dfc8;
}

.type-tag--short-answer {
  color: #c26b6b;
  background: #fff6f6;
  border-color: #f1d2d2;
}

.difficulty-compact {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 0;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.difficulty-value {
  color: #475569;
  font-weight: 600;
}

.difficulty-separator {
  color: #94a3b8;
}

.difficulty-label {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1;
  border: 1px solid transparent;
}

.difficulty-label--easy {
  color: #5f8f7c;
  background: #f4fbf8;
  border-color: #dcefe6;
}

.difficulty-label--medium {
  color: #8a6d3b;
  background: #fffaf2;
  border-color: #f0e2c7;
}

.difficulty-label--hard {
  color: #9c6666;
  background: #fff6f6;
  border-color: #f1d8d8;
}

.table-action {
  padding: 0;
  min-height: 0;
}

@media (max-width: 768px) {
  .hero-card,
  .card-title-row {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-meta {
    width: 100%;
  }

  .filter-card {
    padding: 16px;
  }

  .filter-form {
    gap: 16px;
    max-width: none;
  }

  .filter-grid--primary,
  .filter-grid--secondary {
    grid-template-columns: 1fr;
  }

  .filter-section-head,
  .filter-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-actions {
    justify-content: flex-start;
  }

  .table-card :deep(.el-table .el-table__cell) {
    padding: 12px;
  }

  .table-card :deep(.el-table__body .el-table__row) {
    height: auto;
  }

  .pagination-wrap {
    align-items: flex-start;
  }

  .pagination-summary {
    width: 100%;
  }

  .pagination-summary .page-summary {
    display: none;
  }
}
</style>
