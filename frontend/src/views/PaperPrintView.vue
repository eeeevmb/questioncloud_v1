<template>
  <section class="paper-print-page">
    <div class="print-toolbar">
      <div>
        <el-button text class="back-link-button" @click="goBack">返回试卷</el-button>
        <p class="eyebrow">试卷排版</p>
        <h2>{{ paper?.title || '试卷排版' }}</h2>
        <p class="toolbar-desc">按正式试卷模板分大题展示，左侧可在同一题型内拖拽调整小题顺序。</p>
      </div>
      <div class="toolbar-actions">
        <el-switch v-model="showAnswers" active-text="显示答案解析" inactive-text="仅试题" />
        <el-button :disabled="printItems.length < 2" @click="sortByType">按题型整理</el-button>
        <el-button :disabled="!paper?.items?.length" @click="resetOrder">重置顺序</el-button>
        <el-button :disabled="!printItems.length" @click="handleBrowserPrint">浏览器打印</el-button>
        <el-button type="primary" :loading="exportingPdf" :disabled="!printItems.length" @click="handleExportPdf">
          导出 PDF
        </el-button>
      </div>
    </div>

    <div v-loading="loading || paginating" class="print-workspace">
      <aside class="order-panel">
        <div class="template-settings">
          <div class="template-settings__title">
            <strong>模板设置</strong>
            <span>控制试卷抬头展示</span>
          </div>
          <div class="settings-list">
            <label class="setting-row">
              <span>绝密启用前</span>
              <el-switch v-model="headerSettings.showConfidential" />
            </label>
            <label class="setting-row">
              <span>科目大标题</span>
              <el-switch v-model="headerSettings.showSubjectTitle" />
            </label>
            <el-input
              v-if="headerSettings.showSubjectTitle"
              v-model="headerSettings.subjectTitle"
              placeholder="请输入科目大标题"
              maxlength="20"
              show-word-limit
            />
            <label class="setting-row">
              <span>试卷描述</span>
              <el-switch v-model="headerSettings.showDescription" />
            </label>
            <label class="setting-row">
              <span>注意事项</span>
              <el-switch v-model="headerSettings.showNotice" />
            </label>
          </div>
          <el-input
            v-if="headerSettings.showNotice"
            v-model="headerSettings.noticeText"
            type="textarea"
            :rows="4"
            resize="none"
            placeholder="每行一条注意事项"
          />
        </div>

        <div class="panel-head">
          <div>
            <h3>题目顺序</h3>
            <span>共 {{ printItems.length }} 题，总分 {{ totalScore }}</span>
          </div>
        </div>

        <div v-if="printSections.length" class="order-section-list">
          <section v-for="section in printSections" :key="section.typeCode" class="order-section">
            <div class="order-section-title">
              <div class="order-section-heading">
                <template v-if="editingSectionType === section.typeCode">
                  <el-input
                    v-model="sectionTitleDraft"
                    size="small"
                    maxlength="80"
                    @blur="saveSectionTitle(section)"
                    @keyup.enter="saveSectionTitle(section)"
                    @keyup.esc="cancelSectionTitleEdit"
                  />
                </template>
                <strong v-else>{{ section.heading }}</strong>
                <span>{{ section.items.length }} 题</span>
              </div>
              <div class="section-title-actions">
                <el-button
                  text
                  size="small"
                  @click="startSectionTitleEdit(section)"
                >
                  编辑
                </el-button>
                <el-button
                  v-if="sectionTitleOverrides[section.typeCode]"
                  text
                  size="small"
                  @click="resetSectionTitle(section.typeCode)"
                >
                  恢复
                </el-button>
              </div>
            </div>
            <div class="order-list">
              <div
                v-for="(item, index) in section.items"
                :key="item.key"
                class="order-item"
                :class="{ dragging: draggedKey === item.key }"
                draggable="true"
                @dragstart="handleDragStart($event, section.typeCode, item.key)"
                @dragover.prevent="handleDragOver"
                @drop="handleDrop($event, section.typeCode, index)"
                @dragend="handleDragEnd"
              >
                <span class="drag-grip">☰</span>
                <span class="order-number">{{ index + 1 }}</span>
                <div class="order-copy">
                  <strong>{{ item.detail?.title || item.paperItem.questionTitle || '无标题' }}</strong>
                  <span>{{ formatScore(item.paperItem.score) }} 分</span>
                </div>
              </div>
            </div>
          </section>
        </div>
        <el-empty v-else description="暂无试卷题目" />
      </aside>

      <main class="preview-panel">
        <div class="paper-document" ref="sheetRef">
          <section
            v-for="page in printPages"
            :key="page.key"
            class="paper-page"
          >
            <div class="paper-page__body">
              <template v-for="block in page.blocks" :key="block.key">
                <header
                  v-if="block.type === 'exam-header'"
                  class="exam-header print-block print-block--header"
                >
                  <p v-if="headerSettings.showConfidential" class="confidential-line">绝密★启用前</p>
                  <h1>{{ paper?.title || '未命名试卷' }}</h1>
                  <p v-if="headerSettings.showDescription && paper?.description" class="exam-description">{{ paper.description }}</p>
                  <h2 v-if="headerSettings.showSubjectTitle">{{ normalizedSubjectTitle }}</h2>
                  <div v-if="headerSettings.showNotice && noticeLines.length" class="notice-block">
                    <strong>注意事项：</strong>
                    <p v-for="(notice, index) in noticeLines" :key="`preview_notice_${index}`">{{ index + 1 }}. {{ notice }}</p>
                  </div>
                  <div class="student-line">
                    <span>姓名：________________</span>
                    <span>学号：________________</span>
                    <span>班级：________________</span>
                  </div>
                </header>

                <div
                  v-else-if="block.type === 'section-title'"
                  class="print-block print-block--section-title"
                >
                  <h3 class="major-title">{{ block.section.heading }}</h3>
                </div>

                <article
                  v-else
                  class="print-question print-block print-block--question"
                >
                  <div class="question-row">
                    <span class="question-index">{{ block.globalIndex }}.</span>
                    <MathView class="print-math" :content="block.item.detail?.stem || block.item.paperItem.stem || '—'" />
                  </div>

                  <ol
                    v-if="block.item.detail?.options?.length"
                    class="option-list"
                    :class="`option-list--${resolveOptionLayout(block.item)}`"
                  >
                    <li v-for="option in block.item.detail.options" :key="`${block.item.key}_${option.key}`">
                      <span class="option-key">{{ option.key }}.</span>
                      <MathView class="option-math" :content="option.content || '—'" />
                    </li>
                  </ol>

                  <div
                    v-if="shouldRenderShortAnswerSpace(block.item)"
                    class="short-answer-space"
                    :style="{ height: `${resolveShortAnswerSpaceMm(block.item)}mm` }"
                  />

                  <div v-if="showAnswers" class="answer-block">
                    <div>
                      <span class="answer-label">答案</span>
                      <MathView class="answer-math" :content="resolveAnswer(block.item.detail)" />
                    </div>
                    <div>
                      <span class="answer-label">解析</span>
                      <MathView class="answer-math" :content="block.item.detail?.solution || '暂无解析'" />
                    </div>
                  </div>
                </article>
              </template>
            </div>
          </section>
        </div>

        <div class="paper-page paper-page--measure" ref="measurePageRef" aria-hidden="true">
          <div class="paper-page__body" ref="measureBodyRef">
            <template v-for="block in printBlocks" :key="`measure_${block.key}`">
              <header
                v-if="block.type === 'exam-header'"
                class="exam-header print-block print-block--header"
                :data-block-key="block.key"
              >
                <p v-if="headerSettings.showConfidential" class="confidential-line">绝密★启用前</p>
                <h1>{{ paper?.title || '未命名试卷' }}</h1>
                <p v-if="headerSettings.showDescription && paper?.description" class="exam-description">{{ paper.description }}</p>
                <h2 v-if="headerSettings.showSubjectTitle">{{ normalizedSubjectTitle }}</h2>
                <div v-if="headerSettings.showNotice && noticeLines.length" class="notice-block">
                  <strong>注意事项：</strong>
                  <p v-for="(notice, index) in noticeLines" :key="`measure_notice_${index}`">{{ index + 1 }}. {{ notice }}</p>
                </div>
                <div class="student-line">
                  <span>姓名：________________</span>
                  <span>学号：________________</span>
                  <span>班级：________________</span>
                </div>
              </header>

              <div
                v-else-if="block.type === 'section-title'"
                class="print-block print-block--section-title"
                :data-block-key="block.key"
              >
                <h3 class="major-title">{{ block.section.heading }}</h3>
              </div>

              <article
                v-else
                class="print-question print-block print-block--question"
                :data-block-key="block.key"
              >
                <div class="question-row">
                  <span class="question-index">{{ block.globalIndex }}.</span>
                  <MathView class="print-math" :content="block.item.detail?.stem || block.item.paperItem.stem || '—'" />
                </div>

                <ol
                  v-if="block.item.detail?.options?.length"
                  class="option-list"
                  :class="`option-list--${resolveOptionLayout(block.item)}`"
                >
                  <li v-for="option in block.item.detail.options" :key="`${block.item.key}_${option.key}`">
                    <span class="option-key">{{ option.key }}.</span>
                    <MathView class="option-math" :content="option.content || '—'" />
                  </li>
                </ol>

                <div
                  v-if="shouldRenderShortAnswerSpace(block.item)"
                  class="short-answer-space"
                  :style="{ height: `${resolveShortAnswerSpaceMm(block.item)}mm` }"
                />

                <div v-if="showAnswers" class="answer-block">
                  <div>
                    <span class="answer-label">答案</span>
                    <MathView class="answer-math" :content="resolveAnswer(block.item.detail)" />
                  </div>
                  <div>
                    <span class="answer-label">解析</span>
                    <MathView class="answer-math" :content="block.item.detail?.solution || '暂无解析'" />
                  </div>
                </div>
              </article>
            </template>
          </div>
        </div>
      </main>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import html2canvas from 'html2canvas';
