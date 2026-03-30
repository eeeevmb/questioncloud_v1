<template>
  <section class="chat-page">
      <el-container class="chat-layout">
      <AgentSessionSidebar
        :sessions="sessions"
        :active-session-id="activeSessionId"
        :session-loading="sessionLoading"
        :streaming="streaming"
        :get-session-title="getSessionTitle"
        :get-session-preview="getSessionPreview"
        :format-session-time="formatSessionTime"
        @create-session="createNewSession"
        @select-session="selectSession"
        @session-command="onSessionCommand"
      />

      <el-main class="chat-main">
        <AgentToolbar
          :streaming="streaming"
          @select-questions="handleToolbarSelectQuestions"
          @create-question="handleToolbarCreateQuestion"
          @generate-paper-draft="handleToolbarGeneratePaperDraft"
        />

        <div ref="messageScrollRef" class="message-scroll">
          <AgentMessageList
            :loading="messageLoading"
            :messages="activeMessages"
            :quick-prompts="quickPrompts"
            :role-label="roleLabel"
            @apply-quick-prompt="applyQuickPrompt"
          />
        </div>

        <AgentComposer
          v-model="composerMessage"
          :active-session-id="activeSessionId"
          :streaming="streaming"
          :can-send="canSend"
          @abort="abortStreaming"
          @send="handleSend"
        />
      </el-main>
    </el-container>

    <QuestionPickerDrawer
      v-model:visible="questionPickerVisible"
      v-model:picker-detail-collapse-names="pickerDetailCollapseNames"
      :collection-loading="collectionLoading"
      :question-picker-loading="questionPickerLoading"
      :collection-options="collectionOptions"
      :question-type-options="questionTypeOptions"
      :collection-id="questionPickerCollectionId"
      :query="questionPickerQuery"
      :question-picker-level-error="questionPickerLevelError"
      :records="questionPickerRecords"
      :picker-active-question-id="pickerActiveQuestionId"
      :picker-active-question-detail="pickerActiveQuestionDetail"
      :picker-active-question-no="pickerActiveQuestionNo"
      :picker-detail-options="pickerDetailOptions"
      :picker-detail-answer="pickerDetailAnswer"
      :picker-detail-solution="pickerDetailSolution"
      :picker-detail-loading="pickerDetailLoading"
      :page="questionPickerPage"
      :picker-page-summary="pickerPageSummary"
      :drawer-selected-question-ids="drawerSelectedQuestionIds"
      :drawer-selected-preview="drawerSelectedPreview"
      :can-apply-drawer-selection="canApplyDrawerSelection"
      :can-explain-base="canExplainBase"
      :type-label="typeLabel"
      :difficulty-label="difficultyLabel"
      :question-display-no="questionDisplayNo"
      :is-question-selected-in-drawer="isQuestionSelectedInDrawer"
      @update:collection-id="questionPickerCollectionId = $event"
      @change-collection="onQuestionPickerCollectionChange"
      @filter-change="onQuestionFilterChange"
      @search="loadQuestionCandidates"
      @reset-filters="handleResetQuestionPickerFilters"
      @toggle-question="toggleDrawerQuestion"
      @select-detail="selectQuestionForDetail"
      @page-change="onQuestionPageChange"
      @size-change="onQuestionSizeChange"
      @apply-from-drawer="applyAndExplainFromDrawer"
    />

    <QuestionCreateDrawer
      v-model:visible="createQuestionDrawerVisible"
      v-model:create-question-collection-id="createQuestionCollectionId"
      v-model:create-single-correct="createSingleCorrect"
      v-model:ai-draft-single-correct="aiDraftSingleCorrect"
      :collection-loading="collectionLoading"
      :collection-options="collectionOptions"
      :question-type-options="questionTypeOptions"
      :create-mode="createMode"
      :ai-create-step="aiCreateStep"
      :create-ai-draft-loading="createAiDraftLoading"
      :create-submitting="createSubmitting"
      :has-create-collection-context="hasCreateCollectionContext"
      :ai-create-form="aiCreateForm"
      :ai-draft-form="aiDraftForm"
      :create-form="createForm"
      :is-ai-draft-choice-type="isAiDraftChoiceType"
      :is-ai-draft-single-choice="isAiDraftSingleChoice"
      :is-ai-draft-true-false-type="isAiDraftTrueFalseType"
      :is-create-choice-type="isCreateChoiceType"
      :is-create-single-choice="isCreateSingleChoice"
      :is-create-true-false-type="isCreateTrueFalseType"
      :create-guide-text="createGuideText"
      :draft-preview-title="draftPreviewTitle"
      :draft-preview-stem="draftPreviewStem"
      :draft-preview-solution="draftPreviewSolution"
      :draft-preview-options="draftPreviewOptions"
      :draft-preview-correct-answer="draftPreviewCorrectAnswer"
      :draft-preview-assumptions="draftPreviewAssumptions"
      :create-single-correct="createSingleCorrect"
      :ai-draft-single-correct="aiDraftSingleCorrect"
      @activate-ai-mode="activateAiMode"
      @add-ai-draft-option="addAiDraftOption"
      @remove-ai-draft-option="removeAiDraftOption"
      @generate-ai-draft="handleGenerateAiDraft"
      @back-to-ai-requirement="backToAiRequirementForm"
      @create-question-from-ai-draft="handleCreateQuestionFromAiDraft"
      @change-create-type="onCreateTypeChange"
      @add-create-option="addCreateOption"
      @remove-create-option="removeCreateOption"
      @toggle-create-multiple="toggleCreateMultiple"
      @create-question="handleCreateQuestion"
    />
    <PaperDraftDrawer
      v-model:visible="paperDraftDrawerVisible"
      :collection-loading="collectionLoading"
      :collection-options="collectionOptions"
      :question-type-options="questionTypeOptions"
      :paper-draft-step="paperDraftStep"
      :generate-paper-draft-loading="generatePaperDraftLoading"
      :paper-draft-error-message="paperDraftErrorMessage"
      :paper-draft-result="paperDraftResult"
      :paper-draft-form="paperDraftForm"
      :paper-draft-candidate-questions="paperDraftCandidateQuestions"
      :paper-draft-constraint-level-error="paperDraftConstraintLevelError"
      @add-constrain="addPaperDraftConstrain"
      @remove-constrain="removePaperDraftConstrain"
      @generate="handleGenerateAgentPaperDraft"
      @back-to-requirement="backToPaperRequirementForm"
    />

  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessageBox } from 'element-plus';
