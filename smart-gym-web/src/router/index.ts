import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    /** 需要登录 */
    requiresAuth?: boolean
    /** 需要管理员角色 */
    requiresAdmin?: boolean
    title?: string
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        {
          path: '',
          name: 'admin-home',
          component: () => import('@/views/admin/UsersView.vue'),
          meta: { title: '用户管理' },
        },
      ],
    },
    {
      path: '/',
      component: () => import('@/layouts/MemberLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { requiresAuth: true, title: '首页' },
        },
        {
          path: 'agent',
          name: 'agent-chat',
          component: () => import('@/views/agent-chat/index.vue'),
          meta: { requiresAuth: true, title: 'AI 健康助理' },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/profile/index.vue'),
          meta: { requiresAuth: true, title: '健康档案' },
        },
        {
          path: 'exercises',
          name: 'exercises',
          component: () => import('@/views/exercises/index.vue'),
          meta: { requiresAuth: true, title: '动作库' },
        },
        {
          path: 'workout-plan',
          name: 'workout-plan',
          meta: { requiresAuth: true, title: '训练计划' },
          children: [
            { path: '', name: 'workout-plan-list', component: () => import('@/views/workout-plan/index.vue') },
          ],
        },
        {
          path: 'workout-session',
          name: 'workout-session',
          component: () => import('@/views/workout-session/index.vue'),
          meta: { requiresAuth: true, title: '训练记录' },
        },
        {
          path: 'nutrition',
          name: 'nutrition',
          component: () => import('@/views/nutrition/index.vue'),
          meta: { requiresAuth: true, title: '饮食管理' },
          children: [
            { path: '', name: 'nutrition-home', component: () => import('@/views/nutrition/index.vue') },
          ],
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdmin) {
    // 未拉取过用户信息时先拉取，角色判断依赖它
    if (!auth.user && auth.isLoggedIn) {
      return auth.fetchCurrentUser().then(() => {
        if (!auth.isAdmin) return { path: '/dashboard' }
      })
    }
    if (!auth.isAdmin) return { path: '/dashboard' }
  }

  if (to.path === '/login' && auth.isLoggedIn) {
    return { path: '/dashboard' }
  }
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · SmartGym Copilot` : 'SmartGym Copilot'
})

export default router