import { jsPDF } from 'jspdf';
import MathView from '../components/MathView.vue';
import { fetchPaperDetail } from '../api/paper';
import { fetchQuestionDetail } from '../api/question';
import type { PaperDetailVO, PaperItemVO } from '../types/paper';
import type { QuestionDetail } from '../types/question';
import { loadMathJax } from '../plugins/mathjax';
import { showError, showSuccess } from '../utils/messages';

interface PrintItem {
  key: string;
  paperItem: PaperItemVO;
  detail: QuestionDetail | null;
}

interface PrintSection {
  typeCode: string;
  title: string;
  heading: string;
  items: PrintItem[];
  startIndex: number;
}

interface ExamHeaderBlock {
  type: 'exam-header';
  key: string;
}

interface SectionTitleBlock {
  type: 'section-title';
  key: string;
  section: PrintSection;
}

interface QuestionBlock {
  type: 'question';
  key: string;
  section: PrintSection;
  item: PrintItem;
  sectionIndex: number;
  globalIndex: number;
}

type PrintBlock = ExamHeaderBlock | SectionTitleBlock | QuestionBlock;

interface PrintPage {
  key: string;
  blocks: PrintBlock[];
}

interface HeaderSettings {
  showConfidential: boolean;
  showSubjectTitle: boolean;
  showDescription: boolean;
  showNotice: boolean;
  subjectTitle: string;
  noticeText: string;
}

