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
    locale: localStorage.getItem('locale') || 'zh',
    messages: {
        zh,
        en,
        ja
    }
})

createApp(App).use(i18n).use(router).mount('#app')
