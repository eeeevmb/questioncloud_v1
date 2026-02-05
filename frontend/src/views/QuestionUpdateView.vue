<template>
  <section class="card" v-if="detail">
    <h2>更新题目（当前版本 {{ detail.versionNo }}）</h2>
    <p>题目类型：{{ detail.typeCode }}，更新会生成新版本。</p>
    <form class="form" @submit.prevent="handleSubmit">
      <section class="field-pair single">
        <div>
          <label>标题</label>
          <input v-model="form.title" required />
        </div>
      </section>

      <section class="field-pair with-preview">
        <div>
          <label>题干</label>
          <textarea v-model="form.stem" rows="4" required></textarea>
        </div>
        <div class="preview-card">
          <p>题干预览</p>
          <MathView :content="form.stem" empty-text="暂无题干" :debounce="250" />
        </div>
      </section>

      <section class="field-pair with-preview">
        <div>
          <label>展示答案</label>
          <textarea v-model="form.answer" rows="3"></textarea>
        </div>
        <div class="preview-card">
          <p>答案预览</p>
          <MathView :content="form.answer" empty-text="暂无答案" :debounce="250" />
        </div>
      </section>

      <section class="field-pair with-preview">
        <div>
          <label>解析</label>
          <textarea v-model="form.solution" rows="3"></textarea>
        </div>
        <div class="preview-card">
          <p>解析预览</p>
          <MathView :content="form.solution" empty-text="尚未提供解析" :debounce="250" />
        </div>
      </section>

      <div v-if="detail.typeCode === 'true-false'" class="type-section">
        <h3>判断题答案</h3>
        <select v-model="form.judgeAnswer">
          <option value="T">正确</option>
          <option value="F">错误</option>
        </select>
      </div>

      <section class="type-section" v-if="isChoiceType">
        <header class="section-header">
          <h3>选项内容</h3>
          <div class="choice-actions">
            <button class="secondary-btn" type="button" @click="addOption">新增选项</button>
          </div>
        </header>
        <div v-if="!form.options.length" class="hint">暂无选项</div>
        <div v-for="(option, index) in form.options" :key="index" class="option-editor">
          <div class="option-head">
            <label class="option-key">
              选项编号
              <input v-model="option.key" placeholder="如 A/B/C" />
            </label>
            <div class="option-actions">
              <label class="answer-selector" :class="{ active: isOptionCorrect(option.key) }">
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
              <button class="danger-btn" type="button" @click="removeOption(index)" v-if="form.options.length > 1">移除</button>
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

      <section class="type-section">
        <header class="section-header">
          <div>
            <h3>附件</h3>
            <p class="hint">上传后即可预览，支持调整顺序。</p>
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

      <button class="primary-btn" type="submit" :disabled="saving">
        {{ saving ? '保存中...' : '提交更新' }}
      </button>
    </form>
  </section>
  <div v-else>加载中...</div>
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
import '../styles/question-form.css';

const route = useRoute();
const router = useRouter();
const detail = ref<QuestionDetail | null>(null);
const saving = ref(false);
const form = reactive({
  title: '',
  stem: '',
  answer: '',
  solution: '',
  judgeAnswer: 'T',
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
    detail.value = await fetchQuestionDetail(questionId.value);
    form.title = detail.value.title;
    form.stem = detail.value.stem;
    form.answer = detail.value.answer ?? '';
    form.solution = detail.value.solution ?? '';
    form.assets = detail.value.assets
      ? detail.value.assets.map((asset) => ({ ...asset, fileId: asset.fileId?.toString() ?? '' }))
      : [];
    assetPreviewErrors.clear();
    form.options = detail.value.options
      ? detail.value.options.map((opt) => ({ ...opt }))
      : [];
    if (isChoiceType.value && form.options.length === 0) {
      form.options = [{ key: 'A', content: '' }];
    }

    const parsedCorrect = getCorrectKeys(detail.value);
    form.choiceCorrect = parsedCorrect;
    singleCorrect.value = parsedCorrect[0] ?? '';

    if (detail.value.typeCode === 'true-false') {
      form.judgeAnswer = (detail.value.answerKey as 'T' | 'F') || 'T';
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
.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 12px;
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
  gap: 8px;
  flex-wrap: wrap;
}

.hint {
  color: #94a3b8;
  font-size: 13px;
}
</style>
