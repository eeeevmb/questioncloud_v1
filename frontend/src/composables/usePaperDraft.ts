import { computed, reactive, ref, type ComputedRef } from 'vue';
import { generateAgentPaperDraft } from '../api/ai';
import { createPaper, savePaperItems } from '../api/paper';
import { fetchQuestionDetail } from '../api/question';
import type {
  AgentPaperDraftVO,
  GeneratePaperDraftBucketConstrain,
  GeneratePaperDraftReq,
  PaperDraftPreviewGroup,
  PaperDraftPreviewQuestion
} from '../types/ai';
import type { QuestionDetail } from '../types/question';
import { showError, showSuccess } from '../utils/messages';

interface UsePaperDraftOptions {
  onPaperSaved?: (paperId: string) => void | Promise<void>;
}

function normalizeText(value: unknown): string {
  if (value === undefined || value === null) {
    return '';
  }
  return String(value).trim();
}

function createPaperDraftConstrain(
  typeCode = 'single-choice',
  count = 5,
  difficultyMin = 0,
  difficultyMax = 1
): GeneratePaperDraftBucketConstrain {
  return {
    typeCode,
    count,
    difficultyMin,
    difficultyMax
  };
}

function defaultPaperDraftConstrains(): GeneratePaperDraftBucketConstrain[] {
  return [
    createPaperDraftConstrain('single-choice', 5, 0, 1),
    createPaperDraftConstrain('multiple-choice', 5, 0, 1),
    createPaperDraftConstrain('fill-in', 5, 0, 1),
    createPaperDraftConstrain('short-answer', 5, 0, 1)
  ];
}

function normalizeAgentPaperDraftResult(payload: AgentPaperDraftVO): AgentPaperDraftVO {
  const candidateQuestions = Array.isArray(payload?.candidateQuestions)
    ? payload.candidateQuestions.map((item) => ({
        questionId: normalizeText(item?.questionId),
        questionVersionId: normalizeText(item?.questionVersionId),
        title: normalizeText(item?.title),
        typeCode: normalizeText(item?.typeCode),
        difficulty: Number.isFinite(item?.difficulty) ? Number(item.difficulty) : 0
      }))
    : [];

  return {
    reason: normalizeText(payload?.reason),
    candidateQuestions
  };
}

