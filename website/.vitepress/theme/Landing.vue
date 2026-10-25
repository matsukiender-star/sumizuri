<script setup lang="ts">
import { computed } from 'vue'
import { withBase } from 'vitepress'
import { releaseDate, useRelease, type Device } from './release'

const { release, seen, recommended } = useRelease()

// The button names the device the visitor is on when it is one with an app, and goes to the full list when it is not.
const names: Record<string, string> = { android: 'Android', windows: 'Windows', linux: 'Linux' }
const device = computed<Device | null>(() => (seen.value === 'android' || seen.value === 'windows' || seen.value === 'linux' ? seen.value : null))
const direct = computed(() => (device.value ? recommended(device.value) : undefined))
const label = computed(() => (device.value ? `Download for ${names[device.value]}` : 'Download'))
const href = computed(() => direct.value?.asset.browser_download_url ?? withBase('/download'))

const kinds = [
  { title: 'Manga and comics', text: 'Read page by page, in the direction you like, or in one long scroll.' },
  { title: 'Novels', text: 'Set the text the way you read best.' },
  { title: 'Anime', text: 'Watch with subtitles, and skip intros and credits.' },
  { title: 'Your own files', text: 'Open files from your device.' },
]
</script>

<template>
  <div class="landing">
    <section class="hero">
      <div class="copy">
        <p class="eyebrow">Android · Windows · Linux</p>
        <h1>Everything you read and watch, in one quiet app.</h1>
        <p class="lead">Sumizuri is a media viewer for manga, comics, novels and anime. It comes with no content. You bring your own.</p>
        <div class="actions">
          <a class="button primary" :href="href">{{ label }}</a>
          <a class="button" :href="withBase('/download')">All downloads</a>
        </div>
        <p class="meta" v-if="release">Version {{ release.tag_name }} · {{ releaseDate(release.published_at) }}</p>
        <p class="meta" v-else>&nbsp;</p>
      </div>
      <div class="art" aria-hidden="true">
        <div class="moon" />
        <img :src="withBase('/logo.png')" alt="" width="260" height="260" />
      </div>
    </section>

    <section class="block">
      <h2>What you can read and watch</h2>
      <div class="grid four">
        <article v-for="kind in kinds" :key="kind.title" class="card">
          <h3>{{ kind.title }}</h3>
          <p>{{ kind.text }}</p>
        </article>
      </div>
    </section>

    <section class="block band">
      <h2>Bring your own content</h2>
      <p class="sub">Sumizuri comes with no content. You choose what to add.</p>
      <div class="actions center">
        <a class="button primary" :href="href">{{ label }}</a>
        <a class="button" :href="withBase('/guides')">Read the guides</a>
      </div>
    </section>
  </div>
</template>

<style scoped>
.landing {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 24px 72px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 0.85fr);
  align-items: center;
  gap: 40px;
  padding: 72px 0 56px;
}
.eyebrow {
  margin: 0 0 14px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--vp-c-brand-1);
}
h1 {
  margin: 0;
  font-size: clamp(34px, 5.2vw, 56px);
  line-height: 1.08;
  letter-spacing: -0.02em;
  font-weight: 700;
  border: 0;
  padding: 0;
}
.lead {
  margin: 20px 0 0;
  max-width: 34em;
  font-size: 19px;
  line-height: 1.6;
  color: var(--vp-c-text-2);
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}
.actions.center {
  justify-content: center;
}
.button {
  display: inline-block;
  padding: 12px 24px;
  border-radius: 12px;
  font-weight: 600;
  text-decoration: none;
  background: var(--vp-button-alt-bg);
  color: var(--vp-button-alt-text);
  transition: background 0.15s;
}
.button:hover {
  background: var(--vp-button-alt-hover-bg);
}
.button.primary {
  background: var(--vp-button-brand-bg);
  color: var(--vp-button-brand-text);
}
.button.primary:hover {
  background: var(--vp-button-brand-hover-bg);
}
.meta {
  margin: 16px 0 0;
  font-size: 14px;
  color: var(--vp-c-text-3);
}

.art {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 300px;
}
.moon {
  position: absolute;
  inset: 8% 8%;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 45%, var(--vp-c-brand-soft), transparent 70%);
  filter: blur(28px);
}
.art img {
  position: relative;
  width: min(260px, 70%);
  height: auto;
  border-radius: 28%;
}

.block {
  padding: 40px 0 8px;
}
.block h2 {
  margin: 0 0 10px;
  font-size: 30px;
  letter-spacing: -0.01em;
  border: 0;
  padding: 0;
}
.sub {
  margin: 0 0 24px;
  max-width: 42em;
  font-size: 17px;
  line-height: 1.6;
  color: var(--vp-c-text-2);
}
.grid {
  display: grid;
  gap: 16px;
  margin-top: 20px;
}
.grid.four {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.grid.three {
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
}
.card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 24px;
  border-radius: 20px;
  border: 1px solid var(--vp-c-divider);
  background: var(--vp-c-bg-soft);
}
.card h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}
.card p {
  margin: 0;
  line-height: 1.65;
  color: var(--vp-c-text-2);
}
.more {
  margin-top: auto;
  padding-top: 6px;
  font-weight: 600;
  text-decoration: none;
  color: var(--vp-c-brand-1);
}

.band {
  margin-top: 48px;
  padding: 48px 24px;
  text-align: center;
  border-radius: 28px;
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-divider);
}
.band .sub {
  margin-left: auto;
  margin-right: auto;
}

@media (max-width: 1000px) {
  .grid.four {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .grid.four {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 800px) {
  .hero {
    grid-template-columns: 1fr;
    padding: 40px 0 32px;
  }
  .art {
    order: -1;
    min-height: 200px;
  }
  .art img {
    width: 150px;
  }
  .block h2 {
    font-size: 26px;
  }
}
</style>
