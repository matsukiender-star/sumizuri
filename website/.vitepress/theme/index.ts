import DefaultTheme from 'vitepress/theme'
import type { Theme } from 'vitepress'
import '@fontsource/inter/400.css'
import '@fontsource/inter/500.css'
import '@fontsource/inter/600.css'
import '@fontsource/inter/700.css'
import './custom.css'
import DownloadList from './DownloadList.vue'
import GuideCards from './GuideCards.vue'
import Landing from './Landing.vue'

// The default theme of VitePress with the look of the app laid over it, in custom.css, and the pieces the pages are built from.
export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('DownloadList', DownloadList)
    app.component('GuideCards', GuideCards)
    app.component('Landing', Landing)
  },
} satisfies Theme
