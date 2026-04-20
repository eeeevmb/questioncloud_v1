import { computed, reactive, ref, type ComputedRef } from 'vue';
import { generateQuestionDraft } from '../api/ai';
import { createQuestion, type QuestionCreatePayload } from '../api/question';
import type { QuestionOption } from '../types/question';
import type { GenerateQuestionDraftReq, QuestionDraft } from '../types/ai';
import type { AiCreateFormState, AiDraftFormState, CreateQuestionFormState } from '../types/agent-demo';
import { showError, showInfo, showSuccess } from '../utils/messages';

interface CreatedQuestionMeta {
  questionId: string;
  title: string;
  typeCode: string;
  difficulty: number | null;
  collectionId: string;
}

interface UseQuestionCreateOptions {
  currentCollectionId: ComputedRef<string>;
  normalizeText: (value: unknown) => string;
  normalizeDraftOptions: (rawOptions: QuestionOption[] | null | undefined, includeEmpty?: boolean) => QuestionOption[];
  parseJudgeAnswer: (input?: string | null) => 'T' | 'F' | null;
  onQuestionCreated: (meta: CreatedQuestionMeta) => void;
}

function defaultCreateOptions(): QuestionOption[] {
  return [
    { key: 'A', content: '' },
    { key: 'B', content: '' }
  ];
}

function defaultAiDraftForm(): AiDraftFormState {
  return {
    typeCode: 'single-choice',
    title: '',
    difficulty: null,
    stem: '',
    options: defaultCreateOptions(),
    correctOptions: [],
    judgeAnswer: '',
    answer: '',
    solution: '',
    assumptions: ''
  };
}

function reindexDraftOptions(options: QuestionOption[], normalizeText: UseQuestionCreateOptions['normalizeText']) {
  return options.map((item, index) => ({
    key: String.fromCharCode(65 + index),
    content: normalizeText(item.content)
  }));
}

function parseCorrectOptionsFromText(input: string): string[] {
  if (!input.trim()) {
    return [];
  }
  const result = new Set<string>();
  input
    .split(/[,\uFF0C\u3001;\uFF1B\s]+/)
    .map((item) => item.trim().toUpperCase())
    .filter(Boolean)
    .forEach((item) => {
      if (/^[A-Z]$/.test(item)) {
        result.add(item);
      }
    });
  return [...result];
}

