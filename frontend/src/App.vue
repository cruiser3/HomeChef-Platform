<script setup>
import { useAuthStore } from './store/auth'
import FloatingAssistant from './components/FloatingAssistant.vue'

const auth = useAuthStore()
</script>

<template>
  <RouterView />
  <FloatingAssistant v-if="auth.isAuthenticated" />
</template>
