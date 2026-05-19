<template>
  <div class="auth-page">
    <header class="mobile-header no-border">
      <div class="header-left" @click="handleBack">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z" /></svg>
      </div>
      <div class="mobile-header-title">{{ $t('auth.identity') }}</div>
      <div class="header-right"></div>
    </header>

    <div class="auth-content">
      <div class="welcome-section">
        <h1 class="welcome-title" v-html="$t('auth.welcome')"></h1>
        <div class="lang-switch-bar">
          <span class="lang-pill" :class="{ 'lang-active': locale === 'zh' }" @click="switchLang('zh')">中</span>
          <span class="lang-pill" :class="{ 'lang-active': locale === 'en' }" @click="switchLang('en')">EN</span>
          <span class="lang-pill" :class="{ 'lang-active': locale === 'ja' }" @click="switchLang('ja')">日</span>
        </div>
      </div>

      <div class="auth-form">
        <!-- WeChat Work auto-login status -->
        <div v-if="statusMessage" class="wecom-status">
          <div class="loading-spinner-small"></div>
          <span>{{ statusMessage }}</span>
        </div>

        <!-- WeChat Work login button (not in WeChat env) -->
        <button v-if="!isWeComEnv && !statusMessage" class="mobile-btn-primary wecom-login-btn" @click="wecomAutoLogin">
          <span class="wecom-icon">💬</span> {{ $t('auth.wecomLogin') }}
        </button>

        <div class="other-login">
          <a href="javascript:void(0)" @click="router.push('/login')">{{ $t('auth.pwdLogin') }}</a>
        </div>
      </div>

      <!-- Privacy Policy Modal -->
      <div v-if="showPrivacyModal" class="privacy-modal-overlay">
        <div class="privacy-modal-content">
          <div class="privacy-modal-header">{{ $t('auth.privacyTitle') }}</div>
          <div class="privacy-modal-body">
            <p class="highlight">{{ $t('auth.privacyP1') }}</p>
            <p>{{ $t('auth.privacyP2') }}</p>
            <p>{{ $t('auth.privacyP3') }}</p>
            <div class="policy-item"><strong>{{ $t('auth.privacyH1') }}</strong><span>{{ $t('auth.privacyC1') }}</span></div>
            <div class="policy-item"><strong>{{ $t('auth.privacyH2') }}</strong><span>{{ $t('auth.privacyC2') }}</span></div>
            <div class="policy-item"><strong>{{ $t('auth.privacyH3') }}</strong><span>{{ $t('auth.privacyC3') }}</span></div>
            <p class="footer-note">{{ $t('auth.privacyP4') }}</p>
          </div>
          <div class="privacy-modal-footer">
            <button class="modal-btn logout" @click="showPrivacyModal = false">{{ $t('auth.exit') }}</button>
            <button class="modal-btn accept" @click="showPrivacyModal = false">{{ $t('auth.agreeAndEnter') }}</button>
          </div>
        </div>
      </div>

      <div class="brand-footer">
        <div class="brand-text">
          <span class="logo-icon">💠</span>
          <strong>Genesis</strong> G-HOME
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import api from '../../utils/api';

const router = useRouter();
const route = useRoute();
const { t, locale } = useI18n();

// WeChat Work environment detection
const isWeComEnv = computed(() => {
  const ua = navigator.userAgent.toLowerCase();
  return ua.includes('wxwork');
});

// Auto WeChat Work OAuth (when in WeChat env)
onMounted(async () => {
  const errorParam = route.query.error as string;
  if (errorParam === 'wecom_login_failed') {
    alert(route.query.message || t('auth.loginFailed'));
  }
  if (isWeComEnv.value) {
    wecomAutoLogin();
  }
});

