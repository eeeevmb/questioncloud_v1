<template>
  <section v-if="normalizedAssets.length" class="attachment-gallery">
    <p v-if="title" class="gallery-title">{{ title }}</p>
    <div class="asset-grid">
      <article v-for="asset in normalizedAssets" :key="assetKey(asset)" class="asset-card">
        <div class="preview" @click="openPreview(asset)">
          <img
            v-if="isImage(asset) && !hasError(asset)"
            :src="assetUrl(asset)"
            :alt="title || '附件'"
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
          <button class="secondary-btn" type="button" :disabled="!assetUrl(asset)" @click.stop="openInNewTab(asset)">
            查看原图
          </button>
          <p v-if="hasError(asset)" class="error-text">加载失败，请稍后再试</p>
        </div>
      </article>
    </div>

    <div v-if="previewAsset" class="lightbox" @click.self="closePreview">
      <div class="lightbox-content">
        <button class="close-btn" type="button" @click="closePreview">×</button>
        <div class="preview full">
          <img v-if="previewType === 'IMAGE'" :src="assetUrl(previewAsset)" alt="附件预览" />
          <video v-else-if="previewType === 'VIDEO'" controls autoplay>
            <source :src="assetUrl(previewAsset)" type="video/mp4" />
          </video>
          <div v-else class="file-fallback">暂不支持预览，请使用“查看原图”。</div>
        </div>
      </div>
    </div>
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
}

function closePreview() {
  previewAsset.value = null;
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
  font-weight: 600;
  color: #0f172a;
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.asset-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview {
  width: 100%;
  max-height: 220px;
  min-height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
}

.preview img,
.preview video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.preview.full {
  max-height: none;
  min-height: auto;
  background: transparent;
  cursor: default;
}

.file-fallback {
  font-size: 13px;
  color: #475569;
}

.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.hint {
  font-size: 13px;
  color: #64748b;
}

.error-text {
  width: 100%;
  color: #dc2626;
  font-size: 12px;
}

.lightbox {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.lightbox-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
  padding: 24px;
  background: #fff;
  border-radius: 16px;
}

.close-btn {
  position: absolute;
  top: 8px;
  right: 12px;
  border: none;
  background: transparent;
  color: #0f172a;
  font-size: 24px;
  cursor: pointer;
}
</style>
