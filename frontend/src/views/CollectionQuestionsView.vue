<template>
  <section class="card">
    <header class="section-header">
      <div>
        <h2 v-if="collectionName">题集「{{ collectionName }}」</h2>
        <h2 v-else>题集</h2>
        <p>筛选条件将直接翻译为查询参数。</p>
      </div>
      <button class="primary-btn" type="button" @click="goToCreate">
        添加题目
      </button>
    </header>
    <form class="form-grid filter-form" @submit.prevent="loadQuestions">
      <label>
        关键词
        <input v-model="filters.keyword" placeholder="标题关键字" />
      </label>
      <label>
        题型
        <select v-model="filters.typeCode">
          <option value="">全部</option>
          <option v-for="type in questionTypes" :key="type.code" :value="type.code">
            {{ type.label }}
          </option>
        </select>
      </label>
      <label>
        难度下限
        <input
          v-model.number="filters.levelMin"
          type="number"
          step="0.01"
          min="0"
          max="1"
          inputmode="decimal"
          placeholder="0.00"
        />
      </label>
      <label>
        难度上限
        <input
          v-model.number="filters.levelMax"
          type="number"
          step="0.01"
          min="0"
          max="1"
          inputmode="decimal"
          placeholder="1.00"
        />
      </label>
      <label>
        排序字段
        <select v-model="filters.sortField">
          <option value="updatedAt">更新时间</option>
          <option value="createdAt">创建时间</option>
          <option value="difficulty">难度</option>
          <option value="correctRate">正确率</option>
        </select>
      </label>
      <label>
        排序方向
        <select v-model="filters.sortDirection">
          <option value="DESC">降序</option>
          <option value="ASC">升序</option>
        </select>
      </label>
      <div class="filter-actions">
        <span class="error" v-if="levelError">{{ levelError }}</span>
        <button class="primary-btn" type="submit" :disabled="loading || Boolean(levelError)">
          {{ loading ? '筛选中...' : '应用筛选' }}
        </button>
      </div>
    </form>

    <div v-if="loading">加载中...</div>
    <div v-else>
      <table class="table" v-if="page">
        <thead>
          <tr>
            <th>题目</th>
            <th>题型</th>
            <th>版本</th>
            <th>难度</th>
            <th>正确率</th>
            <th>更新时间</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
        <tr v-for="row in page.records" :key="row.id">
            <td>
              <div class="title-cell">
                <div>{{ row.title }}</div>
              </div>
            </td>
            <td>
              <span :title="row.typeCode">{{ getTypeLabel(row.typeCode) }}</span>
            </td>
            <td>{{ row.versionNo }}</td>
            <td>
              <span :title="row.difficulty == null ? '暂无难度数据' : ''">
                {{ formatDifficulty(row.difficulty) }}
              </span>
            </td>
            <td>
              <span :title="row.correctRate == null ? '暂无作答数据' : ''">
                {{ formatPercent(row.correctRate) }}
              </span>
            </td>
            <td>{{ formatDate(row.updatedAt) }}</td>
            <td>
              <button class="primary-btn" type="button" @click="openDetail(row.id)">
                查看
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="pagination" v-if="page">
        <button class="secondary-btn" type="button" :disabled="filters.pageNum === 1" @click="changePage(filters.pageNum - 1)">上一页</button>
        <span>第 {{ filters.pageNum }} / {{ page.pages || 1 }} 页，共 {{ page.total }} 条</span>
        <button class="secondary-btn" type="button" :disabled="!page.hasNext" @click="changePage(filters.pageNum + 1)">下一页</button>
      </div>
    </div>
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
    page.value = await fetchCollectionQuestions(collectionId.value, {
      pageNum: filters.pageNum,
      pageSize: filters.pageSize,
      keyword: filters.keyword || undefined,
      sortField: filters.sortField,
      sortDirection: filters.sortDirection,
      typeCode: filters.typeCode || undefined,
      levelMin: filters.levelMin ?? undefined,
      levelMax: filters.levelMax ?? undefined
    });
  } catch (error) {
    // 统一拦截
  } finally {
    loading.value = false;
  }
}

function changePage(pageNum: number) {
  filters.pageNum = pageNum;
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
    const match = allCollections.find((item) => item.id === collectionId.value);
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
  align-items: center;
}

.filter-form {
  border-top: 1px solid #e2e8f0;
  margin-top: 16px;
  padding-top: 16px;
}

.filter-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.filter-actions .error {
  color: #dc2626;
  font-size: 13px;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.secondary-btn:disabled,
.primary-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}


.title-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

</style>
