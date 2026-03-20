<template>
  <el-card v-if="detail">
    <template #header>
      <div class="page-header">
        <div>
          <h2>更新题目（当前版本 {{ detail.versionNo }}）</h2>
          <p>题目类型：{{ detail.typeCode }}，更新会生成新版本。</p>
        </div>
      </div>
    </template>

    <el-form label-position="top" class="form">
      <el-form-item label="标题">
        <el-input v-model="form.title" />
      </el-form-item>

      <div class="preview-grid">
        <el-form-item label="题干">
          <el-input v-model="form.stem" type="textarea" :rows="4" />
        </el-form-item>
        <el-card shadow="never">
          <template #header>题干预览</template>
          <MathView :content="form.stem" empty-text="暂无题干" :debounce="250" />
        </el-card>
      </div>

      <div class="preview-grid">
        <el-form-item label="展示答案">
          <el-input v-model="form.answer" type="textarea" :rows="3" />
        </el-form-item>
        <el-card shadow="never">
          <template #header>答案预览</template>
          <MathView :content="form.answer" empty-text="暂无答案" :debounce="250" />
        </el-card>
      </div>

      <div class="preview-grid">
        <el-form-item label="解析">
          <el-input v-model="form.solution" type="textarea" :rows="3" />
        </el-form-item>
        <el-card shadow="never">
          <template #header>解析预览</template>
          <MathView :content="form.solution" empty-text="尚未提供解析" :debounce="250" />
        </el-card>
      </div>

      <el-card v-if="detail.typeCode === 'true-false'" shadow="never" class="sub-card">
        <template #header>判断题答案</template>
        <el-radio-group v-model="form.judgeAnswer">
          <el-radio label="T">正确</el-radio>
          <el-radio label="F">错误</el-radio>
        </el-radio-group>
      </el-card>

      <el-card v-if="isChoiceType" shadow="never" class="sub-card">
        <template #header>
          <div class="section-header">
            <h3>选项内容</h3>
            <el-button @click="addOption">新增选项</el-button>
          </div>
        </template>
        <el-empty v-if="!form.options.length" description="暂无选项" />
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
              <el-button v-if="form.options.length > 1" type="danger" plain @click="removeOption(index)">移除</el-button>
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

      <el-card shadow="never" class="sub-card">
        <template #header>
          <div class="section-header">
            <div>
              <h3>附件</h3>
              <p class="hint">上传后即可预览，支持调整顺序。</p>
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

      <el-button type="primary" :loading="saving" @click="handleSubmit">提交更新</el-button>
    </el-form>
  </el-card>
  <el-card v-else>
    <el-skeleton :rows="6" animated />
  </el-card>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchQuestionDetail, updateQuestion } from '../api/question';
import type { QuestionUpdatePayload } from '../api/question';
import type { QuestionAsset, QuestionDetail, QuestionOption } from '../types/question';
import { uploadAssetFile } from '../api/common';
import { showSuccess } from '../utils/messages';
import { buildFileViewUrl } from '../utils/file';
import MathView from '../components/MathView.vue';

const route = useRoute();
const router = useRouter();
const detail = ref<QuestionDetail | null>(null);
const saving = ref(false);
const form = reactive({
  title: '',
  stem: '',
  answer: '',
  solution: '',
  judgeAnswer: 'T' as 'T' | 'F',
  assets: [] as QuestionAsset[],
  options: [] as QuestionOption[],
  choiceCorrect: [] as string[]
});
const singleCorrect = ref('');
const assetPreviewErrors = reactive(new Map<string, boolean>());

const questionId = computed(() => {
  const id = route.params.questionId;
  return typeof id === 'string' ? id : '';
});

watch(() => route.params.questionId, loadDetail, { immediate: true });

