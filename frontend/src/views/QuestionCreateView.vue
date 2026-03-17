<template>
  <el-card v-if="collectionId">
    <template #header>
      <div class="create-header">
        <div>
          <h2>创建题目</h2>
          <p>当前题集：{{ collectionName || '（名称未知）' }}</p>
        </div>
        <el-button @click="goBack">返回题集</el-button>
      </div>
    </template>

    <el-form label-position="top" class="create-form">
      <div class="form-grid">
        <el-form-item label="题型">
          <el-select v-model="form.typeCode">
            <el-option v-for="type in questionTypes" :key="type.code" :label="type.label" :value="type.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度 (0 ~ 1)">
          <el-input-number v-model="form.difficulty" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" />
        </el-form-item>
      </div>

      <el-form-item label="标题">
        <el-input v-model="form.title" placeholder="请输入标题" />
      </el-form-item>

      <div class="preview-grid">
        <el-form-item label="题干">
          <el-input v-model="form.stem" type="textarea" :rows="4" placeholder="请输入题干" />
          <el-checkbox v-model="showStemPreview" class="preview-toggle">显示预览</el-checkbox>
        </el-form-item>
        <el-card v-if="showStemPreview" shadow="never">
          <template #header>题干预览</template>
          <MathView :content="form.stem" empty-text="暂无题干" :debounce="250" />
        </el-card>
      </div>

      <div class="preview-grid">
        <el-form-item label="参考答案">
          <el-input v-model="form.answer" type="textarea" :rows="3" placeholder="可填写展示用答案" />
        </el-form-item>
        <el-card shadow="never">
          <template #header>答案预览</template>
          <MathView :content="form.answer" empty-text="暂无答案" :debounce="200" />
        </el-card>
      </div>

      <div class="preview-grid">
        <el-form-item label="解析">
          <el-input v-model="form.solution" type="textarea" :rows="3" placeholder="可填写解析" />
          <el-checkbox v-model="showSolutionPreview" class="preview-toggle">显示预览</el-checkbox>
        </el-form-item>
        <el-card v-if="showSolutionPreview" shadow="never">
          <template #header>解析预览</template>
          <MathView :content="form.solution" empty-text="暂无解析" :debounce="250" />
        </el-card>
      </div>

      <el-card v-if="isChoiceType" shadow="never" class="sub-card">
        <template #header>
          <div class="section-header">
            <h3>选项内容</h3>
            <el-button @click="addOption">新增选项</el-button>
          </div>
        </template>
        <el-empty v-if="!form.options.length" description="暂无选项，请添加。" />
        <div v-for="(option, index) in form.options" :key="index" class="option-editor">
          <div class="option-head">
            <el-form-item label="选项编号" class="option-key">
              <el-input v-model="option.key" placeholder="如 A/B/C" />
            </el-form-item>
            <div class="option-actions">
              <div class="answer-selector">
                <el-radio v-if="isSingleChoice" :label="option.key" v-model="singleCorrect">设为正确答案</el-radio>
                <el-checkbox
                  v-else
                  :model-value="form.choiceCorrect.includes(option.key)"
                  @change="toggleMultiple(option.key)"
                >
                  设为正确答案
                </el-checkbox>
              </div>
              <el-button v-if="form.options.length > 1" type="danger" plain @click="removeOption(index)">删除</el-button>
            </div>
          </div>
          <div class="preview-grid">
            <el-form-item label="选项内容">
              <el-input v-model="option.content" type="textarea" :rows="2" placeholder="请输入选项内容" />
            </el-form-item>
            <el-card shadow="never">
              <template #header>选项预览</template>
              <MathView :content="option.content || ''" empty-text="暂无内容" :debounce="250" />
            </el-card>
          </div>
        </div>
      </el-card>

      <el-card v-if="form.typeCode === 'true-false'" shadow="never" class="sub-card">
        <template #header>判断题答案</template>
        <el-radio-group v-model="form.judgeAnswer">
          <el-radio label="T">正确</el-radio>
          <el-radio label="F">错误</el-radio>
        </el-radio-group>
      </el-card>

      <el-card shadow="never" class="sub-card">
        <template #header>
          <div class="section-header">
            <div>
              <h3>附件</h3>
              <p class="hint">上传后即可预览，自动按照顺序提交。</p>
            </div>
            <el-button @click="addAsset">新增附件</el-button>
          </div>
        </template>
        <el-empty v-if="!form.assets.length" description="暂无附件" />
        <div class="asset-grid" v-for="(asset, index) in form.assets" :key="index">
          <el-form-item label="区域">
            <el-select v-model="asset.section">
              <el-option label="题干" value="PRO" />
              <el-option label="解析" value="SOLU" />
            </el-select>
          </el-form-item>
          <div class="asset-upload">
            <div class="thumb" v-if="asset.fileId && assetPreviewUrl(asset) && !assetHasError(asset)">
              <img :src="assetPreviewUrl(asset)" alt="附件预览" @error="() => markAssetError(index)" />
            </div>
            <p v-else-if="asset.fileId" class="hint">附件预览不可用</p>
            <p v-else class="hint">尚未上传</p>
            <div class="asset-actions">
              <input type="file" @change="(event) => uploadAsset(event, index)" />
              <el-button @click="moveAsset(index, -1)" :disabled="index === 0">上移</el-button>
              <el-button @click="moveAsset(index, 1)" :disabled="index === form.assets.length - 1">下移</el-button>
              <el-button @click="openAsset(asset)" :disabled="!asset.fileId">预览</el-button>
              <el-button type="danger" plain @click="removeAsset(index)">移除</el-button>
            </div>
          </div>
        </div>
      </el-card>

      <el-button type="primary" :loading="submitting" :disabled="!collectionId" @click="handleSubmit">创建题目</el-button>
    </el-form>
  </el-card>
  <el-empty v-else description="缺少题集信息，请从题集题目列表页面点击“添加题目”。">
    <el-button type="primary" @click="router.push('/collections')">返回题集管理</el-button>
  </el-empty>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MathView from '../components/MathView.vue';
