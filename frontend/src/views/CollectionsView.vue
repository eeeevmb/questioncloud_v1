<template>
  <section class="card">
    <header class="section-header">
      <div>
        <h2>我的题集</h2>
        <p>以下列表来自 /api/v1/collection，展示当前登录用户的全部题集。</p>
      </div>
      <div class="action-row">
        <button class="secondary-btn" @click="loadCollections" :disabled="listLoading">
          {{ listLoading ? '刷新中...' : '刷新列表' }}
        </button>
      </div>
    </header>
    <div v-if="listLoading">加载题集...</div>
    <div v-else-if="!collections.length" class="empty">暂无题集，请先创建。</div>
    <table class="table" v-else>
      <thead>
        <tr>
          <th>名称</th>
          <th>描述</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in collections" :key="item.collectionId">
          <td>{{ item.name }}</td>
          <td>{{ item.description || '-' }}</td>
          <td class="table-actions">
            <button class="primary-btn" type="button" @click="enterCollection(item.collectionId)">题目列表</button>
            <button class="secondary-btn" type="button" @click="startEdit(item)">编辑</button>
            <button class="danger-btn" type="button" @click="handleDelete(item)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>

  <section class="card">
    <h3>创建题集</h3>
    <form class="form-grid" @submit.prevent="handleCreate">
      <label>
        题集名称
        <input v-model="createForm.name" placeholder="如：高数训练" required />
      </label>
      <label>
        描述
        <input v-model="createForm.description" placeholder="选填" />
      </label>
      <button class="primary-btn" type="submit" :disabled="creating">
        {{ creating ? '创建中...' : '创建题集' }}
      </button>
    </form>
  </section>

  <section class="card" v-if="editingId">
    <header class="section-header">
      <div>
        <h3>编辑题集</h3>
        <p class="mono small">正在编辑 ID：{{ editingId }}</p>
      </div>
      <button class="secondary-btn" type="button" @click="cancelEdit">取消编辑</button>
    </header>
    <form class="form-grid" @submit.prevent="handleUpdate">
      <label>
        新名称
        <input v-model="editForm.name" placeholder="输入新的题集名称" required />
      </label>
      <label>
        新描述
        <input v-model="editForm.description" placeholder="输入新的描述" />
      </label>
      <button class="primary-btn" type="submit" :disabled="updating">
        {{ updating ? '保存中...' : '保存修改' }}
      </button>
    </form>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter, RouterLink } from 'vue-router';
import { createCollection, deleteCollection, fetchCollections, updateCollection } from '../api/collection';
import type { CollectionView } from '../types/collection';
import { showSuccess } from '../utils/messages';

const router = useRouter();

const collections = ref<CollectionView[]>([]);
const listLoading = ref(false);
const creating = ref(false);
const updating = ref(false);
const editingId = ref<string>('');
const createForm = reactive({ name: '', description: '' });
const editForm = reactive({ name: '', description: '' });

onMounted(() => {
  loadCollections();
});

async function loadCollections() {
  listLoading.value = true;
  try {
    collections.value = await fetchCollections();
  } finally {
    listLoading.value = false;
  }
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
    await loadCollections();
  } finally {
    creating.value = false;
  }
}

function startEdit(collection: CollectionView) {
  editingId.value = collection.collectionId;
  editForm.name = collection.name;
  editForm.description = collection.description || '';
}

function cancelEdit() {
  editingId.value = '';
  editForm.name = '';
  editForm.description = '';
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
  if (!confirm(`确定删除题集「${collection.name}」吗？`)) {
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
  const collection = collections.value.find((item) => item.collectionId === collectionId);
  router.push({
    name: 'collection-questions',
    params: { collectionId },
    query: { name: collection?.name }
  });
}
</script>

<style scoped>
.action-row {
  display: flex;
  gap: 12px;
}

.table-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.empty {
  padding: 24px 0;
  color: #475569;
}

</style>
