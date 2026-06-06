<template>
  <section class="import-detail-page">
    <div class="page-toolbar">
      <el-button @click="goBack">返回导入列表</el-button>
      <div class="toolbar-actions">
        <el-button :loading="sessionLoading || itemsLoading" @click="refreshAll">刷新</el-button>
        <el-button
          type="primary"
          :loading="batchSaving"
          :disabled="!pendingCount"
          @click="batchSaveEdits"
        >
          批量保存修改<span v-if="pendingCount">（{{ pendingCount }}）</span>
        </el-button>
        <el-button
          type="success"
          :loading="actionLoading"
          :disabled="commitDisabled"
          @click="handleCommit"
        >
          提交导入
        </el-button>
        <el-button
          type="danger"
          plain
          :loading="actionLoading"
          :disabled="cancelDisabled"
          @click="handleCancel"
        >
          取消导入
        </el-button>
      </div>
    </div>

    <el-card v-loading="sessionLoading" shadow="never" class="session-card">
      <template #header>
        <div class="card-title">导入会话信息</div>
      </template>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="导入会话">#{{ importId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="sessionStatusTagType(session?.status)" effect="plain">
            {{ sessionStatusLabel(session?.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总数">{{ session?.total ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="进度">
          <el-progress :percentage="toProgressPercent(session?.progress)" :stroke-width="10" />
        </el-descriptions-item>
        <el-descriptions-item label="有效条数">
          <el-tag type="success" effect="plain">{{ session?.validCnt ?? 0 }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="无效条数">
          <el-tag type="danger" effect="plain">{{ session?.invalidCnt ?? 0 }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(session?.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDate(session?.updatedAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="items-header">
          <h3>导入项列表</h3>
          <el-form inline>
            <el-form-item label="状态筛选">
              <el-select v-model="itemQuery.status" style="width: 140px" @change="onFilterChange">
                <el-option label="全部" value="ALL" />
                <el-option label="有效" value="VALID" />
                <el-option label="无效" value="INVALID" />
              </el-select>
            </el-form-item>
          </el-form>
        </div>
      </template>

      <el-table
        v-loading="itemsLoading"
        :data="itemPage?.records ?? []"
        border
        row-key="itemId"
        :row-class-name="itemRowClassName"
      >
        <el-table-column prop="indexNo" label="序号" width="80" />
        <el-table-column prop="collectionName" label="题集" min-width="140" />
        <el-table-column label="题目标题" min-width="220">
          <template #default="{ row }">
            {{ effectiveDraft(row).title || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="题型" min-width="120">
          <template #default="{ row }">
            <el-tag effect="plain">{{ typeLabel(effectiveDraft(row).typeCode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="草稿状态" width="120">
          <template #default="{ row }">
            <el-tag :type="itemStatusTagType(row.status)" effect="plain">
              {{ itemStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="错误信息" min-width="240">
          <template #default="{ row }">
            <template v-if="row.errors?.length">
              <el-popover placement="top-start" width="420" trigger="hover">
                <template #reference>
                  <el-tag type="danger">{{ row.errors.length }} 项错误</el-tag>
                </template>
                <ul class="error-list">
                  <li v-for="(error, idx) in row.errors" :key="`${row.itemId}_${idx}`">
                    <span class="field">{{ error.field || '未知字段' }}</span>
                    <span class="msg">{{ error.message || error.code }}</span>
                  </li>
                </ul>
              </el-popover>
            </template>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="180">
          <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
              <el-button v-if="hasPendingEdit(row.itemId)" type="warning" link @click="discardPending(row.itemId)">
                撤销待保存
              </el-button>
              <el-tag v-if="hasPendingEdit(row.itemId)" type="warning" effect="plain">待保存</el-tag>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!itemsLoading && itemPage && !itemPage.records.length" description="暂无导入项" />

      <div class="pagination-wrap" v-if="itemPage">
        <span class="page-summary">{{ itemPageSummary }}</span>
        <el-pagination
          v-model:current-page="itemQuery.pageNum"
          v-model:page-size="itemQuery.pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="itemPage.total"
          :hide-on-single-page="false"
          :disabled="itemsLoading"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="editDialogVisible" title="编辑导入项草稿" width="900px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="当前编辑仅缓存到前端，点击“保存到待提交”后再通过上方“批量保存修改”提交到后端。"
      />

      <el-form label-position="top" class="edit-form">
        <div class="form-grid">
          <el-form-item label="题集">
            <el-input :model-value="editingCollectionName" disabled />
          </el-form-item>
          <el-form-item label="题型">
            <el-select v-model="editor.typeCode">
              <el-option label="单选题" value="single-choice" />
              <el-option label="多选题" value="multiple-choice" />
              <el-option label="判断题" value="true-false" />
              <el-option label="填空题" value="fill-in" />
              <el-option label="简答题" value="short-answer" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度 (0 ~ 1)">
            <el-input-number
              v-model="editor.difficulty"
              :min="0"
              :max="1"
              :step="0.01"
              :precision="2"
              controls-position="right"
            />
          </el-form-item>
        </div>

        <el-form-item label="标题">
          <el-input v-model="editor.title" />
        </el-form-item>

        <el-form-item label="题干">
          <el-input v-model="editor.stem" type="textarea" :rows="4" />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="参考答案">
            <el-input v-model="editor.answer" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="解析">
            <el-input v-model="editor.solution" type="textarea" :rows="3" />
          </el-form-item>
        </div>

        <el-card v-if="isChoiceEditor" shadow="never" class="choice-card">
          <template #header>
            <div class="choice-header">
              <span>选项</span>
              <el-button @click="addEditorOption">新增选项</el-button>
            </div>
          </template>

          <el-empty v-if="!editor.options.length" description="暂无选项" />
          <div v-for="(option, index) in editor.options" :key="index" class="option-row">
            <el-form-item label="编号" class="option-key">
              <el-input v-model="option.key" placeholder="如 A" />
            </el-form-item>
            <el-form-item label="内容" class="option-content">
              <el-input v-model="option.content" type="textarea" :rows="2" />
            </el-form-item>
            <el-button type="danger" plain :disabled="editor.options.length <= 1" @click="removeEditorOption(index)">
              删除
            </el-button>
          </div>

          <el-form-item v-if="isSingleChoiceEditor" label="正确选项（单选）">
            <el-radio-group v-model="singleCorrectOption">
              <el-radio
                v-for="option in normalizedOptionKeys"
                :key="option"
                :label="option"
              >
                {{ option }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-else label="正确选项（可多选）">
            <el-checkbox-group v-model="editor.correctOptions">
              <el-checkbox
                v-for="option in normalizedOptionKeys"
                :key="option"
                :label="option"
              >
                {{ option }}
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </el-card>

        <el-form-item v-if="editor.typeCode === 'true-false'" label="判断题答案">
          <el-radio-group v-model="editor.judgeAnswer">
            <el-radio label="T">正确</el-radio>
            <el-radio label="F">错误</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveToPending">保存到待提交</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import {
  cancelImport,
  commitImport,
  fetchImportItems,
  fetchImportSession,
  updateImportItems
} from '../api/import';
import type {
  ImportItemPageQuery,
  ImportItemQueryStatus,
  ImportItemStatus,
  ImportItemVO,
  ImportSessionStatus,
  ImportSessionVO,
  QuestionDraft
} from '../types/import';
import type { PageResult } from '../types/common';
import type { QuestionOption } from '../types/question';
import { showInfo, showSuccess } from '../utils/messages';

interface EditorState {
  typeCode: string;
  title: string;
  stem: string;
  answer: string;
  solution: string;
  difficulty: number | null;
  options: QuestionOption[];
  correctOptions: string[];
  judgeAnswer: 'T' | 'F' | '';
}

const route = useRoute();
const router = useRouter();

const session = ref<ImportSessionVO | null>(null);
const itemPage = ref<PageResult<ImportItemVO> | null>(null);
const sessionLoading = ref(false);
const itemsLoading = ref(false);
const batchSaving = ref(false);
const actionLoading = ref(false);

const editDialogVisible = ref(false);
const editingItemId = ref('');
const editingCollectionName = ref('');

const itemQuery = reactive<ImportItemPageQuery>({
  pageNum: 1,
  pageSize: 10,
  status: 'ALL'
});

const editor = reactive<EditorState>({
  typeCode: 'single-choice',
  title: '',
  stem: '',
  answer: '',
  solution: '',
  difficulty: 0.5,
  options: [
    { key: 'A', content: '' },
    { key: 'B', content: '' }
  ],
  correctOptions: [],
  judgeAnswer: 'T'
});

const pendingEdits = reactive<Record<string, QuestionDraft>>({});
let autoRefreshTimer: ReturnType<typeof window.setTimeout> | null = null;
let autoRefreshAttempts = 0;
const maxAutoRefreshAttempts = 25;
const autoRefreshInterval = 1200;

const importId = computed(() => {
  const id = route.params.importId;
  return typeof id === 'string' ? id : '';
});

const pendingCount = computed(() => Object.keys(pendingEdits).length);
const isChoiceEditor = computed(() => editor.typeCode === 'single-choice' || editor.typeCode === 'multiple-choice');
const isSingleChoiceEditor = computed(() => editor.typeCode === 'single-choice');
const itemPageSummary = computed(() => {
  const currentPage = itemPage.value;
  if (!currentPage) {
    return '';
  }
  const total = Number.isFinite(currentPage.total) ? currentPage.total : 0;
  const pageSize = currentPage.pageSize || itemQuery.pageSize || 10;
  const pageNum = currentPage.pageNum || itemQuery.pageNum || 1;
  const pages = Math.max(currentPage.pages || Math.ceil(total / Math.max(pageSize, 1)) || 1, 1);
  return `共 ${total} 条 · 第 ${Math.min(pageNum, pages)} / ${pages} 页`;
});

const normalizedOptionKeys = computed(() =>
  editor.options
    .map((option) => option.key.trim())
    .filter((key, index, arr) => Boolean(key) && arr.indexOf(key) === index)
);

const singleCorrectOption = computed({
  get: () => editor.correctOptions[0] ?? '',
  set: (value: string) => {
    editor.correctOptions = value ? [value] : [];
  }
});

const commitDisabled = computed(() => {
  if (!session.value || actionLoading.value || sessionLoading.value) {
    return true;
  }
  if (pendingCount.value > 0) {
    return true;
  }
  return session.value.status !== 1;
});

const cancelDisabled = computed(() => {
  if (!session.value || actionLoading.value || sessionLoading.value) {
    return true;
  }
  return session.value.status !== 1;
});

watch(
  () => editor.typeCode,
  (type) => {
    if (type === 'single-choice' || type === 'multiple-choice') {
      if (editor.options.length < 2) {
        editor.options = [
          { key: 'A', content: '' },
          { key: 'B', content: '' }
        ];
      }
      if (type === 'single-choice' && editor.correctOptions.length > 1) {
        editor.correctOptions = editor.correctOptions.slice(0, 1);
      }
      editor.judgeAnswer = '';
      return;
    }

    editor.options = [];
    editor.correctOptions = [];
    if (type === 'true-false' && !editor.judgeAnswer) {
      editor.judgeAnswer = 'T';
    }
    if (type !== 'true-false') {
      editor.judgeAnswer = '';
    }
  }
);

watch(
  () => route.params.importId,
  () => {
    clearAutoRefreshTimer();
    autoRefreshAttempts = 0;
    clearPendingEdits();
    itemQuery.pageNum = 1;
    void refreshAll();
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  clearAutoRefreshTimer();
});

async function refreshAll() {
  if (!importId.value) {
    return;
  }
  await Promise.all([loadSession(), loadItems()]);
  scheduleAutoRefreshIfNeeded();
}

async function loadSession() {
  if (!importId.value) {
    return;
  }
  sessionLoading.value = true;
  try {
    session.value = await fetchImportSession(importId.value);
  } finally {
    sessionLoading.value = false;
  }
}

async function loadItems() {
  if (!importId.value) {
    return;
  }
  itemsLoading.value = true;
  try {
    const response = await fetchImportItems(importId.value, {
      pageNum: itemQuery.pageNum,
      pageSize: itemQuery.pageSize,
      status: itemQuery.status as ImportItemQueryStatus
    });
    itemPage.value = normalizeImportItemPage(response);
    itemQuery.pageNum = itemPage.value.pageNum || itemQuery.pageNum;
    itemQuery.pageSize = itemPage.value.pageSize || itemQuery.pageSize;
  } finally {
    itemsLoading.value = false;
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

function normalizeImportItemPage(raw: unknown): PageResult<ImportItemVO> {
  const payload = raw && typeof raw === 'object' ? (raw as Record<string, unknown>) : {};
  const recordsRaw = payload.records ?? payload.list ?? payload.items ?? [];
  const records = Array.isArray(recordsRaw) ? recordsRaw : [];
  const pageSize = normalizeInteger(payload.pageSize ?? payload.size, itemQuery.pageSize || 10, 1);
  const total = normalizeInteger(payload.total, records.length, 0);
  const pageNum = normalizeInteger(payload.pageNum ?? payload.current ?? payload.page, itemQuery.pageNum || 1, 1);
  const pages = normalizeInteger(payload.pages ?? payload.pageCount, Math.max(Math.ceil(total / pageSize), 1), 1);
  return {
    records: records as ImportItemVO[],
    total,
    pageNum,
    pageSize,
    pages,
    hasPrevious: Boolean(payload.hasPrevious ?? pageNum > 1),
    hasNext: Boolean(payload.hasNext ?? pageNum < pages)
  };
}

function scheduleAutoRefreshIfNeeded() {
  clearAutoRefreshTimer();
  if (!shouldAutoRefreshImportItems() || autoRefreshAttempts >= maxAutoRefreshAttempts) {
    return;
  }
  autoRefreshAttempts += 1;
  autoRefreshTimer = window.setTimeout(() => {
    void refreshAll();
  }, autoRefreshInterval);
}

function shouldAutoRefreshImportItems() {
  if (!importId.value || pendingCount.value > 0 || actionLoading.value || batchSaving.value) {
    return false;
  }
  if (session.value?.status === 0) {
    return true;
  }
  const loadedPage = itemPage.value;
  const sessionTotal = Number(session.value?.total ?? 0);
  return Boolean(loadedPage && loadedPage.total === 0 && sessionTotal > 0);
}

function clearAutoRefreshTimer() {
  if (!autoRefreshTimer) {
    return;
  }
  window.clearTimeout(autoRefreshTimer);
  autoRefreshTimer = null;
}

function onFilterChange() {
  itemQuery.pageNum = 1;
  void loadItems();
}

function onPageChange(pageNum: number) {
  if (itemsLoading.value) {
    return;
  }
  itemQuery.pageNum = pageNum;
  void loadItems();
}

function onSizeChange(pageSize: number) {
  if (itemsLoading.value) {
    return;
  }
  itemQuery.pageSize = pageSize;
  itemQuery.pageNum = 1;
  void loadItems();
}

function effectiveDraft(item: ImportItemVO): QuestionDraft {
  return pendingEdits[item.itemId] ?? item.draft;
}

function hasPendingEdit(itemId: string): boolean {
  return Boolean(pendingEdits[itemId]);
}

function itemRowClassName({ row }: { row: ImportItemVO }) {
  return row.status === 1 ? 'row-invalid' : '';
}

function openEditDialog(item: ImportItemVO) {
  const source = effectiveDraft(item);
  applyEditor(source);
  editingItemId.value = item.itemId;
  editingCollectionName.value = item.collectionName;
  editDialogVisible.value = true;
}

function applyEditor(source?: QuestionDraft | null) {
  const normalized = source ?? {
    typeCode: 'single-choice',
    title: '',
    stem: ''
  };

  editor.typeCode = normalized.typeCode || 'single-choice';
  editor.title = normalized.title || '';
  editor.stem = normalized.stem || '';
  editor.answer = normalized.answer || '';
  editor.solution = normalized.solution || '';
  editor.difficulty = typeof normalized.difficulty === 'number' ? normalized.difficulty : null;
  editor.options = (normalized.options ?? []).map((option) => ({
    key: option.key ?? '',
    content: option.content ?? ''
  }));
  editor.correctOptions = [...(normalized.correctOptions ?? [])];
  editor.judgeAnswer = (normalized.judgeAnswer as 'T' | 'F' | '' | undefined) ?? '';

  if ((editor.typeCode === 'single-choice' || editor.typeCode === 'multiple-choice') && editor.options.length < 2) {
    editor.options = [
      { key: 'A', content: '' },
      { key: 'B', content: '' }
    ];
  }

  if (editor.typeCode === 'true-false' && !editor.judgeAnswer) {
    editor.judgeAnswer = 'T';
  }
}

function addEditorOption() {
  editor.options.push({ key: '', content: '' });
}

function removeEditorOption(index: number) {
  const removed = editor.options.splice(index, 1)[0];
  if (!removed) {
    return;
  }
  const key = removed.key.trim();
  if (!key) {
    return;
  }
  editor.correctOptions = editor.correctOptions.filter((item) => item !== key);
}

function saveToPending() {
  if (!editingItemId.value) {
    return;
  }
  const draft = buildDraftFromEditor();
  pendingEdits[editingItemId.value] = draft;

  const current = itemPage.value?.records.find((item) => item.itemId === editingItemId.value);
  if (current) {
    current.draft = draft;
  }

  showSuccess('已加入待保存修改');
  editDialogVisible.value = false;
}

function buildDraftFromEditor(): QuestionDraft {
  const typeCode = editor.typeCode.trim();
  const draft: QuestionDraft = {
    typeCode,
    title: editor.title.trim(),
    stem: editor.stem.trim(),
    answer: normalizeNullableText(editor.answer),
    solution: normalizeNullableText(editor.solution),
    difficulty: editor.difficulty
  };

  if (typeCode === 'single-choice' || typeCode === 'multiple-choice') {
    draft.options = editor.options.map((option) => ({
      key: option.key.trim(),
      content: option.content.trim()
    }));
    draft.correctOptions = editor.correctOptions.map((item) => item.trim()).filter(Boolean);
    draft.judgeAnswer = null;
    return draft;
  }

  if (typeCode === 'true-false') {
    draft.options = [];
    draft.correctOptions = [];
    draft.judgeAnswer = editor.judgeAnswer || null;
    return draft;
  }

  draft.options = [];
  draft.correctOptions = [];
  draft.judgeAnswer = null;
  return draft;
}

function normalizeNullableText(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

async function discardPending(itemId: string) {
  if (!pendingEdits[itemId]) {
    return;
  }
  delete pendingEdits[itemId];
  showInfo('已撤销该条待保存修改');
  await loadItems();
}

async function batchSaveEdits() {
  if (!importId.value || pendingCount.value === 0) {
    return;
  }
  batchSaving.value = true;
  try {
    const items = Object.entries(pendingEdits).map(([itemId, draft]) => ({ itemId, draft }));
    await updateImportItems(importId.value, { items });
    clearPendingEdits();
    showSuccess(`已保存 ${items.length} 条修改`);
    await Promise.all([loadSession(), loadItems()]);
  } finally {
    batchSaving.value = false;
  }
}

async function handleCommit() {
  if (!importId.value || !session.value) {
    return;
  }
  if (pendingCount.value > 0) {
    showInfo('请先保存批量修改后再提交导入');
    return;
  }

  let ignoreInvalidDraft = false;
  if ((session.value.invalidCnt ?? 0) > 0) {
    try {
      await ElMessageBox.confirm(
        '当前存在无效导入项。是否忽略无效项并继续提交？',
        '提交导入确认',
        {
          type: 'warning',
          confirmButtonText: '忽略无效并提交',
          cancelButtonText: '取消'
        }
      );
      ignoreInvalidDraft = true;
    } catch {
      return;
    }
  } else {
    try {
      await ElMessageBox.confirm('确认提交当前导入会话？提交后不可撤销。', '提交导入确认', {
        type: 'warning',
        confirmButtonText: '确认提交',
        cancelButtonText: '取消'
      });
    } catch {
      return;
    }
  }

  actionLoading.value = true;
  try {
    await commitImport(importId.value, ignoreInvalidDraft ? { ignoreInvalidDraft: true } : undefined);
    showSuccess('导入提交成功');
    await Promise.all([loadSession(), loadItems()]);
  } finally {
    actionLoading.value = false;
  }
}

async function handleCancel() {
  if (!importId.value) {
    return;
  }
  try {
    await ElMessageBox.confirm('确认取消该导入会话？该操作不可恢复。', '取消导入确认', {
      type: 'warning',
      confirmButtonText: '确认取消',
      cancelButtonText: '返回'
    });
  } catch {
    return;
  }

  actionLoading.value = true;
  try {
    await cancelImport(importId.value);
    showSuccess('导入会话已取消');
    await router.push('/imports');
  } finally {
    actionLoading.value = false;
  }
}

function clearPendingEdits() {
  for (const key of Object.keys(pendingEdits)) {
    delete pendingEdits[key];
  }
}

function goBack() {
  void router.push('/imports');
}

function sessionStatusLabel(status?: ImportSessionStatus) {
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

function sessionStatusTagType(status?: ImportSessionStatus): 'success' | 'warning' | 'info' | 'danger' {
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

function itemStatusLabel(status: ImportItemStatus) {
  switch (status) {
    case 0:
      return '有效';
    case 1:
      return '无效';
    case 2:
      return '已提交';
    default:
      return '未知';
  }
}

function itemStatusTagType(status: ImportItemStatus): 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 0:
      return 'success';
    case 1:
      return 'danger';
    case 2:
      return 'info';
    default:
      return 'info';
  }
}

function toProgressPercent(progress?: number) {
  const value = Number.isFinite(progress) ? Number(progress) : 0;
  return Math.max(0, Math.min(100, Math.round(value * 100)));
}

function formatDate(value?: string) {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function typeLabel(typeCode?: string) {
  const map: Record<string, string> = {
    'single-choice': '单选题',
    'multiple-choice': '多选题',
    'true-false': '判断题',
    'fill-in': '填空题',
    'short-answer': '简答题'
  };
  if (!typeCode) {
    return '—';
  }
  return map[typeCode] ?? typeCode;
}
</script>

<style scoped>
.import-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.card-title {
  font-weight: 600;
}

.session-card {
  border: 1px solid var(--el-border-color-light);
}

.items-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.items-header h3 {
  margin: 6px 0 0;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.error-list {
  margin: 0;
  padding-left: 18px;
}

.error-list li + li {
  margin-top: 6px;
}

.error-list .field {
  color: var(--el-color-danger);
  margin-right: 8px;
}

.error-list .msg {
  color: var(--el-text-color-regular);
}

.muted {
  color: var(--el-text-color-secondary);
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

.edit-form {
  margin-top: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.choice-card {
  margin-top: 8px;
}

.choice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.option-row {
  display: grid;
  grid-template-columns: 120px 1fr auto;
  gap: 10px;
  align-items: end;
}

.option-key,
.option-content {
  margin-bottom: 0;
}

:deep(.row-invalid > td) {
  background: var(--el-color-danger-light-9);
}

@media (max-width: 1024px) {
  .page-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }

  .items-header {
    flex-direction: column;
  }

  .pagination-wrap {
    align-items: flex-start;
  }

  .option-row {
    grid-template-columns: 1fr;
  }
}
</style>
