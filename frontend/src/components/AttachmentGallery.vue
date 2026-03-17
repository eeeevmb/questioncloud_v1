<template>
  <section v-if="normalizedAssets.length" class="attachment-gallery">
    <p v-if="title" class="gallery-title">{{ title }}</p>
    <div class="asset-grid">
      <el-card v-for="asset in normalizedAssets" :key="assetKey(asset)" shadow="never" class="asset-card">
        <div class="preview" @click="openPreview(asset)">
          <el-image
            v-if="isImage(asset) && !hasError(asset)"
            :src="assetUrl(asset)"
            fit="contain"
            @error="markError(asset)"
          />
          <video v-else-if="isVideo(asset) && !hasError(asset)" controls>
            <source :src="assetUrl(asset)" type="video/mp4" />
          </video>
          <div v-else class="file-fallback">
            <span>附件 {{ asset.ordinal ?? '-' }}</span>
          </div>
        </div>
        <div class="meta">
          <span class="hint">附件 {{ asset.ordinal ?? '-' }}</span>
          <el-button size="small" @click.stop="openInNewTab(asset)" :disabled="!assetUrl(asset)">查看原图</el-button>
          <p v-if="hasError(asset)" class="error-text">加载失败，请稍后再试</p>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="previewVisible" width="70%" destroy-on-close>
      <template #header>
        <span>附件预览</span>
      </template>
      <div class="dialog-preview">
        <el-image v-if="previewType === 'IMAGE'" :src="assetUrl(previewAsset)" fit="contain" />
        <video v-else-if="previewType === 'VIDEO'" controls autoplay>
          <source :src="assetUrl(previewAsset)" type="video/mp4" />
        </video>
        <div v-else class="file-fallback">暂不支持预览，请使用“查看原图”。</div>
      </div>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { QuestionAsset } from '../types/question';
import { buildFileViewUrl } from '../utils/file';

interface Props {
  assets?: QuestionAsset[];
  title?: string;
}

const props = defineProps<Props>();
const previewAsset = ref<QuestionAsset | null>(null);
const previewVisible = ref(false);
const loadErrorSet = ref(new Set<string>());

const normalizedAssets = computed(() => {
  if (!props.assets?.length) {
    return [];
  }
  const seen = new Set<string>();
  const sorted = [...props.assets].sort((a, b) => (a.ordinal ?? 0) - (b.ordinal ?? 0));
  const result: QuestionAsset[] = [];
  sorted.forEach((asset) => {
    const key = assetKey(asset);
    if (seen.has(key)) {
      return;
    }
    seen.add(key);
    result.push(asset);
  });
  return result;
});

const previewType = computed(() => {
  if (!previewAsset.value) {
    return '';
  }
  if (isVideo(previewAsset.value)) {
    return 'VIDEO';
  }
  if (isImage(previewAsset.value)) {
    return 'IMAGE';
  }
  return '';
});

function assetKey(asset: QuestionAsset) {
  const slotPart = asset.slotId !== undefined && asset.slotId !== null ? String(asset.slotId) : '';
  const filePart = asset.fileId || `ORD-${asset.ordinal ?? 0}`;
  return [asset.section ?? 'GENERAL', slotPart, filePart].filter(Boolean).join('|');
}

function assetUrl(asset: QuestionAsset | null | undefined) {
  if (!asset?.fileId) {
    return '';
  }
  return buildFileViewUrl(asset.fileId);
}

function isImage(asset: QuestionAsset) {
  return !asset.assetType || asset.assetType === 'IMAGE';
}

function isVideo(asset: QuestionAsset) {
  return asset.assetType === 'VIDEO';
}

function hasError(asset: QuestionAsset) {
  const url = assetUrl(asset);
  return url ? loadErrorSet.value.has(url) : false;
}

function markError(asset: QuestionAsset) {
  const url = assetUrl(asset);
  if (url) {
    loadErrorSet.value.add(url);
  }
}

function openPreview(asset: QuestionAsset) {
  if (!isImage(asset) && !isVideo(asset)) {
    openInNewTab(asset);
    return;
  }
  previewAsset.value = asset;
  previewVisible.value = true;
}

function openInNewTab(asset: QuestionAsset) {
  const url = assetUrl(asset);
  if (url) {
    window.open(url, '_blank');
  }
}
</script>

<style scoped>
.attachment-gallery {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.gallery-title {
  margin: 0;
  font-weight: 600;
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.asset-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview {
  width: 100%;
  min-height: 140px;
  max-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  overflow: hidden;
  cursor: pointer;
}

.preview :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.preview img,
.preview video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.hint {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.error-text {
  width: 100%;
  margin: 0;
  color: var(--el-color-danger);
  font-size: 12px;
}

.dialog-preview {
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog-preview :deep(.el-image) {
  width: 100%;
}

.file-fallback {
  color: var(--el-text-color-secondary);
}
</style>
