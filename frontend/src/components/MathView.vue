<template>
  <div ref="container" class="math-view"></div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { loadMathJax } from '../plugins/mathjax';

interface Props {
  content?: string | null;
  emptyText?: string;
  renderMath?: boolean;
  debounce?: number;
}

const props = withDefaults(defineProps<Props>(), {
  renderMath: true,
  debounce: 0
});
const container = ref<HTMLElement | null>(null);
let typesetQueue: Promise<void> = Promise.resolve();
let debounceTimer: number | null = null;

async function renderContent() {
  const el = container.value;
  if (!el) {
    return;
  }
  const raw = props.content ?? '';
  const trimmed = raw.trim();
  if (!trimmed) {
    el.textContent = props.emptyText ?? '暂无内容';
    return;
  }
  if (!props.renderMath) {
    el.textContent = raw;
    return;
  }
  el.textContent = raw;
  await loadMathJax();
  typesetQueue = typesetQueue
    .then(async () => {
      window.MathJax?.typesetClear?.([el]);
      await window.MathJax?.typesetPromise?.([el]);
    })
    .catch((error) => {
      console.error('[MathJax typeset failed]', error);
    });
  await typesetQueue;
}

function triggerRender() {
  if (props.debounce > 0) {
    if (debounceTimer) {
      window.clearTimeout(debounceTimer);
    }
    debounceTimer = window.setTimeout(() => {
      debounceTimer = null;
      void renderContent();
    }, props.debounce);
    return;
  }
  void renderContent();
}

onMounted(() => {
  triggerRender();
});

watch(
  () => props.content,
  () => {
    triggerRender();
  }
);

watch(
  () => props.renderMath,
  () => {
    triggerRender();
  }
);

watch(
  () => props.debounce,
  () => {
    triggerRender();
  }
);
</script>

<style scoped>
.math-view {
  background: #f9fafb;
  border-radius: 8px;
  padding: 12px;
  line-height: 1.8;
  overflow-x: auto;
  white-space: pre-wrap;
}
</style>