type OptionLayout = 1 | 2 | 4;

const route = useRoute();
const router = useRouter();

const paper = ref<PaperDetailVO | null>(null);
const printItems = ref<PrintItem[]>([]);
const loading = ref(false);
const exportingPdf = ref(false);
const paginating = ref(false);
const showAnswers = ref(false);
const sectionTitleOverrides = ref<Record<string, string>>({});
const editingSectionType = ref('');
const sectionTitleDraft = ref('');
const headerSettings = ref<HeaderSettings>({
  showConfidential: false,
  showSubjectTitle: false,
  showDescription: false,
  showNotice: false,
  subjectTitle: '数学',
  noticeText: [
    '答卷前，考生务必将自己的姓名、准考证号填写在答题卡上。',
    '回答选择题时，选出每小题答案后，用铅笔把答题卡上对应题目的答案标号涂黑。',
    '考试结束后，请将本试卷和答题卡一并交回。'
  ].join('\n')
});
const draggedKey = ref('');
const draggedTypeCode = ref('');
const sheetRef = ref<HTMLElement | null>(null);
const measurePageRef = ref<HTMLElement | null>(null);
const measureBodyRef = ref<HTMLElement | null>(null);
const printPages = ref<PrintPage[]>([]);
let paginationRunId = 0;

const typeOrder: Record<string, number> = {
  'single-choice': 1,
  'multiple-choice': 2,
  'fill-in': 3,
  'true-false': 4,
  'short-answer': 5
};

const typeOptions = [
  { title: '选择题', value: 'single-choice' },
  { title: '多项选择题', value: 'multiple-choice' },
  { title: '填空题', value: 'fill-in' },
  { title: '判断题', value: 'true-false' },
  { title: '解答题', value: 'short-answer' }
];

const chineseSectionNumbers = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十'];
const pageHeightSafetyPx = 4;

