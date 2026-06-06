<template>
  <section class="imports-page">
    <el-card class="hero-card" shadow="never">
      <div class="hero-grid">
        <div>
          <p class="eyebrow">批量导入</p>
          <h2>批量导入</h2>
          <p class="hero-desc">上传 Excel 建立导入会话，进入详情校验并提交题目。</p>
        </div>
        <div class="hero-actions">
          <el-button :loading="listLoading" @click="loadSessions">刷新列表</el-button>
          <el-button type="primary" @click="openCreateDialog">新建导入</el-button>
        </div>
      </div>
    </el-card>

    <div class="metric-grid" aria-label="导入概览">
      <div class="metric-item">
        <span>导入会话</span>
        <strong>{{ allSessions.length }}</strong>
      </div>
      <div class="metric-item">
        <span>待提交</span>
        <strong>{{ readySessionCount }}</strong>
      </div>
      <div class="metric-item">
        <span>无效题目</span>
        <strong>{{ invalidQuestionCount }}</strong>
      </div>
    </div>

    <el-card class="list-card" shadow="never">
      <el-table v-loading="listLoading" :data="pagedSessions" row-key="importId">
        <el-table-column prop="importId" label="导入会话" min-width="160">
          <template #default="{ row }">
            #{{ row.importId }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="sessionStatusTagType(row.status)" effect="plain">
              {{ sessionStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="total" label="总数" width="100" />
        <el-table-column prop="validCnt" label="有效" width="100">
          <template #default="{ row }">
            <el-tag type="success" effect="plain">{{ row.validCnt }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="invalidCnt" label="无效" width="100">
          <template #default="{ row }">
            <el-tag type="danger" effect="plain">{{ row.invalidCnt }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" min-width="160">
          <template #default="{ row }">
            <el-progress :percentage="toProgressPercent(row.progress)" :stroke-width="10" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="150">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="150">
          <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140">
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
  </section>

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
        <el-text v-if="createForm.fileId" type="success" class="upload-success-hint">
          文件已上传，可创建导入会话
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
const readySessionCount = computed(() => allSessions.value.filter((item) => item.status === 1).length);
const invalidQuestionCount = computed(() => {
  return allSessions.value.reduce((sum, item) => sum + Number(item.invalidCnt ?? 0), 0);
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
.imports-page {
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
  gap: 24px;
  align-items: center;
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
  gap: 8px;
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

.upload-tip {
  color: var(--el-text-color-secondary);
}

.upload-success-hint {
  margin-top: 8px;
  display: block;
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
  .hero-grid {
    flex-direction: column;
    align-items: stretch;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