import { createQuestion } from '../api/question';
import { uploadAssetFile } from '../api/common';
import { showError, showSuccess } from '../utils/messages';
import { buildFileViewUrl } from '../utils/file';
import type { QuestionAsset, QuestionOption } from '../types/question';
import type { QuestionCreatePayload } from '../api/question';

const route = useRoute();
const router = useRouter();
const collectionId = computed(() => (typeof route.params.collectionId === 'string' ? route.params.collectionId : ''));
const collectionName = computed(() => (typeof route.query.name === 'string' ? route.query.name : ''));

const form = reactive({
  typeCode: 'single-choice',
  title: '',
  stem: '',
  answer: '',
  solution: '',
  difficulty: 0.5,
  options: createDefaultOptions(),
  choiceCorrect: [] as string[],
  judgeAnswer: 'T' as 'T' | 'F',
  assets: [] as QuestionAsset[]
});

const submitting = ref(false);
const showStemPreview = ref(true);
const showSolutionPreview = ref(true);
const assetPreviewErrors = reactive(new Map<string, boolean>());

const questionTypes = [
  { code: 'single-choice', label: '单选题' },
  { code: 'multiple-choice', label: '多选题' },
  { code: 'true-false', label: '判断题' },
  { code: 'fill-in', label: '填空题' },
  { code: 'short-answer', label: '简答题' }
];

const isChoiceType = computed(() => form.typeCode === 'single-choice' || form.typeCode === 'multiple-choice');
const isSingleChoice = computed(() => form.typeCode === 'single-choice');

const singleCorrect = computed({
  get: () => form.choiceCorrect[0] ?? '',
  set: (val: string) => {
    form.choiceCorrect.splice(0, form.choiceCorrect.length);
    if (val) {
      form.choiceCorrect.push(val);
    }
  }
});

watch(
  () => form.typeCode,
  (type) => {
    if (isChoiceType.value && form.options.length < 2) {
      form.options = createDefaultOptions();
      form.choiceCorrect = [];
    }
    if (!isChoiceType.value) {
      form.options = [];
      form.choiceCorrect = [];
    }
    if (type !== 'true-false') {
      form.judgeAnswer = 'T';
    }
  }
);

watch(
  () => form.difficulty,
  (val) => {
    if (val == null) {
      return;
    }
    if (val < 0) {
      form.difficulty = 0;
    } else if (val > 1) {
      form.difficulty = 1;
    }
  }
);

function createDefaultOptions(): QuestionOption[] {
  return [
    { key: 'A', content: '' },
    { key: 'B', content: '' }
  ];
}

function addOption() {
  form.options.push({ key: '', content: '' });
}

function removeOption(index: number) {
  const removed = form.options.splice(index, 1)[0];
  if (removed) {
    const idx = form.choiceCorrect.indexOf(removed.key);
    if (idx !== -1) {
      form.choiceCorrect.splice(idx, 1);
    }
  }
}