import {
  createAgentSession,
  deleteSession,
  fetchAgentSessions,
  fetchSessionMessages,
  streamAgentChat,
  updateSessionTitle,
  type AgentChatMessageVO,
  type AgentChatPayload,
  type AgentSessionVO
} from '../api/agent';
import { fetchCollections } from '../api/collection';
import type { CollectionView } from '../types/collection';
import type { QuestionOption } from '../types/question';
import { showError, showInfo, showSuccess } from '../utils/messages';
import AgentSessionSidebar from '../components/agent/AgentSessionSidebar.vue';
import AgentToolbar from '../components/agent/AgentToolbar.vue';
import AgentMessageList from '../components/agent/AgentMessageList.vue';
import AgentComposer from '../components/agent/AgentComposer.vue';
import QuestionPickerDrawer from '../components/agent/QuestionPickerDrawer.vue';
import QuestionCreateDrawer from '../components/agent/QuestionCreateDrawer.vue';
import PaperDraftDrawer from '../components/agent/PaperDraftDrawer.vue';
import { usePaperDraft } from '../composables/usePaperDraft';
import { useQuestionCreate } from '../composables/useQuestionCreate';
import { useQuestionPicker } from '../composables/useQuestionPicker';
import type { MessageItem, MessageRole, QuestionPickerQueryState, ResultType } from '../types/agent-demo';

interface SendMessageOptions {
  toolContext?: {
    collectionId?: string;
    selectedQuestionIds?: string[];
  };
}

const AGENT_NAME = '题库小助手';
const quickPrompts = ['帮我讲解这道题', '帮我总结这组题的考点', '给我出几道同类型练习题'];

const questionTypeOptions = [
  { code: 'single-choice', label: '单选题' },
  { code: 'multiple-choice', label: '多选题' },
  { code: 'true-false', label: '判断题' },
  { code: 'fill-in', label: '填空题' },
  { code: 'short-answer', label: '简答题' }
];

