<template>
  <el-card class="auth-card">
    <template #header>
      <div class="header">
        <h1>题云</h1>
        <p>注册或登录后即可管理题集和题目。</p>
      </div>
    </template>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="登录" name="login">
        <el-form label-position="top" @submit.prevent="handleLogin">
          <el-form-item label="用户名 / 邮箱">
            <el-input v-model="loginForm.account" placeholder="用户名或邮箱" clearable />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="loginForm.password" type="password" placeholder="密码" show-password />
          </el-form-item>
          <div class="login-actions">
            <el-button type="primary" :loading="loginLoading" @click="handleLogin">登录</el-button>
            <el-button text type="primary" @click="openResetPasswordDialog">忘记密码？</el-button>
          </div>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="注册" name="register">
        <el-form label-position="top" @submit.prevent="handleRegister">
          <el-form-item label="用户名">
            <el-input v-model="registerForm.username" placeholder="3-16 位用户名" clearable />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="registerForm.email" type="email" placeholder="name@example.com" clearable />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="registerForm.password" type="password" placeholder="至少 8 位" show-password />
          </el-form-item>
          <el-form-item label="验证码">
            <div class="verification-row">
              <el-input
                v-model="registerForm.verificationCode"
                maxlength="6"
                placeholder="请输入 6 位验证码"
                clearable
              />
              <el-button
                class="send-code-button"
                :disabled="sendCodeDisabled"
                :loading="sendCodeLoading"
                @click="handleSendRegisterCode"
              >
                {{ sendCodeButtonText }}
              </el-button>
            </div>
          </el-form-item>
          <el-button type="success" :loading="registerLoading" @click="handleRegister">注册</el-button>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <el-dialog
    v-model="resetPasswordDialogVisible"
    title="找回密码"
    width="420px"
    destroy-on-close
    append-to-body
    @closed="resetResetPasswordForm"
  >
    <el-form label-position="top" @submit.prevent="handleResetPassword">
      <el-form-item label="邮箱">
        <el-input v-model="resetPasswordForm.email" type="email" placeholder="请输入注册邮箱" clearable />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input
          v-model="resetPasswordForm.newPassword"
          type="password"
          placeholder="请输入新密码"
          show-password
        />
      </el-form-item>
      <el-form-item label="验证码">
        <div class="verification-row">
          <el-input
            v-model="resetPasswordForm.verificationCode"
            maxlength="6"
            placeholder="请输入 6 位验证码"
            clearable
          />
          <el-button
            class="send-code-button"
            :disabled="resetCodeDisabled"
            :loading="resetCodeLoading"
            @click="handleSendResetPasswordCode"
          >
            {{ resetCodeButtonText }}
          </el-button>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeResetPasswordDialog">取消</el-button>
        <el-button type="primary" :loading="resetPasswordLoading" @click="handleResetPassword">
          重置密码
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { loginUser, registerUser, resetPassword, sendRegisterCode, sendResetPasswordCode } from '../api/user';
import { useAuthStore } from '../stores/auth';
import { showError, showInfo, showSuccess } from '../utils/messages';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const SEND_CODE_COUNTDOWN_SECONDS = 60;

const activeTab = ref<'login' | 'register'>('login');
const loginForm = reactive({
  account: '',
  password: ''
});
const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  verificationCode: ''
});
const resetPasswordForm = reactive({
  email: '',
  newPassword: '',
  verificationCode: ''
});

const loginLoading = ref(false);
const registerLoading = ref(false);
const sendCodeLoading = ref(false);
const sendCodeCountdown = ref(0);
const resetPasswordDialogVisible = ref(false);
const resetCodeLoading = ref(false);
const resetCodeCountdown = ref(0);
const resetPasswordLoading = ref(false);
let sendCodeTimer: number | null = null;
let resetCodeTimer: number | null = null;

const sendCodeDisabled = computed(() => {
  return sendCodeLoading.value || sendCodeCountdown.value > 0 || !isValidEmail(registerForm.email);
});

const sendCodeButtonText = computed(() => {
  return sendCodeCountdown.value > 0 ? `${sendCodeCountdown.value}s 后重发` : '发送验证码';
});

const resetCodeDisabled = computed(() => {
  return resetCodeLoading.value || resetCodeCountdown.value > 0 || !isValidEmail(resetPasswordForm.email);
});

const resetCodeButtonText = computed(() => {
  return resetCodeCountdown.value > 0 ? `${resetCodeCountdown.value}s 后重发` : '发送验证码';
});

async function handleLogin() {
  if (!loginForm.account || !loginForm.password) {
    showInfo('请输入账号和密码');
    return;
  }
  loginLoading.value = true;
  try {
    await loginUser({ ...loginForm });
    await authStore.refreshUser();
    showSuccess('登录成功');
    const redirect = (route.query.redirect as string) || '/home';
    router.replace(redirect);
  } catch (error) {
    // 错误信息由 axios 拦截器提示
  } finally {
    loginLoading.value = false;
  }
}

function openResetPasswordDialog() {
  resetPasswordDialogVisible.value = true;
}