const paperId = computed(() => String(route.params.paperId ?? ''));
const totalScore = computed(() => formatScore(printItems.value.reduce((sum, item) => sum + Number(item.paperItem.score ?? 0), 0)));
const noticeLines = computed(() =>
  headerSettings.value.noticeText
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
);
const normalizedSubjectTitle = computed(() => headerSettings.value.subjectTitle.trim() || '数学');
const printSections = computed<PrintSection[]>(() => {
  let startIndex = 1;
  return typeOptions
    .map((typeOption) => {
      const items = printItems.value.filter((item) => getItemTypeCode(item) === typeOption.value);
      if (!items.length) {
        return null;
      }
      const scoreTotal = items.reduce((sum, item) => sum + Number(item.paperItem.score ?? 0), 0);
      const uniformScore = resolveUniformScore(items);
      const section: PrintSection = {
        typeCode: typeOption.value,
        title: typeOption.title,
        heading: '',
        items,
        startIndex
      };
      startIndex += items.length;
      return section;
    })
    .filter((section): section is PrintSection => Boolean(section))
    .map((section, sectionIndex) => {
      const defaultHeading = buildDefaultSectionHeading(section, sectionIndex);
      return {
        ...section,
        heading: sectionTitleOverrides.value[section.typeCode] || defaultHeading
      };
    });
});
const printBlocks = computed<PrintBlock[]>(() => {
  if (!paper.value || !printSections.value.length) {
    return [];
  }
  const blocks: PrintBlock[] = [{ type: 'exam-header', key: 'exam-header' }];
  printSections.value.forEach((section) => {
    blocks.push({
      type: 'section-title',
      key: `section_${section.typeCode}`,
      section
    });
    section.items.forEach((item, index) => {
      blocks.push({
        type: 'question',
        key: `question_${item.key}`,
        section,
        item,
        sectionIndex: index,
        globalIndex: section.startIndex + index
      });
    });
  });
  return blocks;
});

onMounted(() => {
  void loadPaper();
});

watch(
  [printBlocks, showAnswers, headerSettings, sectionTitleOverrides],
  () => {
    void schedulePagination();
  },
  { deep: true, flush: 'post' }
);

async function loadPaper() {
  if (!paperId.value) {
    return;
  }
  loading.value = true;
  try {
    const detail = await fetchPaperDetail(paperId.value);
    paper.value = detail;
    printItems.value = await buildPrintItems(detail.items ?? []);
    sortByType();
  } catch (error) {
    showError(error instanceof Error ? error.message : '加载试卷失败');
  } finally {
    loading.value = false;
  }
}

async function buildPrintItems(items: PaperItemVO[]) {
  const detailEntries = await Promise.all(
    items.map(async (item) => {
      try {
        return [item.questionId, await fetchQuestionDetail(item.questionId)] as const;
      } catch {
        return [item.questionId, null] as const;
      }
    })
  );
  const detailMap = new Map(detailEntries);
  return items
    .slice()
    .sort((a, b) => Number(a.seq ?? 0) - Number(b.seq ?? 0))
    .map((item) => ({
      key: `${item.questionId}-${item.questionVersionId}`,
      paperItem: item,
      detail: detailMap.get(item.questionId) ?? null
    }));
}

function resetOrder() {
  const items = paper.value?.items ?? [];
  printItems.value = items
    .slice()
    .sort((a, b) => Number(a.seq ?? 0) - Number(b.seq ?? 0))
    .map((item) => ({
      key: `${item.questionId}-${item.questionVersionId}`,
      paperItem: item,
      detail: printItems.value.find((current) => current.paperItem.questionId === item.questionId)?.detail ?? null
    }));
  sortByType();
}

function sortByType() {
  printItems.value = printItems.value
    .map((item, index) => ({ item, index }))
    .sort((a, b) => {
      const orderA = typeOrder[getItemTypeCode(a.item)] ?? Number.MAX_SAFE_INTEGER;
      const orderB = typeOrder[getItemTypeCode(b.item)] ?? Number.MAX_SAFE_INTEGER;
      if (orderA !== orderB) {
        return orderA - orderB;
      }
      return a.index - b.index;
    })
    .map((entry) => entry.item);
}

function handleDragStart(event: DragEvent, typeCode: string, itemKey: string) {
  draggedKey.value = itemKey;
  draggedTypeCode.value = typeCode;
  if (!event.dataTransfer) {
    return;
  }
  event.dataTransfer.effectAllowed = 'move';
  event.dataTransfer.setData('text/plain', itemKey);
}

function handleDragOver(event: DragEvent) {
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move';
  }
}

