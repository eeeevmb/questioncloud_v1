<template>
  <el-card v-if="detail" class="detail-card">
    <div class="detail-header">
      <div class="title-block">
        <el-button v-if="fromCollectionId" link type="primary" @click="goBack">返回题集</el-button>
        <h1>{{ detail.title }}</h1>
        <p class="subtitle">{{ typeLabel }} · 当前版本 {{ detail.versionNo }}</p>
      </div>
      <div class="action-group">
        <el-button type="primary" @click="goToEdit">修改并生成新版本</el-button>
        <el-button type="danger" plain @click="confirmDelete">删除题目</el-button>
      </div>
    </div>

    <el-descriptions border :column="2">
      <el-descriptions-item label="题目 ID">
        <span class="mono">{{ detail.id }}</span>
        <el-button link type="primary" @click="copyValue(detail.id, '题目ID已复制')">复制</el-button>
      </el-descriptions-item>
      <el-descriptions-item label="当前版本 ID">
        <span class="mono">{{ detail.currentVersionId }}</span>
        <el-button link type="primary" @click="copyValue(detail.currentVersionId, '版本ID已复制')">复制</el-button>
      </el-descriptions-item>
      <el-descriptions-item label="题型编码">
        <span class="mono">{{ detail.typeCode }}</span>
        <el-button link type="primary" @click="copyValue(detail.typeCode, '题型编码已复制')">复制</el-button>
      </el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ detail.updatedAt ? formatDate(detail.updatedAt) : '暂无' }}</el-descriptions-item>
    </el-descriptions>

    <div class="stats-section">
      <el-card v-for="stat in statCards" :key="stat.label" shadow="never">
        <p class="stat-label">{{ stat.label }}</p>
        <p class="stat-value" :class="{ placeholder: stat.placeholder }">{{ stat.value }}</p>
        <p class="stat-hint" v-if="stat.hint">{{ stat.hint }}</p>
      </el-card>
    </div>

    <section class="content-section">
      <h3>题干</h3>
      <MathView :content="detail.stem || ''" empty-text="暂无题干内容" />
      <AttachmentGallery v-if="stemAssets.length" :assets="stemAssets" title="题干附件" />
    </section>

    <section v-if="isChoiceType" class="content-section">
      <h3>选项</h3>
      <el-table v-if="hasOptions" :data="detail.options || []" border>
        <el-table-column prop="key" label="选项" width="90" />
        <el-table-column label="内容" min-width="260">
          <template #default="{ row }">
            <MathView :content="row.content || ''" empty-text="暂无内容" />
          </template>
        </el-table-column>
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag v-if="isCorrectOption(row.key)" type="success">正确</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-alert
        v-else
        title="该题为选择题，但详情接口未返回 options，请后端在 QuestionDetailVO 增加 options 映射。"
        type="warning"
        :closable="false"
      />
    </section>

    <section class="content-section">
      <h3>{{ answerSectionTitle }}</h3>
      <template v-if="isTrueFalseType">
        <el-tag v-if="booleanAnswer" type="success">{{ booleanAnswer }}</el-tag>
        <span v-else class="muted">尚未提供答案</span>
      </template>
      <template v-else>
        <MathView :content="detail.answer || ''" empty-text="暂无答案" />
      </template>
    </section>

    <section class="content-section">
      <h3>解析</h3>
      <MathView :content="detail.solution || ''" empty-text="尚未提供解析" />
      <AttachmentGallery v-if="solutionAssets.length" :assets="solutionAssets" title="解析附件" />
    </section>
  </el-card>

  <el-card v-else>
    <el-skeleton :rows="6" animated />
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { fetchQuestionDetail, deleteQuestion } from '../api/question';
import type { QuestionDetail } from '../types/question';
import { showSuccess } from '../utils/messages';
import MathView from '../components/MathView.vue';
import AttachmentGallery from '../components/AttachmentGallery.vue';

interface StatCard {
  label: string;
  value: string;
  placeholder: boolean;
  hint?: string;
}

const choiceTypes = ['single-choice', 'multiple-choice'];
const route = useRoute();
const router = useRouter();
const detail = ref<QuestionDetail | null>(null);
const loading = ref(false);

const questionId = computed(() => {
  const id = route.params.questionId;
  return typeof id === 'string' ? id : '';
});
const fromCollectionId = computed(() => {
  const id = route.query.collectionId;
  return typeof id === 'string' ? id : null;
});

const isChoiceType = computed(() => choiceTypes.includes(detail.value?.typeCode ?? ''));
const isTrueFalseType = computed(() => detail.value?.typeCode === 'true-false');
const answerSectionTitle = computed(() => (isTrueFalseType.value ? '答案' : '参考答案'));

