<template>
  <section class="papers-page">
    <el-card shadow="never" class="hero-card">
      <div class="hero-grid">
        <div>
          <p class="eyebrow">组卷管理</p>
          <h2>试卷列表</h2>
          <p class="hero-desc">按标题检索试卷，快速进入详情继续组题。</p>
        </div>
        <div class="hero-actions">
          <el-button :loading="listLoading" @click="loadPapers">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">创建试卷</el-button>
        </div>
      </div>
    </el-card>

    <div class="metric-grid" aria-label="试卷概览">
      <div class="metric-item">
        <span>全部试卷</span>
        <strong>{{ paperPage?.total ?? 0 }}</strong>
      </div>
      <div class="metric-item">
        <span>平均题数</span>
        <strong>{{ averageItems }}</strong>
      </div>
      <div class="metric-item">
        <span>最近更新</span>
        <strong>{{ latestUpdatedText }}</strong>
      </div>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :inline="false" class="filter-form">
        <div class="filter-grid">
          <el-form-item label="关键字">
            <el-input
              v-model="queryForm.keyword"
              clearable
              placeholder="输入标题关键字"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="每页条数">
            <el-select v-model="pagination.pageSize" @change="handlePageSizeChange">
              <el-option :value="10" label="10 条 / 页" />
              <el-option :value="20" label="20 条 / 页" />
              <el-option :value="50" label="50 条 / 页" />
            </el-select>
          </el-form-item>
        </div>
        <div class="filter-actions">
          <el-button type="primary" :loading="listLoading" @click="handleSearch">搜索</el-button>
          <el-button :disabled="listLoading" @click="resetFilters">重置</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card shadow="never" class="list-card">
      <el-table v-loading="listLoading" :data="paperPage?.records ?? []" row-key="id">
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="totalItems" label="题数" width="90" />
        <el-table-column label="总分" width="110">
          <template #default="{ row }">
            {{ formatScore(row.totalScore) }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button type="primary" link @click="goDetail(row.id)">进入详情</el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!listLoading && !(paperPage?.records?.length ?? 0)" description="暂无试卷，点击右上角创建。" />

      <div class="pagination-wrap" v-if="paperPage">
        <span class="page-summary">
          共 {{ paperPage.total }} 条 · 第 {{ paperPage.pageNum }} / {{ paperPage.pages || 1 }} 页
        </span>
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="paperPage.total"
          :hide-on-single-page="false"
          :disabled="listLoading"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="createDialogVisible" title="创建试卷" width="520px">
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="createForm.title" placeholder="如：高数第一章练习卷" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="4" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { createPaper, deletePaper, fetchPaperPage } from '../api/paper';
import type { PaperListItemVO, PaperPageResult, PaperSavePayload } from '../types/paper';
import { showSuccess } from '../utils/messages';

const router = useRouter();

const listLoading = ref(false);
const creating = ref(false);
const paperPage = ref<PaperPageResult | null>(null);
const createDialogVisible = ref(false);

const queryForm = reactive<{
  keyword: string;
}>({
  keyword: ''
});

const pagination = reactive({
  pageNum: 1,
  pageSize: 10
});

const createForm = reactive<PaperSavePayload>({
  title: '',
  description: ''
});

onMounted(() => {
  void loadPapers();
});

const activeQuery = computed(() => ({
  pageNum: pagination.pageNum,
  pageSize: pagination.pageSize,
  keyword: queryForm.keyword.trim() || undefined
}));

const averageItems = computed(() => {
  const records = paperPage.value?.records ?? [];
  if (records.length === 0) {
    return 0;
  }
  const total = records.reduce((sum, item) => sum + Number(item.totalItems ?? 0), 0);
  return Math.round(total / records.length);
});

const latestUpdatedText = computed(() => {
  const first = paperPage.value?.records?.[0];
  if (!first?.updatedAt) {
    return '暂无';
  }
  return formatShortDate(first.updatedAt);
});

async function loadPapers() {
  listLoading.value = true;
  try {
    paperPage.value = await fetchPaperPage(activeQuery.value);
  } finally {
    listLoading.value = false;
  }
}

function handleSearch() {
  pagination.pageNum = 1;
  void loadPapers();
}

function handlePageChange(pageNum: number) {
  pagination.pageNum = pageNum;
  void loadPapers();
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize;
  pagination.pageNum = 1;
  void loadPapers();
}

function resetFilters() {
  queryForm.keyword = '';
  pagination.pageNum = 1;
  pagination.pageSize = 10;
  void loadPapers();
}

function openCreateDialog() {
  createForm.title = '';
  createForm.description = '';
  createDialogVisible.value = true;
}

async function handleCreate() {
  if (!createForm.title.trim()) {
    return;
  }
  creating.value = true;
  try {
    const created = await createPaper({
      title: createForm.title.trim(),
      description: createForm.description?.trim() || ''
    });
    showSuccess('试卷创建成功');
    createDialogVisible.value = false;
    await router.push({ name: 'paper-detail', params: { paperId: created.paperId } });
  } finally {
    creating.value = false;
  }
}

function goDetail(paperId: string) {
  void router.push({ name: 'paper-detail', params: { paperId } });
}

async function handleDelete(row: PaperListItemVO) {
  try {
    await ElMessageBox.confirm(`确定删除试卷「${row.title}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    });
  } catch {
    return;
  }
  await deletePaper(row.id);
  showSuccess('试卷已删除');
  await loadPapers();
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '—';
  }
  return value.replace('T', ' ').slice(0, 19);
}

function formatShortDate(value?: string | null) {
  if (!value) {
    return '暂无';
  }
  const normalized = value.replace('T', ' ');
  return normalized.slice(5, 16);
}

function formatScore(value: number) {
  return Number(value ?? 0).toFixed(2);
}
</script>

<style scoped>
.papers-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  color: #172033;
}

.hero-card {
  border-color: #dce9f9;
  border-radius: 8px;
  background:
    linear-gradient(90deg, rgba(47, 128, 237, 0.1), rgba(86, 204, 242, 0.04) 48%, #fff 100%),
    #fff;
  box-shadow: 0 10px 28px rgba(31, 63, 114, 0.05);
}

.hero-card :deep(.el-card__body) {
  padding: 22px 24px;
}

.hero-grid {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 6px;
  color: #2f80ed;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.hero-grid h2 {
  margin: 0;
  color: #111827;
  font-size: 24px;
  line-height: 1.25;
}

.hero-desc {
  margin: 8px 0 0;
  color: #6b778c;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric-item {
  padding: 14px 16px;
  border: 1px solid #e4edf8;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 8px 22px rgba(31, 63, 114, 0.04);
}

.metric-item span {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.metric-item strong {
  display: block;
  margin-top: 5px;
  color: #172033;
  font-size: 24px;
  line-height: 1.1;
}

.filter-card {
  position: sticky;
  top: 0;
  z-index: 1;
  border-color: #e3eaf3;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(8px);
  box-shadow: 0 8px 22px rgba(31, 63, 114, 0.04);
}

.filter-card :deep(.el-card__body) {
  padding: 16px 18px;
}

.filter-form {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(200px, 1fr));
  gap: 12px 16px;
  flex: 1;
}

.filter-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.row-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.list-card {
  overflow: hidden;
  border-radius: 8px;
  border-color: #e3eaf3;
  box-shadow: 0 10px 28px rgba(31, 63, 114, 0.05);
}

.list-card :deep(.el-card__body) {
  padding: 0;
}

.list-card :deep(.el-table) {
  --el-table-border-color: transparent;
}

.list-card :deep(.el-table th.el-table__cell) {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
}

.list-card :deep(.el-table .el-table__cell) {
  padding: 13px 18px;
}

.pagination-wrap {
  padding: 14px 18px 18px;
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

@media (max-width: 960px) {
  .hero-grid,
  .filter-form {
    flex-direction: column;
  }

  .filter-grid {
    grid-template-columns: 1fr;
    width: 100%;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