function handleDrop(event: DragEvent, targetTypeCode: string, targetGroupIndex: number) {
  event.preventDefault();
  const sourceKey = draggedKey.value || event.dataTransfer?.getData('text/plain') || '';
  if (!sourceKey || draggedTypeCode.value !== targetTypeCode) {
    clearDragState();
    return;
  }
  const sameTypeItems = printItems.value.filter((item) => getItemTypeCode(item) === targetTypeCode);
  const sourceGroupIndex = sameTypeItems.findIndex((item) => item.key === sourceKey);
  if (sourceGroupIndex < 0 || sourceGroupIndex === targetGroupIndex) {
    clearDragState();
    return;
  }

  const reorderedGroup = [...sameTypeItems];
  const [moved] = reorderedGroup.splice(sourceGroupIndex, 1);
  if (!moved) {
    clearDragState();
    return;
  }
  reorderedGroup.splice(targetGroupIndex, 0, moved);

  const groupQueue = [...reorderedGroup];
  printItems.value = printItems.value.map((item) => {
    if (getItemTypeCode(item) !== targetTypeCode) {
      return item;
    }
    return groupQueue.shift() ?? item;
  });
  clearDragState();
}

function handleDragEnd() {
  clearDragState();
}

function clearDragState() {
  draggedKey.value = '';
  draggedTypeCode.value = '';
}

function startSectionTitleEdit(section: PrintSection) {
  editingSectionType.value = section.typeCode;
  sectionTitleDraft.value = section.heading;
}

function saveSectionTitle(section: PrintSection) {
  if (editingSectionType.value !== section.typeCode) {
    return;
  }
  const title = sectionTitleDraft.value.trim();
  if (title) {
    sectionTitleOverrides.value = {
      ...sectionTitleOverrides.value,
      [section.typeCode]: title
    };
  }
  cancelSectionTitleEdit();
}

function cancelSectionTitleEdit() {
  editingSectionType.value = '';
  sectionTitleDraft.value = '';
}

function resetSectionTitle(typeCode: string) {
  const nextOverrides = { ...sectionTitleOverrides.value };
  delete nextOverrides[typeCode];
  sectionTitleOverrides.value = nextOverrides;
  if (editingSectionType.value === typeCode) {
    cancelSectionTitleEdit();
  }
}

async function handleExportPdf() {
  const sheet = sheetRef.value;
  if (!sheet || exportingPdf.value) {
    return;
  }
  exportingPdf.value = true;
  const previousScrollX = window.scrollX;
  const previousScrollY = window.scrollY;
  try {
    await schedulePagination();
    await ensureMathReady(sheet);
    const pages = Array.from(sheet.querySelectorAll<HTMLElement>('.paper-page'));
    if (!pages.length) {
      throw new Error('暂无可导出的试卷页面');
    }
    sheet.classList.add('paper-document--exporting');
    await waitForFrame();
    const pdf = new jsPDF('p', 'mm', 'a4');
    const pageWidth = pdf.internal.pageSize.getWidth();
    const pageHeight = pdf.internal.pageSize.getHeight();

    for (const [index, page] of pages.entries()) {
      page.scrollIntoView({ block: 'start' });
      await waitForFrame();
      const canvas = await html2canvas(page, {
        backgroundColor: '#ffffff',
        scale: Math.min(window.devicePixelRatio || 2, 2),
        scrollX: 0,
        scrollY: -window.scrollY,
        width: page.offsetWidth,
        height: page.offsetHeight,
        windowWidth: document.documentElement.scrollWidth,
        windowHeight: Math.max(window.innerHeight, page.offsetHeight),
        useCORS: true
      });
      if (index > 0) {
        pdf.addPage();
      }
      pdf.setFillColor(255, 255, 255);
      pdf.rect(0, 0, pageWidth, pageHeight, 'F');
      pdf.addImage(
        canvas,
        'PNG',
        0,
        0,
        pageWidth,
        pageHeight,
        undefined,
        'FAST'
      );
    }

    pdf.save(`${sanitizeFilename(paper.value?.title || '试卷')}.pdf`);
    showSuccess('PDF 已生成');
  } catch (error) {
    showError(error instanceof Error ? error.message : 'PDF 导出失败');
  } finally {
    sheet.classList.remove('paper-document--exporting');
    window.scrollTo(previousScrollX, previousScrollY);
    exportingPdf.value = false;
  }
}

async function handleBrowserPrint() {
  await schedulePagination();
  await ensureMathReady(sheetRef.value ?? undefined);
  window.setTimeout(() => {
    window.print();
  }, 150);
}

async function schedulePagination() {
  const runId = ++paginationRunId;
  if (!printBlocks.value.length) {
    printPages.value = [];
    return;
  }
  paginating.value = true;
  try {
    await nextTick();
    await ensureMathReady(measureBodyRef.value ?? undefined);
    await waitForFrame();
    if (runId !== paginationRunId) {
      return;
    }
    printPages.value = paginateMeasuredBlocks();
    await nextTick();
    await ensureMathReady(sheetRef.value ?? undefined);
  } finally {
    if (runId === paginationRunId) {
      paginating.value = false;
    }
  }
}