const typeLabelMap = Object.fromEntries(questionTypeOptions.map((item) => [item.code, item.label])) as Record<string, string>;

const contextState = reactive({
  collectionIdValues: [] as string[]
});
const composerMessage = ref('');

const sessions = ref<AgentSessionVO[]>([]);
const activeSessionId = ref('');
const messagesBySession = reactive<Record<string, MessageItem[]>>({});
const streaming = ref(false);
const currentAbortController = ref<AbortController | null>(null);
const collectionLoading = ref(false);
const sessionLoading = ref(false);
const messageLoading = ref(false);
const collectionOptions = ref<CollectionView[]>([]);
const messageScrollRef = ref<HTMLElement | null>(null);

const currentCollectionId = computed(() => contextState.collectionIdValues[0] ?? '');
const {
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
  toggleDrawerQuestion
} = useQuestionPicker({
  currentCollectionId,
  normalizeText,
  normalizeDraftOptions,
  parseJudgeAnswer
});
const {
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
} = useQuestionCreate({
  currentCollectionId,
  normalizeText,
  normalizeDraftOptions,
  parseJudgeAnswer,
  onQuestionCreated: (meta) => {
    contextState.collectionIdValues = [meta.collectionId];
  }
});
const {
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
} = usePaperDraft(currentCollectionId);

const activeMessages = computed(() => {
  if (!activeSessionId.value) {
    return [];
  }
  return messagesBySession[activeSessionId.value] ?? [];
});

const canSend = computed(() => {
  if (streaming.value || !activeSessionId.value) {
    return false;
  }
  const hasMessage = Boolean(composerMessage.value.trim());
  return hasMessage;
});

const canExplainBase = computed(() => {
  if (streaming.value) {
    return false;
  }
  if (!activeSessionId.value) {
    return false;
  }
  return true;
});

onMounted(async () => {
  await Promise.all([loadCollections(), loadSessions()]);
  if (sessions.value.length) {
    await selectSession(String(sessions.value[0].sessionId));
  } else {
    await createNewSession();
  }
});

async function loadCollections() {
  collectionLoading.value = true;
  try {
    const list = await fetchCollections();
    collectionOptions.value = list;

    if (currentCollectionId.value && !list.some((item) => item.collectionId === currentCollectionId.value)) {
      contextState.collectionIdValues = [];
    }
    syncQuestionPickerCollectionId(list.map((item) => item.collectionId));
    syncCreateCollectionId(list.map((item) => item.collectionId));
    syncPaperDraftCollectionIds(list.map((item) => item.collectionId));

  } catch (error) {
    showError(error instanceof Error ? error.message : '加载题集失败');
  } finally {
    collectionLoading.value = false;
  }
}

async function loadSessions() {
  sessionLoading.value = true;
  try {
    const list = await fetchAgentSessions();
    sessions.value = sortSessions(list);
  } catch (error) {
    showError(error instanceof Error ? error.message : '加载会话列表失败');
  } finally {
    sessionLoading.value = false;
  }
}

function sortSessions(list: AgentSessionVO[]) {
  return [...list].sort((a, b) => {
    const aTime = Date.parse(a.updatedAt || a.createdAt || '') || 0;
    const bTime = Date.parse(b.updatedAt || b.createdAt || '') || 0;
    return bTime - aTime;
  });
}

async function createNewSession() {
  if (streaming.value) {
    return;
  }
  try {
    const session = await createAgentSession(AGENT_NAME);
    const sid = String(session.sessionId);
    sessions.value = sortSessions([session, ...sessions.value]);
    activeSessionId.value = sid;
    messagesBySession[sid] = [];
    composerMessage.value = '';
    await nextTick();
    scrollToBottom();
    showSuccess('新会话已创建');
  } catch (error) {
    showError(error instanceof Error ? error.message : '创建会话失败');
  }
}

async function selectSession(sessionId: string) {
  if (!sessionId || (streaming.value && sessionId !== activeSessionId.value)) {
    if (streaming.value) {
      showInfo('正在生成回复，请先等待当前会话完成');
    }
    return;
  }

  activeSessionId.value = sessionId;
  if (messagesBySession[sessionId]) {
    await nextTick();
    scrollToBottom();
    return;
  }

  messageLoading.value = true;
  try {
    const rawMessages = await fetchSessionMessages(sessionId);
    messagesBySession[sessionId] = mapServerMessages(rawMessages, sessionId);
  } catch (error) {
    showError(error instanceof Error ? error.message : '加载会话消息失败');
    messagesBySession[sessionId] = [];
  } finally {
    messageLoading.value = false;
    await nextTick();
    scrollToBottom();
  }
}