const wecomAutoLogin = async () => {
  loading.value = true;
  statusMessage.value = t('auth.redirectingWecom');
  try {
    const redirect = route.query.redirect as string || '/m';
    const res: any = await api.get('/auth/wecom/authorize', { params: { redirect } });
    window.location.href = res.url;
  } catch (err) {
    console.error('WeChat Work OAuth failed', err);
    statusMessage.value = '';
    loading.value = false;
  }
};

const switchLang = async (lang: string) => {
  locale.value = lang;
  localStorage.setItem('locale', lang);
  try {
    await api.post('/sys/profile/locale', { locale: lang });
  } catch (e) {
    // Silently fail if user is not logged in yet
  }
};

const handleBack = () => {
  router.back();
};

const loading = ref(false);
const showPrivacyModal = ref(false);
const statusMessage = ref('');

</script>

<style scoped>
.auth-page {
  background: #fff;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.no-border { border: none; }

.auth-content {
  padding: 24px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.welcome-section {
  margin-top: 40px;
  position: relative;
  text-align: center;
}

.welcome-title {
  font-size: 24px;
  font-weight: bold;
  line-height: 1.4;
  color: #333;
}

.lang-switch-bar {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}

.lang-pill {
  font-size: 13px;
  font-weight: 600;
  padding: 4px 14px;
  border-radius: 20px;
  background: #f0f0f0;
  color: #888;
  transition: all 0.25s ease;
  cursor: pointer;
}

.lang-pill.lang-active {
  background: #1677ff;
  color: #fff;
}

.auth-form {
  margin-top: 60px;
}

.brand-footer {
  margin-top: auto;
  padding: 40px 0;
  text-align: center;
}

.brand-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 16px;
  color: #333;
}

.logo-icon {
  font-size: 24px;
  color: #2b579a;
}

/* Privacy Modal Styles */
.privacy-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 32px;
}

.privacy-modal-content {
  background: #fff;
  border-radius: 20px;
  width: 100%;
  max-width: 320px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  animation: modalScale 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes modalScale {
  from { opacity: 0; transform: scale(0.9); }
  to { opacity: 1; transform: scale(1); }
}

.privacy-modal-header {
  padding: 24px 20px 16px;
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}

.privacy-modal-body {
  padding: 0 24px 24px;
  font-size: 14px;
  color: #4a4a4a;
  line-height: 1.6;
  max-height: 400px;
  overflow-y: auto;
}

.highlight {
  color: var(--mobile-primary);
  font-weight: 600;
  margin-bottom: 12px;
}

.policy-item {
  margin: 16px 0;
}

.policy-item strong {
  display: block;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.footer-note {
  margin-top: 20px;
  font-size: 13px;
  color: #888;
}

.privacy-modal-footer {
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  border-top: 1px solid #f0f0f0;
}

.modal-btn {
  padding: 16px;
  border: none;
  background: none;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.modal-btn.logout {
  color: #999;
  border-right: 1px solid #f0f0f0;
}

.modal-btn.accept {
  color: var(--mobile-primary);
}

.modal-btn:active {
  background: #f8f8f8;
}

.other-login {
  margin-top: 32px;
  text-align: center;
}

.other-login a {
  color: #666;
  font-size: 14px;
  text-decoration: none;
  display: inline-block;
  padding: 8px 16px;
  border: 1px solid #eee;
  border-radius: 20px;
  transition: all 0.3s;
}

.other-login a:hover {
  color: var(--mobile-primary);
  border-color: var(--mobile-primary);
  background: rgba(var(--mobile-primary-rgb), 0.05);
}

.wecom-login-btn {
  background: #07C160 !important;
  color: #fff;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 10px;
  height: 48px;
  border-radius: 6px;
  width: 100%;
  font-size: 16px;
  cursor: pointer;
}

.wecom-icon {
  font-size: 20px;
}

.wecom-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 20px;
  color: #07C160;
  font-size: 15px;
}

.loading-spinner-small {
  width: 24px;
  height: 24px;
  border: 2px solid #e8e8e8;
  border-top-color: #07C160;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