function paginateMeasuredBlocks() {
  const measureBody = measureBodyRef.value;
  if (!measureBody) {
    return [{ key: 'page_1', blocks: [...printBlocks.value] }];
  }
  const pageContentHeight = measureBody.clientHeight - pageHeightSafetyPx;
  const blockHeights = resolveMeasuredBlockHeights(measureBody);
  const pages: PrintPage[] = [];
  let currentBlocks: PrintBlock[] = [];
  let usedHeight = 0;

  const commitPage = () => {
    if (!currentBlocks.length) {
      return;
    }
    pages.push({
      key: `page_${pages.length + 1}`,
      blocks: currentBlocks
    });
    currentBlocks = [];
    usedHeight = 0;
  };

  const appendBlock = (block: PrintBlock) => {
    currentBlocks.push(block);
    usedHeight += blockHeights.get(block.key) ?? 0;
  };

  printBlocks.value.forEach((block, index) => {
    const blockHeight = blockHeights.get(block.key) ?? 0;
    if (block.type === 'section-title') {
      const nextBlock = printBlocks.value[index + 1];
      const firstQuestionHeight = nextBlock?.type === 'question' ? blockHeights.get(nextBlock.key) ?? 0 : 0;
      if (currentBlocks.length && usedHeight + blockHeight + firstQuestionHeight > pageContentHeight) {
        commitPage();
      }
      appendBlock(block);
      return;
    }

    const previousBlock = currentBlocks[currentBlocks.length - 1];
    const keepWithSectionTitle =
      block.type === 'question' &&
      block.sectionIndex === 0 &&
      previousBlock?.type === 'section-title' &&
      previousBlock.section.typeCode === block.section.typeCode;
    if (currentBlocks.length && usedHeight + blockHeight > pageContentHeight && !keepWithSectionTitle) {
      commitPage();
    }
    appendBlock(block);
  });

  commitPage();
  return pages;
}

function resolveMeasuredBlockHeights(measureBody: HTMLElement) {
  const heightMap = new Map<string, number>();
  printBlocks.value.forEach((block) => {
    const element = measureBody.querySelector<HTMLElement>(`[data-block-key="${escapeCssIdentifier(block.key)}"]`);
    if (!element) {
      heightMap.set(block.key, 0);
      return;
    }
    heightMap.set(block.key, Math.ceil(element.getBoundingClientRect().height));
  });
  return heightMap;
}

function escapeCssIdentifier(value: string) {
  return window.CSS?.escape ? window.CSS.escape(value) : value.replace(/"/g, '\\"');
}

async function ensureMathReady(target?: HTMLElement) {
  await nextTick();
  await loadMathJax();
  await window.MathJax?.typesetPromise?.(target ? [target] : undefined);
}

function waitForFrame() {
  return new Promise<void>((resolve) => {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => resolve());
    });
  });
}

function goBack() {
  void router.push({ name: 'paper-detail', params: { paperId: paperId.value } });
}

function getItemTypeCode(item: PrintItem) {
  return item.detail?.typeCode || item.paperItem.typeCode || 'unknown';
}

function resolveUniformScore(items: PrintItem[]) {
  if (!items.length) {
    return null;
  }
  const firstScore = Number(items[0].paperItem.score ?? 0);
  const isUniform = items.every((item) => Number(item.paperItem.score ?? 0) === firstScore);
  return isUniform ? firstScore : null;
}

function buildDefaultSectionHeading(section: PrintSection, sectionIndex: number) {
  const scoreTotal = section.items.reduce((sum, item) => sum + Number(item.paperItem.score ?? 0), 0);
  const uniformScore = resolveUniformScore(section.items);
  return `${chineseSectionNumbers[sectionIndex] ?? sectionIndex + 1}、${section.title}：本题共 ${section.items.length} 小题，${uniformScore ? `每小题 ${formatCleanScore(uniformScore)} 分，` : ''}共 ${formatCleanScore(scoreTotal)} 分。`;
}

function formatScore(value?: number | null) {
  return Number(value ?? 0).toFixed(2);
}

function formatCleanScore(value?: number | null) {
  const numberValue = Number(value ?? 0);
  if (Number.isInteger(numberValue)) {
    return String(numberValue);
  }
  return String(Number(numberValue.toFixed(2)));
}

function resolveAnswer(detail?: QuestionDetail | null) {
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
  return detail.answer || detail.answerKey || '暂无答案';
}

function shouldRenderShortAnswerSpace(item: PrintItem) {
  return !showAnswers.value && getItemTypeCode(item) === 'short-answer';
}