async function refreshSessionsKeepCurrent() {
  const current = activeSessionId.value;
  await loadSessions();
  if (!current) {
    return;
  }
  const exists = sessions.value.some((session) => String(session.sessionId) === current);
  if (!exists && sessions.value.length) {
    await selectSession(String(sessions.value[0].sessionId));
  }
}

async function handleSessionCommand(command: string, session: AgentSessionVO) {
  const sid = String(session.sessionId);

  if (command === 'rename') {
    let title = '';
    try {
      const result = await ElMessageBox.prompt('请输入新的会话标题，最多 20 个字符', '重命名会话', {
        inputValue: session.title ?? '',
        inputPattern: /^.{1,20}$/,
        inputErrorMessage: '标题长度需为 1-20 个字符',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      });
      title = result.value;
    } catch {
      return;
    }

    try {
      await updateSessionTitle(sid, title);
      showSuccess('会话已重命名');
      await refreshSessionsKeepCurrent();
    } catch (error) {
      showError(error instanceof Error ? error.message : '重命名会话失败');
    }
    return;
  }

  if (command === 'delete') {
    try {
      await ElMessageBox.confirm('删除后将无法恢复，是否继续删除当前会话？', '删除会话', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      });
    } catch {
      return;
    }

    try {
      await deleteSession(sid);
      delete messagesBySession[sid];
      if (activeSessionId.value === sid) {
        activeSessionId.value = '';
      }
      showSuccess('会话已删除');
      await loadSessions();
      if (!activeSessionId.value && sessions.value.length) {
        await selectSession(String(sessions.value[0].sessionId));
      }
    } catch (error) {
      showError(error instanceof Error ? error.message : '删除会话失败');
    }
  }
}

function onSessionCommand(command: string | number | object, session: AgentSessionVO) {
  void handleSessionCommand(String(command), session);
}

function ensureCollectionsLoaded() {
  if (!collectionOptions.value.length && !collectionLoading.value) {
    void loadCollections();
  }
}

function handleToolbarSelectQuestions() {
  ensureCollectionsLoaded();
  openQuestionPicker();
}

function handleToolbarCreateQuestion() {
  ensureCollectionsLoaded();
  openCreateQuestionDrawer();
}

function handleToolbarGeneratePaperDraft() {
  ensureCollectionsLoaded();
  openPaperDraftDrawer();
}

function normalizeText(value: unknown): string {
  if (value === undefined || value === null) {
    return '';
  }
  return String(value).trim();
}

function normalizeDraftOptions(rawOptions: QuestionOption[] | null | undefined, includeEmpty = false): QuestionOption[] {
  if (!Array.isArray(rawOptions)) {
    return [];
  }
  return rawOptions
    .map((item, index) => {
      const key = normalizeText(item?.key).toUpperCase() || String.fromCharCode(65 + index);
      const content = normalizeText(item?.content);
      return { key, content };
    })
    .filter((item) => (includeEmpty ? item.key : item.key && item.content));
}

function parseJudgeAnswer(input?: string | null): 'T' | 'F' | null {
  const normalized = normalizeText(input).toUpperCase();
  if (!normalized) {
    return null;
  }
  if (['T', 'TRUE', '正确', '对', '是'].includes(normalized)) {
    return 'T';
  }
  if (['F', 'FALSE', '错误', '错', '否'].includes(normalized)) {
    return 'F';
  }
  return null;
}

