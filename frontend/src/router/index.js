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
    meta: { label: '厨房', requiresAuth: true, requiresFamily: true },
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
    meta: { label: '家庭', requiresAuth: true, requiresFamily: true },
  },
]

const profileRoutes = [
  {
    path: 'profile',
    name: 'profile',
    component: () => import('../views/ProfileView.vue'),
    meta: { label: '我的', requiresAuth: true },
  },
]

const moduleRoutes = mainNavigation
  .filter((item) => !['dishes', 'cart', 'kitchen', 'shopping', 'family'].includes(item.name))
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
