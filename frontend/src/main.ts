import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import App from './App.vue';
import router from './router';
import './assets/main.css';
import 'element-plus/dist/index.css';
import { setUnauthorizedHandler } from './utils/auth-events';
import { useAuthStore } from './stores/auth';
import { loadMathJax } from './plugins/mathjax';

const app = createApp(App);
const pinia = createPinia();

loadMathJax().catch((error) => {
  console.error('[MathJax] 初始化失败', error);
});

setUnauthorizedHandler(() => {
  const authStore = useAuthStore(pinia);
  authStore.clear();
  if (router.currentRoute.value.path !== '/auth') {
    router.replace({ path: '/auth', query: { redirect: router.currentRoute.value.fullPath } });
  }
});

app.use(pinia);
app.use(ElementPlus, {
  locale: zhCn
});
app.use(router);
app.mount('#app');
