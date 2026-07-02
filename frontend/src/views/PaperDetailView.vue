<template>
  <section class="paper-detail-page">
    <div class="page-toolbar">
      <el-button text class="back-link-button" @click="goBack">返回列表</el-button>
      <div class="toolbar-actions toolbar-actions-compact">
        <el-button :loading="loading" @click="loadPaper">刷新</el-button>
        <el-button type="primary" plain :disabled="!(paper?.items?.length ?? 0)" @click="goPrintLayout">排版导出</el-button>
        <el-button type="warning" plain :loading="actionLoading" @click="handleClearItems">清空题目</el-button>
        <el-button type="danger" plain :loading="actionLoading" @click="handleDeletePaper">删除试卷</el-button>
      </div>
    </div>

    <el-card v-loading="loading" shadow="never" class="info-card">
      <template #header>
        <div class="paper-header">
          <div>
            <p class="eyebrow">试卷详情</p>
            <div class="paper-title-row">
              <h2>{{ paper?.title || '试卷详情' }}</h2>
              <el-dropdown trigger="click" @command="handleBuildEntryCommand">
                <el-button class="paper-build-entry-btn" plain>
                  开始组卷
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="manual">自由组卷</el-dropdown-item>
                    <el-dropdown-item command="random">随机组卷</el-dropdown-item>
                    <el-dropdown-item command="ai">AI组卷</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          <el-button circle class="paper-edit-icon-btn" @click="openBasicInfoDrawer">
            <el-icon><Edit /></el-icon>
          </el-button>
        </div>
      </template>

      <div class="paper-summary-bar">
        <div class="paper-summary-inline">
          <span class="paper-summary-label">试卷 ID</span>
          <strong class="paper-summary-value">{{ paperId }}</strong>
        </div>
        <div class="paper-summary-inline">
          <span class="paper-summary-label">所属用户</span>
          <strong class="paper-summary-value">{{ ownerDisplayName }}</strong>
        </div>
        <div class="paper-summary-inline">
          <span class="paper-summary-label">总分</span>
          <strong class="paper-summary-value">{{ formatScore(paper?.totalScore) }}</strong>
        </div>
        <div class="paper-summary-inline">
          <span class="paper-summary-label">总题数</span>
          <strong class="paper-summary-value">{{ paper?.totalItems ?? 0 }}</strong>
        </div>
      </div>
    </el-card>

    <el-drawer v-model="basicInfoDrawerVisible" title="修改试卷基本信息" size="520px">
      <el-form label-position="top" class="single-column-form">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" placeholder="试卷标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="试卷描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="basicInfoDrawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSavePaperBasic">保存修改</el-button>
        </div>
      </template>
    </el-drawer>

    <div class="paper-preview-grid" :class="{ 'paper-preview-grid--single': !(paper?.items?.length ?? 0) }">
      <el-card shadow="never" class="paper-items-card paper-items-list-card">
        <template #header>
          <div class="card-title-row">
            <h3>当前试卷题目</h3>
            <span class="hint">点击左侧题目，右侧查看完整详情。</span>
          </div>
        </template>

        <el-empty v-if="!(paper?.items?.length ?? 0)" description="当前试卷还没有题目" />
        <div v-else class="paper-items-list">
          <div class="paper-items-list-head">
            <span>题目</span>
            <span>题型</span>
          </div>
          <button
            v-for="item in paper?.items ?? []"
            :key="item.questionVersionId"
            type="button"
            class="paper-item-row"
            :class="{ active: item.questionId === activeSavedPreviewQuestionId }"
            @click="handleSavedPreviewRowClick(item)"
          >
            <div class="paper-item-row-main">
              <strong class="paper-item-row-title">{{ item.questionTitle || '未命名题目' }}</strong>
            </div>
            <el-tag
              size="small"
              effect="plain"
              class="paper-item-type-tag"
              :class="paperItemTypeTagClass(item.typeCode)"
              :type="paperItemTypeTagType(item.typeCode)"
            >
              {{ questionTypeLabel(item.typeCode) }}
            </el-tag>
          </button>
        </div>
      </el-card>

      <el-card v-if="paper?.items?.length ?? 0" shadow="never" class="paper-items-card paper-detail-preview-card">
        <template #header>
          <div class="card-title-row">
            <h3>题目预览</h3>
            <span class="hint">默认展示第一题，点击左侧可切换。</span>
          </div>
        </template>

        <div v-loading="savedPreviewLoading" class="paper-detail-preview-body">
          <el-empty v-if="!activeSavedPreviewQuestionId" class="compact-empty" description="请选择题目查看详情" />
          <el-empty
            v-else-if="!activeSavedPreviewDetail && !savedPreviewLoading"
            class="compact-empty"
            description="题目详情加载失败"
          />
          <template v-else-if="activeSavedPreviewDetail">
            <div class="selector-detail-head paper-detail-preview-head">
              <div>
                <h4>{{ activeSavedPreviewDetail.title || '未命名题目' }}</h4>
                <p class="hint">试卷题目详情预览</p>
                <div
                  v-if="savedPreviewKnowledgeTags.length"
                  v-loading="savedPreviewKnowledgeTagsLoading"
                  class="knowledge-tag-row knowledge-tag-row--compact"
                >
                  <el-tag
                    v-for="tag in savedPreviewKnowledgeTags"
                    :key="`${tag.canonicalName}_${tag.isMain}`"
                    size="small"
                    effect="light"
                    class="knowledge-tag-chip"
                    :class="tag.isMain === 1 ? 'knowledge-tag-chip--main' : 'knowledge-tag-chip--secondary'"
                  >
                    {{ tag.canonicalName }}
                  </el-tag>
                </div>
              </div>
              <div class="detail-tags">
                <el-tag size="small" effect="plain">{{ questionTypeLabel(activeSavedPreviewDetail.typeCode) }}</el-tag>
                <el-tag size="small" type="success" effect="plain">
                  难度 {{ Number(activeSavedPreviewDetail.difficulty ?? 0).toFixed(2) }}
                </el-tag>
                <el-tag size="small" type="warning" effect="plain">
                  正确率 {{ formatPercent(activeSavedPreviewDetail.correctRate) }}
                </el-tag>
              </div>
            </div>

            <div class="selector-detail-section">
              <p class="expand-label">题干</p>
              <AssistantMessageContent :content="activeSavedPreviewDetail.stem || '—'" />
            </div>

            <div v-if="activeSavedPreviewDetail.options?.length" class="selector-detail-section">
              <p class="expand-label">选项</p>
              <ol class="detail-option-list">
                <li v-for="option in activeSavedPreviewDetail.options" :key="`${option.key}_${option.content}`">
                  <span class="detail-option-key">{{ option.key }}.</span>
                  <AssistantMessageContent :content="option.content || '（无内容）'" />
                </li>
              </ol>
            </div>

            <div class="selector-detail-section">
              <p class="expand-label">答案</p>
              <AssistantMessageContent :content="activeSavedPreviewAnswer" />
            </div>

            <div class="selector-detail-section">
              <p class="expand-label">解析</p>
              <AssistantMessageContent :content="activeSavedPreviewDetail.solution || '暂无解析'" />
            </div>
          </template>
        </div>
      </el-card>
    </div>

  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { Edit } from '@element-plus/icons-vue';