function closeResetPasswordDialog() {
  resetPasswordDialogVisible.value = false;
  resetResetPasswordForm();
}

async function handleSendRegisterCode() {
  const email = registerForm.email.trim();
  if (!isValidEmail(email)) {
    showError('请输入正确的邮箱地址');
    return;
  }

  sendCodeLoading.value = true;
  try {
    await sendRegisterCode({ email });
    startCountdown('register');
    showSuccess('验证码已发送，请查收邮箱');
  } catch (error) {
    // 已统一提示
  } finally {
    sendCodeLoading.value = false;
  }
}

async function handleSendResetPasswordCode() {
  const email = resetPasswordForm.email.trim();
  if (!isValidEmail(email)) {
    showError('请输入正确的邮箱地址');
    return;
  }

  resetCodeLoading.value = true;
  try {
    await sendResetPasswordCode({ email });
    startCountdown('reset');
    showSuccess('重置密码验证码已发送，请查收邮箱');
  } catch (error) {
    // 已统一提示
  } finally {
    resetCodeLoading.value = false;
  }
}

async function handleRegister() {
  if (!registerForm.username || !registerForm.email || !registerForm.password || !registerForm.verificationCode) {
    showInfo('请完整填写注册信息');
    return;
  }
  if (!isValidEmail(registerForm.email)) {
    showError('请输入正确的邮箱地址');
    return;
  }
  if (!/^\d{6}$/.test(registerForm.verificationCode)) {
    showError('请输入 6 位数字验证码');
    return;
  }
  registerLoading.value = true;
  try {
    await registerUser({ ...registerForm });
    showSuccess('注册成功，请使用账号登录');
    resetRegisterForm();
    activeTab.value = 'login';
  } catch (error) {
    // 已统一提示
  } finally {
    registerLoading.value = false;
  }
}

async function handleResetPassword() {
  if (!resetPasswordForm.email || !resetPasswordForm.newPassword || !resetPasswordForm.verificationCode) {
    showInfo('请完整填写找回密码信息');
    return;
  }
  if (!isValidEmail(resetPasswordForm.email)) {
    showError('请输入正确的邮箱地址');
    return;
  }
  if (!/^\d{6}$/.test(resetPasswordForm.verificationCode)) {
    showError('请输入 6 位数字验证码');
    return;
  }

  resetPasswordLoading.value = true;
  try {
    await resetPassword({ ...resetPasswordForm });
    loginForm.account = resetPasswordForm.email.trim();
    loginForm.password = '';
    showSuccess('密码重置成功，请使用新密码登录');
    closeResetPasswordDialog();
  } catch (error) {
    // 已统一提示
  } finally {
    resetPasswordLoading.value = false;
  }
}

function isValidEmail(email: string) {
  return EMAIL_PATTERN.test(email.trim());
}

function startCountdown(scene: 'register' | 'reset') {
  if (scene === 'register') {
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
    return;
  }

  clearResetCodeTimer();
  resetCodeCountdown.value = SEND_CODE_COUNTDOWN_SECONDS;
  resetCodeTimer = window.setInterval(() => {
    if (resetCodeCountdown.value <= 1) {
      resetCodeCountdown.value = 0;
      clearResetCodeTimer();
      return;
    }
    resetCodeCountdown.value -= 1;
  }, 1000);
}

function clearSendCodeTimer() {
  if (sendCodeTimer !== null) {
    window.clearInterval(sendCodeTimer);
    sendCodeTimer = null;
  }
}

function clearResetCodeTimer() {
  if (resetCodeTimer !== null) {
    window.clearInterval(resetCodeTimer);
    resetCodeTimer = null;
  }
}

function resetRegisterForm() {
  registerForm.username = '';
  registerForm.email = '';
  registerForm.password = '';
  registerForm.verificationCode = '';
  sendCodeCountdown.value = 0;
  clearSendCodeTimer();
}

function resetResetPasswordForm() {
  resetPasswordForm.email = '';
  resetPasswordForm.newPassword = '';
  resetPasswordForm.verificationCode = '';
  resetCodeCountdown.value = 0;
  clearResetCodeTimer();
}

onBeforeUnmount(() => {
  clearSendCodeTimer();
  clearResetCodeTimer();
});
</script>

<style scoped>
.auth-card {
  max-width: 460px;
  margin: 48px auto;
}

.header h1 {
  margin: 0;
}

.header p {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
}

.verification-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.send-code-button {
  flex-shrink: 0;
  min-width: 128px;
}

.login-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-input__wrapper input:-webkit-autofill),
:deep(.el-input__wrapper input:-webkit-autofill:hover),
:deep(.el-input__wrapper input:-webkit-autofill:focus),
:deep(.el-textarea__inner:-webkit-autofill),
:deep(.el-textarea__inner:-webkit-autofill:hover),
:deep(.el-textarea__inner:-webkit-autofill:focus) {
  -webkit-text-fill-color: var(--el-text-color-primary);
  box-shadow: 0 0 0 1000px #ffffff inset;
  transition: background-color 9999s ease-out 0s;
}
</style>