export function usePaperDraft(currentCollectionId: ComputedRef<string>, options: UsePaperDraftOptions = {}) {
  const paperDraftDrawerVisible = ref(false);
  const paperDraftStep = ref<1 | 2>(1);
  const generatePaperDraftLoading = ref(false);
  const paperDraftDetailLoading = ref(false);
  const savePaperDraftLoading = ref(false);
  const paperDraftErrorMessage = ref('');
  const paperDraftDetailErrorMessage = ref('');
  const paperDraftResult = ref<AgentPaperDraftVO | null>(null);
  const paperDraftQuestionDetailMap = ref<Record<string, QuestionDetail | null>>({});
  const paperDraftGeneratingTips = ['理解组卷需求', '检索候选题库', '生成组卷理由与草稿'];
  const paperDraftForm = reactive<GeneratePaperDraftReq>({
    message: '',
    collectionIds: [],
    constrains: defaultPaperDraftConstrains()
  });

  const paperDraftCandidateQuestions = computed(() => paperDraftResult.value?.candidateQuestions ?? []);
  const canSavePaperDraft = computed(
    () => Boolean(paperDraftResult.value && paperDraftCandidateQuestions.value.length) && !generatePaperDraftLoading.value && !savePaperDraftLoading.value
  );
  const paperDraftGenerateStatusText = computed(() =>
    generatePaperDraftLoading.value ? '正在生成组卷草稿，通常需要几秒钟' : ''
  );
  const paperDraftExpectedTotal = computed(() => paperDraftForm.constrains.reduce((sum, item) => sum + (Number(item.count) || 0), 0));
  const paperDraftActualTotal = computed(() => paperDraftCandidateQuestions.value.length);
  const paperDraftShortageMessage = computed(() => {
    if (!paperDraftResult.value || !paperDraftExpectedTotal.value || paperDraftActualTotal.value >= paperDraftExpectedTotal.value) {
      return '';
    }
    return `当前题库匹配到 ${paperDraftActualTotal.value} 题，少于目标 ${paperDraftExpectedTotal.value} 题。建议放宽难度范围、减少题量，或补充题库题目后重新生成。`;
  });
  const paperDraftPreviewGroups = computed<PaperDraftPreviewGroup[]>(() => {
    const usedQuestionIds = new Set<string>();
    const typeLabelMap: Record<string, string> = {
      'single-choice': '单选题',
      'multiple-choice': '多选题',
      'true-false': '判断题',
      'fill-in': '填空题',
      'short-answer': '简答题'
    };

    const toPreviewQuestion = (candidate: AgentPaperDraftVO['candidateQuestions'][number]): PaperDraftPreviewQuestion => {
      const detail = paperDraftQuestionDetailMap.value[candidate.questionId];
      return {
        questionId: candidate.questionId,
        questionVersionId: candidate.questionVersionId,
        typeCode: detail?.typeCode || candidate.typeCode,
        title: detail?.title || candidate.title,
        stem: detail?.stem || candidate.title || '暂无题干内容',
        difficulty: Number.isFinite(detail?.difficulty) ? Number(detail?.difficulty) : candidate.difficulty
      };
    };

    const groups = paperDraftForm.constrains.map((constrain) => {
      const questions = paperDraftCandidateQuestions.value
        .filter((item) => item.typeCode === constrain.typeCode)
        .map((item) => {
          usedQuestionIds.add(item.questionId);
          return toPreviewQuestion(item);
        });
      return {
        typeCode: constrain.typeCode,
        typeLabel: typeLabelMap[constrain.typeCode] ?? constrain.typeCode,
        expectedCount: Number(constrain.count) || 0,
        difficultyMin: Number(constrain.difficultyMin) || 0,
        difficultyMax: Number(constrain.difficultyMax) || 0,
        questions
      };
    });

    const extraCandidates = paperDraftCandidateQuestions.value.filter((item) => !usedQuestionIds.has(item.questionId));
    const extraGroups = extraCandidates.reduce<Record<string, PaperDraftPreviewQuestion[]>>((acc, item) => {
      const key = item.typeCode || 'unknown';
      acc[key] = acc[key] || [];
      acc[key].push(toPreviewQuestion(item));
      return acc;
    }, {});

    Object.entries(extraGroups).forEach(([typeCode, questions]) => {
      groups.push({
        typeCode,
        typeLabel: typeLabelMap[typeCode] ?? typeCode,
        expectedCount: 0,
        difficultyMin: 0,
        difficultyMax: 1,
        questions
      });
    });

    return groups.filter((group) => group.expectedCount > 0 || group.questions.length > 0);
  });
  const paperDraftConstraintLevelError = computed(() => {
    for (let i = 0; i < paperDraftForm.constrains.length; i += 1) {
      const item = paperDraftForm.constrains[i];
      if (
        typeof item?.difficultyMin === 'number' &&
        typeof item?.difficultyMax === 'number' &&
        item.difficultyMin > item.difficultyMax
      ) {
        return `第 ${i + 1} 条约束的难度下限不能大于上限`;
      }
    }
    return '';
  });

  function resetPaperDraftForm() {
    paperDraftForm.message = '';
    paperDraftForm.collectionIds = currentCollectionId.value ? [currentCollectionId.value] : [];
    paperDraftForm.constrains = defaultPaperDraftConstrains();
  }

  function syncPaperDraftCollectionIds(validCollectionIds: string[]) {
    if (!paperDraftForm.collectionIds.length) {
      return;
    }
    paperDraftForm.collectionIds = paperDraftForm.collectionIds.filter((id) => validCollectionIds.includes(id));
  }

  function openPaperDraftDrawer() {
    if (!hasRecoverablePaperDraft()) {
      resetPaperDraftWorkspace();
    }
    paperDraftDrawerVisible.value = true;
  }

  function resetPaperDraftWorkspace() {
    resetPaperDraftForm();
    paperDraftResult.value = null;
    paperDraftErrorMessage.value = '';
    paperDraftDetailErrorMessage.value = '';
    paperDraftQuestionDetailMap.value = {};
    generatePaperDraftLoading.value = false;
    paperDraftDetailLoading.value = false;
    savePaperDraftLoading.value = false;
    paperDraftStep.value = 1;
  }

  function hasRecoverablePaperDraft() {
    return generatePaperDraftLoading.value || Boolean(paperDraftResult.value) || Boolean(paperDraftErrorMessage.value);
  }

  function addPaperDraftConstrain() {
    paperDraftForm.constrains.push(createPaperDraftConstrain());
  }

  function removePaperDraftConstrain(index: number) {
    paperDraftForm.constrains.splice(index, 1);
  }

  function buildGeneratePaperDraftRequest(): GeneratePaperDraftReq {
    const message = paperDraftForm.message.trim();
    if (!message) {
      throw new Error('请先填写组卷需求');
    }

    const collectionIds = paperDraftForm.collectionIds.map((item) => normalizeText(item)).filter(Boolean);
    if (!collectionIds.length) {
      throw new Error('请至少选择一个题集');
    }

    if (!paperDraftForm.constrains.length) {
      throw new Error('请至少添加一条题型约束');
    }

    const constrains = paperDraftForm.constrains.map((item, index) => {
      const typeCode = normalizeText(item?.typeCode);
      const count = Number(item?.count);
      const difficultyMin = Number(item?.difficultyMin);
      const difficultyMax = Number(item?.difficultyMax);
      const rowText = `第 ${index + 1} 条约束`;

      if (!typeCode) {
        throw new Error(`${rowText}缺少题型`);
      }
      if (!Number.isInteger(count) || count <= 0) {
        throw new Error(`${rowText}题数必须是大于 0 的整数`);
      }
      if (!Number.isFinite(difficultyMin) || !Number.isFinite(difficultyMax)) {
        throw new Error(`${rowText}缺少难度范围`);
      }
      if (difficultyMin > difficultyMax) {
        throw new Error(`${rowText}的难度下限不能大于上限`);
      }

      return {
        typeCode,
        count,
        difficultyMin,
        difficultyMax
      };
    });

    return {
      message,
      collectionIds: [...new Set(collectionIds)],
      constrains
    };
  }

  async function handleGenerateAgentPaperDraft() {
    if (generatePaperDraftLoading.value) {
      return;
    }

    let requestPayload: GeneratePaperDraftReq;
    try {
      requestPayload = buildGeneratePaperDraftRequest();
    } catch (error: any) {
      const message = error?.message ?? '组卷需求参数校验失败';
      paperDraftErrorMessage.value = '';
      showError(message);
      return;
    }

    paperDraftStep.value = 2;
    paperDraftErrorMessage.value = '';
    paperDraftDetailErrorMessage.value = '';
    paperDraftResult.value = null;
    paperDraftQuestionDetailMap.value = {};
    generatePaperDraftLoading.value = true;
    try {
      const result = await generateAgentPaperDraft(requestPayload);
      const normalizedResult = normalizeAgentPaperDraftResult(result);
      paperDraftResult.value = normalizedResult;
      void loadPaperDraftQuestionDetails(normalizedResult.candidateQuestions);
    } catch (error: any) {
      const message = error?.message ?? '组卷草稿生成失败，请重试';
      paperDraftErrorMessage.value = normalizePaperDraftErrorMessage(message);
      showError(paperDraftErrorMessage.value);
    } finally {
      generatePaperDraftLoading.value = false;
    }
  }

  async function handleSavePaperDraftAsPaper() {
    if (!paperDraftResult.value) {
      showError('请先生成组卷草稿');
      return;
    }
    const items = paperDraftCandidateQuestions.value
      .filter((item) => item.questionId && item.questionVersionId)
      .map((item) => ({
        questionId: item.questionId,
        questionVersionId: item.questionVersionId,
        score: resolveDefaultPaperScore(item.typeCode)
      }));

    if (!items.length) {
      showError('当前草稿没有可保存的题目');
      return;
    }

    savePaperDraftLoading.value = true;
    try {
      const created = await createPaper({
        title: createPaperDraftTitle(),
        description: createPaperDraftDescription()
      });
      await savePaperItems(created.paperId, items);
      showSuccess('组卷草稿已保存为试卷');
      paperDraftDrawerVisible.value = false;
      await options.onPaperSaved?.(created.paperId);
    } catch (error) {
      showError(error instanceof Error ? error.message : '保存试卷失败');
    } finally {
      savePaperDraftLoading.value = false;
    }
  }

  function backToPaperRequirementForm() {
    paperDraftStep.value = 1;
  }

  function normalizePaperDraftErrorMessage(message: string) {
    if (message.includes('题目数量不足')) {
      return '题目数量不足，请调整组卷条件或补充题库题目后重新生成。';
    }
    return message;
  }

  function createPaperDraftTitle() {
    const topic = normalizeText(paperDraftForm.message)
      .replace(/\s+/g, '')
      .slice(0, 18);
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hour = String(now.getHours()).padStart(2, '0');
    const minute = String(now.getMinutes()).padStart(2, '0');
    const second = String(now.getSeconds()).padStart(2, '0');
    return `AI组卷-${topic || '未命名'}-${month}${day}${hour}${minute}${second}`;
  }

  function createPaperDraftDescription() {
    const parts = ['由 AI 助手根据组卷要求生成。'];
    const requirement = normalizeText(paperDraftForm.message);
    const reason = normalizeText(paperDraftResult.value?.reason);
    if (requirement) {
      parts.push(`组卷要求：${requirement}`);
    }
    if (reason) {
      parts.push(`组卷理由：${reason}`);
    }
    return parts.join('\n\n');
  }

  function resolveDefaultPaperScore(typeCode?: string) {
    if (typeCode === 'true-false') {
      return 2;
    }
    if (typeCode === 'short-answer') {
      return 10;
    }
    return 5;
  }

  async function loadPaperDraftQuestionDetails(candidates: AgentPaperDraftVO['candidateQuestions']) {
    const ids = [...new Set(candidates.map((item) => item.questionId).filter(Boolean))];
    if (!ids.length) {
      return;
    }

    paperDraftDetailLoading.value = true;
    paperDraftDetailErrorMessage.value = '';
    try {
      const settled = await Promise.allSettled(ids.map((id) => fetchQuestionDetail(id)));
      const nextMap: Record<string, QuestionDetail | null> = {};
      let failedCount = 0;
      settled.forEach((item, index) => {
        const id = ids[index];
        if (item.status === 'fulfilled') {
          nextMap[id] = item.value;
        } else {
          nextMap[id] = null;
          failedCount += 1;
        }
      });
      paperDraftQuestionDetailMap.value = nextMap;
      if (failedCount > 0) {
        paperDraftDetailErrorMessage.value = `有 ${failedCount} 道题详情加载失败，已使用题目标题作为兜底展示。`;
      }
    } catch {
      paperDraftDetailErrorMessage.value = '题目详情加载失败，已使用题目标题作为兜底展示。';
    } finally {
      paperDraftDetailLoading.value = false;
    }
  }

  return {
    paperDraftDrawerVisible,
    paperDraftStep,
    generatePaperDraftLoading,
    paperDraftDetailLoading,
    savePaperDraftLoading,
    paperDraftErrorMessage,
    paperDraftDetailErrorMessage,
    paperDraftResult,
    paperDraftForm,
    paperDraftGeneratingTips,
    paperDraftGenerateStatusText,
    paperDraftCandidateQuestions,
    canSavePaperDraft,
    paperDraftPreviewGroups,
    paperDraftExpectedTotal,
    paperDraftActualTotal,
    paperDraftShortageMessage,
    paperDraftConstraintLevelError,
    syncPaperDraftCollectionIds,
    openPaperDraftDrawer,
    addPaperDraftConstrain,
    removePaperDraftConstrain,
    handleGenerateAgentPaperDraft,
    handleSavePaperDraftAsPaper,
    backToPaperRequirementForm
  };
}