export function useQuestionCreate(options: UseQuestionCreateOptions) {
  const createQuestionDrawerVisible = ref(false);
  const createQuestionCollectionId = ref('');
  const createSubmitting = ref(false);
  const createAiDraftLoading = ref(false);
  const createMode = ref<'ai' | 'manual'>('ai');
  const aiCreateStep = ref<1 | 2>(1);

  const createForm = reactive<CreateQuestionFormState>({
    questionType: 'single-choice',
    title: '',
    stem: '',
    difficulty: null,
    options: defaultCreateOptions(),
    choiceCorrect: [],
    judgeAnswer: 'T',
    answer: '',
    solution: ''
  });
  const aiCreateForm = reactive<AiCreateFormState>({
    topic: '',
    questionType: 'single-choice',
    scenario: '',
    requirements: ''
  });
  const aiDraftPreview = ref<QuestionDraft | null>(null);
  const aiDraftForm = reactive<AiDraftFormState>(defaultAiDraftForm());
  const aiDraftGeneratingTips = ['理解需求', '组织题干与选项', '生成答案与解析'];

  const hasCreateCollectionContext = computed(() => Boolean(createQuestionCollectionId.value));
  const isCreateChoiceType = computed(
    () => createForm.questionType === 'single-choice' || createForm.questionType === 'multiple-choice'
  );
  const isCreateSingleChoice = computed(() => createForm.questionType === 'single-choice');
  const isCreateTrueFalseType = computed(() => createForm.questionType === 'true-false');

  const createSingleCorrect = computed({
    get: () => createForm.choiceCorrect[0] ?? '',
    set: (value: string) => {
      createForm.choiceCorrect.splice(0, createForm.choiceCorrect.length);
      if (value?.trim()) {
        createForm.choiceCorrect.push(value.trim());
      }
    }
  });

  const createGuideText = computed(() => {
    if (isCreateChoiceType.value) {
      return '先填写题干和选项即可开始创建，正确答案和解析可继续完善。';
    }
    if (isCreateTrueFalseType.value) {
      return '请填写清晰的判断陈述，并选择正确或错误。';
    }
    return '先填写题干即可创建，答案和解析可后续补充。';
  });

  const isAiDraftChoiceType = computed(
    () => aiDraftForm.typeCode === 'single-choice' || aiDraftForm.typeCode === 'multiple-choice'
  );
  const isAiDraftSingleChoice = computed(() => aiDraftForm.typeCode === 'single-choice');
  const isAiDraftTrueFalseType = computed(() => aiDraftForm.typeCode === 'true-false');

  const aiDraftSingleCorrect = computed({
    get: () => aiDraftForm.correctOptions[0] ?? '',
    set: (value: string) => {
      const normalized = options.normalizeText(value).toUpperCase();
      aiDraftForm.correctOptions = normalized ? [normalized] : [];
    }
  });

  const draftPreviewTitle = computed(() => options.normalizeText(aiDraftForm.title));
  const draftPreviewStem = computed(() => options.normalizeText(aiDraftForm.stem));
  const draftPreviewSolution = computed(() => options.normalizeText(aiDraftForm.solution));
  const draftPreviewOptions = computed(() => options.normalizeDraftOptions(aiDraftForm.options));
  const draftPreviewCorrectAnswer = computed(() => formatDraftCorrectAnswerFromForm());
  const draftPreviewAssumptions = computed(() => options.normalizeText(aiDraftForm.assumptions));
  const createAiDraftStatusText = computed(() =>
    createAiDraftLoading.value ? '正在生成题目草稿，通常需要几秒钟' : ''
  );

  function syncCreateCollectionId(validCollectionIds: string[]) {
    if (createQuestionCollectionId.value && !validCollectionIds.includes(createQuestionCollectionId.value)) {
      createQuestionCollectionId.value = '';
    }
  }

  function openCreateQuestionDrawer() {
    createQuestionCollectionId.value = options.currentCollectionId.value;
    createMode.value = 'ai';
    aiCreateStep.value = 1;
    resetAiCreateForm();
    resetCreateForm();
    resetAiDraftForm();
    aiDraftPreview.value = null;
    createQuestionDrawerVisible.value = true;
  }

  function activateAiMode() {
    createMode.value = 'ai';
    aiCreateStep.value = aiDraftPreview.value ? 2 : 1;
  }

  function resetCreateForm() {
    createForm.questionType = 'single-choice';
    createForm.title = '';
    createForm.stem = '';
    createForm.difficulty = null;
    createForm.options = defaultCreateOptions();
    createForm.choiceCorrect = [];
    createForm.judgeAnswer = 'T';
    createForm.answer = '';
    createForm.solution = '';
  }

  function resetAiCreateForm() {
    aiCreateForm.topic = '';
    aiCreateForm.questionType = 'single-choice';
    aiCreateForm.scenario = '';
    aiCreateForm.requirements = '';
  }

  function resetAiDraftForm() {
    const next = defaultAiDraftForm();
    aiDraftForm.typeCode = next.typeCode;
    aiDraftForm.title = next.title;
    aiDraftForm.difficulty = next.difficulty;
    aiDraftForm.stem = next.stem;
    aiDraftForm.options = next.options;
    aiDraftForm.correctOptions = next.correctOptions;
    aiDraftForm.judgeAnswer = next.judgeAnswer;
    aiDraftForm.answer = next.answer;
    aiDraftForm.solution = next.solution;
    aiDraftForm.assumptions = next.assumptions;
  }

  function onCreateTypeChange() {
    if (isCreateChoiceType.value) {
      if (createForm.options.length < 2) {
        createForm.options = defaultCreateOptions();
      }
      if (isCreateSingleChoice.value && createForm.choiceCorrect.length > 1) {
        createForm.choiceCorrect = createForm.choiceCorrect.slice(0, 1);
      }
      createForm.judgeAnswer = 'T';
      createForm.answer = '';
      return;
    }

    createForm.options = defaultCreateOptions();
    createForm.choiceCorrect = [];
    if (isCreateTrueFalseType.value) {
      createForm.answer = '';
      return;
    }
    createForm.judgeAnswer = 'T';
  }

  function addCreateOption() {
    createForm.options.push({ key: '', content: '' });
  }

  function removeCreateOption(index: number) {
    const removed = createForm.options.splice(index, 1)[0];
    if (!removed?.key) {
      return;
    }
    createForm.choiceCorrect = createForm.choiceCorrect.filter((item) => item !== removed.key);
  }

  function toggleCreateMultiple(key?: string) {
    if (!key?.trim()) {
      return;
    }
    const normalized = key.trim();
    const index = createForm.choiceCorrect.indexOf(normalized);
    if (index === -1) {
      createForm.choiceCorrect.push(normalized);
      return;
    }
    createForm.choiceCorrect.splice(index, 1);
  }

  function normalizeCreateOptions() {
    return createForm.options
      .map((option) => ({
        key: option.key.trim().toUpperCase(),
        content: option.content.trim()
      }))
      .filter((option) => option.key && option.content);
  }

  function resolveCreateTitle(stem: string) {
    const rawTitle = createForm.title.trim();
    if (rawTitle) {
      return rawTitle;
    }
    if (stem.length <= 18) {
      return stem;
    }
    return `${stem.slice(0, 18)}...`;
  }

  function buildCreatePayload(): QuestionCreatePayload {
    const stem = createForm.stem.trim();
    if (!stem) {
      throw new Error('请先填写题干');
    }
    if (!createQuestionCollectionId.value) {
      throw new Error('请先选择题集');
    }

    const payload: QuestionCreatePayload = {
      typeCode: createForm.questionType,
      title: resolveCreateTitle(stem),
      stem,
      answer: null,
      solution: createForm.solution.trim() || null,
      difficulty: createForm.difficulty,
      collectionId: createQuestionCollectionId.value,
      assets: []
    };

    if (isCreateChoiceType.value) {
      const normalizedOptions = normalizeCreateOptions();
      if (normalizedOptions.length < 2) {
        throw new Error('选择题请至少填写 2 个有效选项');
      }
      payload.options = normalizedOptions;
      const validKeys = new Set(normalizedOptions.map((option) => option.key));
      payload.correctOptions = createForm.choiceCorrect.map((key) => key.trim().toUpperCase()).filter((key) => validKeys.has(key));
      return payload;
    }

    if (isCreateTrueFalseType.value) {
      payload.judgeAnswer = createForm.judgeAnswer;
      return payload;
    }

    payload.answer = createForm.answer.trim() || null;
    return payload;
  }

  async function handleCreateQuestion() {
    if (!hasCreateCollectionContext.value || !createQuestionCollectionId.value) {
      showInfo('请先选择题集');
      return;
    }

    createSubmitting.value = true;
    try {
      const payload = buildCreatePayload();
      const created = await createQuestion(payload);
      options.onQuestionCreated({
        questionId: created.questionId,
        title: payload.title,
        typeCode: payload.typeCode,
        difficulty: payload.difficulty ?? null,
        collectionId: createQuestionCollectionId.value
      });
      createQuestionDrawerVisible.value = false;
      showSuccess('题目已创建');
    } catch (error: any) {
      showError(error?.message ?? '创建题目失败');
    } finally {
      createSubmitting.value = false;
    }
  }

  function addAiDraftOption() {
    aiDraftForm.options = reindexDraftOptions([...aiDraftForm.options, { key: '', content: '' }], options.normalizeText);
  }

  function removeAiDraftOption(index: number) {
    const next = [...aiDraftForm.options];
    next.splice(index, 1);
    aiDraftForm.options = reindexDraftOptions(next.length >= 2 ? next : defaultCreateOptions(), options.normalizeText);
    const validKeys = new Set(aiDraftForm.options.map((item) => item.key));
    aiDraftForm.correctOptions = aiDraftForm.correctOptions.filter((item) => validKeys.has(item));
  }

  function formatDraftCorrectAnswerFromForm(): string {
    const typeCode = options.normalizeText(aiDraftForm.typeCode || aiCreateForm.questionType);
    if (typeCode === 'single-choice' || typeCode === 'multiple-choice') {
      const normalized = aiDraftForm.correctOptions.map((item) => options.normalizeText(item).toUpperCase()).filter(Boolean);
      if (normalized.length > 0) {
        return normalized.join('、');
      }
    }
    if (typeCode === 'true-false') {
      const judge = options.parseJudgeAnswer(aiDraftForm.judgeAnswer || aiDraftForm.answer);
      if (judge === 'T') {
        return '正确';
      }
      if (judge === 'F') {
        return '错误';
      }
    }
    return options.normalizeText(aiDraftForm.answer);
  }

  function applyGeneratedDraftToEditableForm(draft: QuestionDraft) {
    const typeCode = options.normalizeText(draft.typeCode) || aiCreateForm.questionType;
    const normalizedOptions = reindexDraftOptions(options.normalizeDraftOptions(draft.options, true), options.normalizeText);
    const choiceOptions = normalizedOptions.length >= 2 ? normalizedOptions : defaultCreateOptions();
    const validKeys = new Set(choiceOptions.map((item) => item.key));
    const directCorrect = Array.isArray(draft.correctOptions)
      ? draft.correctOptions.map((item) => options.normalizeText(item).toUpperCase()).filter((item) => validKeys.has(item))
      : [];
    const fallbackCorrect = parseCorrectOptionsFromText(options.normalizeText(draft.answer)).filter((item) => validKeys.has(item));

    aiDraftForm.typeCode = typeCode;
    aiDraftForm.title = options.normalizeText(draft.title);
    aiDraftForm.difficulty = typeof draft.difficulty === 'number' ? draft.difficulty : null;
    aiDraftForm.stem = options.normalizeText(draft.stem);
    aiDraftForm.options = choiceOptions;
    const mergedCorrect = [...new Set([...directCorrect, ...fallbackCorrect])];
    aiDraftForm.correctOptions = typeCode === 'single-choice' ? mergedCorrect.slice(0, 1) : mergedCorrect;
    aiDraftForm.judgeAnswer = options.parseJudgeAnswer(draft.judgeAnswer ?? draft.answer) ?? '';
    aiDraftForm.answer = options.normalizeText(draft.answer);
    aiDraftForm.solution = options.normalizeText(draft.solution);
    aiDraftForm.assumptions = options.normalizeText(draft.assumptions);
  }

  function buildGenerateDraftRequest(): GenerateQuestionDraftReq {
    const topic = aiCreateForm.topic.trim();
    if (!topic) {
      throw new Error('请先填写知识点 / 出题方向');
    }

    const payload: GenerateQuestionDraftReq = {
      topic,
      typeCode: aiCreateForm.questionType
    };

    const scene = aiCreateForm.scenario.trim();
    if (scene) {
      payload.scene = scene;
    }

    const extraRequirements = aiCreateForm.requirements.trim();
    if (extraRequirements) {
      payload.extraRequirements = extraRequirements;
    }

    return payload;
  }

  async function handleGenerateAiDraft() {
    if (createAiDraftLoading.value) {
      return;
    }
    if (!hasCreateCollectionContext.value || !createQuestionCollectionId.value) {
      showInfo('请先选择题集');
      return;
    }

    createAiDraftLoading.value = true;
    try {
      const requestPayload = buildGenerateDraftRequest();
      const draft = await generateQuestionDraft(requestPayload);
      if (!options.normalizeText(draft?.stem)) {
        throw new Error('草稿缺少题干，请调整需求后重新生成');
      }
      applyGeneratedDraftToEditableForm(draft);
      aiDraftPreview.value = draft;
      aiCreateStep.value = 2;
      showSuccess('已生成题目草稿，请确认后再创建');
    } catch (error: any) {
      showError(error?.message ?? '题目草稿生成失败，请重试');
    } finally {
      createAiDraftLoading.value = false;
    }
  }

  function backToAiRequirementForm() {
    aiCreateStep.value = 1;
    aiDraftPreview.value = null;
    resetAiDraftForm();
  }

  function buildAiDraftPayload(): QuestionCreatePayload {
    if (!aiDraftPreview.value) {
      throw new Error('请先生成题目草稿');
    }
    if (!createQuestionCollectionId.value) {
      throw new Error('请先选择题集');
    }

    const typeCode = options.normalizeText(aiDraftForm.typeCode) || aiCreateForm.questionType;
    const stem = options.normalizeText(aiDraftForm.stem);
    if (!stem) {
      throw new Error('草稿题干为空，请重新生成');
    }

    const payload: QuestionCreatePayload = {
      typeCode,
      title: options.normalizeText(aiDraftForm.title) || resolveCreateTitle(stem),
      stem,
      answer: null,
      solution: options.normalizeText(aiDraftForm.solution) || null,
      difficulty: aiDraftForm.difficulty,
      collectionId: createQuestionCollectionId.value,
      assets: []
    };

    if (typeCode === 'single-choice' || typeCode === 'multiple-choice') {
      const normalizedOptions = options.normalizeDraftOptions(aiDraftForm.options);
      if (normalizedOptions.length < 2) {
        throw new Error('草稿选项不足，请重新生成或切换手动创建');
      }
      payload.options = normalizedOptions;
      const validKeys = new Set(normalizedOptions.map((option) => option.key));
      const directCorrect = aiDraftForm.correctOptions.map((item) => options.normalizeText(item).toUpperCase()).filter((item) => validKeys.has(item));
      const fallbackCorrect = parseCorrectOptionsFromText(options.normalizeText(aiDraftForm.answer)).filter((item) => validKeys.has(item));
      const mergedCorrect = [...new Set([...directCorrect, ...fallbackCorrect])];
      if (!mergedCorrect.length) {
        throw new Error('草稿缺少正确答案，请重新生成或切换手动创建');
      }
      if (typeCode === 'single-choice' && mergedCorrect.length > 1) {
        throw new Error('单选题草稿包含多个正确答案，请重新生成或切换手动创建');
      }
      payload.correctOptions = mergedCorrect;
      return payload;
    }

    if (typeCode === 'true-false') {
      const judgeAnswer = options.parseJudgeAnswer(aiDraftForm.judgeAnswer || aiDraftForm.answer);
      if (!judgeAnswer) {
        throw new Error('草稿缺少判断题答案，请重新生成或切换手动创建');
      }
      payload.judgeAnswer = judgeAnswer;
      return payload;
    }

    payload.answer = options.normalizeText(aiDraftForm.answer) || null;
    return payload;
  }

  async function handleCreateQuestionFromAiDraft() {
    if (!hasCreateCollectionContext.value || !createQuestionCollectionId.value) {
      showInfo('请先选择题集');
      return;
    }
    if (!aiDraftPreview.value) {
      showInfo('请先生成题目草稿');
      return;
    }

    createSubmitting.value = true;
    try {
      const payload = buildAiDraftPayload();
      const created = await createQuestion(payload);
      options.onQuestionCreated({
        questionId: created.questionId,
        title: payload.title,
        typeCode: payload.typeCode,
        difficulty: payload.difficulty ?? null,
        collectionId: createQuestionCollectionId.value
      });
      createQuestionDrawerVisible.value = false;
      showSuccess('题目已写入题库');
    } catch (error: any) {
      showError(error?.message ?? '写入题库失败');
    } finally {
      createSubmitting.value = false;
    }
  }

  return {
    createQuestionDrawerVisible,
    createQuestionCollectionId,
    createSubmitting,
    createAiDraftLoading,
    createMode,
    aiCreateStep,
    createForm,
    aiCreateForm,
    aiDraftForm,
    hasCreateCollectionContext,
    isCreateChoiceType,
    isCreateSingleChoice,
    isCreateTrueFalseType,
    createSingleCorrect,
    createGuideText,
    isAiDraftChoiceType,
    isAiDraftSingleChoice,
    isAiDraftTrueFalseType,
    aiDraftSingleCorrect,
    draftPreviewTitle,
    draftPreviewStem,
    draftPreviewSolution,
    draftPreviewOptions,
    draftPreviewCorrectAnswer,
    draftPreviewAssumptions,
    createAiDraftStatusText,
    aiDraftGeneratingTips,
    syncCreateCollectionId,
    openCreateQuestionDrawer,
    activateAiMode,
    onCreateTypeChange,
    addCreateOption,
    removeCreateOption,
    toggleCreateMultiple,
    addAiDraftOption,
    removeAiDraftOption,
    handleGenerateAiDraft,
    backToAiRequirementForm,
    handleCreateQuestionFromAiDraft,
    handleCreateQuestion
  };
}