function resolveShortAnswerSpaceMm(item: PrintItem) {
  const score = Number(item.paperItem.score ?? 0);
  return Math.min(Math.max(score * 6, 30), 70);
}

function resolveOptionLayout(item: PrintItem): OptionLayout {
  const options = item.detail?.options ?? [];
  if (!options.length) {
    return 4;
  }

  const optionContents = options.map((option) => option.content || '');
  const normalizedLengths = optionContents.map((content) => normalizeOptionText(content).length);
  const maxLength = Math.max(...normalizedLengths);
  const averageLength = normalizedLengths.reduce((sum, length) => sum + length, 0) / normalizedLengths.length;
  const hasRichContent = optionContents.some((content) => hasFormulaContent(content) || hasImageContent(content));
  const isMultipleChoice = getItemTypeCode(item) === 'multiple-choice';

  if (hasRichContent || maxLength > 30 || averageLength > 24) {
    return 1;
  }
  if (isMultipleChoice) {
    return 2;
  }
  if (maxLength > 12 || averageLength > 10) {
    return 2;
  }
  return 4;
}

function normalizeOptionText(value: string) {
  return value
    .replace(/<[^>]*>/g, '')
    .replace(/!\[[^\]]*]\([^)]+\)/g, '')
    .replace(/\\\(|\\\)|\\\[|\\\]|\$\$/g, '')
    .replace(/\\[a-zA-Z]+/g, '')
    .replace(/\s+/g, '')
    .trim();
}