import AssistantMessageContent from '../components/AssistantMessageContent.vue';
import { fetchQuestionKnowledgeTags } from '../api/knowledge';
import { generateAgentPaperDraft } from '../api/ai';
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
  AgentPaperDraftVO,
  GeneratePaperDraftBucketConstrain,
  GeneratePaperDraftReq
} from '../types/ai';
import type { QuestionKnowledgeTag } from '../types/knowledge';
import type {
  PaperDetailVO,
  PaperDraftItemRow,
  PaperItemDetailRef,
  PaperItemVO,
  PaperItemSavePayload,
  PaperRandomBuildReq,
  PaperRandomBuildRule,
  PaperSavePayload
} from '../types/paper';
import type { CollectionQuestionQuery, CollectionView } from '../types/collection';
import type { QuestionDetail, QuestionSummary } from '../types/question';
import { useAuthStore } from '../stores/auth';
import { showError, showSuccess } from '../utils/messages';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const loading = ref(false);
const saving = ref(false);
const actionLoading = ref(false);
const previewLoading = ref(false);
const generatingAiDraft = ref(false);
const submittingItems = ref(false);
const collectionLoading = ref(false);
const questionSelectorLoading = ref(false);

const paper = ref<PaperDetailVO | null>(null);
const collectionOptions = ref<CollectionView[]>([]);
const randomPreviewRows = ref<PaperDraftItemRow[]>([]);
const aiDraftRows = ref<PaperDraftItemRow[]>([]);
const draftRows = ref<PaperDraftItemRow[]>([]);
const questionList = ref<QuestionSummary[]>([]);
const questionTotal = ref(0);
const selectedQuestions = ref<QuestionSummary[]>([]);
const activeQuestionPreviewId = ref('');
const activeQuestionPreviewDetail = ref<QuestionDetail | null>(null);
const questionPreviewLoading = ref(false);
const activeRandomPreviewKey = ref('');
const activeRandomPreviewDetail = ref<QuestionDetail | null>(null);
const randomPreviewDetailLoading = ref(false);
const activeAiPreviewKey = ref('');
const activeAiPreviewDetail = ref<QuestionDetail | null>(null);
const aiPreviewDetailLoading = ref(false);
const activeSavedPreviewQuestionId = ref('');
const activeSavedPreviewDetail = ref<QuestionDetail | null>(null);
const savedPreviewLoading = ref(false);
const savedPreviewKnowledgeTags = ref<QuestionKnowledgeTag[]>([]);
const savedPreviewKnowledgeTagsLoading = ref(false);
const draftPreviewVisible = ref(false);
const activeDraftPreviewQuestionId = ref('');
const activeDraftPreviewDetail = ref<QuestionDetail | null>(null);
const draftPreviewLoading = ref(false);

const basicInfoDrawerVisible = ref(false);
const buildMode = ref<'manual' | 'random' | 'ai'>('manual');
const workspacePanel = ref<'draft' | 'saved'>('draft');
const draftTableRef = ref();
const manualQuestionCollectionId = ref('');
const draftBatchScore = ref(5);
const activeRandomPreset = ref('');
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
const aiForm = reactive<GeneratePaperDraftReq>({
  message: '',
  collectionIds: [],
  constrains: [createAiConstrain()]
});
const aiDraftResult = ref<AgentPaperDraftVO | null>(null);
const aiDraftError = ref('');

const questionQuery = reactive<CollectionQuestionQuery>({
  pageNum: 1,
  pageSize: 20,
  keyword: '',
  typeCode: ''
});
const draftFilters = reactive<{
  source: 'all' | 'paper' | 'manual' | 'preview' | 'ai';
  typeCode: string;
}>({
  source: 'all',
  typeCode: 'all'
});

const typeOptions = [
  { label: '单选题', value: 'single-choice' },
  { label: '多选题', value: 'multiple-choice' },
  { label: '判断题', value: 'true-false' },
  { label: '填空题', value: 'fill-in' },
  { label: '简答题', value: 'short-answer' }
];
const draftTypeOrder: Record<string, number> = {
  'single-choice': 1,
  'multiple-choice': 2,
  'fill-in': 3,
  'true-false': 4,
  'short-answer': 5
};
const randomRulePresets = [
  {
    key: 'basic-practice',
    label: '基础练习',
    description: '单选 10 题 + 多选 5 题 + 判断 5 题',
    rules: [
      { typeCode: 'single-choice', count: 10, expectedDifficulty: 0.5 },
      { typeCode: 'multiple-choice', count: 5, expectedDifficulty: 0.6 },
      { typeCode: 'true-false', count: 5, expectedDifficulty: 0.4 }
    ]
  },
  {
    key: 'balanced-test',
    label: '综合测验',
    description: '单选 8 题 + 多选 4 题 + 填空 4 题 + 简答 2 题',
    rules: [
      { typeCode: 'single-choice', count: 8, expectedDifficulty: 0.5 },
      { typeCode: 'multiple-choice', count: 4, expectedDifficulty: 0.6 },
      { typeCode: 'fill-in', count: 4, expectedDifficulty: 0.6 },
      { typeCode: 'short-answer', count: 2, expectedDifficulty: 0.7 }
    ]
  },
  {
    key: 'subjective-focus',
    label: '主观强化',
    description: '填空 6 题 + 简答 4 题，适合复习巩固',
    rules: [
      { typeCode: 'fill-in', count: 6, expectedDifficulty: 0.6 },
      { typeCode: 'short-answer', count: 4, expectedDifficulty: 0.7 }
    ]
  }
] as const;

const paperId = computed(() => String(route.params.paperId ?? ''));
const ownerDisplayName = computed(() => authStore.user?.username || paper.value?.ownerId || '—');
const savedAverageDifficulty = computed(() => {
  const items = paper.value?.items ?? [];
  const valid = items.filter((item) => item.difficulty !== undefined && item.difficulty !== null);
  if (!valid.length) {
    return '—';
  }
  const total = valid.reduce((sum, item) => sum + Number(item.difficulty ?? 0), 0);
  return (total / valid.length).toFixed(2);
});
const activeQuestionAnswer = computed(() => {
  return resolveQuestionAnswer(activeQuestionPreviewDetail.value);
});
const activeRandomPreviewAnswer = computed(() => {
  return resolveQuestionAnswer(activeRandomPreviewDetail.value);
});
const activeAiPreviewAnswer = computed(() => {
  return resolveQuestionAnswer(activeAiPreviewDetail.value);
});
const activeSavedPreviewAnswer = computed(() => {
  return resolveQuestionAnswer(activeSavedPreviewDetail.value);
});
const activeDraftPreviewAnswer = computed(() => {
  return resolveQuestionAnswer(activeDraftPreviewDetail.value);
});
const filteredDraftRows = computed(() => {
  return draftRows.value.filter((row) => {
    if (draftFilters.source !== 'all' && row.source !== draftFilters.source) {
      return false;
    }
    if (draftFilters.typeCode !== 'all' && row.typeCode !== draftFilters.typeCode) {
      return false;
    }
    return true;
  });
});
const draftTotalScore = computed(() => formatScore(draftRows.value.reduce((sum, row) => sum + Number(row.score ?? 0), 0)));
const hasDraftFilters = computed(() => draftFilters.source !== 'all' || draftFilters.typeCode !== 'all');
const isDraftReorderEnabled = computed(() => !hasDraftFilters.value);
const randomRuleSummary = computed(() => {
  const validRules = randomForm.rules.filter((rule) => rule.typeCode?.trim() && Number(rule.count) > 0);
  const targetQuestions = validRules.reduce((sum, rule) => sum + Number(rule.count || 0), 0);
  return {
    selectedCollections: normalizeCollectionIds(randomForm.collectionIds).length,
    validRules: validRules.length,
    invalidRules: Math.max(randomForm.rules.length - validRules.length, 0),
    targetQuestions,
    estimatedScore: formatScore(targetQuestions)
  };
});
const canPreviewRandomBuild = computed(() => randomRuleSummary.value.selectedCollections > 0 && randomRuleSummary.value.validRules > 0);
const aiRuleSummary = computed(() => {
  const validConstrains = aiForm.constrains.filter((item) => {
    const typeCode = item.typeCode?.trim();
    const count = Number(item.count);
    const difficultyMin = Number(item.difficultyMin);
    const difficultyMax = Number(item.difficultyMax);
    return (
      Boolean(typeCode) &&
      Number.isInteger(count) &&
      count > 0 &&
      Number.isFinite(difficultyMin) &&
      Number.isFinite(difficultyMax) &&
      difficultyMin <= difficultyMax
    );
  });
  const targetQuestions = validConstrains.reduce((sum, item) => sum + Number(item.count || 0), 0);
  return {
    selectedCollections: normalizeCollectionIds(aiForm.collectionIds).length,
    validConstrains: validConstrains.length,
    targetQuestions,
    estimatedScore: formatScore(targetQuestions)
  };
});
const currentRandomPresetDescription = computed(() => {
  return randomRulePresets.find((preset) => preset.key === activeRandomPreset.value)?.description ?? '';
});

