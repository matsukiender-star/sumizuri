<script setup lang="ts">
import { withBase } from 'vitepress'

const guides = [
  { title: 'Writing extensions', text: 'Connect Sumizuri to an API of your own.', href: '/extensions', tag: 'For developers' },
  { title: 'Profile block scripts', text: 'Make your own blocks for the profile page.', href: '/profile-blocks', tag: 'For tinkerers' },
  { title: 'Run a sync server', text: 'Keep your library the same on every device.', href: '/sync-server', tag: 'For self-hosters' },
]
</script>

<template>
  <div class="cards">
    <a v-for="guide in guides" :key="guide.href" class="card" :href="withBase(guide.href)">
      <span class="tag">{{ guide.tag }}</span>
      <span class="title">{{ guide.title }}</span>
      <span class="text">{{ guide.text }}</span>
      <span class="go">Read the guide →</span>
    </a>
  </div>
</template>

<style scoped>
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
  margin-top: 24px;
}
.card {
  display: grid;
  gap: 8px;
  align-content: start;
  padding: 22px;
  border-radius: 20px;
  border: 1px solid var(--vp-c-divider);
  background: var(--vp-c-bg-soft);
  text-decoration: none;
  color: var(--vp-c-text-1);
  transition: border-color 0.15s, transform 0.15s;
}
.card:hover {
  border-color: var(--vp-c-brand-1);
  transform: translateY(-2px);
}
.tag {
  font-size: 12px;
  font-weight: 600;
  color: var(--vp-c-brand-1);
}
.title {
  font-size: 20px;
  font-weight: 700;
}
.text {
  color: var(--vp-c-text-2);
  line-height: 1.6;
}
.go {
  margin-top: 6px;
  font-weight: 600;
  color: var(--vp-c-brand-1);
}
</style>
