import { computed, reactive, ref, type ComputedRef } from 'vue';
import { fetchCollectionQuestions } from '../api/collection';
import { fetchQuestionDetail } from '../api/question';
import type { QuestionDetail, QuestionOption, QuestionSummary, QuestionSummaryPage } from '../types/question';
import type { QuestionPickerQueryState } from '../types/agent-demo';
import { showError, showInfo } from '../utils/messages';

interface QuestionMeta {
  title: string;
  typeCode: string;
  difficulty: number | null;
}

interface UseQuestionPickerOptions {
  currentCollectionId: ComputedRef<string>;
  normalizeText: (value: unknown) => string;
  normalizeDraftOptions: (rawOptions: QuestionOption[] | null | undefined, includeEmpty?: boolean) => QuestionOption[];
  parseJudgeAnswer: (input?: string | null) => 'T' | 'F' | null;
}

function normalizeInteger(value: unknown, fallback: number, min = 0) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return fallback;
  }
  const normalized = Math.trunc(parsed);
  return normalized < min ? min : normalized;
}

export function useQuestionPicker(options: UseQuestionPickerOptions) {
  const questionPickerVisible = ref(false);
  const questionPickerLoading = ref(false);
  const questionPickerCollectionId = ref('');
  const questionPickerPage = ref<QuestionSummaryPage | null>(null);
  const pickerActiveQuestionId = ref('');
  const pickerQuestionDetailLoadingId = ref('');
  const pickerDetailCollapseNames = ref<string[]>([]);
  const questionPickerQuery = reactive<QuestionPickerQueryState>({
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    typeCode: '',
    levelMin: 0,
    levelMax: 1
  });

  const drawerSelectedQuestionIds = ref<string[]>([]);
  const selectedQuestionMetaMap = reactive<Record<string, QuestionMeta>>({});
  const questionDetailMap = reactive<Record<string, QuestionDetail>>({});

  const canApplyDrawerSelection = computed(() => Boolean(questionPickerCollectionId.value) && drawerSelectedQuestionIds.value.length > 0);
  const questionPickerRecords = computed(() => questionPickerPage.value?.records ?? []);
  const pickerPageSummary = computed(() => {
    const page = questionPickerPage.value;
    if (!page) {
      return '';
    }
    const total = Number.isFinite(page.total) ? page.total : 0;
    const current = page.pageNum || questionPickerQuery.pageNum || 1;
    const pageSize = page.pageSize || questionPickerQuery.pageSize || 10;
    const pages = Math.max(page.pages || Math.ceil(total / Math.max(pageSize, 1)) || 1, 1);
    return `共 ${total} 题 · 第 ${Math.min(current, pages)} / ${pages} 页`;
  });
  const pickerActiveQuestionDetail = computed(() => {
    if (!pickerActiveQuestionId.value) {
      return null;
    }
    return questionDetailMap[pickerActiveQuestionId.value] ?? null;
  });
  const pickerActiveQuestionNo = computed(() => {
    if (!pickerActiveQuestionId.value) {
      return '-';
    }
    const idx = questionPickerRecords.value.findIndex((item) => item.id === pickerActiveQuestionId.value);
    if (idx === -1) {
      return '-';
    }
    return String(questionDisplayNo(idx));
  });
  const pickerDetailOptions = computed(() => options.normalizeDraftOptions(pickerActiveQuestionDetail.value?.options ?? []));
  const pickerDetailAnswer = computed(() => {
    const detail = pickerActiveQuestionDetail.value;
    if (!detail) {
      return '';
    }
    if (detail.typeCode === 'single-choice' || detail.typeCode === 'multiple-choice') {
      const keys = (detail.correctOptions ?? []).map((item) => options.normalizeText(item).toUpperCase()).filter(Boolean);
      if (keys.length) {
        return keys.join('、');
      }
    }
    if (detail.typeCode === 'true-false') {
      const judge = options.parseJudgeAnswer(detail.judgeAnswer ?? detail.answer);
      if (judge === 'T') {
        return '正确';
      }
      if (judge === 'F') {
        return '错误';
      }
    }
    return options.normalizeText(detail.answer) || options.normalizeText(detail.answerKey);
  });
  const pickerDetailSolution = computed(() => options.normalizeText(pickerActiveQuestionDetail.value?.solution));
  const pickerDetailLoading = computed(
    () => Boolean(pickerActiveQuestionId.value) && pickerQuestionDetailLoadingId.value === pickerActiveQuestionId.value
  );
  const questionPickerLevelError = computed(() => {
    if (
      questionPickerQuery.levelMin !== undefined &&
      questionPickerQuery.levelMax !== undefined &&
      questionPickerQuery.levelMin > questionPickerQuery.levelMax
    ) {
      return '难度下限不能大于上限';
    }
    return '';
  });
  const drawerSelectedPreview = computed(() =>
    drawerSelectedQuestionIds.value.slice(0, 4).map((id, index) => {
      const meta = selectedQuestionMetaMap[id];
      return meta?.title ? clipText(meta.title, 12) : `第 ${index + 1} 题`;
    })
  );

  function syncQuestionPickerCollectionId(validCollectionIds: string[]) {
    if (!questionPickerCollectionId.value) {
      return;
    }
    if (!validCollectionIds.includes(questionPickerCollectionId.value)) {
      questionPickerCollectionId.value = '';
      resetQuestionPickerState();
    }
  }

  function openQuestionPicker() {
    questionPickerVisible.value = true;
    drawerSelectedQuestionIds.value = [];
    pickerDetailCollapseNames.value = [];
    if (!questionPickerCollectionId.value) {
      questionPickerCollectionId.value = options.currentCollectionId.value;
    }

    if (questionPickerCollectionId.value) {
      void loadQuestionCandidates();
      return;
    }

    questionPickerPage.value = null;
    pickerActiveQuestionId.value = '';
  }

  function resetQuestionPickerFilters() {
    questionPickerQuery.keyword = '';
    questionPickerQuery.typeCode = '';
    questionPickerQuery.levelMin = 0;
    questionPickerQuery.levelMax = 1;
    questionPickerQuery.pageNum = 1;
    questionPickerQuery.pageSize = 10;
  }

  function handleResetQuestionPickerFilters() {
    resetQuestionPickerFilters();
    if (questionPickerCollectionId.value) {
      void loadQuestionCandidates();
    }
  }

  function onQuestionPickerCollectionChange() {
    drawerSelectedQuestionIds.value = [];
    questionPickerPage.value = null;
    pickerActiveQuestionId.value = '';
    pickerQuestionDetailLoadingId.value = '';
    pickerDetailCollapseNames.value = [];
    resetQuestionPickerFilters();

    if (!questionPickerCollectionId.value) {
      return;
    }

    void loadQuestionCandidates();
  }

  function onQuestionFilterChange() {
    if (!questionPickerCollectionId.value) {
      return;
    }
    questionPickerQuery.pageNum = 1;
    void loadQuestionCandidates();
  }

  function onQuestionPageChange(pageNum: number) {
    if (questionPickerLoading.value) {
      return;
    }
    questionPickerQuery.pageNum = pageNum;
    void loadQuestionCandidates();
  }

  function onQuestionSizeChange(pageSize: number) {
    if (questionPickerLoading.value) {
      return;
    }
    questionPickerQuery.pageSize = pageSize;
    questionPickerQuery.pageNum = 1;
    void loadQuestionCandidates();
  }

  function normalizeQuestionPickerPage(raw: unknown): QuestionSummaryPage {
    const payload = raw && typeof raw === 'object' ? (raw as Record<string, unknown>) : {};
    const recordsRaw = payload.records ?? payload.list ?? payload.items ?? [];
    const records = Array.isArray(recordsRaw) ? (recordsRaw as QuestionSummary[]) : [];
    const fallbackPageSize = questionPickerQuery.pageSize || 10;
    const pageSize = normalizeInteger(payload.pageSize ?? payload.size, fallbackPageSize, 1);
    const total = normalizeInteger(payload.total, records.length, 0);
    const fallbackPageNum = questionPickerQuery.pageNum || 1;
    const pageNum = normalizeInteger(payload.pageNum ?? payload.current ?? payload.page, fallbackPageNum, 1);
    const pagesFallback = Math.max(Math.ceil(total / pageSize), 1);
    const pages = normalizeInteger(payload.pages ?? payload.pageCount, pagesFallback, 1);

    return {
      records,
      total,
      pageNum,
      pageSize,
      pages,
      hasPrevious: Boolean(payload.hasPrevious ?? pageNum > 1),
      hasNext: Boolean(payload.hasNext ?? pageNum < pages)
    };
  }

  async function loadQuestionCandidates() {
    if (!questionPickerCollectionId.value) {
      return;
    }

    if (questionPickerLevelError.value) {
      showInfo(questionPickerLevelError.value);
      return;
    }

    questionPickerLoading.value = true;
    try {
      const response = await fetchCollectionQuestions(questionPickerCollectionId.value, {
        pageNum: questionPickerQuery.pageNum,
        pageSize: questionPickerQuery.pageSize,
        keyword: questionPickerQuery.keyword || undefined,
        typeCode: questionPickerQuery.typeCode || undefined,
        levelMin: questionPickerQuery.levelMin,
        levelMax: questionPickerQuery.levelMax,
        sortField: 'updatedAt',
        sortDirection: 'DESC'
      });
      const page = normalizeQuestionPickerPage(response);
      questionPickerPage.value = page;
      questionPickerQuery.pageNum = page.pageNum || questionPickerQuery.pageNum;
      questionPickerQuery.pageSize = page.pageSize || questionPickerQuery.pageSize;
      page.records.forEach((question) => cacheQuestionMeta(question));

      if (!page.records.length) {
        pickerActiveQuestionId.value = '';
        pickerDetailCollapseNames.value = [];
        return;
      }

      const activeStillExists = page.records.some((item) => item.id === pickerActiveQuestionId.value);
      const target = activeStillExists ? page.records.find((item) => item.id === pickerActiveQuestionId.value) ?? page.records[0] : page.records[0];
      await selectQuestionForDetail(target);
    } catch (error) {
      showError(error instanceof Error ? error.message : '加载题目列表失败');
    } finally {
      questionPickerLoading.value = false;
    }
  }

  async function loadQuestionDetail(questionId: string) {
    if (!questionId || questionDetailMap[questionId]) {
      return;
    }
    pickerQuestionDetailLoadingId.value = questionId;
    try {
      const detail = await fetchQuestionDetail(questionId);
      questionDetailMap[questionId] = detail;
    } catch (error) {
      showError(error instanceof Error ? error.message : '加载题目详情失败');
    } finally {
      if (pickerQuestionDetailLoadingId.value === questionId) {
        pickerQuestionDetailLoadingId.value = '';
      }
    }
  }

  async function selectQuestionForDetail(question: QuestionSummary) {
    pickerActiveQuestionId.value = question.id;
    pickerDetailCollapseNames.value = [];
    cacheQuestionMeta(question);
    await loadQuestionDetail(question.id);
  }

  function cacheQuestionMeta(question: QuestionSummary | { id: string; title: string; typeCode: string; difficulty?: number | null }) {
    selectedQuestionMetaMap[question.id] = {
      title: question.title,
      typeCode: question.typeCode,
      difficulty: question.difficulty ?? null
    };
  }

  function isQuestionSelectedInDrawer(questionId: string) {
    return drawerSelectedQuestionIds.value.includes(questionId);
  }

  function toggleDrawerQuestion(question: QuestionSummary, checked: string | number | boolean) {
    const shouldSelect = Boolean(checked);
    const id = question.id;
    const index = drawerSelectedQuestionIds.value.indexOf(id);

    if (shouldSelect && index === -1) {
      drawerSelectedQuestionIds.value.push(id);
      cacheQuestionMeta(question);
      return;
    }

    if (!shouldSelect && index !== -1) {
      drawerSelectedQuestionIds.value.splice(index, 1);
    }
  }

  function questionDisplayNo(index: number) {
    const pageNum = questionPickerQuery.pageNum || 1;
    const pageSize = questionPickerQuery.pageSize || 10;
    return (pageNum - 1) * pageSize + index + 1;
  }

  function clipText(text: string, limit: number) {
    if (text.length <= limit) {
      return text;
    }
    return `${text.slice(0, limit)}...`;
  }

  function resetQuestionPickerState() {
    questionPickerCollectionId.value = '';
    questionPickerPage.value = null;
    pickerActiveQuestionId.value = '';
    pickerQuestionDetailLoadingId.value = '';
    pickerDetailCollapseNames.value = [];
    resetQuestionPickerFilters();
  }

  return {
    questionPickerVisible,
    questionPickerLoading,
    questionPickerCollectionId,
    questionPickerPage,
    pickerActiveQuestionId,
    pickerDetailCollapseNames,
    questionPickerQuery,
    drawerSelectedQuestionIds,
    canApplyDrawerSelection,
    questionPickerRecords,
    pickerPageSummary,
    pickerActiveQuestionDetail,
    pickerActiveQuestionNo,
    pickerDetailOptions,
    pickerDetailAnswer,
    pickerDetailSolution,
    pickerDetailLoading,
    questionDisplayNo,
    questionPickerLevelError,
    drawerSelectedPreview,
    syncQuestionPickerCollectionId,
    openQuestionPicker,
    handleResetQuestionPickerFilters,
    onQuestionPickerCollectionChange,
    onQuestionFilterChange,
    onQuestionPageChange,
    onQuestionSizeChange,
    loadQuestionCandidates,
    selectQuestionForDetail,
    isQuestionSelectedInDrawer,
    toggleDrawerQuestion,
    cacheQuestionMeta
  };
}