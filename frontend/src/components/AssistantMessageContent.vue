<template>
  <div ref="container" class="assistant-markdown" v-html="renderedHtml"></div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import MarkdownIt from 'markdown-it';
import { loadMathJax } from '../plugins/mathjax';

interface Props {
  content?: string;
}

const props = withDefaults(defineProps<Props>(), {
  content: ''
});
const container = ref<HTMLElement | null>(null);
let typesetQueue: Promise<void> = Promise.resolve();

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true
});

function normalizeInlineMathDelimiters(input: string): string {
  const segments = input.split(/(```[\s\S]*?```|`[^`\n]*`)/g);
  return segments
    .map((segment) => {
      if (!segment) {
        return segment;
      }
      if (
        (segment.startsWith('```') && segment.endsWith('```')) ||
        (segment.startsWith('`') && segment.endsWith('`'))
      ) {
        return segment;
      }

      let normalized = segment;

      // 把被转义的 \$...\$ 恢复成 $...$
      normalized = normalized.replace(/\\\$([^\n]+?)\\\$/g, (_matched, expr: string) => {
        return `$${expr.trim()}$`;
      });

      // 修正行内公式两侧多余空格，例如 `$ a$`、`$x $`、`$ x $`
      // 只处理单个 $...$，避免误伤块级公式 $$...$$
      normalized = normalized.replace(/(^|[^$])\$\s+([^$\n]*?)\s*\$/g, (_matched, prefix: string, expr: string) => {
        return `${prefix}$${expr.trim()}$`;
      });
      normalized = normalized.replace(/(^|[^$])\$([^$\n]*?)\s+\$/g, (_matched, prefix: string, expr: string) => {
        return `${prefix}$${expr.trim()}$`;
      });

      return normalized;
    })
    .join('');
}

const renderedHtml = computed(() => {
  const raw = normalizeInlineMathDelimiters(props.content ?? '');
  if (!raw.trim()) {
    return '<p class="empty-text">（无内容）</p>';
  }
  try {
    return markdown.render(raw);
  } catch (error) {
    return `<pre>${markdown.utils.escapeHtml(raw)}</pre>`;
  }
});

async function typesetMath() {
  const el = container.value;
  if (!el) {
    return;
  }
  await loadMathJax();
  typesetQueue = typesetQueue
    .then(async () => {
      window.MathJax?.typesetClear?.([el]);
      await window.MathJax?.typesetPromise?.([el]);
    })
    .catch((error) => {
      console.error('[AssistantMessageContent MathJax typeset failed]', error);
    });
  await typesetQueue;
}

async function renderAndTypeset() {
  await nextTick();
  await typesetMath();
}

onMounted(() => {
  void renderAndTypeset();
});

watch(
  renderedHtml,
  () => {
    void renderAndTypeset();
  }
);
</script>

<style scoped>
.assistant-markdown {
  color: var(--el-text-color-primary);
  line-height: 1.7;
  font-size: 14px;
  word-break: break-word;
  overflow: visible;
}

.assistant-markdown :deep(*:first-child) {
  margin-top: 0;
}

.assistant-markdown :deep(*:last-child) {
  margin-bottom: 0;
}

.assistant-markdown :deep(h1),
.assistant-markdown :deep(h2),
.assistant-markdown :deep(h3),
.assistant-markdown :deep(h4) {
  margin: 0.8em 0 0.45em;
  line-height: 1.35;
  font-weight: 700;
}

.assistant-markdown :deep(h1) {
  font-size: 1.3em;
}

.assistant-markdown :deep(h2) {
  font-size: 1.2em;
}

.assistant-markdown :deep(h3) {
  font-size: 1.1em;
}

.assistant-markdown :deep(p) {
  margin: 0.55em 0;
  overflow: visible;
}

.assistant-markdown :deep(ul),
.assistant-markdown :deep(ol) {
  margin: 0.55em 0;
  padding-left: 1.4em;
}

.assistant-markdown :deep(li + li) {
  margin-top: 0.2em;
}

.assistant-markdown :deep(blockquote) {
  margin: 0.7em 0;
  padding: 0.45em 0.8em;
  border-left: 3px solid var(--el-color-primary-light-5);
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  border-radius: 6px;
}

.assistant-markdown :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 0.92em;
}

.assistant-markdown :deep(p code),
.assistant-markdown :deep(li code),
.assistant-markdown :deep(td code) {
  padding: 0.1em 0.35em;
  border-radius: 4px;
  background: rgba(15, 23, 42, 0.08);
}

.assistant-markdown :deep(pre) {
  margin: 0.75em 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: #0f172a;
  color: #e2e8f0;
  overflow-x: auto;
  line-height: 1.55;
}

.assistant-markdown :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

.assistant-markdown :deep(hr) {
  border: none;
  border-top: 1px solid var(--el-border-color);
  margin: 0.85em 0;
}

.assistant-markdown :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0.7em 0;
  display: block;
  overflow-x: auto;
}

.assistant-markdown :deep(th),
.assistant-markdown :deep(td) {
  border: 1px solid var(--el-border-color-light);
  padding: 6px 8px;
  text-align: left;
  white-space: nowrap;
}

.assistant-markdown :deep(th) {
  background: var(--el-fill-color-light);
}

.assistant-markdown :deep(mjx-container) {
  max-width: 100%;
  line-height: 1.2;
  text-indent: 0;
  white-space: normal;
}

.assistant-markdown :deep(mjx-container[display='true']) {
  margin: 0.9em 0;
  padding: 0.2em 0.1em 0.3em;
  overflow-x: auto;
  overflow-y: visible;
}

.assistant-markdown :deep(.empty-text) {
  color: var(--el-text-color-secondary);
}
</style>
