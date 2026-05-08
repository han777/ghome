<template>
  <div class="auth-page">
    <header class="mobile-header no-border">
      <div class="header-left" @click="router.back()">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z" /></svg>
      </div>
      <div class="mobile-header-title">{{ $t('auth.identity') }}</div>
      <div class="header-right">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M12,16A2,2 0 0,1 14,18A2,2 0 0,1 12,20A2,2 0 0,1 10,18A2,2 0 0,1 12,16M12,10A2,2 0 0,1 14,12A2,2 0 0,1 12,14A2,2 0 0,1 10,12A2,2 0 0,1 12,10M12,4A2,2 0 0,1 14,6A2,2 0 0,1 12,8A2,2 0 0,1 10,6A2,2 0 0,1 12,4Z" /></svg>
      </div>
    </header>

    <div class="auth-content">
      <div class="welcome-section">
        <h1 class="welcome-title" v-html="$t('auth.welcome')"></h1>
        <div class="lang-switch" @click="toggleLang">{{ nextLangLabel }} 🌐</div>
      </div>

      <div class="auth-form">
        <div class="input-group">
          <label>{{ $t('auth.phone') }}<span class="required">*</span></label>
          <input type="tel" v-model="phone" :placeholder="$t('auth.phonePlaceholder')" />
        </div>
        <div class="input-group">
          <label>{{ $t('auth.code') }}<span class="required">*</span></label>
          <div class="code-input-wrap">
            <input type="text" v-model="code" :placeholder="$t('auth.codePlaceholder')" />
            <span class="code-btn" @click="countdown === 0 && sendCode()">
              {{ countdown > 0 ? $t('auth.sent', { time: countdown }) : $t('auth.getCode') }}
            </span>
          </div>
        </div>

        <button class="mobile-btn-primary login-btn" :disabled="loading" @click="handleLogin">
          {{ loading ? $t('auth.logining') : $t('auth.login') }}
        </button>

        <div class="agreement-row">
          <input type="checkbox" id="agree" checked />
          <label for="agree">
            {{ $t('auth.agreePrefix') }}<a href="javascript:void(0)" @click="showPrivacyModal = true">{{ $t('auth.policy') }}</a>
          </label>
        </div>

        <div class="other-login">
          <a href="javascript:void(0)" @click="router.push('/login')">{{ $t('auth.pwdLogin') }}</a>
        </div>
      </div>

      <!-- Privacy Policy Modal -->
      <div v-if="showPrivacyModal" class="privacy-modal-overlay">
        <div class="privacy-modal-content">
          <div class="privacy-modal-header">
            {{ $t('auth.privacyTitle') }}
          </div>
          <div class="privacy-modal-body">
            <p class="highlight">{{ $t('auth.privacyP1') }}</p>
            <p>{{ $t('auth.privacyP2') }}</p>
            <p>{{ $t('auth.privacyP3') }}</p>
            
            <div class="policy-item">
              <strong>{{ $t('auth.privacyH1') }}</strong>
              <span>{{ $t('auth.privacyC1') }}</span>
            </div>
            
            <div class="policy-item">
              <strong>{{ $t('auth.privacyH2') }}</strong>
              <span>{{ $t('auth.privacyC2') }}</span>
            </div>
            
            <div class="policy-item">
              <strong>{{ $t('auth.privacyH3') }}</strong>
              <span>{{ $t('auth.privacyC3') }}</span>
            </div>
            
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
          <strong>Genesis</strong> 健适医疗
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import api from '../../utils/api';

const router = useRouter();
const route = useRoute();
const { t, locale } = useI18n();
import { computed } from 'vue';

const nextLangLabel = computed(() => {
  if (locale.value === 'zh') return 'English';
  if (locale.value === 'en') return '日本語';
  return '简体中文';
});

const toggleLang = () => {
  if (locale.value === 'zh') locale.value = 'en';
  else if (locale.value === 'en') locale.value = 'ja';
  else locale.value = 'zh';
  localStorage.setItem('locale', locale.value);
};

const phone = ref('');
const code = ref('');
const countdown = ref(0);
const loading = ref(false);
const showPrivacyModal = ref(false);

const startCountdown = () => {
  countdown.value = 60;
  const timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer);
    }
  }, 1000);
};

const sendCode = async () => {
  if (!phone.value || !/^1[3-9]\d{9}$/.test(phone.value)) {
    alert(t('auth.invalidPhone'));
    return;
  }
  try {
    await api.post('/auth/send-code', { phone: phone.value });
    startCountdown();
  } catch (error: any) {
    alert(error.response?.data?.message || t('auth.sendFailed'));
  }
};

const handleLogin = async () => {
  if (!phone.value || !code.value) {
    alert(t('auth.requirePhoneCode'));
    return;
  }
  loading.value = true;
  try {
    const res: any = await api.post('/auth/mobile-login', { 
      phone: phone.value, 
      code: code.value 
    });
    localStorage.setItem('token', res.token);
    
    // Fetch profile to check roles
    try {
      const user: any = await api.get('/sys/profile');
      const roles = (user.roles || []).map((r: any) => r.roleCode);
      
      const isAdmin = roles.includes('ROLE_ADMIN');
      const isUser = roles.includes('ROLE_USER');
      
      const redirect = route.query.redirect as string;
      if (redirect && redirect !== '/m/booking') {
         router.push(redirect);
         return;
      }

      if (isAdmin && isUser) {
        router.push('/role-selection');
      } else if (isAdmin) {
        router.push('/admin');
      } else if (isUser) {
        router.push('/m');
      } else {
        router.push('/m'); // Fallback for mobile
      }
    } catch (profileErr) {
      console.error('Failed to fetch profile', profileErr);
      router.push('/m/booking'); // Fallback
    }
  } catch (error: any) {
    alert(error.response?.data?.message || t('auth.loginFailed'));
  } finally {
    loading.value = false;
  }
};
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

.lang-switch {
  position: absolute;
  top: 0;
  right: 0;
  font-size: 13px;
  color: #666;
}

.auth-form {
  margin-top: 60px;
}

.input-group {
  margin-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 8px;
}

.input-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
}

.required { color: #ff4d4f; margin-left: 2px; }

.input-group input {
  width: 100%;
  border: none;
  font-size: 16px;
  outline: none;
  padding: 4px 0;
}

.code-input-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.code-btn {
  font-size: 14px;
  color: var(--mobile-primary);
  opacity: 0.7;
}

.login-btn {
  margin-top: 40px;
  height: 48px;
  border-radius: 6px;
  width: 100%;
  display: block;
}

.agreement-row {
  margin-top: 20px;
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #666;
  align-items: flex-start;
}

.agreement-row a { color: var(--mobile-primary); text-decoration: none; }

.footer-links {
  margin-top: 24px;
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.primary-link { color: var(--mobile-primary); text-decoration: none; }
.secondary-link { color: var(--mobile-primary); text-decoration: none; }

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
</style>
