import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import { mainNavigation } from '../navigation.js'
import { createAuthGuard } from './auth-guard.js'

const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        redirect: { name: 'dishes' },
      },
      ...mainNavigation.map((item) => ({
        path: item.path.slice(1),
        name: item.name,
        component: () => import('../views/ModuleView.vue'),
        meta: {
          ...item,
          requiresAuth: true,
        },
      })),
    ],
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/NotFoundView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(createAuthGuard())

export default router
