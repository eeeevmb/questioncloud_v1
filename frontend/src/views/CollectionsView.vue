<template>
  <el-card>
    <template #header>
      <div class="section-header">
        <div>
          <h2>我的题集</h2>
          <p>以下列表来自 /api/v1/collection，展示当前登录用户的全部题集。</p>
        </div>
        <div class="action-row">
          <el-button :loading="listLoading" @click="loadCollections">刷新列表</el-button>
          <el-button type="primary" @click="openCreateDialog">创建题集</el-button>
        </div>
      </div>
    </template>

    <el-table v-loading="listLoading" :data="pagedCollections" border>
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="description" label="描述" min-width="220">
        <template #default="{ row }">
          {{ row.description || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="primary" @click="enterCollection(row.collectionId)">题目列表</el-button>
            <el-button size="small" @click="startEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!listLoading && !allCollections.length" description="暂无题集，请先创建。" />
    <div class="pagination-wrap" v-if="!listLoading && allCollections.length">
      <span class="page-summary">本地分页：共 {{ allCollections.length }} 条 · 第 {{ pageState.pageNum }} / {{ totalPages }} 页</span>
      <el-pagination
        v-model:current-page="pageState.pageNum"
        v-model:page-size="pageState.pageSize"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        :total="allCollections.length"
        :hide-on-single-page="false"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>
  </el-card>

  <el-dialog v-model="createDialogVisible" title="创建题集" width="520px">
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

  <el-dialog v-model="editDialogVisible" title="编辑题集" width="520px">
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
    // 后端 /api/v1/collection 当前返回全量 List，这里使用前端本地分页切片。
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
.section-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.section-header h2 {
  margin: 0;
}

.section-header p {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
}

.action-row {
  display: flex;
  gap: 8px;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pagination-wrap {
  margin-top: 14px;
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
}
</style>
