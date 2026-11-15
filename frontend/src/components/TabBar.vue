<script setup>
import { mainNavigation } from '../navigation.js'

defineProps({
  mode: {
    type: String,
    default: 'mobile',
    validator: (value) => ['mobile', 'desktop'].includes(value),
  },
})
</script>

<template>
  <nav class="tab-bar" :class="`tab-bar--${mode}`" aria-label="主导航">
    <RouterLink
      v-for="item in mainNavigation"
      :key="item.name"
      class="tab-link"
      :to="item.path"
      :aria-label="item.label"
    >
      <svg
        class="tab-icon"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <template v-if="item.icon === 'home'">
          <path d="m3.5 10 8.5-7 8.5 7" />
          <path d="M5.5 9v11h13V9M9.5 20v-6h5v6" />
        </template>
        <template v-else-if="item.icon === 'kitchen'">
          <path d="M4 10h16v10H4zM6 10V6h12v4M8 6V4m8 2V4" />
          <path d="M8 14h8" />
        </template>
        <template v-else-if="item.icon === 'cart'">
          <path d="M3 4h2l2.2 11.2a2 2 0 0 0 2 1.6h8.6a2 2 0 0 0 1.9-1.4L22 8H6" />
          <circle cx="10" cy="20" r="1" />
          <circle cx="18" cy="20" r="1" />
        </template>
        <template v-else-if="item.icon === 'list'">
          <path d="M9 5h11M9 12h11M9 19h11" />
          <path d="m3 5 1 1 2-2m-3 8 1 1 2-2m-3 8 1 1 2-2" />
        </template>
        <template v-else>
          <circle cx="9" cy="8" r="3" />
          <path d="M3.5 20v-1.5a5.5 5.5 0 0 1 11 0V20zM16 5.5a3 3 0 0 1 0 5.8m2 3a4.5 4.5 0 0 1 2.5 4.1V20h-3" />
        </template>
      </svg>
      <span class="tab-label">{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>
