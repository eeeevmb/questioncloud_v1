<template>
  <div class="auth-wrapper">
    <!-- 背景装饰圆 -->
    <div class="bg-circle circle-1"></div>
    <div class="bg-circle circle-2"></div>

    <div class="auth-box">
      <!-- 左侧：产品语境展示区 -->
      <div class="product-info">
        <div class="logo-area">
          <div class="logo-icon"></div>
          <h1>智能云题库</h1>
        </div>
        <p class="slogan">专为教师打造的一站式试题管理平台</p>
        <ul class="features">
          <li> 海量试题云端存储</li>
          <li> 智能组卷，一键生成</li>
          <li> 专属题库，安全私密</li>
        </ul>
      </div>

      <!-- 右侧：登录/注册表单 -->
      <div class="form-section">
        <div class="auth-header">
          <h2 :class="{ active: isLoginMode }" @click="toggleMode(true)">登录</h2>
          <span class="divider"></span>
          <h2 :class="{ active: !isLoginMode }" @click="toggleMode(false)">注册</h2>
        </div>

        <form @submit.prevent="handleSubmit" class="auth-form">
          
          <!-- 注册模式：用户名 -->
          <div class="form-group" v-if="!isLoginMode">
            <label>教师姓名 / 用户名</label>
            <input 
              v-model="form.username" 
              type="text" 
              placeholder="请输入您的用户名" 
              :class="{ error: errors.username }"
            />
            <span class="error-msg" v-if="errors.username">{{ errors.username }}</span>
          </div>

          <!-- 注册模式：邮箱 -->
          <div class="form-group" v-if="!isLoginMode">
            <label>教育邮箱</label>
            <input 
              v-model="form.email" 
              type="email" 
              placeholder="example@school.edu.cn" 
              :class="{ error: errors.email }"
            />
            <span class="error-msg" v-if="errors.email">{{ errors.email }}</span>
          </div>

          <!-- 登录模式：账号 -->
          <div class="form-group" v-if="isLoginMode">
            <label>账号</label>
            <input 
              v-model="form.account" 
              type="text" 
              placeholder="用户名或邮箱" 
              :class="{ error: errors.account }"
            />
            <span class="error-msg" v-if="errors.account">{{ errors.account }}</span>
          </div>

          <!-- 通用：密码 -->
          <div class="form-group">
            <label>密码</label>
            <input 
              v-model="form.password" 
              type="password" 
              placeholder="8-20位，含字母/数字/符号" 
              :class="{ error: errors.password }"
            />
            <span class="error-msg" v-if="errors.password">{{ errors.password }}</span>
          </div>

          <!-- 提交按钮 -->
          <button type="submit" :disabled="loading" class="submit-btn">
            {{ loading ? '系统处理中...' : (isLoginMode ? '进入题库系统' : '创建教师账号') }}
          </button>

          <!-- 接口报错提示 -->
          <div class="api-error" v-if="apiErrorMessage">
            ⚠️ {{ apiErrorMessage }}
          </div>
        </form>
      </div>
    </div>
    
    <!-- 底部版权 -->
    <div class="footer-copyright">
      © 2024 智能教学辅助系统 | 面向未来教育
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';

// 定义事件，通知父组件登录成功
const emit = defineEmits(['login-success']);

const isLoginMode = ref(true); 
const loading = ref(false);
const apiErrorMessage = ref('');
const form = reactive({ username: '', email: '', password: '', account: '' });
const errors = reactive({});

// 切换模式
const toggleMode = (isLogin) => {
  isLoginMode.value = isLogin;
  apiErrorMessage.value = '';
  // 清空表单
  Object.keys(form).forEach(k => form[k] = '');
  Object.keys(errors).forEach(k => delete errors[k]);
};

// 验证逻辑 (保持你的原样)
const validate = () => {
  let isValid = true;
  Object.keys(errors).forEach(key => delete errors[key]);

  if (!form.password || form.password.length < 8 || form.password.length > 20) {
    errors.password = '密码需8-20位，建议包含字母与数字';
    isValid = false;
  }

  if (isLoginMode.value) {
    if (!form.account) { errors.account = '请输入账号'; isValid = false; }
  } else {
    const userRegex = /^[\u4e00-\u9fa5a-zA-Z0-9_-]{3,16}$/;
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    
    if (!userRegex.test(form.username)) {
      errors.username = '3-16位，支持中文、字母、数字';
      isValid = false;
    }
    if (!emailRegex.test(form.email)) {
      errors.email = '邮箱格式不正确';
      isValid = false;
    }
  }
  return isValid;
};