async function loadDetail() {
  if (!questionId.value) {
    return;
  }
  try {
    const detailData = await fetchQuestionDetail(questionId.value);
    detail.value = detailData;
    form.title = detailData.title;
    form.stem = detailData.stem;
    form.answer = detailData.answer ?? '';
    form.solution = detailData.solution ?? '';
    form.assets = detailData.assets
      ? detailData.assets.map((asset) => ({ ...asset, fileId: asset.fileId?.toString() ?? '' }))
      : [];
    assetPreviewErrors.clear();
    form.options = detailData.options
      ? detailData.options.map((opt) => ({ ...opt }))
      : [];
    if ((detailData.typeCode === 'single-choice' || detailData.typeCode === 'multiple-choice') && form.options.length === 0) {
      form.options = [{ key: 'A', content: '' }];
    }

    const parsedCorrect = getCorrectKeys(detailData);
    form.choiceCorrect = parsedCorrect;
    singleCorrect.value = parsedCorrect[0] ?? '';

    if (detailData.typeCode === 'true-false') {
      form.judgeAnswer = (detailData.answerKey as 'T' | 'F') || 'T';
    }
  } catch (error) {
    // 统一提示
  }
}

const isChoiceType = computed(() => {
  const type = detail.value?.typeCode;
  return type === 'single-choice' || type === 'multiple-choice';
});

const isSingleChoice = computed(() => detail.value?.typeCode === 'single-choice');

watch(singleCorrect, (val) => {
  if (isSingleChoice.value) {
    form.choiceCorrect = val ? [val] : [];
  }
});

function getCorrectKeys(info: QuestionDetail) {
  if (info.correctOptions && info.correctOptions.length) {
    return info.correctOptions.map((item) => item.trim()).filter(Boolean);
  }
  if (!info.answerKey) {
    return [];
  }
  return info.answerKey
    .split(/[,，;；\s]+/)
    .filter(Boolean)
    .map((segment) => segment.trim().toUpperCase());
}

function addAsset() {
  form.assets.push({ fileId: '', section: 'PRO', ordinal: form.assets.length + 1 });
}

function removeAsset(index: number) {
  form.assets.splice(index, 1);
}

function addOption() {
  form.options.push({ key: '', content: '' });
}

function removeOption(index: number) {
  const removed = form.options.splice(index, 1)[0];
  if (!removed) {
    return;
  }
  if (isSingleChoice.value && singleCorrect.value === removed.key) {
    singleCorrect.value = '';
  }
  if (removed.key) {
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
    if (import.meta.env.DEV) {
      console.debug('[asset] 上传成功', fileId);
    }
    showSuccess('附件上传成功');
  } catch (error) {
    // 统一提示
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

function buildPayload(): QuestionUpdatePayload {
  if (!detail.value) {
    throw new Error('题目不存在');
  }
  const payload: QuestionUpdatePayload = {
    title: form.title,
    stem: form.stem,
    answer: form.answer,
    solution: form.solution,
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

  if (detail.value.typeCode === 'true-false') {
    payload.judgeAnswer = form.judgeAnswer;
  } else {
    payload.judgeAnswer = undefined;
  }
  if (isChoiceType.value) {
    payload.options = form.options
      .filter((opt) => opt.key?.trim() && opt.content?.trim())
      .map((opt) => ({ key: opt.key.trim(), content: opt.content.trim() }));
    payload.correctOptions = isSingleChoice.value
      ? (singleCorrect.value ? [singleCorrect.value] : [])
      : form.choiceCorrect.filter((item) => item?.trim());
  } else {
    payload.options = [];
    payload.correctOptions = undefined;
  }
  return payload;
}

async function handleSubmit() {
  if (!questionId.value) {
    return;
  }
  saving.value = true;
  try {
    await updateQuestion(questionId.value, buildPayload());
    showSuccess('题目已更新，已生成新版本');
    router.push({ name: 'question-detail', params: { questionId: questionId.value }, query: route.query });
  } catch (error) {
    // 已提示
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.page-header h2 {
  margin: 0;
}

.page-header p {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  align-items: start;
}

.sub-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 12px;
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
</style>