async function applyAndExplainFromDrawer() {
  if (!canExplainBase.value) {
    if (streaming.value) {
      showInfo('正在生成回复，请稍后再发起题目讲解');
    } else if (!activeSessionId.value) {
      showInfo('请先创建或选择一个会话');
    }
    return;
  }
  if (!questionPickerCollectionId.value) {
    showInfo('请先选择题集');
    return;
  }
  if (!drawerSelectedQuestionIds.value.length) {
    showInfo('请先勾选题目');
    return;
  }
  const selectedQuestionIds = normalizeLongIdList(drawerSelectedQuestionIds.value);
  if (!selectedQuestionIds.length) {
    showInfo('所选题目无效，请重新选择');
    return;
  }
  contextState.collectionIdValues = [questionPickerCollectionId.value];
  questionPickerVisible.value = false;
  await sendChatMessage('请基于我刚刚选择的题目，逐题讲解考点、解题思路和易错点。', 'explain', {
    toolContext: {
      collectionId: questionPickerCollectionId.value,
      selectedQuestionIds
    }
  });
}

async function handleSend() {
  const userText = composerMessage.value.trim();
  if (!userText) {
    return;
  }
  composerMessage.value = '';
  await sendChatMessage(userText, inferResultTypeFromText(userText));
}

async function sendChatMessage(
  message: string,
  resultType: ResultType = 'general',
  options: SendMessageOptions = {}
): Promise<string> {
  if (!activeSessionId.value || streaming.value) {
    return '';
  }

  const collectionIds = options.toolContext?.collectionId ? normalizeLongIdList([options.toolContext.collectionId]) : [];
  const selectedIds = options.toolContext?.selectedQuestionIds
    ? normalizeLongIdList(options.toolContext.selectedQuestionIds)
    : [];
  const sid = activeSessionId.value;
  const list = ensureSessionMessages(sid);

  appendMessage(list, 'user', message, resultType);
  const assistantMessageId = appendPendingAssistantMessage(list, resultType);
  await nextTick();
  scrollToBottom();

  streaming.value = true;
  const controller = new AbortController();
  currentAbortController.value = controller;

  const payload: AgentChatPayload = {
    sessionId: sid,
    message,
    agentName: AGENT_NAME,
    context: {
      collectionIds,
      selectedQuestionIds: selectedIds
    }
  };

  let assistantFinalText = '';
  try {
    await streamAgentChat(
      payload,
      {
        onChunk: (text) => {
          updateMessage(list, assistantMessageId, (prev) => prev + text);
          scrollToBottom();
        },
        onDone: () => {
          const assistant = list.find((item) => item.id === assistantMessageId);
          if (assistant && assistant.role === 'assistant') {
            if (!assistant.content.trim()) {
              assistant.content = '已收到请求，但暂未返回可展示内容。';
            }
            assistant.status = 'done';
          }
          streaming.value = false;
          currentAbortController.value = null;
        }
      },
      controller.signal
    );
    const assistant = list.find((item) => item.id === assistantMessageId);
    assistantFinalText = assistant?.content ?? '';
    await refreshSessionsKeepCurrent();
  } catch (error) {
    if (!controller.signal.aborted) {
      showError(error instanceof Error ? error.message : '发送消息失败');
      const assistant = list.find((item) => item.id === assistantMessageId);
      if (assistant && assistant.role === 'assistant') {
        assistant.content = assistant.content.trim() || '消息发送失败，请稍后重试。';
        assistant.status = 'error';
      }
    }
  } finally {
    streaming.value = false;
    currentAbortController.value = null;
    await nextTick();
    scrollToBottom();
  }
  return assistantFinalText;
}

function ensureSessionMessages(sessionId: string): MessageItem[] {
  if (!messagesBySession[sessionId]) {
    messagesBySession[sessionId] = [];
  }
  return messagesBySession[sessionId];
}

function appendMessage(list: MessageItem[], role: MessageRole, content: string, resultType?: ResultType): string {
  const id = `${Date.now()}_${Math.random().toString(36).slice(2)}`;
  list.push({
    id,
    role,
    content,
    status: role === 'assistant' ? 'done' : undefined,
    resultType
  });
  return id;
}

function appendPendingAssistantMessage(list: MessageItem[], resultType: ResultType): string {
  const id = `${Date.now()}_${Math.random().toString(36).slice(2)}`;
  list.push({
    id,
    role: 'assistant',
    content: '',
    status: 'pending',
    resultType
  });
  return id;
}

function updateMessage(list: MessageItem[], id: string, updater: (prev: string) => string) {
  const target = list.find((item) => item.id === id);
  if (!target) {
    return;
  }
  target.content = updater(target.content);
  if (target.role === 'assistant') {
    target.status = target.content.trim() ? 'streaming' : target.status;
  }
}

