<template>
  <el-card>
    <h2>更换头像</h2>
    <p class="subtitle">上传并裁剪图片，建议使用清晰的人像照片。</p>
    <input type="file" accept="image/png,image/jpeg" @change="handleFileChange" />

    <div class="editor" v-if="previewUrl">
      <div class="canvas">
        <img ref="imageRef" :src="previewUrl" alt="待裁剪头像" />
      </div>
      <div class="preview-panel">
        <div class="preview" ref="previewRef"></div>
        <p>预览</p>
        <el-form label-position="top">
          <el-form-item label="导出尺寸">
            <el-select v-model="outputSize">
              <el-option :value="256" label="256 × 256" />
              <el-option :value="320" label="320 × 320" />
            </el-select>
          </el-form-item>
          <el-form-item label="导出格式">
            <el-select v-model="outputFormat">
              <el-option value="image/png" label="PNG" />
              <el-option value="image/jpeg" label="JPEG" />
            </el-select>
          </el-form-item>
        </el-form>
        <el-button type="primary" :loading="uploading" @click="submit">上传头像</el-button>
      </div>
    </div>
    <p class="hint">支持 png / jpg，文件 ≤ 5MB，裁剪区域固定为 1:1。</p>
  </el-card>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue';
import Cropper from 'cropperjs';
import 'cropperjs/dist/cropper.css';
import { uploadAvatar } from '../api/user';
import { showError, showSuccess } from '../utils/messages';
import { useAuthStore } from '../stores/auth';
import { useRouter } from 'vue-router';

const MAX_SIZE = 5 * 1024 * 1024;
const authStore = useAuthStore();
const router = useRouter();
const imageRef = ref<HTMLImageElement | null>(null);
const previewRef = ref<HTMLDivElement | null>(null);
const cropper = ref<Cropper | null>(null);
const previewUrl = ref('');
const uploading = ref(false);
const outputSize = ref(256);
const outputFormat = ref<'image/png' | 'image/jpeg'>('image/png');
let objectUrl: string | null = null;

function destroyCropper() {
  cropper.value?.destroy();
  cropper.value = null;
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  if (!/^image\/(png|jpeg|jpg)$/.test(file.type)) {
    showError('仅支持 PNG 或 JPEG 图片');
    return;
  }
  if (file.size > MAX_SIZE) {
    showError('图片大小不能超过 5MB');
    return;
  }
  if (objectUrl) {
    URL.revokeObjectURL(objectUrl);
  }
  objectUrl = URL.createObjectURL(file);
  previewUrl.value = objectUrl;
  requestAnimationFrame(initCropper);
}

function initCropper() {
  destroyCropper();
  if (!imageRef.value) {
    return;
  }
  cropper.value = new Cropper(imageRef.value, {
    aspectRatio: 1,
    viewMode: 1,
    minCropBoxWidth: 128,
    minCropBoxHeight: 128,
    dragMode: 'move',
    autoCropArea: 1,
    preview: previewRef.value ?? undefined
  });
}

function getCroppedBlob(): Promise<Blob> {
  return new Promise((resolve, reject) => {
    if (!cropper.value) {
      reject(new Error('请先选择图片并裁剪'));
      return;
    }
    const canvas = cropper.value.getCroppedCanvas({
      width: outputSize.value,
      height: outputSize.value,
      imageSmoothingQuality: 'high'
    });
    canvas.toBlob((blob) => {
      if (!blob) {
        reject(new Error('生成头像失败'));
      } else {
        resolve(blob);
      }
    }, outputFormat.value, outputFormat.value === 'image/jpeg' ? 0.85 : undefined);
  });
}

async function submit() {
  try {
    uploading.value = true;
    const blob = await getCroppedBlob();
    const extension = outputFormat.value === 'image/png' ? 'png' : 'jpg';
    const file = new File([blob], `avatar.${extension}`, { type: outputFormat.value });
    await uploadAvatar(file);
    authStore.bumpAvatarVersion();
    showSuccess('头像更新成功');
    router.push('/home');
  } catch (error: any) {
    showError(error?.message ?? '上传失败');
  } finally {
    uploading.value = false;
  }
}

onBeforeUnmount(() => {
  destroyCropper();
  if (objectUrl) {
    URL.revokeObjectURL(objectUrl);
  }
});
</script>

<style scoped>
.subtitle {
  margin: 8px 0 16px;
  color: var(--el-text-color-secondary);
}

.editor {
  display: flex;
  gap: 24px;
  margin-top: 16px;
}

.canvas {
  flex: 2;
  min-height: 360px;
}

.canvas img {
  max-width: 100%;
}

.preview-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}

.preview {
  width: 160px;
  height: 160px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid var(--el-border-color);
}

.preview img {
  width: 100%;
}

.hint {
  margin-top: 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

@media (max-width: 768px) {
  .editor {
    flex-direction: column;
  }
}
</style>
