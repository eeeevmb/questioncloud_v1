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
          <el-tag :type="statusTagType(paper?.status)" effect="plain">
            {{ statusLabel(paper?.status) }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="4" border>
        <el-descriptions-item label="试卷 ID">{{ paperId }}</el-descriptions-item>
        <el-descriptions-item label="题数">{{ paper?.totalItems ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ formatScore(paper?.totalScore) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(paper?.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(paper?.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="所属用户">{{ paper?.ownerId || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-title-row">
          <h3>基本信息</h3>
          <span class="hint">仅支持修改标题和描述</span>
        </div>
      </template>

      <el-form label-position="top" class="single-column-form">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" placeholder="试卷标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="试卷描述" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-title-row">
          <div>
            <h3>自由组卷</h3>
            <span class="hint">从你自己的题集中选择题目，加入草稿区域后再统一保存。</span>
          </div>
          <div class="card-actions">
            <el-button type="primary" plain :disabled="!manualQuestionCollectionId" @click="showQuestionSelector = true">
              选择题目
            </el-button>
          </div>
        </div>
      </template>

      <el-form label-position="top" class="single-column-form">
        <el-form-item label="题集范围">
          <el-select
            v-model="manualQuestionCollectionId"
            filterable
            clearable
            placeholder="请选择题集"
            :loading="collectionLoading"
            style="max-width: 360px"
            @change="handleManualCollectionChange"
          >
            <el-option
              v-for="collection in collectionOptions"
              :key="collection.collectionId"
              :label="collection.name"
              :value="collection.collectionId"
            />
          </el-select>
          <div class="field-tip">手动选题会从这里选中的题集内分页检索题目。</div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-title-row">
          <div>
            <h3>随机组卷</h3>
            <span class="hint">按题集范围和题型规则生成候选题目，再填充到草稿区域。</span>
          </div>
          <div class="card-actions">
            <el-button :loading="previewLoading" @click="handlePreviewRandomBuild">随机预览</el-button>
            <el-button type="primary" plain :disabled="!randomPreviewRows.length" @click="fillDraftItemsFromPreview">
              填充到草稿区域
            </el-button>
          </div>
        </div>
      </template>

      <el-form label-position="top" class="single-column-form">
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
          <div class="field-tip">支持多选，预览结果会从选中的题集范围内生成。</div>
        </el-form-item>

        <div class="rule-block">
          <div class="rule-block-header">
            <span>规则列表</span>
            <el-button size="small" @click="addRandomRule">新增规则</el-button>
          </div>

          <div class="rule-list">
            <div v-for="(rule, index) in randomForm.rules" :key="index" class="rule-row">
              <el-select v-model="rule.typeCode" placeholder="题型">
                <el-option v-for="option in typeOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
              <el-input-number v-model="rule.count" :min="1" :step="1" :precision="0" controls-position="right" />
              <el-input-number
                v-model="rule.expectedDifficulty"
                :min="0"
                :max="1"
                :step="0.1"
                :precision="1"
                controls-position="right"
                placeholder="0~1"
              />
              <el-button type="danger" plain :disabled="randomForm.rules.length === 1" @click="removeRandomRule(index)">
                删除
              </el-button>
            </div>
          </div>
        </div>
      </el-form>

      <div class="preview-panel">
        <div class="section-subtitle">预览结果</div>
        <el-table v-loading="previewLoading" :data="randomPreviewRows" border row-key="key">
          <el-table-column prop="questionTitle" label="标题" min-width="220">
            <template #default="{ row }">
              <span class="stem-preview">{{ row.questionTitle || '无标题' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="题干" min-width="280">
            <template #default="{ row }">
              <span class="stem-preview">{{ row.stem || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="难度" width="100">
            <template #default="{ row }">
              {{ row.difficulty !== undefined && row.difficulty !== null ? row.difficulty.toFixed(2) : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="题型" width="130">
            <template #default="{ row }">{{ row.typeCode || '—' }}</template>
          </el-table-column>
          <el-table-column label="分值" width="100">
            <template #default="{ row }">{{ formatScore(row.score) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row, $index }">
              <el-button type="primary" link size="small" :loading="row.replacing" @click="handleReplacePreviewItem(row, $index)">
                换一题
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!previewLoading && !randomPreviewRows.length" description="暂无预览结果" />
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-title-row">
          <div>
            <h3>草稿区域</h3>
            <span class="hint">可从自由组卷或随机预览加入题目，拖拽行即可调整顺序。</span>
          </div>
          <div class="card-actions">
            <el-button :disabled="!draftRows.length" @click="clearDraftRows">清空草稿</el-button>
            <el-button type="primary" :loading="submittingItems" @click="handleSaveItems">保存到试卷</el-button>
          </div>
        </div>
      </template>

      <el-table ref="draftTableRef" :data="draftRows" border row-key="key">
        <el-table-column label="" width="52" align="center">
          <template #default>
            <span class="drag-handle" title="拖拽排序">☰</span>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="draftSourceTagType(row.source)">
              {{ draftSourceLabel(row.source) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="questionTitle" label="标题" min-width="220">
          <template #default="{ row }">
            <span class="stem-preview">{{ row.questionTitle || '无标题' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="题干" min-width="280">
          <template #default="{ row }">
            <span class="stem-preview">{{ row.stem || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="100">
          <template #default="{ row }">
            {{ row.difficulty !== undefined && row.difficulty !== null ? row.difficulty.toFixed(2) : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="题型" width="130">
          <template #default="{ row }">{{ row.typeCode || '—' }}</template>
        </el-table-column>
        <el-table-column label="分值" width="180">
          <template #default="{ row }">
            <el-input-number v-model="row.score" :min="0.5" :step="0.5" :precision="2" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right" align="center">
          <template #default="{ row, $index }">
            <el-dropdown trigger="click" @command="(command) => handleDraftAction(command, row, $index)">
              <button class="draft-action-trigger" type="button" :disabled="row.replacing" aria-label="草稿操作">
                :
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="replace">换一题</el-dropdown-item>
                  <el-dropdown-item command="delete">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!draftRows.length" description="草稿区域为空，请先添加题目" />
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-title-row">
          <h3>当前已保存题目</h3>
          <span class="hint">这里展示的是已经写入试卷的题目列表。</span>
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

    <el-dialog v-model="showQuestionSelector" title="选择题目" width="90%" :close-on-click-modal="false">
      <div class="question-selector">
        <div class="selector-toolbar">
          <el-select
            v-model="manualQuestionCollectionId"
            placeholder="题集"
            clearable
            filterable
            style="width: 220px"
            @change="handleManualCollectionChange"
          >
            <el-option
              v-for="collection in collectionOptions"
              :key="collection.collectionId"
              :label="collection.name"
              :value="collection.collectionId"
            />
          </el-select>
          <el-select v-model="questionQuery.typeCode" placeholder="题型" clearable style="width: 150px" @change="handleQuestionQueryChange">
            <el-option v-for="option in typeOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
          <el-input v-model="questionQuery.keyword" placeholder="搜索标题" clearable style="width: 250px" @keyup.enter="handleQuestionQueryChange" />
          <el-button type="primary" :disabled="!manualQuestionCollectionId" @click="handleQuestionQueryChange">搜索</el-button>
        </div>

        <el-table
          v-loading="questionSelectorLoading"
          :data="questionList"
          border
          row-key="id"
          @selection-change="handleQuestionSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="title" label="标题" min-width="220">
            <template #default="{ row }">
              <span class="stem-preview">{{ row.title }}</span>
            </template>
          </el-table-column>
          <el-table-column label="题型" width="130">
            <template #default="{ row }">{{ row.typeCode }}</template>
          </el-table-column>
          <el-table-column label="难度" width="100">
            <template #default="{ row }">{{ Number(row.difficulty ?? 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="正确率" width="110">
            <template #default="{ row }">{{ formatPercent(row.correctRate) }}</template>
          </el-table-column>
        </el-table>

        <div class="pagination-container">
          <el-pagination
            v-model:current-page="questionQuery.pageNum"
            v-model:page-size="questionQuery.pageSize"
            :total="questionTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="loadQuestions"
            @size-change="loadQuestions"
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="showQuestionSelector = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedQuestions.length" @click="handleAddSelectedQuestions">
          添加选中题目 ({{ selectedQuestions.length }})
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import {
  clearPaperItems,
  deletePaper,
  fetchPaperDetail,
  previewRandomBuild,
  randomReplaceItem,
  savePaperItems,
  updatePaper
} from '../api/paper';
import { fetchCollections, fetchCollectionQuestions } from '../api/collection';
import { fetchQuestionDetail } from '../api/question';
import type {
  PaperDetailVO,
  PaperDraftItemRow,
  PaperItemDetailRef,
  PaperItemSavePayload,
  PaperRandomBuildReq,
  PaperRandomBuildRule,
  PaperSavePayload
} from '../types/paper';
import type { CollectionQuestionQuery, CollectionView } from '../types/collection';
import type { QuestionSummary } from '../types/question';
import { showError, showSuccess } from '../utils/messages';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const saving = ref(false);
const actionLoading = ref(false);
const previewLoading = ref(false);
const submittingItems = ref(false);
const collectionLoading = ref(false);
const questionSelectorLoading = ref(false);

const paper = ref<PaperDetailVO | null>(null);
const collectionOptions = ref<CollectionView[]>([]);
const randomPreviewRows = ref<PaperDraftItemRow[]>([]);
const draftRows = ref<PaperDraftItemRow[]>([]);
const questionList = ref<QuestionSummary[]>([]);
const questionTotal = ref(0);
const selectedQuestions = ref<QuestionSummary[]>([]);

const showQuestionSelector = ref(false);
const draftTableRef = ref();
const manualQuestionCollectionId = ref('');
let draggedRowIndex: number | null = null;

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

const questionQuery = reactive<CollectionQuestionQuery>({
  pageNum: 1,
  pageSize: 20,
  keyword: '',
  typeCode: ''
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

onUnmounted(() => {
  teardownDraftRowDnD();
});

watch(
  () => route.params.paperId,
  () => {
    void loadPaper();
  }
);

watch(
  draftRows,
  async () => {
    await nextTick();
    bindDraftRowDnD();
  },
  { deep: true }
);

watch(showQuestionSelector, (visible) => {
  if (visible && manualQuestionCollectionId.value) {
    questionQuery.pageNum = 1;
    void loadQuestions();
  }
});

async function loadPaper() {
  if (!paperId.value) {
    return;
  }
  loading.value = true;
  try {
    paper.value = await fetchPaperDetail(paperId.value);
    syncEditForm();
    syncDraftRowsFromPaper();
  } catch (error) {
    showError(error instanceof Error ? error.message : '加载试卷失败');
  } finally {
    loading.value = false;
  }
}

async function loadCollections() {
  collectionLoading.value = true;
  try {
    const list = await fetchCollections();
    collectionOptions.value = list;
    if (!manualQuestionCollectionId.value && list.length) {
      manualQuestionCollectionId.value = list[0].collectionId;
    }
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

function syncDraftRowsFromPaper() {
  if (!paper.value?.items?.length) {
    draftRows.value = [];
    return;
  }

  draftRows.value = paper.value.items.map((item) => ({
    key: createRowKey(item.questionId, item.questionVersionId),
    questionId: item.questionId,
    questionVersionId: item.questionVersionId,
    score: item.score,
    typeCode: item.typeCode,
    difficulty: item.difficulty,
    questionTitle: item.questionTitle,
    stem: item.stem,
    source: 'paper'
  }));
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
    showSuccess(`已生成 ${randomPreviewRows.value.length} 道候选题`);
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

  const nonPreviewRows = draftRows.value.filter((row) => row.source !== 'preview');
  draftRows.value = [
    ...nonPreviewRows,
    ...randomPreviewRows.value.map((row) => ({
      key: createRowKey(row.questionId, row.questionVersionId),
      questionId: row.questionId,
      questionVersionId: row.questionVersionId,
      score: row.score,
      typeCode: row.typeCode,
      difficulty: row.difficulty,
      questionTitle: row.questionTitle,
      stem: row.stem,
      source: 'preview' as const
    }))
  ];
  showSuccess('已填充到草稿区域');
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

async function handleReplaceDraftItem(row: PaperDraftItemRow, index: number) {
  if (!paperId.value) {
    return;
  }

  const collectionIds = normalizeCollectionIds(randomForm.collectionIds);
  if (!collectionIds.length) {
    showError('请先选择题集');
    return;
  }
  if (!row.typeCode) {
    showError('题目类型不存在');
    return;
  }

  row.replacing = true;
  try {
    const newItem = await randomReplaceItem(paperId.value, {
      collectionIds,
      excludedQuestionIds: draftRows.value.map((item) => item.questionId),
      typeCode: row.typeCode,
      expectedDifficulty: row.difficulty
    });

    draftRows.value[index] = {
      key: createRowKey(String(newItem.questionId), String(newItem.questionVersionId)),
      questionId: String(newItem.questionId),
      questionVersionId: String(newItem.questionVersionId),
      score: row.score,
      typeCode: newItem.typeCode || row.typeCode,
      difficulty: newItem.difficulty,
      questionTitle: newItem.questionTitle,
      stem: newItem.stem,
      source: 'preview',
      replacing: false
    };
    showSuccess('已替换题目');
  } catch (error) {
    row.replacing = false;
    showError(error instanceof Error ? error.message : '换一题失败');
  }
}

function handleDraftAction(command: string, row: PaperDraftItemRow, index: number) {
  if (command === 'delete') {
    removeDraftRow(index);
    return;
  }
  if (command === 'replace') {
    void handleReplaceDraftItem(row, index);
  }
}

async function handleReplacePreviewItem(row: PaperDraftItemRow, index: number) {
  if (!paperId.value) {
    return;
  }

  const collectionIds = normalizeCollectionIds(randomForm.collectionIds);
  if (!collectionIds.length) {
    showError('请先选择题集');
    return;
  }
  if (!row.typeCode) {
    showError('题目类型不存在');
    return;
  }

  row.replacing = true;
  try {
    const newItem = await randomReplaceItem(paperId.value, {
      collectionIds,
      excludedQuestionIds: randomPreviewRows.value.map((item) => item.questionId),
      typeCode: row.typeCode,
      expectedDifficulty: row.difficulty
    });

    randomPreviewRows.value[index] = {
      key: createRowKey(String(newItem.questionId), String(newItem.questionVersionId)),
      questionId: String(newItem.questionId),
      questionVersionId: String(newItem.questionVersionId),
      score: row.score,
      typeCode: newItem.typeCode || row.typeCode,
      difficulty: newItem.difficulty,
      questionTitle: newItem.questionTitle,
      stem: newItem.stem,
      source: 'preview',
      replacing: false
    };
    showSuccess('已替换题目');
  } catch (error) {
    row.replacing = false;
    showError(error instanceof Error ? error.message : '换一题失败');
  }
}

async function handleSaveItems() {
  if (!paperId.value) {
    return;
  }

  const payload = normalizeDraftItems(draftRows.value);
  if (!payload.length) {
    showError('请至少添加一题到草稿区域');
    return;
  }

  submittingItems.value = true;
  try {
    await savePaperItems(paperId.value, payload);
    showSuccess('题目列表已保存到试卷');
    await loadPaper();
  } catch (error) {
    showError(error instanceof Error ? error.message : '保存题目失败');
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
    showError(error instanceof Error ? error.message : '清空失败');
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

async function loadQuestions() {
  if (!manualQuestionCollectionId.value) {
    showError('请先选择题集');
    return;
  }

  questionSelectorLoading.value = true;
  try {
    const result = await fetchCollectionQuestions(manualQuestionCollectionId.value, {
      ...questionQuery,
      keyword: questionQuery.keyword?.trim() || undefined,
      typeCode: questionQuery.typeCode?.trim() || undefined
    });
    questionList.value = result.records;
    questionTotal.value = result.total;
  } catch (error) {
    showError(error instanceof Error ? error.message : '加载题目失败');
  } finally {
    questionSelectorLoading.value = false;
  }
}

function handleQuestionQueryChange() {
  questionQuery.pageNum = 1;
  void loadQuestions();
}

function handleManualCollectionChange() {
  selectedQuestions.value = [];
  questionList.value = [];
  questionTotal.value = 0;
  questionQuery.pageNum = 1;
}

function handleQuestionSelectionChange(selection: QuestionSummary[]) {
  selectedQuestions.value = selection;
}

async function handleAddSelectedQuestions() {
  if (!selectedQuestions.value.length) {
    return;
  }

  const existingKeys = new Set(draftRows.value.map((row) => row.key));
  const uniqueQuestions = selectedQuestions.value.filter(
    (question) => !existingKeys.has(createRowKey(question.id, question.currentVersionId))
  );

  if (!uniqueQuestions.length) {
    showError('选中的题目已全部存在于草稿区域');
    return;
  }

  questionSelectorLoading.value = true;
  try {
    const details = await Promise.all(uniqueQuestions.map((question) => fetchQuestionDetail(question.id)));
    const newRows: PaperDraftItemRow[] = details.map((detail) => ({
      key: createRowKey(detail.id, detail.currentVersionId),
      questionId: detail.id,
      questionVersionId: detail.currentVersionId,
      score: 1,
      typeCode: detail.typeCode,
      difficulty: detail.difficulty,
      questionTitle: detail.title,
      stem: detail.stem,
      source: 'manual'
    }));

    draftRows.value = [...draftRows.value, ...newRows];
    selectedQuestions.value = [];
    showQuestionSelector.value = false;
    showSuccess(`已添加 ${newRows.length} 道题目到草稿区域`);
  } catch (error) {
    showError(error instanceof Error ? error.message : '添加题目失败');
  } finally {
    questionSelectorLoading.value = false;
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

function draftSourceLabel(source?: PaperDraftItemRow['source']) {
  switch (source) {
    case 'preview':
      return '预览';
    case 'manual':
      return '手动';
    case 'paper':
      return '试卷';
    default:
      return '未知';
  }
}

function draftSourceTagType(source?: PaperDraftItemRow['source']) {
  switch (source) {
    case 'preview':
      return 'success';
    case 'manual':
      return 'warning';
    case 'paper':
      return 'info';
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

function normalizeCollectionIds(values: string[]) {
  return values.map((value) => value?.trim()).filter((value): value is string => Boolean(value));
}

function normalizeRandomRules(rules: PaperRandomBuildRule[]) {
  return rules
    .map((rule) => ({
      typeCode: rule.typeCode?.trim() || '',
      count: Number(rule.count),
      expectedDifficulty:
        rule.expectedDifficulty !== undefined && rule.expectedDifficulty !== null
          ? Number(rule.expectedDifficulty)
          : undefined
    }))
    .filter((rule) => rule.typeCode && Number.isFinite(rule.count) && rule.count > 0);
}

function expandPreviewRows(result: PaperItemDetailRef[], rules: PaperRandomBuildRule[]) {
  const rows: PaperDraftItemRow[] = [];
  let cursor = 0;

  for (const rule of rules) {
    const slice = result.slice(cursor, cursor + rule.count);
    for (const item of slice) {
      rows.push({
        key: createRowKey(String(item.questionId), String(item.questionVersionId)),
        questionId: String(item.questionId),
        questionVersionId: String(item.questionVersionId),
        score: 1,
        typeCode: item.typeCode || rule.typeCode,
        difficulty: item.difficulty,
        questionTitle: item.questionTitle,
        stem: item.stem,
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
    expectedDifficulty: undefined
  };
}

function createRowKey(questionId: string, questionVersionId: string) {
  return `${questionId}-${questionVersionId}`;
}

function handleDragStart(event: DragEvent, index: number) {
  draggedRowIndex = index;
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move';
    event.dataTransfer.setData('text/plain', String(index));
  }
}

function handleDragOver(event: DragEvent) {
  event.preventDefault();
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move';
  }
}

function handleDrop(event: DragEvent, targetIndex: number) {
  event.preventDefault();
  if (draggedRowIndex === null || draggedRowIndex === targetIndex) {
    draggedRowIndex = null;
    return;
  }

  const rows = [...draftRows.value];
  const [draggedRow] = rows.splice(draggedRowIndex, 1);
  rows.splice(targetIndex, 0, draggedRow);
  draftRows.value = rows;
  draggedRowIndex = null;
}

function teardownDraftRowDnD() {
  const body = draftTableRef.value?.$el?.querySelector?.('.el-table__body-wrapper tbody') as HTMLTableSectionElement | null;
  if (!body) {
    return;
  }

  Array.from(body.querySelectorAll('tr')).forEach((row) => {
    row.draggable = false;
    row.ondragstart = null;
    row.ondragover = null;
    row.ondrop = null;
    row.ondragend = null;
  });
}

function bindDraftRowDnD() {
  teardownDraftRowDnD();

  const body = draftTableRef.value?.$el?.querySelector?.('.el-table__body-wrapper tbody') as HTMLTableSectionElement | null;
  if (!body) {
    return;
  }

  Array.from(body.querySelectorAll('tr')).forEach((row, index) => {
    row.draggable = true;
    row.ondragstart = (event) => handleDragStart(event as DragEvent, index);
    row.ondragover = (event) => handleDragOver(event as DragEvent);
    row.ondrop = (event) => handleDrop(event as DragEvent, index);
    row.ondragend = () => {
      draggedRowIndex = null;
    };
  });
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

.toolbar-actions,
.card-actions,
.selector-toolbar {
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

.single-column-form {
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
  grid-template-columns: 1.2fr 0.7fr 0.9fr auto;
  gap: 10px;
  align-items: center;
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

.pagination-container {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.drag-handle {
  cursor: grab;
  color: var(--el-text-color-secondary);
  user-select: none;
}

.draft-action-trigger {
  border: 0;
  background: transparent;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
  padding: 0 6px;
}

.draft-action-trigger:disabled {
  cursor: not-allowed;
  opacity: 0.5;
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
