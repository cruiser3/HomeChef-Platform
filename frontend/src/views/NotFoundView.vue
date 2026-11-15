<template>
  <main class="standalone-page">
    <section class="standalone-card">
      <span class="status-tag status-tag--neutral">404</span>
      <h1>找不到这个页面</h1>
      <p class="standalone-copy">请检查地址，或返回家庭菜品入口。</p>
      <RouterLink class="primary-link" :to="{ name: 'dishes' }">返回小家</RouterLink>
    </section>
  </main>
</template>
