<template>
  <el-card>
    <template #header>
      <div class="section-header">
        <div>
          <h2 v-if="collectionName">题集「{{ collectionName }}」</h2>
          <h2 v-else>题集</h2>
          <p>筛选条件将直接翻译为查询参数。</p>
        </div>
        <el-button type="primary" @click="goToCreate">添加题目</el-button>
      </div>
    </template>

    <el-form label-position="top" class="filter-form">
      <div class="filter-grid">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="标题关键字" clearable />
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="filters.typeCode" placeholder="全部" clearable>
            <el-option v-for="type in questionTypes" :key="type.code" :label="type.label" :value="type.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度下限">
          <el-input-number v-model="filters.levelMin" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="难度上限">
          <el-input-number v-model="filters.levelMax" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="排序字段">
          <el-select v-model="filters.sortField">
            <el-option label="更新时间" value="updatedAt" />
            <el-option label="创建时间" value="createdAt" />
            <el-option label="难度" value="difficulty" />
            <el-option label="正确率" value="correctRate" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序方向">
          <el-select v-model="filters.sortDirection">
            <el-option label="降序" value="DESC" />
            <el-option label="升序" value="ASC" />
          </el-select>
        </el-form-item>
      </div>
      <el-alert v-if="levelError" :title="levelError" type="error" :closable="false" class="level-error" />
      <div class="filter-actions">
        <el-button type="primary" :loading="loading" :disabled="Boolean(levelError)" @click="loadQuestions">应用筛选</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </el-form>

    <el-table v-loading="loading" :data="page?.records ?? []" border>
      <el-table-column prop="title" label="题目" min-width="260" />
      <el-table-column prop="typeCode" label="题型" min-width="110">
        <template #default="{ row }">
          <el-tag>{{ getTypeLabel(row.typeCode) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="versionNo" label="版本" width="80" />
      <el-table-column prop="difficulty" label="难度" width="110">
        <template #default="{ row }">{{ formatDifficulty(row.difficulty) }}</template>
      </el-table-column>
      <el-table-column prop="correctRate" label="正确率" width="110">
        <template #default="{ row }">{{ formatPercent(row.correctRate) }}</template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" min-width="180">
        <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDetail(row.id)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && page && !page.records.length" description="暂无题目" />

    <div class="pagination-wrap" v-if="page">
      <span class="page-summary">{{ pageSummary }}</span>
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
  return `共 ${total} 条 · 第 ${Math.min(pageNum, pages)} / ${pages} 页`;
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

function formatPercent(value?: number | null) {
  if (value === undefined || value === null) {
    return '—';
  }
  return `${(value * 100).toFixed(1)}%`;
}

function formatDifficulty(value?: number | null) {
  if (value === undefined || value === null) {
    return '—';
  }
  return value.toFixed(2);
}

function formatDate(date: string) {
  return new Date(date).toLocaleString();
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
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.section-header h2 {
  margin: 0;
}

.section-header p {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
}

.filter-form {
  margin-bottom: 16px;
}

.filter-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.level-error {
  margin-bottom: 12px;
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.page-summary {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
  }

  .pagination-wrap {
    align-items: flex-start;
  }
}
</style>