onMounted(() => {
  void loadPaper();
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
    if (!isDraftReorderEnabled.value) {
      teardownDraftRowDnD();
      return;
    }
    await nextTick();
    bindDraftRowDnD();
  },
  { deep: true }
);

watch(
  () => [draftFilters.source, draftFilters.typeCode],
  async () => {
    if (!isDraftReorderEnabled.value) {
      teardownDraftRowDnD();
      return;
    }
    await nextTick();
    bindDraftRowDnD();
  }
);

watch(
  () => paper.value?.items,
  () => {
    syncSavedPreviewSelection();
  },
  { deep: true }
);

watch(draftPreviewVisible, (visible) => {
  if (visible) {
    return;
  }
  activeDraftPreviewQuestionId.value = '';
  activeDraftPreviewDetail.value = null;
});

watch(workspacePanel, async (panel) => {
  if (panel === 'saved') {
    syncSavedPreviewSelection();
    return;
  }
  if (panel !== 'draft' || !isDraftReorderEnabled.value) {
    return;
  }
  await nextTick();
  bindDraftRowDnD();
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

async function ensureCollectionsLoaded() {
  if (collectionOptions.value.length) {
    return;
  }
  await loadCollections();
}

function syncEditForm() {
  editForm.title = paper.value?.title || '';
  editForm.description = paper.value?.description || '';
}

async function openManualBuildDrawer() {
  buildMode.value = 'manual';
  await ensureCollectionsLoaded();
  if (!manualQuestionCollectionId.value && collectionOptions.value.length) {
    manualQuestionCollectionId.value = collectionOptions.value[0].collectionId;
  }
  if (!manualQuestionCollectionId.value) {
    questionList.value = [];
    questionTotal.value = 0;
    activeQuestionPreviewId.value = '';
    activeQuestionPreviewDetail.value = null;
    return;
  }
  questionQuery.pageNum = 1;
  await loadQuestions();
}

async function openRandomBuildDrawer() {
  buildMode.value = 'random';
  await ensureCollectionsLoaded();
}

async function openAiBuildDrawer() {
  buildMode.value = 'ai';
  await ensureCollectionsLoaded();
}

function openBasicInfoDrawer() {
  syncEditForm();
  basicInfoDrawerVisible.value = true;
}

function syncDraftRowsFromPaper() {
  if (!paper.value?.items?.length) {
    draftRows.value = [];
    activeSavedPreviewQuestionId.value = '';
    activeSavedPreviewDetail.value = null;
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

  syncSavedPreviewSelection();
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
    basicInfoDrawerVisible.value = false;
  } catch (error) {
    showError(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function handlePreviewRandomBuild(options: { silentValidation?: boolean; silentSuccess?: boolean } = {}) {
  if (!paperId.value) {
    return;
  }

  const collectionIds = normalizeCollectionIds(randomForm.collectionIds);
  if (!collectionIds.length) {
    if (!options.silentValidation) {
      showError('请先选择题集');
    }
    return;
  }

  const rules = normalizeRandomRules(randomForm.rules);
  if (!rules.length) {
    if (!options.silentValidation) {
      showError('请至少配置一条有效规则');
    }
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
    syncRandomPreviewSelection();
    if (!options.silentSuccess) {
      showSuccess(`已生成 ${randomPreviewRows.value.length} 道候选题`);
    }
  } catch (error) {
    if (!options.silentValidation) {
      showError(error instanceof Error ? error.message : '随机预览失败');
    }
  } finally {
    previewLoading.value = false;
  }
}

function clearAiDraftState() {
  aiDraftRows.value = [];
  aiDraftResult.value = null;
  aiDraftError.value = '';
  activeAiPreviewKey.value = '';
  activeAiPreviewDetail.value = null;
}

function handleAiCollectionChange() {
  clearAiDraftState();
}

function handleAiConstraintFieldChange() {
  clearAiDraftState();
}

function addAiConstrain() {
  aiForm.constrains.push(createAiConstrain());
  clearAiDraftState();
}

function removeAiConstrain(index: number) {
  if (aiForm.constrains.length === 1) {
    return;
  }
  aiForm.constrains.splice(index, 1);
  clearAiDraftState();
}

function normalizeAiConstrains(constrains: GeneratePaperDraftBucketConstrain[]) {
  return constrains.map((item, index) => {
    const typeCode = item.typeCode?.trim() || '';
    const count = Number(item.count);
    const difficultyMin = Number(item.difficultyMin);
    const difficultyMax = Number(item.difficultyMax);
    const prefix = `第 ${index + 1} 条约束`;

    if (!typeCode) {
      throw new Error(`${prefix}缺少题型`);
    }
    if (!Number.isInteger(count) || count <= 0) {
      throw new Error(`${prefix}的题数必须是大于 0 的整数`);
    }
    if (!Number.isFinite(difficultyMin) || !Number.isFinite(difficultyMax)) {
      throw new Error(`${prefix}缺少难度范围`);
    }
    if (difficultyMin > difficultyMax) {
      throw new Error(`${prefix}的难度下限不能大于上限`);
    }

    return {
      typeCode,
      count,
      difficultyMin,
      difficultyMax
    };
  });
}

async function handleGenerateAiPaperDraft() {
  const message = aiForm.message.trim();
  if (!message) {
    showError('请先填写 AI 组卷需求');
    return;
  }

  const collectionIds = normalizeCollectionIds(aiForm.collectionIds);
  if (!collectionIds.length) {
    showError('请至少选择一个题集');
    return;
  }

  let constrains: GeneratePaperDraftBucketConstrain[];
  try {
    constrains = normalizeAiConstrains(aiForm.constrains);
  } catch (error) {
    showError(error instanceof Error ? error.message : 'AI 组卷参数校验失败');
    return;
  }

  generatingAiDraft.value = true;
  aiDraftError.value = '';
  aiDraftRows.value = [];
  try {
    const payload: GeneratePaperDraftReq = {
      message,
      collectionIds,
      constrains
    };
    const result = await generateAgentPaperDraft(payload);
    aiDraftResult.value = result;
    aiDraftRows.value = result.candidateQuestions.map((item) => ({
      key: createRowKey(String(item.questionId), String(item.questionVersionId)),
      questionId: String(item.questionId),
      questionVersionId: String(item.questionVersionId),
      score: 1,
      typeCode: item.typeCode,
      difficulty: item.difficulty,
      questionTitle: item.title,
      stem: '',
      source: 'ai'
    }));
    syncAiPreviewSelection();
    showSuccess(`AI 已生成 ${aiDraftRows.value.length} 道候选题`);
  } catch (error) {
    aiDraftResult.value = null;
    aiDraftError.value = error instanceof Error ? error.message : 'AI 组卷失败';
    showError(aiDraftError.value);
  } finally {
    generatingAiDraft.value = false;
  }
}

function fillDraftItemsFromAi() {
  if (!aiDraftRows.value.length) {
    showError('请先生成 AI 组卷结果');
    return;
  }

  const nonAiRows = draftRows.value.filter((row) => row.source !== 'ai');
  draftRows.value = [
    ...nonAiRows,
    ...aiDraftRows.value.map((row) => ({
      key: createRowKey(row.questionId, row.questionVersionId),
      questionId: row.questionId,
      questionVersionId: row.questionVersionId,
      score: row.score,
      typeCode: row.typeCode,
      difficulty: row.difficulty,
      questionTitle: row.questionTitle,
      stem: row.stem,
      source: 'ai' as const
    }))
  ];
  workspacePanel.value = 'draft';
  showSuccess('AI 候选题已填充到草稿区域');
}

function syncAiPreviewSelection() {
  const nextActive =
    aiDraftRows.value.find((row) => row.key === activeAiPreviewKey.value) ??
    aiDraftRows.value[0] ??
    null;
  if (!nextActive) {
    activeAiPreviewKey.value = '';
    activeAiPreviewDetail.value = null;
    return;
  }
  activeAiPreviewKey.value = nextActive.key;
  void loadAiPreviewDetail(nextActive.questionId);
}

function handleAiPreviewCurrentChange(currentRow?: PaperDraftItemRow | null) {
  if (!currentRow?.key) {
    return;
  }
  activeAiPreviewKey.value = currentRow.key;
  void loadAiPreviewDetail(currentRow.questionId);
}

function handleAiPreviewRowClick(row: PaperDraftItemRow) {
  activeAiPreviewKey.value = row.key;
  void loadAiPreviewDetail(row.questionId);
}

async function loadAiPreviewDetail(questionId: string) {
  if (!questionId) {
    activeAiPreviewDetail.value = null;
    return;
  }
  if (aiPreviewDetailLoading.value) {
    return;
  }
  if (activeAiPreviewDetail.value?.id === questionId) {
    return;
  }
  aiPreviewDetailLoading.value = true;
  try {
    activeAiPreviewDetail.value = await fetchQuestionDetail(questionId);
  } catch (error) {
    activeAiPreviewDetail.value = null;
    showError(error instanceof Error ? error.message : '加载题目详情失败');
  } finally {
    aiPreviewDetailLoading.value = false;
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
  workspacePanel.value = 'draft';
  showSuccess('已填充到草稿区域');
}

function addRandomRule() {
  handleRandomRuleFieldChange();
  randomForm.rules.push(createRandomRule());
}

function removeRandomRule(index: number) {
  if (randomForm.rules.length === 1) {
    return;
  }
  handleRandomRuleFieldChange();
  randomForm.rules.splice(index, 1);
}

function removeDraftRow(index: number) {
  draftRows.value.splice(index, 1);
}

function sortDraftRowsByType() {
  if (hasDraftFilters.value) {
    showError('请先重置筛选后再整理顺序');
    return;
  }
  if (draftRows.value.length < 2) {
    return;
  }

  draftRows.value = draftRows.value
    .map((row, index) => ({ row, index }))
    .sort((a, b) => {
      const orderA = a.row.typeCode ? draftTypeOrder[a.row.typeCode] ?? Number.MAX_SAFE_INTEGER : Number.MAX_SAFE_INTEGER;
      const orderB = b.row.typeCode ? draftTypeOrder[b.row.typeCode] ?? Number.MAX_SAFE_INTEGER : Number.MAX_SAFE_INTEGER;
      if (orderA !== orderB) {
        return orderA - orderB;
      }
      return a.index - b.index;
    })
    .map((item) => item.row);

  showSuccess('已按题型整理草稿顺序');
}

function clearDraftRows() {
  draftRows.value = [];
}

function resetDraftFilters() {
  draftFilters.source = 'all';
  draftFilters.typeCode = 'all';
}

function clearRandomPreviewState() {
  randomPreviewRows.value = [];
  activeRandomPreviewKey.value = '';
  activeRandomPreviewDetail.value = null;
}

function syncRandomPreviewSelection() {
  const nextActive =
    randomPreviewRows.value.find((row) => row.key === activeRandomPreviewKey.value) ??
    randomPreviewRows.value[0] ??
    null;
  if (!nextActive) {
    activeRandomPreviewKey.value = '';
    activeRandomPreviewDetail.value = null;
    return;
  }
  activeRandomPreviewKey.value = nextActive.key;
  void loadRandomPreviewDetail(nextActive.questionId);
}

function handleRandomPreviewCurrentChange(currentRow?: PaperDraftItemRow | null) {
  if (!currentRow?.key) {
    return;
  }
  activeRandomPreviewKey.value = currentRow.key;
  void loadRandomPreviewDetail(currentRow.questionId);
}

function handleRandomPreviewRowClick(row: PaperDraftItemRow) {
  activeRandomPreviewKey.value = row.key;
  void loadRandomPreviewDetail(row.questionId);
}

async function loadRandomPreviewDetail(questionId: string) {
  if (!questionId) {
    activeRandomPreviewDetail.value = null;
    return;
  }
  if (randomPreviewDetailLoading.value) {
    return;
  }
  if (activeRandomPreviewDetail.value?.id === questionId) {
    return;
  }
  randomPreviewDetailLoading.value = true;
  try {
    activeRandomPreviewDetail.value = await fetchQuestionDetail(questionId);
  } catch (error) {
    activeRandomPreviewDetail.value = null;
    showError(error instanceof Error ? error.message : '加载题目详情失败');
  } finally {
    randomPreviewDetailLoading.value = false;
  }
}

function handleRandomCollectionChange() {
  clearRandomPreviewState();
}

function handleRandomRuleFieldChange() {
  activeRandomPreset.value = '';
  clearRandomPreviewState();
}

function applyRandomRulePreset(presetKey: string) {
  const preset = randomRulePresets.find((item) => item.key === presetKey);
  if (!preset) {
    return;
  }
  randomForm.rules = preset.rules.map((rule) => ({
    typeCode: rule.typeCode,
    count: rule.count,
    expectedDifficulty: rule.expectedDifficulty
  }));
  activeRandomPreset.value = preset.key;
  clearRandomPreviewState();
}

function resetRandomRules() {
  randomForm.rules = [createRandomRule()];
  activeRandomPreset.value = '';
  clearRandomPreviewState();
}

function applyBatchScoreToFilteredRows() {
  const targetScore = Number(draftBatchScore.value);
  if (!Number.isFinite(targetScore) || targetScore < 0.5) {
    showError('请填写有效分值，且不能小于 0.5');
    return;
  }
  if (!filteredDraftRows.value.length) {
    showError('当前没有可批量修改的草稿题目');
    return;
  }
  const matchedKeys = new Set(filteredDraftRows.value.map((row) => row.key));
  draftRows.value = draftRows.value.map((row) => {
    if (!matchedKeys.has(row.key)) {
      return row;
    }
    return {
      ...row,
      score: Number(targetScore.toFixed(2))
    };
  });
  showSuccess(`已将 ${filteredDraftRows.value.length} 道题目的分值更新为 ${targetScore.toFixed(2)}`);
}

async function handleReplaceDraftItem(row: PaperDraftItemRow, index: number) {
  if (!paperId.value) {
    return;
  }

  const collectionIds = resolveReplaceCollectionIds(row.source);
  if (!collectionIds.length) {
    showError(row.source === 'ai' ? '请先为 AI 组卷选择题集' : '请先选择题集');
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
      source: row.source === 'ai' ? 'ai' : 'preview',
      replacing: false
    };
    showSuccess('已替换题目');
  } catch (error) {
    row.replacing = false;
    showError(error instanceof Error ? error.message : '换一题失败');
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
    if (activeRandomPreviewKey.value === row.key) {
      activeRandomPreviewKey.value = randomPreviewRows.value[index].key;
      void loadRandomPreviewDetail(randomPreviewRows.value[index].questionId);
    }
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
    const nextActive = result.records.find((item) => item.id === activeQuestionPreviewId.value) ?? result.records[0];
    if (!nextActive) {
      activeQuestionPreviewId.value = '';
      activeQuestionPreviewDetail.value = null;
      return;
    }
    activeQuestionPreviewId.value = nextActive.id;
    void loadQuestionPreviewDetail(nextActive.id);
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
  activeQuestionPreviewId.value = '';
  activeQuestionPreviewDetail.value = null;
}

function handleQuestionSelectionChange(selection: QuestionSummary[]) {
  selectedQuestions.value = selection;
}

function handleQuestionCurrentChange(currentRow?: QuestionSummary | null) {
  if (!currentRow?.id) {
    return;
  }
  activeQuestionPreviewId.value = currentRow.id;
  void loadQuestionPreviewDetail(currentRow.id);
}

function handleQuestionRowClick(row: QuestionSummary) {
  activeQuestionPreviewId.value = row.id;
  void loadQuestionPreviewDetail(row.id);
}

function syncSavedPreviewSelection() {
  const items = paper.value?.items ?? [];
  const nextActive =
    items.find((item) => item.questionId === activeSavedPreviewQuestionId.value) ??
    items[0] ??
    null;

  if (!nextActive) {
    activeSavedPreviewQuestionId.value = '';
    activeSavedPreviewDetail.value = null;
    savedPreviewKnowledgeTags.value = [];
    return;
  }

  activeSavedPreviewQuestionId.value = nextActive.questionId;
  void loadSavedPreviewDetail(nextActive.questionId, nextActive.questionVersionId);
}

function handleSavedPreviewCurrentChange(currentRow?: PaperItemVO | null) {
  if (!currentRow?.questionId) {
    return;
  }
  activeSavedPreviewQuestionId.value = currentRow.questionId;
  void loadSavedPreviewDetail(currentRow.questionId, currentRow.questionVersionId);
}

function handleSavedPreviewRowClick(row: PaperItemVO) {
  activeSavedPreviewQuestionId.value = row.questionId;
  void loadSavedPreviewDetail(row.questionId, row.questionVersionId);
}

function openDraftPreview(row: PaperDraftItemRow) {
  draftPreviewVisible.value = true;
  activeDraftPreviewQuestionId.value = row.questionId;
  void loadDraftPreviewDetail(row.questionId);
}

function handleDraftAction(command: string | number | object, row: PaperDraftItemRow, index: number) {
  if (command === 'replace') {
    void handleReplaceDraftItem(row, index);
    return;
  }
  if (command === 'delete') {
    removeDraftRow(index);
  }
}

async function loadDraftPreviewDetail(questionId: string) {
  if (!questionId) {
    activeDraftPreviewDetail.value = null;
    return;
  }
  if (draftPreviewLoading.value) {
    return;
  }
  if (activeDraftPreviewDetail.value?.id === questionId) {
    return;
  }
  draftPreviewLoading.value = true;
  try {
    activeDraftPreviewDetail.value = await fetchQuestionDetail(questionId);
  } catch (error) {
    activeDraftPreviewDetail.value = null;
    showError(error instanceof Error ? error.message : '加载题目详情失败');
  } finally {
    draftPreviewLoading.value = false;
  }
}

async function loadSavedPreviewDetail(questionId: string, questionVersionId?: string) {
  if (!questionId) {
    activeSavedPreviewDetail.value = null;
    savedPreviewKnowledgeTags.value = [];
    return;
  }
  if (savedPreviewLoading.value) {
    return;
  }
  if (activeSavedPreviewDetail.value?.id === questionId) {
    return;
  }
  savedPreviewLoading.value = true;
  try {
    activeSavedPreviewDetail.value = await fetchQuestionDetail(questionId);
    await loadSavedPreviewKnowledgeTags(questionVersionId || activeSavedPreviewDetail.value.currentVersionId);
  } catch (error) {
    activeSavedPreviewDetail.value = null;
    savedPreviewKnowledgeTags.value = [];
    showError(error instanceof Error ? error.message : '加载题目详情失败');
  } finally {
    savedPreviewLoading.value = false;
  }
}

async function loadSavedPreviewKnowledgeTags(questionVersionId?: string | null) {
  if (!questionVersionId) {
    savedPreviewKnowledgeTags.value = [];
    return;
  }
  savedPreviewKnowledgeTagsLoading.value = true;
  try {
    savedPreviewKnowledgeTags.value = await fetchQuestionKnowledgeTags(questionVersionId);
  } catch {
    savedPreviewKnowledgeTags.value = [];
  } finally {
    savedPreviewKnowledgeTagsLoading.value = false;
  }
}

async function loadQuestionPreviewDetail(questionId: string) {
  if (!questionId) {
    activeQuestionPreviewDetail.value = null;
    return;
  }
  if (questionPreviewLoading.value) {
    return;
  }
  if (activeQuestionPreviewDetail.value?.id === questionId) {
    return;
  }
  questionPreviewLoading.value = true;
  try {
    activeQuestionPreviewDetail.value = await fetchQuestionDetail(questionId);
  } catch (error) {
    activeQuestionPreviewDetail.value = null;
    showError(error instanceof Error ? error.message : '加载题目详情失败');
  } finally {
    questionPreviewLoading.value = false;
  }
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
    workspacePanel.value = 'draft';
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

function goPaperBuild(mode: 'manual' | 'random' | 'ai') {
  if (!paperId.value) {
    return;
  }
  void router.push({
    name: 'paper-build',
    params: { paperId: paperId.value },
    query: { mode }
  });
}

function handleBuildEntryCommand(command: string | number | object) {
  if (command === 'manual' || command === 'random' || command === 'ai') {
    goPaperBuild(command);
  }
}

function goPrintLayout() {
  if (!paperId.value) {
    return;
  }
  void router.push({ name: 'paper-print', params: { paperId: paperId.value } });
}

function draftSourceLabel(source?: PaperDraftItemRow['source']) {
  switch (source) {
    case 'ai':
      return 'AI';
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
    case 'ai':
      return 'primary';
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

function formatDraftStemPreview(stem?: string | null) {
  if (!stem) {
    return '—';
  }

  const normalized = stem
    .replace(/\$\$[\s\S]+?\$\$/g, ' [公式] ')
    .replace(/\$[^$\n]+\$/g, ' [公式] ')
    .replace(/\\\[[\s\S]+?\\\]/g, ' [公式] ')
    .replace(/\\\([\s\S]+?\\\)/g, ' [公式] ')
    .replace(/\s+/g, ' ')
    .trim();

  if (!normalized) {
    return '—';
  }

  return normalized.length > 28 ? `${normalized.slice(0, 28)}...` : normalized;
}

function formatDraftStemTooltip(stem?: string | null) {
  if (!stem) {
    return '—';
  }
  return stem.replace(/\s+/g, ' ').trim() || '—';
}

function resolveQuestionAnswer(detail?: QuestionDetail | null) {
  if (!detail) {
    return '暂无答案';
  }
  if (detail.typeCode === 'true-false') {
    if (detail.judgeAnswer === 'T') {
      return '正确';
    }
    if (detail.judgeAnswer === 'F') {
      return '错误';
    }
  }
  if (detail.correctOptions?.length) {
    return detail.correctOptions.join(', ');
  }
  return detail.answer || '暂无答案';
}

function questionTypeLabel(typeCode?: string | null) {
  return typeOptions.find((option) => option.value === typeCode)?.label ?? typeCode ?? '未知题型';
}

function paperItemTypeTagType(typeCode?: string | null) {
  switch (typeCode) {
    case 'single-choice':
      return 'primary';
    case 'multiple-choice':
      return 'success';
    case 'fill-in':
      return 'warning';
    case 'true-false':
      return 'info';
    case 'short-answer':
      return 'danger';
    default:
      return 'info';
  }
}

function paperItemTypeTagClass(typeCode?: string | null) {
  return `paper-item-type-tag--${typeCode ?? 'default'}`;
}

function normalizeCollectionIds(values: string[]) {
  return values.map((value) => value?.trim()).filter((value): value is string => Boolean(value));
}

function resolveReplaceCollectionIds(source?: PaperDraftItemRow['source']) {
  if (source === 'ai') {
    return normalizeCollectionIds(aiForm.collectionIds);
  }
  return normalizeCollectionIds(randomForm.collectionIds);
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

function createAiConstrain(): GeneratePaperDraftBucketConstrain {
  return {
    typeCode: 'single-choice',
    count: 5,
    difficultyMin: 0,
    difficultyMax: 1
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
  if (!isDraftReorderEnabled.value) {
    return;
  }

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

.back-link-button {
  padding-left: 0;
  color: var(--el-text-color-secondary);
}

.toolbar-actions-compact .el-button {
  --el-button-size: 34px;
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

.paper-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.paper-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.paper-build-entry-btn {
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border-color: #d7e6fb;
  color: var(--el-color-primary);
  background: #f7fbff;
}

.paper-build-entry-btn:hover {
  border-color: #b8d6fb;
  background: #eef6ff;
}

.paper-edit-icon-btn {
  border-color: #e7edf5;
  color: var(--el-text-color-secondary);
  background: #fff;
}

.paper-edit-icon-btn:hover {
  color: var(--el-color-primary);
  border-color: #d7e6fb;
  background: #f7fbff;
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

.ai-form-item-narrow {
  max-width: 100%;
}

.paper-preview-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.86fr) minmax(0, 1.24fr);
  gap: 16px;
  align-items: start;
}

.paper-preview-grid--single {
  grid-template-columns: minmax(0, 1fr);
}

.info-card,
.workspace-shell-card {
  border-radius: 18px;
  border-color: #edf1f6;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.paper-summary-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  padding: 2px 0;
}

.paper-summary-inline {
  min-width: 0;
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
  padding-right: 18px;
  position: relative;
}

.paper-summary-inline::after {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  width: 1px;
  height: 16px;
  transform: translateY(-50%);
  background: var(--el-border-color-lighter);
}

.paper-summary-inline:last-child::after {
  display: none;
}

.paper-summary-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.paper-summary-value {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.25;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.paper-items-card {
  border-radius: 18px;
  border-color: #edf1f6;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.paper-items-card :deep(.el-card__body) {
  padding-top: 12px;
}

.paper-items-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 540px;
}

.paper-items-list-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 8px 12px 14px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  font-weight: 700;
}

.paper-item-row {
  width: 100%;
  border: 0;
  border-top: 1px solid #f1f4f8;
  border-radius: 0;
  background: #fff;
  padding: 18px 12px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

.paper-item-row:hover {
  background: #f8fbff;
}

.paper-item-row.active {
  background: #f4f8ff;
  box-shadow: inset 3px 0 0 var(--el-color-primary);
}

.paper-item-row-main {
  min-width: 0;
  display: flex;
  align-items: center;
}

.paper-item-row-title {
  color: var(--el-text-color-primary);
  font-size: 16px;
  line-height: 1.5;
  font-weight: 500;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.paper-item-type-tag {
  flex: 0 0 auto;
  min-width: 72px;
  justify-content: center;
  font-weight: 600;
  border-radius: 10px;
}

.paper-detail-preview-card :deep(.el-card__body) {
  padding-top: 12px;
}

.paper-detail-preview-body {
  min-height: 540px;
}

.paper-detail-preview-head {
  margin-bottom: 22px;
}

.builder-workspace {
  min-width: 0;
}

.workspace-shell-card :deep(.el-card__body) {
  padding: 0;
}

.workspace-shell {
  display: grid;
  grid-template-columns: 176px minmax(0, 1fr);
  min-height: 760px;
}

.mode-sidebar {
  background: #fff;
  border-right: 1px solid #eef2f7;
  padding: 22px 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mode-sidebar-head {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 18px 8px;
}

.mode-sidebar-head h3 {
  margin: 0;
  font-size: 16px;
}

.mode-sidebar-badge {
  font-size: 12px;
  line-height: 1.4;
  color: var(--el-text-color-secondary);
}

.workspace-main-pane {
  min-width: 0;
  padding: 22px 28px 28px;
}

.workspace-shell-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 18px;
  margin-bottom: 16px;
  border-bottom: 1px solid #eef2f7;
}

.workspace-shell-head h3 {
  margin: 0;
}

.mode-launcher-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mode-launcher-btn {
  position: relative;
  border: 0;
  border-radius: 0 14px 14px 0;
  background: transparent;
  padding: 12px 14px 12px 18px;
  margin-right: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.mode-launcher-btn:hover {
  background: #f7faff;
}

.mode-launcher-btn.active {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.12), rgba(64, 158, 255, 0.02));
  box-shadow: none;
}

.mode-launcher-accent {
  width: 3px;
  align-self: stretch;
  border-radius: 999px;
  background: transparent;
  flex: 0 0 3px;
}

.mode-launcher-btn.active .mode-launcher-accent {
  background: var(--el-color-primary);
}

.mode-launcher-content {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mode-launcher-content strong {
  font-size: 13px;
  font-weight: 700;
  line-height: 1.3;
  color: var(--el-text-color-primary);
}

.mode-launcher-content span {
  font-size: 11px;
  line-height: 1.4;
  color: var(--el-text-color-placeholder);
}

.mode-launcher-arrow {
  font-size: 14px;
  line-height: 1;
  color: var(--el-text-color-placeholder);
  flex: 0 0 auto;
  opacity: 0.75;
}

.mode-launcher-btn.active .mode-launcher-arrow {
  color: var(--el-color-primary);
  opacity: 1;
}

.workspace-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.mode-panel,
.workspace-tabs {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mode-section {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 18px;
}

.workspace-tabs :deep(.el-tabs__header) {
  margin-bottom: 10px;
}

.workspace-tabs :deep(.el-tabs__nav-wrap)::after {
  display: none;
}

.workspace-tabs :deep(.el-tabs__nav-scroll) {
  width: 100%;
}

.workspace-tabs :deep(.el-tabs__nav) {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  float: none;
}

.workspace-tabs :deep(.el-tabs__active-bar) {
  display: none;
}

.workspace-tabs :deep(.el-tabs__item) {
  height: auto;
  padding: 0 !important;
  color: inherit;
}

.workspace-tab-label {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: #fff;
  padding: 10px 12px;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.workspace-tabs :deep(.el-tabs__item.is-active) .workspace-tab-label {
  border-color: var(--el-color-primary);
  background: linear-gradient(90deg, var(--el-color-primary-light-9), #ffffff 55%);
  box-shadow: inset 0 0 0 1px rgba(64, 158, 255, 0.08);
}

.workspace-tab-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.workspace-tab-count {
  min-width: 26px;
  height: 22px;
  border-radius: 999px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 22px;
  text-align: center;
  padding: 0 8px;
  box-sizing: border-box;
}

.workspace-tabs :deep(.el-tabs__item.is-active) .workspace-tab-count {
  background: var(--el-color-primary);
  color: #fff;
}

.workspace-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 0;
}

.workspace-meta-item {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: #f7f9fc;
  border-radius: 999px;
  padding: 4px 10px;
}

.draft-workspace-meta {
  margin-top: 6px;
  margin-bottom: 16px;
  gap: 6px;
}

.draft-workspace-meta .workspace-meta-item {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
  padding: 3px 8px;
}

.mode-panel-head,
.workspace-panel-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.mode-panel-head h4,
.workspace-panel-head h4 {
  margin: 0;
}

.workspace-panel-head .hint {
  margin-top: 2px;
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

.rule-preset-strip {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rule-preset-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rule-row {
  display: grid;
  grid-template-columns: minmax(220px, 280px) 118px 164px 96px;
  gap: 18px;
  align-items: center;
  padding: 12px 14px;
  border: 1px solid #eef2f7;
  border-radius: 16px;
  background: #ffffff;
}

.rule-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.rule-type-cell {
  min-width: 0;
}

.rule-delete-wrap {
  align-self: stretch;
  justify-content: flex-end;
}

.rule-delete-button {
  width: 96px;
}

.rule-row :deep(.el-select),
.rule-row :deep(.el-input-number) {
  width: 100%;
}

.rule-row :deep(.el-input-number .el-input__wrapper),
.rule-row :deep(.el-select__wrapper) {
  min-height: 38px;
}

.ai-constrain-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.constraint-tip-card {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  padding: 10px 12px;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #fbfcfd;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.ai-constrain-row {
  display: grid;
  grid-template-columns: minmax(220px, 280px) 118px 144px 144px 96px;
  gap: 18px;
  align-items: center;
  padding: 12px 14px;
  border: 1px solid #eef2f7;
  border-radius: 16px;
  background: #ffffff;
}

.ai-constrain-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.ai-constrain-type-cell {
  min-width: 0;
}

.ai-constrain-mini-label {
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  color: var(--el-text-color-placeholder);
  letter-spacing: 0.02em;
}

.ai-constrain-delete-wrap {
  align-self: stretch;
  justify-content: flex-end;
}

.ai-constrain-delete {
  width: 96px;
}

.ai-constrain-row :deep(.el-select),
.ai-constrain-row :deep(.el-input-number) {
  width: 100%;
}

.ai-constrain-row :deep(.el-input-number .el-input__wrapper),
.ai-constrain-row :deep(.el-select__wrapper) {
  min-height: 38px;
}

.reason-card {
  border-radius: 12px;
}

.reason-text {
  margin: 0;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.table-panel {
  border: 1px solid #edf1f6;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}

.table-panel :deep(.el-table th.el-table__cell) {
  padding: 9px 0;
  background: #fafbfd;
}

.table-panel :deep(.el-table td.el-table__cell) {
  padding: 8px 0;
}

.table-panel :deep(.el-table .cell) {
  line-height: 1.45;
}

.table-panel-mid {
  max-height: 420px;
  overflow: auto;
}

.table-panel-tall {
  max-height: calc(100vh - 280px);
  overflow: auto;
}

.compact-empty {
  padding: 10px 0 4px;
}

.compact-empty :deep(.el-empty__image) {
  width: 72px;
}

.compact-empty :deep(.el-empty__description) {
  margin-top: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.row-expand {
  padding: 4px 8px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.expand-label {
  margin: 0 0 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.section-subtitle {
  font-weight: 600;
}

.stem-preview {
  display: -webkit-box;
  line-height: 1.5;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: normal;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.pagination-container {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.question-selector {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.selector-toolbar-card {
  border: 1px solid #eef2f7;
  border-radius: 14px;
  background: #fbfcfd;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.selector-toolbar-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1.3fr) minmax(160px, 0.8fr) minmax(220px, 1fr) auto;
  gap: 12px;
  align-items: end;
}

.selector-toolbar-grid-manual {
  grid-template-columns: minmax(220px, 0.85fr) minmax(260px, 1.15fr) auto;
  grid-template-areas:
    'collection collection collection'
    'type keyword actions';
  align-items: start;
}

.selector-tool-field {
  margin-bottom: 0;
}

.selector-tool-field-collection {
  grid-area: collection;
}

.selector-tool-field-type {
  grid-area: type;
}

.selector-tool-field-keyword {
  grid-area: keyword;
}

.selector-tool-field :deep(.el-form-item__label) {
  font-size: 11px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: 0.02em;
  color: var(--el-text-color-placeholder);
  padding-bottom: 6px;
}

.selector-tool-field :deep(.el-select),
.selector-tool-field :deep(.el-input) {
  width: 100%;
}

.selector-toolbar-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-bottom: 2px;
}

.selector-toolbar-actions-manual {
  grid-area: actions;
  align-self: end;
  padding-bottom: 0;
}

.selector-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 12px;
  min-height: 560px;
}

.selector-list-pane,
.selector-detail-pane {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.selector-pane-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.selector-table-panel {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.selector-detail-card {
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: #fff;
  padding: 14px;
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.manual-selector-workspace {
  min-height: 600px;
  gap: 16px;
  grid-template-columns: minmax(0, 0.94fr) minmax(0, 1.06fr);
  align-items: stretch;
}

.manual-selector-workspace .selector-pane-head {
  min-height: 54px;
}

.manual-selector-list-pane,
.manual-selector-detail-pane {
  min-height: calc(100vh - 300px);
  height: calc(100vh - 300px);
}

.manual-selector-table-panel {
  height: calc(100vh - 380px);
  max-height: calc(100vh - 380px);
}

.manual-selector-pagination {
  margin-top: 10px;
  flex-shrink: 0;
}

.manual-selector-detail-card {
  height: calc(100vh - 380px);
  min-height: calc(100vh - 380px);
}

.saved-preview-workspace {
  min-height: 560px;
  gap: 16px;
}

.saved-table-panel {
  max-height: calc(100vh - 360px);
}

.saved-preview-detail-card {
  min-height: calc(100vh - 360px);
}

.ai-preview-workspace {
  min-height: 520px;
  gap: 16px;
  grid-template-columns: minmax(0, 0.82fr) minmax(0, 1.18fr);
  align-items: stretch;
}

.random-preview-workspace {
  min-height: 520px;
  gap: 16px;
  align-items: stretch;
}

.random-preview-workspace .selector-pane-head {
  min-height: 54px;
}

.random-preview-table-panel {
  height: calc(100vh - 420px);
  max-height: calc(100vh - 420px);
}

.random-preview-detail-card {
  height: calc(100vh - 420px);
  min-height: calc(100vh - 420px);
}

.ai-preview-workspace .selector-pane-head {
  min-height: 54px;
}

.ai-preview-table-panel {
  height: calc(100vh - 380px);
  max-height: calc(100vh - 380px);
}

.ai-preview-detail-card {
  height: calc(100vh - 380px);
  min-height: calc(100vh - 380px);
}

.selector-detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.selector-detail-head h4 {
  margin: 0;
}

.knowledge-tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.knowledge-tag-row--compact {
  margin-top: 12px;
}

.knowledge-tag-chip {
  border-radius: 999px;
  font-weight: 600;
}

.knowledge-tag-chip--main {
  color: #1d4ed8;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.knowledge-tag-chip--secondary {
  color: #475569;
  background: #f8fafc;
  border-color: #e2e8f0;
}

.selector-detail-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.draft-tools-card {
  border: 1px solid #eef2f7;
  border-radius: 14px;
  background: #fbfcfd;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.draft-tools-card-spaced {
  margin-bottom: 16px;
}

.draft-tools-inline {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.draft-tools-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 12px;
  align-items: end;
  flex: 1;
}

.draft-tool-field {
  margin-bottom: 0;
}

.draft-tool-field :deep(.el-form-item__label) {
  font-size: 11px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: 0.02em;
  color: var(--el-text-color-placeholder);
  padding-bottom: 6px;
}

.draft-tool-field :deep(.el-input-number),
.draft-tool-field :deep(.el-select) {
  width: 100%;
}

.draft-tool-field :deep(.el-select__wrapper),
.draft-tool-field :deep(.el-input__wrapper) {
  min-height: 38px;
  border-radius: 10px;
  background: #fff;
}

.draft-tool-field :deep(.el-input-number .el-input__wrapper) {
  min-height: 38px;
  border-radius: 10px;
}

.draft-tools-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.draft-tools-inline-actions {
  flex: 0 0 auto;
  padding-bottom: 2px;
}

.draft-tools-hint {
  display: block;
  margin-top: -2px;
  color: var(--el-text-color-placeholder);
}

.draft-table-panel {
  margin-top: 2px;
}

.table-title-text,
.table-stem-text {
  display: -webkit-box;
  line-height: 1.45;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  white-space: normal;
}

.table-title-text {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.table-title-text-single,
.table-stem-text-single {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-stem-text {
  color: var(--el-text-color-regular);
}

.draft-preview-trigger {
  width: 100%;
  display: block;
  padding: 0;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.draft-preview-trigger:hover .table-title-text,
.draft-preview-trigger:hover .table-stem-text {
  color: var(--el-color-primary);
}

.draft-stem-trigger {
  width: 100%;
  overflow: hidden;
}

.table-stem-math-single {
  display: block;
  width: 100%;
  overflow: hidden;
}

.table-stem-math-single :deep(.math-view) {
  background: transparent;
  padding: 0;
  border-radius: 0;
  line-height: 1.45;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.table-stem-math-single :deep(.MathJax),
.table-stem-math-single :deep(mjx-container) {
  display: inline !important;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
}

.draft-action-trigger {
  width: 28px;
  height: 28px;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.draft-action-trigger span {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
}

.draft-action-trigger:hover {
  background: #f5f7fa;
  color: var(--el-color-primary);
}

.draft-organize-wrap {
  display: inline-flex;
  align-items: center;
}

.draft-head-main {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.draft-organize-btn {
  min-width: 56px;
  height: 32px;
  padding: 0 12px;
  border-color: #e7edf5;
  color: var(--el-text-color-secondary);
  background: #fff;
  border-radius: 8px;
}

.draft-organize-btn:hover {
  color: var(--el-color-primary);
  border-color: #d7e6fb;
  background: #f7fbff;
}

.draft-organize-inline-btn {
  font-weight: 500;
}

.detail-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.detail-option-list {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-option-list li {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.detail-option-key {
  min-width: 18px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.drag-handle {
  cursor: grab;
  color: var(--el-text-color-secondary);
  user-select: none;
}

.drag-handle.disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.draft-inline-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.draft-inline-actions-compact {
  gap: 6px;
}

.draft-preview-detail-card {
  min-height: 420px;
  max-height: 72vh;
}

@media (max-width: 960px) {
  .page-toolbar,
  .paper-header,
  .workspace-shell-head,
  .mode-panel-head,
  .workspace-panel-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .paper-preview-grid {
    grid-template-columns: 1fr;
  }

  .paper-items-list-head {
    padding: 6px 8px 12px;
  }

  .paper-item-row {
    padding: 14px 8px;
  }

  .paper-item-row-title {
    font-size: 15px;
  }

  .selector-workspace {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .workspace-shell {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .workspace-main-pane {
    padding: 18px;
  }

  .mode-sidebar {
    border-right: 0;
    border-bottom: 1px solid #eef2f7;
    padding: 18px 0;
  }

  .mode-sidebar-head {
    padding: 0 18px 8px;
  }

  .draft-tools-grid {
    grid-template-columns: 1fr;
  }

  .selector-toolbar-grid {
    grid-template-columns: 1fr;
  }

  .selector-toolbar-grid-manual {
    grid-template-areas: none;
  }

  .selector-toolbar-actions {
    justify-content: flex-start;
  }

  .ai-form-item-narrow {
    max-width: 100%;
  }

  .paper-summary-bar {
    gap: 8px 12px;
  }

  .paper-summary-inline {
    padding-right: 12px;
  }

  .draft-tools-inline {
    align-items: stretch;
  }

  .saved-preview-workspace {
    min-height: auto;
  }

  .saved-table-panel,
  .saved-preview-detail-card {
    max-height: none;
    min-height: auto;
  }

  .draft-tools-inline-actions {
    width: 100%;
    padding-bottom: 0;
  }

  .draft-workspace-meta {
    margin-bottom: 14px;
  }

  .draft-tools-card-spaced {
    margin-bottom: 14px;
  }

  .workspace-tabs :deep(.el-tabs__nav) {
    grid-template-columns: 1fr;
  }

  .table-panel-tall {
    max-height: none;
  }

  .table-panel-mid {
    max-height: none;
  }

  .rule-row {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 12px;
  }

  .rule-delete-wrap {
    justify-content: flex-start;
  }

  .rule-delete-button {
    width: 100%;
  }

  .ai-constrain-row {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 12px;
  }

  .ai-constrain-delete-wrap {
    justify-content: flex-start;
  }

  .ai-constrain-delete {
    width: 100%;
  }

  .ai-preview-workspace {
    min-height: auto;
  }

  .manual-selector-workspace {
    min-height: auto;
  }

  .random-preview-workspace {
    min-height: auto;
  }

  .manual-selector-list-pane,
  .manual-selector-detail-pane {
    min-height: auto;
  }

  .manual-selector-table-panel,
  .manual-selector-detail-card,
  .random-preview-table-panel,
  .random-preview-detail-card,
  .ai-preview-table-panel,
  .ai-preview-detail-card {
    height: auto;
    max-height: none;
    min-height: auto;
  }
}
</style>



















