<template>
  <section class="paper-detail-page">
    <div class="page-toolbar">
      <el-button @click="goBack">返回列表</el-button>
      <div class="toolbar-actions">
        <el-button :loading="loading" @click="loadPaper">刷新</el-button>
        <el-button type="primary" :loading="saving" @click="handleSavePaperBasic">保存修改</el-button>
        <el-button type="warning" plain :loading="actionLoading" @click="handleClearItems">清空题目</el-button>
        <el-button type="danger" plain :loading="actionLoading" @click="handleDeletePaper">删除试卷</el-button>
      </div>
    </div>

    <el-card v-loading="loading" shadow="never" class="info-card">
      <template #header>
        <div class="card-title-row">
          <div>
            <p class="eyebrow">试卷详情</p>
            <h2>{{ paper?.title || '试卷详情' }}</h2>
          </div>
          <el-tag :type="statusTagType(paper?.status)" effect="plain">{{ statusLabel(paper?.status) }}</el-tag>
        </div>
      </template>

      <el-descriptions :column="4" border>
        <el-descriptions-item label="试卷 ID">{{ paperId }}</el-descriptions-item>
        <el-descriptions-item label="题数">{{ paper?.totalItems ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ formatScore(paper?.totalScore) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(paper?.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(paper?.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="归属用户">{{ paper?.ownerId || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="edit-card">
      <template #header>
        <div class="card-title-row">
          <h3>基本信息</h3>
          <span class="hint">仅可修改标题和描述</span>
        </div>
      </template>

      <el-form label-position="top" class="paper-form">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" placeholder="试卷标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="试卷描述" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="random-card">
      <template #header>
        <div class="card-title-row">
          <div>
            <h3>随机组卷</h3>
            <span class="hint">按题集名称选择范围，再配置题型规则后先预览</span>
          </div>
          <div class="card-actions">
            <el-button :loading="previewLoading" @click="handlePreviewRandomBuild">随机预览</el-button>
            <el-button :disabled="!randomPreviewRows.length" type="primary" plain @click="fillDraftItemsFromPreview">
              填充到待保存列表
            </el-button>
          </div>
        </div>
      </template>

      <el-form label-position="top" class="random-form">
        <el-form-item label="题集">
          <el-select
            v-model="randomForm.collectionIds"
            multiple
            filterable
            clearable
            placeholder="请选择题集"
            :loading="collectionLoading"
          >
            <el-option
              v-for="collection in collectionOptions"
              :key="collection.collectionId"
              :label="collection.name"
              :value="collection.collectionId"
            />
          </el-select>
          <div class="field-tip">可多选，最终按你选中的题集范围进行随机预览。</div>
        </el-form-item>

        <div class="rule-block">
          <div class="rule-block-header">
            <span>规则列表</span>
            <el-button size="small" @click="addRandomRule">新增规则</el-button>
          </div>

          <div class="rule-list">
            <div v-for="(rule, index) in randomForm.rules" :key="index" class="rule-row">
              <el-select v-model="rule.typeCode" placeholder="题型" class="rule-type">
                <el-option v-for="option in typeOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
              <el-input-number v-model="rule.count" :min="1" :step="1" :precision="0" controls-position="right" class="rule-count" />
              <el-input-number v-model="rule.score" :min="0.5" :step="0.5" :precision="2" controls-position="right" class="rule-score" />
              <el-button type="danger" plain :disabled="randomForm.rules.length === 1" @click="removeRandomRule(index)">删除</el-button>
            </div>
          </div>
        </div>
      </el-form>

      <div class="preview-panel">
        <div class="section-subtitle">预览结果</div>
        <el-table v-loading="previewLoading" :data="randomPreviewRows" border row-key="key">
          <el-table-column prop="questionId" label="题目 ID" min-width="180" />
          <el-table-column prop="questionVersionId" label="版本 ID" min-width="180" />
          <el-table-column prop="typeCode" label="题型" width="160" />
          <el-table-column prop="score" label="分值" width="100">
            <template #default="{ row }">{{ formatScore(row.score) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!previewLoading && !randomPreviewRows.length" description="暂无预览结果" />
      </div>
    </el-card>

    <el-card shadow="never" class="draft-card">
      <template #header>
        <div class="card-title-row">
          <div>
            <h3>待保存题目列表</h3>
            <span class="hint">由随机预览结果填充，可调整分值后一次性保存到试卷</span>
          </div>
          <div class="card-actions">
            <el-button :disabled="!draftRows.length" @click="clearDraftRows">清空待保存列表</el-button>
            <el-button type="primary" :loading="submittingItems" @click="handleSaveItems">保存到试卷</el-button>
          </div>
        </div>
      </template>

      <el-table :data="draftRows" border row-key="key">
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.source === 'preview' ? 'success' : 'info'" effect="plain">
              {{ row.source === 'preview' ? '预览' : '手工' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="题目 ID" min-width="220">
          <template #default="{ row }">
            {{ row.questionId }}
          </template>
        </el-table-column>
        <el-table-column label="版本 ID" min-width="220">
          <template #default="{ row }">
            {{ row.questionVersionId }}
          </template>
        </el-table-column>
        <el-table-column label="分值" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.score" :min="0.5" :step="0.5" :precision="2" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="题型" width="160">
          <template #default="{ row }">
            {{ row.typeCode || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ $index }">
            <el-button type="danger" link @click="removeDraftRow($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!draftRows.length" description="暂无待保存题目，请先执行随机预览并填充。" />
    </el-card>

    <el-card shadow="never" class="items-card">
      <template #header>
        <div class="card-title-row">
          <h3>当前已保存题目</h3>
          <span class="hint">这里展示的是试卷已经入库的题目列表</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="paper?.items ?? []" border row-key="seq">
        <el-table-column prop="seq" label="序号" width="80" />
        <el-table-column prop="questionTitle" label="标题" min-width="220" />
        <el-table-column prop="typeCode" label="题型" width="140" />
        <el-table-column prop="score" label="分值" width="100">
          <template #default="{ row }">{{ formatScore(row.score) }}</template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度" width="100">
          <template #default="{ row }">{{ formatScore(row.difficulty) }}</template>
        </el-table-column>
        <el-table-column prop="correctRate" label="正确率" width="110">
          <template #default="{ row }">{{ formatPercent(row.correctRate) }}</template>
        </el-table-column>
        <el-table-column label="题干" min-width="280">
          <template #default="{ row }">
            <span class="stem-preview">{{ row.stem || '—' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !(paper?.items?.length ?? 0)" description="当前试卷还没有题目" />
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import {
  clearPaperItems,
  deletePaper,
  fetchPaperDetail,
  previewRandomBuild,
  savePaperItems,
  updatePaper
} from '../api/paper';
import { fetchCollections } from '../api/collection';
import type {
  PaperDetailVO,
  PaperDraftItemRow,
  PaperItemSavePayload,
  PaperRandomBuildReq,
  PaperRandomBuildRule,
  PaperSavePayload
} from '../types/paper';
import type { CollectionView } from '../types/collection';
import { showError, showSuccess } from '../utils/messages';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const saving = ref(false);
const actionLoading = ref(false);
const previewLoading = ref(false);
const submittingItems = ref(false);
const collectionLoading = ref(false);
const collectionOptions = ref<CollectionView[]>([]);
const paper = ref<PaperDetailVO | null>(null);
const randomPreviewRows = ref<PaperDraftItemRow[]>([]);
const draftRows = ref<PaperDraftItemRow[]>([]);

const editForm = reactive<PaperSavePayload>({
  title: '',
  description: ''
});

const randomForm = reactive<{
  collectionIds: string[];
  rules: PaperRandomBuildRule[];
}>({
  collectionIds: [],
  rules: [createRandomRule()]
});

const typeOptions = [
  { label: '单选题', value: 'single-choice' },
  { label: '多选题', value: 'multiple-choice' },
  { label: '判断题', value: 'true-false' },
  { label: '填空题', value: 'fill-in' },
  { label: '简答题', value: 'short-answer' }
];

const paperId = computed(() => String(route.params.paperId ?? ''));

onMounted(() => {
  void loadPaper();
  void loadCollections();
});

watch(
  () => route.params.paperId,
  () => {
    void loadPaper();
  }
);

async function loadPaper() {
  if (!paperId.value) {
    return;
  }
  loading.value = true;
  try {
    paper.value = await fetchPaperDetail(paperId.value);
    syncEditForm();
  } finally {
    loading.value = false;
  }
}

async function loadCollections() {
  collectionLoading.value = true;
  try {
    const list = await fetchCollections();
    collectionOptions.value = list;
  } catch (error) {
    showError(error instanceof Error ? error.message : '加载题集失败');
  } finally {
    collectionLoading.value = false;
  }
}

function syncEditForm() {
  editForm.title = paper.value?.title || '';
  editForm.description = paper.value?.description || '';
}

async function handleSavePaperBasic() {
  if (!paperId.value || !editForm.title.trim()) {
    showError('请先填写试卷标题');
    return;
  }
  saving.value = true;
  try {
    await updatePaper(paperId.value, {
      title: editForm.title.trim(),
      description: editForm.description?.trim() || ''
    });
    showSuccess('试卷已保存');
    await loadPaper();
  } catch (error) {
    showError(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function handlePreviewRandomBuild() {
  if (!paperId.value) {
    return;
  }

  const collectionIds = normalizeCollectionIds(randomForm.collectionIds);
  if (!collectionIds.length) {
    showError('请先选择题集');
    return;
  }

  const rules = normalizeRandomRules(randomForm.rules);
  if (!rules.length) {
    showError('请至少配置一条有效规则');
    return;
  }

  previewLoading.value = true;
  try {
    const request: PaperRandomBuildReq = {
      collectionIds,
      rules
    };
    const result = await previewRandomBuild(paperId.value, request);
    randomPreviewRows.value = expandPreviewRows(result, rules);
    showSuccess(`已生成 ${randomPreviewRows.value.length} 条候选题`);
  } catch (error) {
    showError(error instanceof Error ? error.message : '随机预览失败');
  } finally {
    previewLoading.value = false;
  }
}

function fillDraftItemsFromPreview() {
  if (!randomPreviewRows.value.length) {
    showError('请先生成预览结果');
    return;
  }

  draftRows.value = randomPreviewRows.value.map((row) => ({
    key: createRowKey(row.questionId, row.questionVersionId),
    questionId: row.questionId,
    questionVersionId: row.questionVersionId,
    score: row.score,
    typeCode: row.typeCode,
    source: 'preview'
  }));
  showSuccess('已填充到待保存列表');
}

function addRandomRule() {
  randomForm.rules.push(createRandomRule());
}

function removeRandomRule(index: number) {
  if (randomForm.rules.length === 1) {
    return;
  }
  randomForm.rules.splice(index, 1);
}

function removeDraftRow(index: number) {
  draftRows.value.splice(index, 1);
}

function clearDraftRows() {
  draftRows.value = [];
}

async function handleSaveItems() {
  if (!paperId.value) {
    return;
  }

  const payload = normalizeDraftItems(draftRows.value);
  if (!payload.length) {
    showError('请至少填写一条待保存题目');
    return;
  }

  submittingItems.value = true;
  try {
    await savePaperItems(paperId.value, payload);
    showSuccess('题目列表已保存到试卷');
    await loadPaper();
  } catch (error) {
    showError(error instanceof Error ? error.message : '保存题目列表失败');
  } finally {
    submittingItems.value = false;
  }
}

async function handleClearItems() {
  if (!paperId.value) {
    return;
  }
  try {
    await ElMessageBox.confirm('确定清空当前试卷的全部题目吗？', '清空确认', {
      type: 'warning',
      confirmButtonText: '清空',
      cancelButtonText: '取消'
    });
  } catch {
    return;
  }
  actionLoading.value = true;
  try {
    await clearPaperItems(paperId.value);
    showSuccess('题目已清空');
    await loadPaper();
  } catch (error) {
    showError(error instanceof Error ? error.message : '清空题目失败');
  } finally {
    actionLoading.value = false;
  }
}

async function handleDeletePaper() {
  if (!paperId.value) {
    return;
  }
  try {
    await ElMessageBox.confirm('确定删除当前试卷吗？此操作不可恢复。', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    });
  } catch {
    return;
  }
  actionLoading.value = true;
  try {
    await deletePaper(paperId.value);
    showSuccess('试卷已删除');
    await router.replace({ name: 'paper-list' });
  } catch (error) {
    showError(error instanceof Error ? error.message : '删除试卷失败');
  } finally {
    actionLoading.value = false;
  }
}

function goBack() {
  void router.push({ name: 'paper-list' });
}

function statusLabel(status?: number | null) {
  switch (status) {
    case 0:
      return '草稿';
    case 1:
      return '已发布';
    case 2:
      return '已归档';
    default:
      return '未知';
  }
}

function statusTagType(status?: number | null) {
  switch (status) {
    case 0:
      return 'info';
    case 1:
      return 'success';
    case 2:
      return 'warning';
    default:
      return 'info';
  }
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '—';
  }
  return value.replace('T', ' ').slice(0, 19);
}

function formatScore(value?: number | null) {
  return Number(value ?? 0).toFixed(2);
}

function formatPercent(value?: number | null) {
  return `${(Number(value ?? 0) * 100).toFixed(2)}%`;
}

function normalizeCollectionIds(values: string[]): string[] {
  return values
    .map((value) => value?.trim())
    .filter((value): value is string => Boolean(value));
}

function normalizeRandomRules(rules: PaperRandomBuildRule[]): PaperRandomBuildRule[] {
  return rules
    .map((rule) => ({
      typeCode: rule.typeCode?.trim() || '',
      count: Number(rule.count),
      score: Number(rule.score)
    }))
    .filter((rule) => rule.typeCode && Number.isFinite(rule.count) && rule.count > 0 && Number.isFinite(rule.score) && rule.score >= 0.5);
}

function expandPreviewRows(
  result: { questionId: string; questionVersionId: string }[],
  rules: PaperRandomBuildRule[]
): PaperDraftItemRow[] {
  const rows: PaperDraftItemRow[] = [];
  let cursor = 0;

  for (const rule of rules) {
    const slice = result.slice(cursor, cursor + rule.count);
    for (const item of slice) {
      rows.push({
        key: createRowKey(item.questionId, item.questionVersionId),
        questionId: String(item.questionId),
        questionVersionId: String(item.questionVersionId),
        score: Number(rule.score),
        typeCode: rule.typeCode,
        source: 'preview'
      });
    }
    cursor += rule.count;
  }

  return rows;
}

function normalizeDraftItems(rows: PaperDraftItemRow[]): PaperItemSavePayload[] {
  const payload: PaperItemSavePayload[] = [];
  for (const row of rows) {
    const questionId = row.questionId?.trim();
    const questionVersionId = row.questionVersionId?.trim();
    const score = Number(row.score);
    if (!questionId || !questionVersionId || !Number.isFinite(score) || score < 0.5) {
      continue;
    }
    payload.push({
      questionId,
      questionVersionId,
      score: Number(score.toFixed(2))
    });
  }
  return payload;
}

function createRandomRule(): PaperRandomBuildRule {
  return {
    typeCode: '',
    count: 1,
    score: 1
  };
}

function createRowKey(questionId: string, questionVersionId: string) {
  return `${questionId}-${questionVersionId}`;
}
</script>

<style scoped>
.paper-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--el-color-primary);
  font-weight: 700;
  letter-spacing: 0.08em;
}

.card-title-row h2,
.card-title-row h3 {
  margin: 0;
}

.hint,
.field-tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.paper-form,
.random-form {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.rule-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rule-block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-weight: 600;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rule-row {
  display: grid;
  grid-template-columns: 1.2fr 0.7fr 0.7fr auto;
  gap: 10px;
  align-items: center;
}

.rule-type,
.rule-count,
.rule-score {
  width: 100%;
}

.preview-panel {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-subtitle {
  font-weight: 600;
}

.stem-preview {
  display: inline-block;
  line-height: 1.5;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 960px) {
  .page-toolbar,
  .card-title-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .rule-row {
    grid-template-columns: 1fr;
  }
}
</style>
