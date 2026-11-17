import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import { mainNavigation } from '../navigation.js'
import { createAuthGuard } from './auth-guard.js'

const dishRoutes = [
  {
    path: 'dishes',
    name: 'dishes',
    component: () => import('../views/DishListView.vue'),
    meta: { label: '小家', requiresAuth: true, requiresFamily: true },
  },
  {
    path: 'dishes/:id',
    name: 'dish-detail',
    component: () => import('../views/DishDetailView.vue'),
    meta: { label: '菜品详情', requiresAuth: true, requiresFamily: true },
  },
  {
    path: 'dish-edit/:id?',
    name: 'dish-edit',
    component: () => import('../views/DishEditView.vue'),
    meta: { label: '菜品编辑', requiresAuth: true, requiresFamily: true },
  },
]

const plazaRoutes = [
  {
    path: 'plaza',
    name: 'plaza',
    component: () => import('../views/SystemDishListView.vue'),
    meta: { label: '广场', requiresAuth: true, requiresFamily: true },
  },
  {
    path: 'plaza/:id',
    name: 'system-dish-detail',
    component: () => import('../views/SystemDishDetailView.vue'),
    meta: { label: '菜谱详情', requiresAuth: true, requiresFamily: true },
  },
]

const cartRoutes = [
  {
    path: 'cart',
    name: 'cart',
    component: () => import('../views/CartView.vue'),
    meta: { label: '购物车', requiresAuth: true, requiresFamily: true },
  },
]

const orderRoutes = [
  {
    path: 'orders',
    name: 'orders',
    component: () => import('../views/OrderListView.vue'),
    meta: { label: '订单', requiresAuth: true, requiresFamily: true },
  },
  {
    path: 'orders/:id',
    name: 'order-detail',
    component: () => import('../views/OrderDetailView.vue'),
    meta: { label: '订单详情', requiresAuth: true, requiresFamily: true },
  },
]

const shoppingRoutes = [
  {
    path: 'shopping',
    name: 'shopping',
    component: () => import('../views/ShoppingListView.vue'),
    meta: { label: '采购清单', requiresAuth: true, requiresFamily: true },
  },
]

const familyRoutes = [
  {
    path: 'family',
    name: 'family',
    component: () => import('../views/FamilyView.vue'),
    meta: { label: '我的家庭', requiresAuth: true, requiresFamily: true },
  },
]

const profileRoutes = [
  {
    path: 'profile',
    name: 'profile',
    component: () => import('../views/MeView.vue'),
    meta: { label: '我的', requiresAuth: true },
  },
  {
    path: 'profile/account',
    name: 'profile-account',
    component: () => import('../views/ProfileView.vue'),
    meta: { label: '个人资料', requiresAuth: true },
  },
]

const moduleRoutes = mainNavigation
  .filter((item) => !['dishes', 'plaza', 'cart', 'kitchen', 'shopping', 'profile'].includes(item.name))
  .map((item) => ({
    path: item.path.slice(1),
    name: item.name,
    component: () => import('../views/ModuleView.vue'),
    meta: { ...item, requiresAuth: true, requiresFamily: true },
  }))

const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', redirect: { name: 'dishes' } },
      ...dishRoutes,
      ...plazaRoutes,
      ...cartRoutes,
      ...orderRoutes,
      ...shoppingRoutes,
      ...familyRoutes,
      ...profileRoutes,
      ...moduleRoutes,
    ],
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
  },
  {
    path: '/family-select',
    name: 'family-select',
    component: () => import('../views/FamilySelectView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/assistant',
    name: 'assistant',
    component: () => import('../views/AssistantView.vue'),
    meta: { requiresAuth: true },
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
  scrollBehavior(to, from, savedPosition) {
    // 浏览器前进/后退（有历史位置）时恢复；其余（应用内 push，如进入详情页）滚到顶部。
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
})

router.beforeEach(createAuthGuard())

export default router