function normalizeLongIdList(values: string[]): string[] {
  return values
    .map((item) => item.trim())
    .filter((item) => /^\d+$/.test(item))
    .filter((item) => {
      try {
        return BigInt(item) > 0n;
      } catch {
        return false;
      }
    });
}

function mapServerMessages(rawMessages: AgentChatMessageVO[], sessionId: string): MessageItem[] {
  let latestIntent: ResultType = 'general';
  return rawMessages
    .map((item, index) => {
      const role = normalizeRole(item.role);
      const rawContent = (item.text ?? item.thinking ?? '').trim();
      const content = role === 'assistant' ? normalizeWelcomeMessage(rawContent) : rawContent;
      if (role === 'user') {
        latestIntent = inferResultTypeFromText(content);
      }
      return {
        id: `${sessionId}_${index}`,
        role,
        content,
        status: role === 'assistant' ? 'done' : undefined,
        resultType: role === 'assistant' ? latestIntent : undefined
      } as MessageItem;
    })
    .filter((item) => item.role !== 'system' && Boolean(item.content));
}

function normalizeRole(role?: string): MessageRole {
  if (role === 'assistant') {
    return 'assistant';
  }
  if (role === 'user') {
    return 'user';
  }
  return 'system';
}

function normalizeWelcomeMessage(content: string) {
  if (!content) {
    return '';
  }
  return content;
}

function roleLabel(role: MessageRole) {
  if (role === 'assistant') {
    return '助手';
  }
  if (role === 'user') {
    return '我';
  }
  return '系统';
}

function getSessionTitle(session: AgentSessionVO, index: number) {
  const title = session.title?.trim();
  if (title) {
    return title;
  }
  return index === 0 ? '新会话' : '未命名会话';
}

function inferResultTypeFromText(input: string): ResultType {
  const text = input.trim();
  if (!text) {
    return 'general';
  }
  if (
    text.includes('讲解') ||
    text.includes('解析') ||
    text.includes('思路') ||
    text.includes('分析') ||
    text.includes('为什么') ||
    text.includes('考点')
  ) {
    return 'explain';
  }
  if (
    text.includes('出题') ||
    text.includes('生成题') ||
    text.includes('练习题') ||
    text.includes('模拟题') ||
    text.includes('同类型题')
  ) {
    return 'generate';
  }
  if (
    text.includes('查找') ||
    text.includes('搜索') ||
    text.includes('检索') ||
    text.includes('搜题') ||
    text.includes('找题') ||
    text.includes('题库') ||
    text.includes('筛选')
  ) {
    return 'search';
  }
  return 'general';
}

function getSessionPreview(sessionId: string) {
  const list = messagesBySession[sessionId];
  if (!list?.length) {
    return '等待开始学习';
  }
  const userMessages = list.filter((item) => item.role === 'user' && item.content.trim());
  if (!userMessages.length) {
    return '等待你的提问';
  }
  const intents = userMessages.map((item) => inferResultTypeFromText(item.content));
  const hasExplain = intents.includes('explain');
  const hasSearch = intents.includes('search');
  const hasGenerate = intents.includes('generate');

  if (hasSearch && hasExplain) {
    return '检索题目并讲解';
  }
  if (hasExplain) {
    return '题目讲解中';
  }
  if (hasSearch) {
    return '题目检索中';
  }
  if (hasGenerate) {
    return 'AI 出题中';
  }
  return '学习交流中';
}

function formatSessionTime(value?: string) {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  return date.toLocaleString();
}

function typeLabel(code?: string) {
  if (!code) {
    return '未知题型';
  }
  return typeLabelMap[code] ?? code;
}

function difficultyLabel(difficulty?: number | null) {
  if (difficulty === undefined || difficulty === null) {
    return '难度未知';
  }
  return `难度 ${difficulty.toFixed(2)}`;
}

function applyQuickPrompt(prompt: string) {
  composerMessage.value = prompt;
}

function abortStreaming() {
  currentAbortController.value?.abort();
  currentAbortController.value = null;
  streaming.value = false;
}

function scrollToBottom() {
  const el = messageScrollRef.value;
  if (!el) {
    return;
  }
  el.scrollTop = el.scrollHeight;
}