// === 核心修改：提交真实数据 ===
const handleSubmit = async () => {
  if (!validate()) return;
  
  loading.value = true;
  apiErrorMessage.value = '';

  // 根据模式选择真实的后端接口地址
  const url = isLoginMode.value ? '/api/v1/user/login' : '/api/v1/user/register';
  
  // 构造真实的数据包
  const payload = isLoginMode.value 
    ? { account: form.account, password: form.password }
    : { username: form.username, email: form.email, password: form.password };

  try {
    // 发送真实的 fetch 请求
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    
    // 解析后端返回的 JSON
    const result = await res.json();

    // 200000 是后端定义的成功代码
    if (result.code === '200000') {
      if (isLoginMode.value) {
        // === 登录成功 ===
        // 1. 保存 Token (鉴权凭证)
        localStorage.setItem('tokenName', result.data.tokenName);
        localStorage.setItem('tokenValue', result.data.tokenValue);
        
        // 2. 通知 App.vue 切换页面，并传入用户信息
        emit('login-success', result.data);
      } else {
        // === 注册成功 ===
        alert('注册成功！请使用刚才的账号登录。');
        toggleMode(true); // 自动切回登录模式
      }
    } else {
      // 显示后端的错误提示 (比如"密码错误"或"用户不存在")
      apiErrorMessage.value = result.message || '操作失败';
    }
  } catch (err) {
    apiErrorMessage.value = '无法连接到服务器，请确认后端已启动 (localhost:8080)';
    console.error(err);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
/* 全局容器：使用柔和的渐变背景 */
.auth-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  position: relative;
  overflow: hidden;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  flex-direction: column;
}

/* 背景装饰球 */
.bg-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  z-index: 1;
}
.circle-1 {
  width: 300px;
  height: 300px;
  background: rgba(64, 169, 255, 0.4);
  top: -50px;
  left: -50px;
}
.circle-2 {
  width: 400px;
  height: 400px;
  background: rgba(105, 192, 255, 0.3);
  bottom: -100px;
  right: -100px;
}

/* 核心卡片：左右布局 */
.auth-box {
  display: flex;
  width: 850px;
  height: 520px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  z-index: 2;
}

/* 左侧：产品展示区 */
.product-info {
  flex: 1;
  background: linear-gradient(135deg, #1890ff 0%, #0050b3 100%);
  padding: 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: white;
  position: relative;
}

.logo-area {
  margin-bottom: 20px;
}
.logo-icon {
  font-size: 48px;
  margin-bottom: 10px;
}
.product-info h1 {
  font-size: 32px;
  margin: 0;
  font-weight: 600;
  letter-spacing: 1px;
}
.slogan {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 40px;
  font-weight: 300;
}

.features {
  list-style: none;
  padding: 0;
  margin: 0;
}
.features li {
  margin-bottom: 15px;
  font-size: 15px;
  display: flex;
  align-items: center;
  opacity: 0.9;
}

/* 右侧：表单区 */
.form-section {
  flex: 1;
  padding: 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.auth-header {
  display: flex;
  align-items: center;
  margin-bottom: 35px;
}
.auth-header h2 {
  margin: 0;
  cursor: pointer;
  color: #999;
  font-size: 18px;
  transition: all 0.3s;
}
.auth-header h2.active {
  color: #1890ff;
  font-size: 24px;
  font-weight: bold;
}
.divider {
  height: 20px;
  width: 1px;
  background: #eee;
  margin: 0 20px;
}

/* 表单样式优化 */
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: #666;
  font-weight: 500;
}
.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.3s;
  background-color: #fcfcfc;
  box-sizing: border-box; /* 必须加这个 */
}
.form-group input:focus {
  border-color: #1890ff;
  background-color: white;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
  outline: none;
}
.form-group input.error {
  border-color: #ff4d4f;
  background-color: #fff1f0;
}
.error-msg {
  color: #ff4d4f;
  font-size: 12px;
  margin-top: 5px;
  display: block;
}

/* 按钮样式 */
.submit-btn {
  width: 100%;
  padding: 12px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 10px;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}
.submit-btn:hover {
  background: #40a9ff;
  transform: translateY(-1px);
}
.submit-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
  box-shadow: none;
}

.api-error {
  margin-top: 20px;
  padding: 10px;
  background: #fff2f0;
  color: #ff4d4f;
  border-radius: 6px;
  text-align: center;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.footer-copyright {
  margin-top: 20px;
  font-size: 12px;
  color: #888;
  z-index: 2;
}

/* 响应式适配：手机端变成单列 */
@media (max-width: 900px) {
  .auth-box {
    width: 90%;
    height: auto;
    flex-direction: column;
  }
  .product-info {
    padding: 30px;
    text-align: center;
  }
  .features {
    display: none; /* 手机端隐藏特性列表，保持简洁 */
  }
  .form-section {
    padding: 30px;
  }
}
</style>