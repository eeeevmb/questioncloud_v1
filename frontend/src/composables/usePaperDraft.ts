import { computed, reactive, ref, type ComputedRef } from 'vue';
import { generateAgentPaperDraft } from '../api/ai';
import type { AgentPaperDraftVO, GeneratePaperDraftBucketConstrain, GeneratePaperDraftReq } from '../types/ai';
import { showError } from '../utils/messages';

function normalizeText(value: unknown): string {
  if (value === undefined || value === null) {
    return '';
  }
  return String(value).trim();
}

function defaultPaperDraftConstrain(): GeneratePaperDraftBucketConstrain {
  return {
    typeCode: 'single-choice',
    count: 5,
    difficultyMin: 0,
    difficultyMax: 1
  };
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

export function usePaperDraft(currentCollectionId: ComputedRef<string>) {
  const paperDraftDrawerVisible = ref(false);
  const paperDraftStep = ref<1 | 2>(1);
  const generatePaperDraftLoading = ref(false);
  const paperDraftErrorMessage = ref('');
  const paperDraftResult = ref<AgentPaperDraftVO | null>(null);
  const paperDraftForm = reactive<GeneratePaperDraftReq>({
    message: '',
    collectionIds: [],
    constrains: [defaultPaperDraftConstrain()]
  });

  const paperDraftCandidateQuestions = computed(() => paperDraftResult.value?.candidateQuestions ?? []);
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
    paperDraftForm.constrains = [defaultPaperDraftConstrain()];
  }

  function syncPaperDraftCollectionIds(validCollectionIds: string[]) {
    if (!paperDraftForm.collectionIds.length) {
      return;
    }
    paperDraftForm.collectionIds = paperDraftForm.collectionIds.filter((id) => validCollectionIds.includes(id));
  }

  function openPaperDraftDrawer() {
    resetPaperDraftForm();
    paperDraftResult.value = null;
    paperDraftErrorMessage.value = '';
    generatePaperDraftLoading.value = false;
    paperDraftStep.value = 1;
    paperDraftDrawerVisible.value = true;
  }

  function addPaperDraftConstrain() {
    paperDraftForm.constrains.push(defaultPaperDraftConstrain());
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
    paperDraftResult.value = null;
    generatePaperDraftLoading.value = true;
    try {
      const result = await generateAgentPaperDraft(requestPayload);
      paperDraftResult.value = normalizeAgentPaperDraftResult(result);
    } catch (error: any) {
      const message = error?.message ?? '组卷草稿生成失败，请重试';
      paperDraftErrorMessage.value = message;
      showError(message);
    } finally {
      generatePaperDraftLoading.value = false;
    }
  }

  function backToPaperRequirementForm() {
    paperDraftStep.value = 1;
  }

  return {
    paperDraftDrawerVisible,
    paperDraftStep,
    generatePaperDraftLoading,
    paperDraftErrorMessage,
    paperDraftResult,
    paperDraftForm,
    paperDraftCandidateQuestions,
    paperDraftConstraintLevelError,
    syncPaperDraftCollectionIds,
    openPaperDraftDrawer,
    addPaperDraftConstrain,
    removePaperDraftConstrain,
    handleGenerateAgentPaperDraft,
    backToPaperRequirementForm
  };
}