function toggleMultiple(key?: string) {
  if (!key) {
    return;
  }
  const idx = form.choiceCorrect.indexOf(key);
  if (idx === -1) {
    form.choiceCorrect.push(key);
  } else {
    form.choiceCorrect.splice(idx, 1);
  }
}

function addAsset() {
  form.assets.push({ fileId: '', section: 'PRO', ordinal: form.assets.length + 1 });
}

function removeAsset(index: number) {
  form.assets.splice(index, 1);
}

async function uploadAsset(event: Event, index: number) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  try {
    const { fileId } = await uploadAssetFile(file);
    if (!fileId) {
      throw new Error('附件上传失败');
    }
    form.assets[index].fileId = String(fileId);
    assetPreviewErrors.delete(String(fileId));
    form.assets[index].ordinal = index + 1;
    if (import.meta.env.DEV) {
      console.debug('[asset] 上传成功', fileId);
    }
    showSuccess('附件上传成功');
  } catch (error) {
    showError('附件上传失败');
  } finally {
    input.value = '';
  }
}

function assetPreviewUrl(asset: QuestionAsset) {
  if (!asset.fileId) {
    return '';
  }
  return buildFileViewUrl(asset.fileId);
}

function assetHasError(asset: QuestionAsset) {
  return asset.fileId ? assetPreviewErrors.get(asset.fileId) === true : false;
}

function markAssetError(index: number) {
  const asset = form.assets[index];
  if (asset?.fileId) {
    assetPreviewErrors.set(asset.fileId, true);
  }
}

function openAsset(asset: QuestionAsset) {
  const url = assetPreviewUrl(asset);
  if (url) {
    window.open(url, '_blank');
  }
}

function moveAsset(index: number, direction: number) {
  const target = index + direction;
  if (target < 0 || target >= form.assets.length) {
    return;
  }
  const [item] = form.assets.splice(index, 1);
  form.assets.splice(target, 0, item);
}

function buildPayload(): QuestionCreatePayload {
  if (!collectionId.value) {
    throw new Error('缺少题集信息');
  }
  const payload: QuestionCreatePayload = {
    typeCode: form.typeCode,
    title: form.title,
    stem: form.stem,
    answer: form.answer || null,
    solution: form.solution || null,
    difficulty: form.difficulty ?? null,
    collectionId: collectionId.value,
    assets: form.assets
      .map((asset, idx) => {
        const fileId = asset.fileId?.toString().trim();
        if (!fileId) {
          return null;
        }
        return {
          fileId,
          section: asset.section,
          ordinal: idx + 1
        };
      })
      .filter((asset): asset is QuestionAsset => Boolean(asset))
  };

  if (isChoiceType.value) {
    payload.options = form.options
      .filter((opt) => opt.key?.trim() && opt.content?.trim())
      .map((opt) => ({ key: opt.key.trim(), content: opt.content.trim() }));
    payload.correctOptions = isSingleChoice.value
      ? singleCorrect.value
        ? [singleCorrect.value]
        : []
      : form.choiceCorrect.filter((item) => item?.trim());
  }

  if (form.typeCode === 'true-false') {
    payload.judgeAnswer = form.judgeAnswer;
  } else if (!isChoiceType.value) {
    payload.judgeAnswer = undefined;
  }

  return payload;
}

async function handleSubmit() {
  if (!collectionId.value) {
    showError('缺少题集信息');
    return;
  }
  submitting.value = true;
  try {
    const payload = buildPayload();
    await createQuestion(payload);
    showSuccess('题目创建成功');
    router.push({ name: 'collection-questions', params: { collectionId: collectionId.value } });
  } catch (error: any) {
    showError(error?.message ?? '创建失败');
  } finally {
    submitting.value = false;
  }
}

function goBack() {
  if (collectionId.value) {
    router.push({ name: 'collection-questions', params: { collectionId: collectionId.value } });
  } else {
    router.push('/collections');
  }
}
</script>

<style scoped>
.create-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.create-header h2 {
  margin: 0;
}

.create-header p {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
}

.create-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.preview-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  align-items: start;
}

.preview-toggle {
  margin-top: 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.section-header h3 {
  margin: 0;
}

.hint {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.sub-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-editor {
  border-top: 1px solid var(--el-border-color-light);
  padding-top: 12px;
}

.option-editor:first-child {
  border-top: none;
  padding-top: 0;
}

.option-head {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.option-key {
  min-width: 140px;
  margin-bottom: 0;
}

.option-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.answer-selector {
  min-width: 150px;
}

.asset-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  align-items: start;
}

.asset-upload {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.thumb {
  width: 120px;
  height: 120px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.asset-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .create-header {
    flex-direction: column;
  }
}
</style>
