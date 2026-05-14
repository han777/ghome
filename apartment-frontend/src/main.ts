import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { createI18n } from 'vue-i18n'
import zh from './locales/zh.json'
import en from './locales/en.json'
import ja from './locales/ja.json'

export const i18n = createI18n({
    legacy: false,
    locale: detectLocale(),
    messages: {
        zh,
        en,
        ja
    }
})

function detectLocale(): string {
    const saved = localStorage.getItem('locale')
    if (saved && ['zh', 'en', 'ja'].includes(saved)) return saved
    const browserLangs = navigator.languages || [navigator.language]
    for (const lang of browserLangs) {
        const lower = lang.toLowerCase()
        if (lower.startsWith('zh')) return 'zh'
        if (lower.startsWith('ja')) return 'ja'
        if (lower.startsWith('en')) return 'en'
    }
    return 'zh'
}

createApp(App).use(i18n).use(router).mount('#app')
