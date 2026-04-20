<template>
  <section class="collections-page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">题库空间</p>
        <h2>我的题集</h2>
        <p class="heading-desc">按课程、章节或专题组织题目，进入题集后可维护题目内容。</p>
      </div>
      <div class="heading-actions">
        <div class="collection-count">
          <span class="count-value">{{ allCollections.length }}</span>
          <span class="count-label">个题集</span>
        </div>
        <el-button :loading="listLoading" @click="loadCollections">刷新</el-button>
        <el-button type="primary" @click="openCreateDialog">创建题集</el-button>
      </div>
    </div>

    <el-card class="list-card" shadow="never">
      <el-table v-loading="listLoading" :data="pagedCollections" row-key="collectionId">
        <el-table-column prop="name" label="题集名称" min-width="220" />
        <el-table-column prop="description" label="说明" min-width="280">
          <template #default="{ row }">
            {{ row.description || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" align="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button type="primary" link @click="enterCollection(row.collectionId)">进入</el-button>
              <el-button link @click="startEdit(row)">编辑</el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!listLoading && !allCollections.length" description="暂无题集，请先创建。" />
      <div class="pagination-wrap" v-if="!listLoading && shouldShowPagination">
        <span class="page-summary">第 {{ pageState.pageNum }} / {{ totalPages }} 页</span>
        <el-pagination
          v-model:current-page="pageState.pageNum"
          v-model:page-size="pageState.pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="allCollections.length"
          :hide-on-single-page="true"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>
  </section>

  <el-dialog v-model="createDialogVisible" title="创建题集" width="min(520px, calc(100vw - 32px))" class="collection-dialog">
    <el-form label-position="top">
      <el-form-item label="题集名称">
        <el-input v-model="createForm.name" placeholder="如：高数训练" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="createForm.description" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="editDialogVisible" title="编辑题集" width="min(520px, calc(100vw - 32px))" class="collection-dialog">
    <el-form label-position="top">
      <el-form-item label="新名称">
        <el-input v-model="editForm.name" placeholder="输入新的题集名称" />
      </el-form-item>
      <el-form-item label="新描述">
        <el-input v-model="editForm.description" placeholder="输入新的描述" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="cancelEdit">取消</el-button>
      <el-button type="primary" :loading="updating" @click="handleUpdate">保存修改</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { createCollection, deleteCollection, fetchCollections, updateCollection } from '../api/collection';
import type { CollectionView } from '../types/collection';
import { showSuccess } from '../utils/messages';

const router = useRouter();

const allCollections = ref<CollectionView[]>([]);
const listLoading = ref(false);
const creating = ref(false);
const updating = ref(false);
const editingId = ref<string>('');
const createDialogVisible = ref(false);
const editDialogVisible = ref(false);
const createForm = reactive({ name: '', description: '' });
const editForm = reactive({ name: '', description: '' });
const pageState = reactive({
  pageNum: 1,
  pageSize: 10
});

const totalPages = computed(() => Math.max(Math.ceil(allCollections.value.length / Math.max(pageState.pageSize, 1)), 1));
const shouldShowPagination = computed(() => allCollections.value.length > pageState.pageSize);
const pagedCollections = computed(() => {
  const start = (pageState.pageNum - 1) * pageState.pageSize;
  return allCollections.value.slice(start, start + pageState.pageSize);
});

onMounted(() => {
  loadCollections();
});

async function loadCollections() {
  listLoading.value = true;
  try {
    // 当前接口返回全量 List，前端仅负责列表切片和页码状态。
    allCollections.value = await fetchCollections();
    normalizePageState();
  } finally {
    listLoading.value = false;
  }
}

function normalizePageState() {
  if (pageState.pageSize < 1) {
    pageState.pageSize = 10;
  }
  if (pageState.pageNum < 1) {
    pageState.pageNum = 1;
  }
  if (pageState.pageNum > totalPages.value) {
    pageState.pageNum = totalPages.value;
  }
}

function onPageChange(pageNum: number) {
  pageState.pageNum = pageNum;
  normalizePageState();
}

function onSizeChange(pageSize: number) {
  pageState.pageSize = pageSize;
  pageState.pageNum = 1;
  normalizePageState();
}

function openCreateDialog() {
  createDialogVisible.value = true;
}

async function handleCreate() {
  if (!createForm.name.trim()) {
    return;
  }
  creating.value = true;
  try {
    await createCollection({ ...createForm });
    showSuccess('题集创建成功');
    createForm.name = '';
    createForm.description = '';
    createDialogVisible.value = false;
    await loadCollections();
  } finally {
    creating.value = false;
  }
}

function startEdit(collection: CollectionView) {
  editingId.value = collection.collectionId;
  editForm.name = collection.name;
  editForm.description = collection.description || '';
  editDialogVisible.value = true;
}

function cancelEdit() {
  editingId.value = '';
  editForm.name = '';
  editForm.description = '';
  editDialogVisible.value = false;
}

async function handleUpdate() {
  if (!editingId.value) {
    return;
  }
  updating.value = true;
  try {
    await updateCollection(editingId.value, {
      name: editForm.name,
      description: editForm.description
    });
    showSuccess('题集更新成功');
    await loadCollections();
    cancelEdit();
  } finally {
    updating.value = false;
  }
}

async function handleDelete(collection: CollectionView) {
  try {
    await ElMessageBox.confirm(`确定删除题集「${collection.name}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    });
  } catch {
    return;
  }
  await deleteCollection(collection.collectionId);
  showSuccess('题集删除成功');
  if (collection.collectionId === editingId.value) {
    cancelEdit();
  }
  await loadCollections();
}

function enterCollection(collectionId: string) {
  const collection = allCollections.value.find((item) => item.collectionId === collectionId);
  router.push({
    name: 'collection-questions',
    params: { collectionId },
    query: { name: collection?.name }
  });
}
</script>

<style scoped>
.collections-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-heading {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
  padding: 20px 24px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 18px;
  background: #fff;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.page-heading h2 {
  margin: 0;
  font-size: 24px;
  line-height: 1.25;
  color: #111827;
}

.heading-desc {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.heading-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.collection-count {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  min-width: 86px;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
}

.count-value {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.count-label {
  font-size: 12px;
}

.list-card {
  border-radius: 18px;
  border-color: var(--el-border-color-lighter);
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
  font-weight: 600;
}

.list-card :deep(.el-table .el-table__cell) {
  padding: 13px 18px;
}

.table-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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

@media (max-width: 768px) {
  .page-heading {
    flex-direction: column;
    align-items: stretch;
    padding: 18px;
  }

  .heading-actions {
    flex-wrap: wrap;
  }

  .collection-count {
    width: 100%;
    justify-content: center;
  }

  .list-card :deep(.el-table .el-table__cell) {
    padding: 12px;
  }
}
</style>
