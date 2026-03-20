import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/home' },
    {
      path: '/auth',
      name: 'auth',
      component: () => import('../views/AuthView.vue'),
      meta: { public: true }
    },
    {
      path: '/home',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/collections',
      name: 'collections',
      component: () => import('../views/CollectionsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/collections/:collectionId/questions',
      name: 'collection-questions',
      component: () => import('../views/CollectionQuestionsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/collections/:collectionId/questions/new',
      name: 'create-question',
      component: () => import('../views/QuestionCreateView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/questions/:questionId',
      name: 'question-detail',
      component: () => import('../views/QuestionDetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/questions/:questionId/edit',
      name: 'question-edit',
      component: () => import('../views/QuestionUpdateView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/math-sample',
      name: 'math-sample',
      component: () => import('../views/MathSampleView.vue'),
      meta: { public: true }
    },
    {
      path: '/avatar/edit',
      name: 'avatar-edit',
      component: () => import('../views/AvatarCropView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/agent-demo',
      name: 'agent-demo',
      component: () => import('../views/AgentDemoView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/imports',
      name: 'imports',
      component: () => import('../views/ImportsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/imports/:importId',
      name: 'import-detail',
      component: () => import('../views/ImportDetailView.vue'),
      meta: { requiresAuth: true }
    }
  ]
});

router.beforeEach(async (to, _from, next) => {
  if (to.meta.public) {
    next();
    return;
  }

  const authStore = useAuthStore();
  try {
    await authStore.initialize();
  } catch (error) {
    // initialize already triggered a redirect if necessary
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ path: '/auth', query: { redirect: to.fullPath } });
    return;
  }

  next();
});

export default router;