function hasFormulaContent(value: string) {
  return /(\$\$?|\\\(|\\\[|\\frac|\\sqrt|\\sum|\\int|\\begin|\\cdot|\\times|\^|_)/.test(value);
}

function hasImageContent(value: string) {
  return /(<img\b|!\[[^\]]*]\([^)]+\)|\.(png|jpe?g|gif|webp|svg)(\?|#|$))/i.test(value);
}

function sanitizeFilename(value: string) {
  return value.replace(/[\\/:*?"<>|]/g, '_').trim() || '试卷';
}
</script>

<style scoped>
.paper-print-page {
  min-height: calc(100vh - 58px);
  color: #172033;
}

.print-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  padding: 20px 24px;
  margin-bottom: 16px;
  border: 1px solid #dce9f9;
  border-radius: 8px;
  background:
    linear-gradient(90deg, rgba(47, 128, 237, 0.1), rgba(86, 204, 242, 0.04) 48%, #fff 100%),
    #fff;
  box-shadow: 0 10px 28px rgba(31, 63, 114, 0.05);
}

.back-link-button {
  margin: 0 0 8px -12px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #2f80ed;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.print-toolbar h2 {
  margin: 0;
  color: #111827;
  font-size: 24px;
}

.toolbar-desc {
  margin: 8px 0 0;
  color: #6b778c;
  line-height: 1.6;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.print-workspace {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(720px, 1fr);
  gap: 16px;
  align-items: start;
}

.order-panel {
  position: sticky;
  top: 16px;
  padding: 16px;
  border: 1px solid #e3eaf3;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 10px 28px rgba(31, 63, 114, 0.05);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.template-settings {
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e7eef8;
}

.template-settings__title {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: baseline;
  margin-bottom: 10px;
}

.template-settings__title strong {
  color: #172033;
  font-size: 15px;
}

.template-settings__title span {
  color: #64748b;
  font-size: 12px;
}

.settings-list {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #334155;
  font-size: 13px;
}

.panel-head h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

.panel-head span,
.order-section-title span,
.order-copy span {
  color: #64748b;
  font-size: 12px;
}

.order-section-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: calc(100vh - 260px);
  overflow: auto;
  padding-right: 2px;
}

.order-section-title {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
  margin-bottom: 8px;
}

.order-section-heading {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.order-section-title strong {
  display: block;
  min-width: 0;
  color: #172033;
  font-size: 13px;
  line-height: 1.5;
}

.section-title-actions {
  display: flex;
  flex-shrink: 0;
  gap: 2px;
  margin-top: -4px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-item {
  display: grid;
  grid-template-columns: 28px 28px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  padding: 11px 10px;
  border: 1px solid #e4edf8;
  border-radius: 8px;
  background: #fff;
  cursor: grab;
}

.order-item.dragging {
  opacity: 0.55;
  border-color: #2f80ed;
  background: #eef5ff;
}

.drag-grip {
  color: #94a3b8;
  font-size: 16px;
  line-height: 1;
}

.order-number {
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #eef5ff;
  color: #2f80ed;
  font-size: 12px;
  font-weight: 800;
}

.order-copy {
  min-width: 0;
}

.order-copy strong {
  display: block;
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-panel {
  overflow-x: auto;
  padding: 0 0 40px;
}

.paper-document {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
}

.paper-page {
  width: 210mm;
  height: 297mm;
  margin: 0 auto;
  padding: 15mm 16mm 18mm;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e1e7f0;
  box-sizing: border-box;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
  color: #111827;
  font-family: "Times New Roman", "Noto Serif SC", "SimSun", serif;
}

.paper-page__body {
  height: 100%;
  overflow: hidden;
}

.paper-document--exporting {
  gap: 0;
}

.paper-document--exporting .paper-page {
  border: 0;
  box-shadow: none;
}

.paper-page--measure {
  position: absolute;
  left: -10000px;
  top: 0;
  margin: 0;
  visibility: hidden;
  pointer-events: none;
  z-index: -1;
}

.paper-page--measure .paper-page__body {
  overflow: visible;
}

.exam-header {
  color: #111827;
}

.confidential-line {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 700;
}

.exam-header h1,
.exam-header h2 {
  margin: 0;
  text-align: center;
  letter-spacing: 0;
}

.exam-header h1 {
  font-size: 22px;
  line-height: 1.45;
}

.exam-header h2 {
  margin-top: 8px;
  font-size: 28px;
  line-height: 1.35;
}

.exam-description {
  margin: 8px 0 0;
  color: #333;
  font-size: 13px;
  text-align: center;
}

.notice-block {
  margin-top: 14px;
  font-size: 13px;
  line-height: 1.9;
}

.notice-block strong {
  display: block;
}

.notice-block p {
  margin: 3px 0;
  text-indent: 2em;
}

.student-line {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 12px;
  color: #374151;
  font-size: 13px;
  flex-wrap: wrap;
}

.print-block {
  box-sizing: border-box;
}

.major-title {
  margin: 0;
  color: #111827;
  font-size: 15px;
  line-height: 1.9;
  font-weight: 800;
}

.print-block--section-title {
  padding-top: 18px;
  padding-bottom: 12px;
}

.paper-page__body > .print-block--section-title:first-child {
  padding-top: 0;
}

.print-question {
  padding-top: 14px;
  break-inside: avoid;
  page-break-inside: avoid;
}

.print-block--section-title + .print-question {
  padding-top: 0;
}

.question-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 6px;
  align-items: start;
}

.question-index {
  min-width: 20px;
  font-weight: 400;
  line-height: 1.9;
}

.print-math,
.option-math,
.answer-math {
  color: #111827;
}

.paper-page :deep(.math-view) {
  padding: 0;
  background: transparent;
  border-radius: 0;
  line-height: 1.9;
  overflow: visible;
}

.option-list {
  display: grid;
  gap: 8px 20px;
  margin: 10px 0 0 30px;
  padding: 0;
  list-style: none;
}

.option-list--4 {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.option-list--2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.option-list--1 {
  grid-template-columns: minmax(0, 1fr);
}

.option-list li {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 6px;
  align-items: start;
  break-inside: avoid;
}

.option-key {
  font-weight: 700;
  line-height: 1.9;
}

.short-answer-space {
  box-sizing: border-box;
}

.answer-block {
  display: grid;
  gap: 8px;
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px dashed #cbd5e1;
  background: #f8fafc;
}

.answer-label {
  display: block;
  margin-bottom: 4px;
  color: #475569;
  font-size: 12px;
  font-weight: 800;
}

@media (max-width: 1180px) {
  .print-workspace {
    grid-template-columns: minmax(260px, 320px) minmax(720px, 1fr);
  }

  .option-list--4 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media print {
  :global(html),
  :global(body),
  :global(#app),
  :global(.app-shell),
  :global(.app-shell > .el-container),
  :global(.app-main) {
    width: auto !important;
    height: auto !important;
    min-height: auto !important;
    max-width: none !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: visible !important;
  }

  :global(body) {
    background: #fff !important;
  }

  :global(.app-header),
  .print-toolbar,
  .order-panel {
    display: none !important;
  }

  .paper-print-page {
    min-height: auto;
  }

  .print-workspace {
    display: block;
  }

  .preview-panel {
    overflow: visible;
    padding: 0;
  }

  .paper-document {
    gap: 0;
  }

  .paper-page {
    width: auto;
    height: auto;
    min-height: 297mm;
    margin: 0;
    border: 0;
    box-shadow: none;
    break-after: page;
    page-break-after: always;
  }

  .paper-page:last-child {
    break-after: auto;
    page-break-after: auto;
  }

  .paper-page__body {
    height: auto;
    overflow: visible;
  }
}

@page {
  size: A4;
  margin: 0;
}
</style>