const typeLabelMap: Record<string, string> = {
  'single-choice': '单选题',
  'multiple-choice': '多选题',
  'true-false': '判断题',
  'fill-in': '填空题',
  'short-answer': '简答题'
};

const typeLabel = computed(() => {
  if (!detail.value) {
    return '';
  }
  return typeLabelMap[detail.value.typeCode] ?? detail.value.typeCode;
});

const statCards = computed<StatCard[]>(() => {
  if (!detail.value) {
    return [];
  }
  return [
    buildStat('难度', detail.value.difficulty, '未设置'),
    buildStat('正确率', detail.value.correctRate, '暂无数据', (v) => `${(v * 100).toFixed(1)}%`),
    buildStat('曝光系数', detail.value.exposureFactor, '暂无数据'),
    buildStat('尝试次数', detail.value.attempts, '暂无'),
    {
      label: '最后曝光',
      value: detail.value.lastExposedAt ? formatDate(detail.value.lastExposedAt) : '尚未曝光',
      placeholder: !detail.value.lastExposedAt
    }
  ];
});

function buildStat(
  label: string,
  value?: number | null,
  fallback = '暂无数据',
  formatter?: (value: number) => string
): StatCard {
  if (value === undefined || value === null) {
    return { label, value: fallback, placeholder: true };
  }
  return { label, value: formatter ? formatter(value) : value.toFixed(2), placeholder: false };
}

const hasOptions = computed(() => Boolean(detail.value?.options?.length));

const correctOptionSet = computed(() => {
  const set = new Set<string>();
  const options = detail.value?.correctOptions;
  if (options?.length) {
    options.forEach((opt) => opt && set.add(opt.trim().toUpperCase()));
    return set;
  }
  const key = detail.value?.answerKey;
  if (!key) {
    return set;
  }
  key.split(/[,，;；\s]+/)
    .filter(Boolean)
    .forEach((segment) => {
      segment.split('').forEach((char) => {
        if (char.trim()) {
          set.add(char.toUpperCase());
        }
      });
    });
  return set;
});

const stemAssets = computed(() => detail.value?.assets?.filter((asset) => asset.section === 'PRO') ?? []);
const solutionAssets = computed(() => detail.value?.assets?.filter((asset) => asset.section === 'SOLU') ?? []);

const booleanAnswer = computed(() => {
  if (detail.value?.answer?.trim()) {
    return detail.value.answer;
  }
  const raw = detail.value?.judgeAnswer ?? detail.value?.answerKey;
  if (!raw) {
    return '';
  }
  const first = raw.trim().toUpperCase();
  if (first === 'T') {
    return '正确';
  }
  if (first === 'F') {
    return '错误';
  }
  return raw;
});

onMounted(loadDetail);
watch(() => route.params.questionId, loadDetail);

async function loadDetail() {
  if (!questionId.value) {
    return;
  }
  loading.value = true;
  try {
    detail.value = await fetchQuestionDetail(questionId.value);
  } finally {
    loading.value = false;
  }
}

function goToEdit() {
  router.push({
    name: 'question-edit',
    params: { questionId: questionId.value },
    query: { collectionId: fromCollectionId.value ?? undefined }
  });
}

function goBack() {
  if (fromCollectionId.value) {
    router.push({
      name: 'collection-questions',
      params: { collectionId: fromCollectionId.value }
    });
  } else {
    router.push('/collections');
  }
}

async function confirmDelete() {
  if (!questionId.value || !detail.value) {
    return;
  }
  let value = '';
  try {
    const result = await ElMessageBox.prompt(`将删除题目「${detail.value.title}」，输入“删除”确认`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      inputPattern: /^删除$/,
      inputErrorMessage: '请输入“删除”'
    });
    value = result.value;
  } catch {
    return;
  }
  if (value !== '删除') {
    return;
  }
  await deleteQuestion(questionId.value);
  showSuccess('题目删除成功');
  goBack();
}

function isCorrectOption(key?: string) {
  if (!key) {
    return false;
  }
  return correctOptionSet.value.has(key.trim().toUpperCase());
}

function copyValue(value: string, message: string) {
  navigator.clipboard.writeText(value).then(() => showSuccess(message));
}

function formatDate(date: string) {
  return new Date(date).toLocaleString();
}
</script>

<style scoped>
.detail-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.title-block h1 {
  margin: 0;
}

.subtitle {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
}

.action-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.mono {
  font-family: Menlo, Consolas, monospace;
}

.stats-section {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.stat-label {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.stat-value {
  margin: 8px 0 0;
  font-size: 20px;
  font-weight: 600;
}

.stat-value.placeholder {
  color: var(--el-text-color-secondary);
}

.stat-hint {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.content-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.content-section h3 {
  margin: 0;
}

.muted {
  color: var(--el-text-color-secondary);
}

@media (max-width: 768px) {
  .detail-header {
    flex-direction: column;
  }
}
</style>
