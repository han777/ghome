import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { createI18n } from 'vue-i18n'
import zh from './locales/zh.json'
import en from './locales/en.json'

const i18n = createI18n({
    legacy: false,
    locale: 'zh',
    messages: {
        zh,
        en
    }
})

createApp(App).use(i18n).use(router).mount('#app')