onBeforeUnmount(() => {
  currentAbortController.value?.abort();
});
</script>

<style scoped>
.chat-page {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  overscroll-behavior-y: none;
}

.chat-layout {
  height: 100%;
  min-height: 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
}

.session-aside {
  min-height: 0;
  border-right: 1px solid var(--el-border-color-light);
  background: #f7f9fc;
  display: flex;
  flex-direction: column;
}

.aside-header {
  padding: 14px 12px;
  border-bottom: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.aside-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.aside-title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.aside-subtitle {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.session-loading {
  padding: 12px;
}

.session-scroll {
  flex: 1;
  min-height: 0;
  padding: 10px;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.session-item {
  border: 1px solid #e9edf5;
  border-radius: 12px;
  background: #fff;
  display: flex;
  align-items: center;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.session-item:hover {
  border-color: var(--el-color-primary-light-6);
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.06);
}

.session-item.active {
  border-color: var(--el-color-primary);
  background: linear-gradient(120deg, var(--el-color-primary-light-9), #ffffff 75%);
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.14);
}

.session-main {
  flex: 1;
  border: none;
  background: transparent;
  text-align: left;
  padding: 10px 10px 10px 12px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-main .title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-main .preview {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-main .time {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.session-meta-line {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.session-more {
  padding: 6px 9px;
  border-radius: 8px;
  color: var(--el-text-color-secondary);
}

.session-more:hover {
  background: var(--el-fill-color-light);
}

.chat-main {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 0;
  min-height: 0;
}

.toolbar-card {
  border: none;
  border-bottom: 1px solid var(--el-border-color-light);
  border-radius: 0;
}

.toolbar-card :deep(.el-card__body) {
  padding: 8px 12px;
}

.workspace-toolbar {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tool-btn {
  height: 40px;
  padding: 0 22px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
}

.tool-btn-secondary {
  height: 40px;
  padding: 0 18px;
  font-size: 14px;
  border-radius: 10px;
}

.message-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 16px 18px;
  background: linear-gradient(180deg, #f8fbff 0%, #f3f7fd 60%, #f8fbff 100%);
  overscroll-behavior: contain;
}

.loading-wrap {
  padding: 10px;
}

.empty-wrap {
  min-height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 10px;
}

.empty-tip {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 980px;
  margin: 0 auto;
}

.message-row {
  display: flex;
}

.message-row.user {
  justify-content: flex-end;
}

.message-row.assistant,
.message-row.system {
  justify-content: flex-start;
}

.message-bubble {
  max-width: min(74ch, 86%);
  border-radius: 14px;
  padding: 11px 13px;
  border: 1px solid #e5eaf2;
  background: #fff;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.06);
}

.message-bubble.pending {
  background: var(--el-fill-color-light);
}

.message-bubble.error {
  border-color: var(--el-color-danger-light-5);
  background: var(--el-color-danger-light-9);
}

.message-row.user .message-bubble {
  background: linear-gradient(120deg, #e8f1ff, #f3f8ff 70%);
  border-color: #bcd7ff;
}

.message-bubble .role {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.message-bubble .content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
}

.message-bubble .pending-text {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.composer-wrap {
  border-top: 1px solid var(--el-border-color-light);
  padding: 10px 12px 12px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), #ffffff);
  backdrop-filter: blur(6px);
}

.composer-card {
  border: 1px solid #d9e3f0;
  border-radius: 14px;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
}

.composer-card :deep(.el-card__body) {
  padding: 10px 12px;
}

.composer-card :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  background: transparent;
  padding: 2px 0;
  font-size: 14px;
  line-height: 1.7;
}

.composer-head {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.composer-title {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.composer-footer {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.action-group {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

@media (max-width: 1024px) {
  .chat-page {
    height: 100%;
    min-height: 0;
  }

  .chat-layout {
    display: flex;
    flex-direction: column;
  }

  .session-aside {
    width: 100% !important;
    max-height: 280px;
    border-right: none;
    border-bottom: 1px solid var(--el-border-color-light);
  }

  .workspace-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .tool-btn,
  .tool-btn-secondary {
    width: 100%;
  }

  .composer-footer {
    flex-direction: column;
    align-items: flex-start;
  }

}
</style>
