<template>
  <el-card>
    <template #header>
      <div class="section-header">
        <div>
          <h2>批量导入</h2>
          <p>管理导入会话并进入详情处理导入项。</p>
        </div>
        <div class="action-row">
          <el-button :loading="listLoading" @click="loadSessions">刷新列表</el-button>
          <el-button type="primary" @click="openCreateDialog">新建导入</el-button>
        </div>
      </div>
    </template>

    <el-table v-loading="listLoading" :data="pagedSessions" border row-key="importId">
      <el-table-column prop="importId" label="导入会话" min-width="180">
        <template #default="{ row }">
          #{{ row.importId }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="sessionStatusTagType(row.status)" effect="plain">
            {{ sessionStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="total" label="总数" width="90" />
      <el-table-column prop="validCnt" label="有效" width="90">
        <template #default="{ row }">
          <el-tag type="success" effect="plain">{{ row.validCnt }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="invalidCnt" label="无效" width="90">
        <template #default="{ row }">
          <el-tag type="danger" effect="plain">{{ row.invalidCnt }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" min-width="220">
        <template #default="{ row }">
          <el-progress :percentage="toProgressPercent(row.progress)" :stroke-width="10" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="180">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" min-width="180">
        <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="goDetail(row.importId)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!listLoading && !allSessions.length" description="暂无可用导入会话" />
    <div class="pagination-wrap" v-if="!listLoading && allSessions.length">
      <span class="page-summary">本地分页：共 {{ allSessions.length }} 条 · 第 {{ pageState.pageNum }} / {{ totalPages }} 页</span>
      <el-pagination
        v-model:current-page="pageState.pageNum"
        v-model:page-size="pageState.pageSize"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        :total="allSessions.length"
        :hide-on-single-page="false"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>
  </el-card>

  <el-dialog v-model="createDialogVisible" title="新建导入会话" width="560px">
    <el-form label-position="top">
      <el-form-item label="导入格式">
        <el-input :model-value="createForm.format" disabled />
      </el-form-item>
      <el-form-item label="导入文件（Excel）">
        <el-upload
          v-model:file-list="uploadFileList"
          drag
          :auto-upload="true"
          :limit="1"
          accept=".xlsx,.xls"
          :before-upload="beforeUpload"
          :http-request="handleUploadExcel"
          :on-remove="handleUploadRemove"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="upload-tip">仅支持 .xls / .xlsx，单文件不超过 20MB。</div>
          </template>
        </el-upload>
        <el-text v-if="createForm.fileId" type="success" class="file-id-hint">
          已上传，fileId：{{ createForm.fileId }}
        </el-text>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="createDialogVisible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="createLoading || uploadLoading"
        :disabled="!createForm.fileId"
        @click="handleCreate"
      >
        创建并进入详情
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { UploadProps, UploadRequestOptions, UploadUserFile } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';
import { createImportSession, fetchImportSessions } from '../api/import';
import { uploadAssetFile } from '../api/common';
import type { ImportSessionStatus, ImportSessionVO } from '../types/import';
import { showInfo, showSuccess } from '../utils/messages';

const router = useRouter();

const listLoading = ref(false);
const createLoading = ref(false);
const uploadLoading = ref(false);
const createDialogVisible = ref(false);
const allSessions = ref<ImportSessionVO[]>([]);
const uploadFileList = ref<UploadUserFile[]>([]);

const createForm = reactive({
  fileId: '',
  format: 'excel'
});
const pageState = reactive({
  pageNum: 1,
  pageSize: 10
});

const totalPages = computed(() => Math.max(Math.ceil(allSessions.value.length / Math.max(pageState.pageSize, 1)), 1));
const pagedSessions = computed(() => {
  const start = (pageState.pageNum - 1) * pageState.pageSize;
  return allSessions.value.slice(start, start + pageState.pageSize);
});

onMounted(() => {
  void loadSessions();
});

async function loadSessions() {
  listLoading.value = true;
  try {
    // 后端 /api/v1/imports 当前返回全量 List，这里使用前端本地分页切片。
    const list = await fetchImportSessions();
    allSessions.value = [...list].sort((a, b) => compareIdDesc(a.importId, b.importId));
    normalizePageState();
  } finally {
    listLoading.value = false;
  }
}

function compareIdDesc(a: string, b: string): number {
  try {
    const aNum = BigInt(a);
    const bNum = BigInt(b);
    if (aNum === bNum) {
      return 0;
    }
    return aNum > bNum ? -1 : 1;
  } catch {
    return b.localeCompare(a);
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
  uploadFileList.value = [];
  createForm.fileId = '';
  createForm.format = 'excel';
  createDialogVisible.value = true;
}

const beforeUpload: UploadProps['beforeUpload'] = (rawFile) => {
  const name = rawFile.name.toLowerCase();
  if (!name.endsWith('.xlsx') && !name.endsWith('.xls')) {
    showInfo('请上传 Excel 文件（.xls 或 .xlsx）');
    return false;
  }
  if (rawFile.size > 20 * 1024 * 1024) {
    showInfo('文件大小不能超过 20MB');
    return false;
  }
  return true;
};

async function handleUploadExcel(options: UploadRequestOptions) {
  uploadLoading.value = true;
  try {
    const file = options.file as File;
    const result = await uploadAssetFile(file);
    createForm.fileId = String(result.fileId);
    options.onSuccess?.(result);
    showSuccess('导入文件上传成功');
  } catch (error) {
    options.onError?.(error as any);
    throw error;
  } finally {
    uploadLoading.value = false;
  }
}

function handleUploadRemove() {
  createForm.fileId = '';
}

async function handleCreate() {
  if (!createForm.fileId) {
    return;
  }
  createLoading.value = true;
  try {
    const created = await createImportSession({
      fileId: createForm.fileId,
      format: createForm.format
    });
    showSuccess('导入会话已创建');
    createDialogVisible.value = false;
    await router.push({
      name: 'import-detail',
      params: { importId: String(created.importId) }
    });
  } finally {
    createLoading.value = false;
  }
}

function goDetail(importId: string) {
  void router.push({ name: 'import-detail', params: { importId } });
}

function sessionStatusLabel(status: ImportSessionStatus) {
  switch (status) {
    case 0:
      return '解析中';
    case 1:
      return '准备提交';
    case 2:
      return '提交中';
    case 3:
      return '提交完毕';
    case 4:
      return '已取消';
    case 5:
      return '失败';
    default:
      return '未知';
  }
}

function sessionStatusTagType(status: ImportSessionStatus): 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 1:
      return 'warning';
    case 3:
      return 'success';
    case 4:
      return 'info';
    case 5:
      return 'danger';
    default:
      return 'info';
  }
}

function toProgressPercent(progress: number) {
  const value = Number.isFinite(progress) ? progress : 0;
  return Math.max(0, Math.min(100, Math.round(value * 100)));
}

function formatDate(value?: string | null) {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
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

.action-row {
  display: flex;
  gap: 8px;
}

.upload-tip {
  color: var(--el-text-color-secondary);
}

.file-id-hint {
  margin-top: 8px;
  display: block;
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
