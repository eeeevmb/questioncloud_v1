<template>
  <section class="card" v-if="collectionId">
    <header class="create-header">
      <div>
        <h2>创建题目</h2>
        <p>当前题集：{{ collectionName || '（名称未知）' }}</p>
      </div>
      <button class="secondary-btn" type="button" @click="goBack">返回题集</button>
    </header>

    <form class="create-form" @submit.prevent="handleSubmit">
      <div class="form-grid">
        <label>
          题型
          <select v-model="form.typeCode">
            <option v-for="type in questionTypes" :key="type.code" :value="type.code">{{ type.label }}</option>
          </select>
        </label>
        <label>
          难度 (0 ~ 1)
          <input v-model.number="form.difficulty" type="number" min="0" max="1" step="0.01" />
        </label>
      </div>

      <div class="field-pair single">
        <div>
          <label>标题</label>
          <input v-model="form.title" placeholder="请输入标题" />
        </div>
      </div>

      <div class="field-pair with-preview">
        <div>
          <label>题干 <span class="required">*</span></label>
          <textarea v-model="form.stem" rows="4" placeholder="请输入题干" required></textarea>
          <label class="preview-toggle">
            <input type="checkbox" v-model="showStemPreview" />
            显示预览
          </label>
        </div>
        <div class="preview-card" v-if="showStemPreview">
          <p>题干预览</p>
          <MathView :content="form.stem" empty-text="暂无题干" :debounce="250" />
        </div>
      </div>

      <div class="field-pair with-preview">
        <div>
          <label>参考答案</label>
          <textarea v-model="form.answer" rows="3" placeholder="可填写展示用答案"></textarea>
        </div>
        <div class="preview-card">
          <p>答案预览</p>
          <MathView :content="form.answer" empty-text="暂无答案" :debounce="200" />
        </div>
      </div>

      <div class="field-pair with-preview">
        <div>
          <label>解析</label>
          <textarea v-model="form.solution" rows="3" placeholder="可填写解析"></textarea>
          <label class="preview-toggle">
            <input type="checkbox" v-model="showSolutionPreview" />
            显示预览
          </label>
        </div>
        <div class="preview-card" v-if="showSolutionPreview">
          <p>解析预览</p>
          <MathView :content="form.solution" empty-text="暂无解析" :debounce="250" />
        </div>
      </div>

      <section v-if="isChoiceType" class="type-section">
        <header class="section-header">
          <h3>选项内容</h3>
          <button class="secondary-btn" type="button" @click="addOption">新增选项</button>
        </header>
        <div v-if="!form.options.length" class="hint">暂无选项，请添加。</div>
        <div v-for="(option, index) in form.options" :key="index" class="option-editor">
          <div class="option-head">
            <label class="option-key">
              选项编号
              <input v-model="option.key" placeholder="如 A/B/C" />
            </label>
            <div class="option-actions">
              <label
                class="answer-selector"
                :class="{ active: isOptionCorrect(option.key) }"
              >
                <template v-if="isSingleChoice">
                  <input type="radio" :value="option.key" v-model="singleCorrect" />
                </template>
                <template v-else>
                  <input
                    type="checkbox"
                    :value="option.key"
                    :checked="form.choiceCorrect.includes(option.key)"
                    @change="toggleMultiple(option.key)"
                  />
                </template>
                <span>设为正确答案</span>
              </label>
              <button class="danger-btn" type="button" v-if="form.options.length > 1" @click="removeOption(index)">删除</button>
            </div>
          </div>
          <div class="option-body">
            <textarea v-model="option.content" rows="2" placeholder="请输入选项内容"></textarea>
            <div class="preview-card">
              <p>选项预览</p>
              <MathView :content="option.content || ''" empty-text="暂无内容" :debounce="250" />
            </div>
          </div>
        </div>
      </section>

      <section v-if="form.typeCode === 'true-false'" class="type-section">
        <h3>判断题答案</h3>
        <select v-model="form.judgeAnswer">
          <option value="T">正确</option>
          <option value="F">错误</option>
        </select>
      </section>

      <section class="type-section">
        <header class="section-header">
          <div>
            <h3>附件</h3>
            <p class="hint">上传后即可预览，自动按照顺序提交。</p>
          </div>
          <button class="secondary-btn" type="button" @click="addAsset">新增附件</button>
        </header>
        <div v-if="!form.assets.length" class="hint">暂无附件</div>
        <div class="asset-grid" v-for="(asset, index) in form.assets" :key="index">
          <label>
            区域
            <select v-model="asset.section">
              <option value="PRO">题干</option>
              <option value="SOLU">解析</option>
            </select>
          </label>
          <div class="asset-upload">
            <div class="thumb" v-if="asset.fileId && assetPreviewUrl(asset) && !assetHasError(asset)">
              <img :src="assetPreviewUrl(asset)" alt="附件预览" @error="() => markAssetError(index)" />
            </div>
            <p v-else-if="asset.fileId" class="hint">附件预览不可用</p>
            <p v-else class="hint">尚未上传</p>
            <div class="asset-actions">
              <input type="file" @change="(event) => uploadAsset(event, index)" />
              <button class="secondary-btn" type="button" @click="moveAsset(index, -1)" :disabled="index === 0">上移</button>
              <button class="secondary-btn" type="button" @click="moveAsset(index, 1)" :disabled="index === form.assets.length - 1">下移</button>
              <button class="secondary-btn" type="button" @click="openAsset(asset)" :disabled="!asset.fileId">预览</button>
              <button class="danger-btn" type="button" @click="removeAsset(index)">移除</button>
            </div>
          </div>
        </div>
      </section>

      <button class="primary-btn" type="submit" :disabled="submitting || !collectionId">
        {{ submitting ? '提交中...' : '创建题目' }}
      </button>
    </form>
  </section>
  <section v-else class="card empty">
    <h2>缺少题集信息</h2>
    <p>请从题集题目列表页面点击“添加题目”。</p>
    <RouterLink to="/collections" class="primary-btn">返回题集管理</RouterLink>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import MathView from '../components/MathView.vue';
import { createQuestion } from '../api/question';
import { uploadAssetFile } from '../api/common';
import { showError, showSuccess } from '../utils/messages';
import { buildFileViewUrl } from '../utils/file';
import type { QuestionAsset, QuestionOption } from '../types/question';
import type { QuestionCreatePayload } from '../api/question';
import '../styles/question-form.css';

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
  judgeAnswer: 'T',
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

function isOptionCorrect(key?: string) {
  if (!key) {
    return false;
  }
  return isSingleChoice.value ? singleCorrect.value === key : form.choiceCorrect.includes(key);
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
  align-items: center;
  gap: 12px;
}

.create-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.preview-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #475569;
  margin-top: 8px;
}

.form-grid select {
  width: 100%;
  border: 1px solid #cbd5f5;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 14px;
}

.inline-row {
  display: flex;
  gap: 12px;
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.asset-upload {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.asset-upload .thumb {
  width: 120px;
  height: 120px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.asset-upload .thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.asset-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hint {
  color: #94a3b8;
  font-size: 13px;
}

.empty {
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
