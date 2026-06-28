<template>
  <el-dialog
    :model-value="modelValue"
    title="找回密码"
    width="420px"
    destroy-on-close
    append-to-body
    @update:model-value="handleDialogVisibleChange"
    @closed="resetForm"
  >
    <el-form label-position="top" @submit.prevent="handleResetPassword">
      <el-form-item label="邮箱">
        <el-input
          v-model="form.email"
          type="email"
          placeholder="请输入注册邮箱"
          :disabled="emailReadonly"
          clearable
        />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input
          v-model="form.newPassword"
          type="password"
          placeholder="请输入新密码"
          show-password
        />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input
          v-model="form.confirmNewPassword"
          type="password"
          placeholder="请再次输入新密码"
          show-password
        />
      </el-form-item>
      <el-form-item label="验证码">
        <div class="verification-row">
          <el-input
            v-model="form.verificationCode"
            maxlength="6"
            placeholder="请输入 6 位验证码"
            clearable
          />
          <el-button
            class="send-code-button"
            :disabled="sendCodeDisabled"
            :loading="sendCodeLoading"
            @click="handleSendCode"
          >
            {{ sendCodeButtonText }}
          </el-button>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="resetPasswordLoading" @click="handleResetPassword">
          重置密码
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { resetPassword, sendResetPasswordCode } from '../api/user';
import { showError, showInfo, showSuccess } from '../utils/messages';

interface Props {
  modelValue: boolean;
  initialEmail?: string;
  emailReadonly?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  initialEmail: '',
  emailReadonly: false
});

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  success: [email: string];
}>();

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const SEND_CODE_COUNTDOWN_SECONDS = 60;

const form = reactive({
  email: '',
  newPassword: '',
  confirmNewPassword: '',
  verificationCode: ''
});

const sendCodeLoading = ref(false);
const resetPasswordLoading = ref(false);
const sendCodeCountdown = ref(0);
let sendCodeTimer: number | null = null;

const sendCodeDisabled = computed(() => {
  return sendCodeLoading.value || sendCodeCountdown.value > 0 || !isValidEmail(form.email);
});

const sendCodeButtonText = computed(() => {
  return sendCodeCountdown.value > 0 ? `${sendCodeCountdown.value}s 后重发` : '发送验证码';
});

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      applyInitialEmail();
    }
  },
  { immediate: true }
);

watch(
  () => props.initialEmail,
  () => {
    if (!props.modelValue) {
      applyInitialEmail();
    }
  }
);

async function handleSendCode() {
  const email = form.email.trim();
  if (!isValidEmail(email)) {
    showError('请输入正确的邮箱地址');
    return;
  }
  if (!form.newPassword || !form.confirmNewPassword) {
    showInfo('请先填写新密码和确认新密码');
    return;
  }
  if (form.newPassword !== form.confirmNewPassword) {
    showError('两次输入的新密码不一致');
    return;
  }

  sendCodeLoading.value = true;
  try {
    await sendResetPasswordCode({ email });
    startCountdown();
    showSuccess('重置密码验证码已发送，请查收邮箱');
  } finally {
    sendCodeLoading.value = false;
  }
}

async function handleResetPassword() {
  if (!form.email || !form.newPassword || !form.confirmNewPassword || !form.verificationCode) {
    showInfo('请完整填写找回密码信息');
    return;
  }
  if (!isValidEmail(form.email)) {
    showError('请输入正确的邮箱地址');
    return;
  }
  if (form.newPassword !== form.confirmNewPassword) {
    showError('两次输入的新密码不一致');
    return;
  }
  if (!/^\d{6}$/.test(form.verificationCode)) {
    showError('请输入 6 位数字验证码');
    return;
  }

  resetPasswordLoading.value = true;
  try {
    const email = form.email.trim();
    await resetPassword({
      email,
      newPassword: form.newPassword,
      verificationCode: form.verificationCode
    });
    showSuccess('密码重置成功，请使用新密码登录');
    emit('success', email);
    closeDialog();
  } finally {
    resetPasswordLoading.value = false;
  }
}

function handleDialogVisibleChange(visible: boolean) {
  emit('update:modelValue', visible);
}

function closeDialog() {
  emit('update:modelValue', false);
}

function isValidEmail(email: string) {
  return EMAIL_PATTERN.test(email.trim());
}

function applyInitialEmail() {
  form.email = props.initialEmail.trim();
}

function resetForm() {
  form.email = props.initialEmail.trim();
  form.newPassword = '';
  form.confirmNewPassword = '';
  form.verificationCode = '';
  sendCodeCountdown.value = 0;
  clearSendCodeTimer();
}

function startCountdown() {
  clearSendCodeTimer();
  sendCodeCountdown.value = SEND_CODE_COUNTDOWN_SECONDS;
  sendCodeTimer = window.setInterval(() => {
    if (sendCodeCountdown.value <= 1) {
      sendCodeCountdown.value = 0;
      clearSendCodeTimer();
      return;
    }
    sendCodeCountdown.value -= 1;
  }, 1000);
}

function clearSendCodeTimer() {
  if (sendCodeTimer !== null) {
    window.clearInterval(sendCodeTimer);
    sendCodeTimer = null;
  }
}

onBeforeUnmount(() => {
  clearSendCodeTimer();
});
</script>

<style scoped>
.verification-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.send-code-button {
  flex-shrink: 0;
  min-width: 128px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
