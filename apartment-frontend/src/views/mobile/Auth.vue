<template>
  <div class="auth-page">
    <header class="mobile-header no-border">
      <div class="header-left" @click="handleBack">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z" /></svg>
      </div>
      <div class="mobile-header-title">{{ isCollectPhoneMode ? '补充手机号' : $t('auth.identity') }}</div>
      <div class="header-right"></div>
    </header>

    <div class="auth-content">
      <!-- collect-phone mode: user already logged in via wecom, just needs phone -->
      <div v-if="isCollectPhoneMode" class="welcome-section">
        <h1 class="welcome-title">完善个人信息</h1>
        <p class="collect-phone-hint">请补充您的手机号，以便接收订单通知</p>
      </div>

      <!-- normal login mode -->
      <div v-else class="welcome-section">
        <h1 class="welcome-title" v-html="$t('auth.welcome')"></h1>
        <div class="lang-switch-bar">
          <span class="lang-pill" :class="{ 'lang-active': locale === 'zh' }" @click="switchLang('zh')">中</span>
          <span class="lang-pill" :class="{ 'lang-active': locale === 'en' }" @click="switchLang('en')">EN</span>
          <span class="lang-pill" :class="{ 'lang-active': locale === 'ja' }" @click="switchLang('ja')">日</span>
        </div>
      </div>

      <div class="auth-form">
        <!-- WeChat Work auto-login status (normal mode only) -->
        <div v-if="statusMessage" class="wecom-status">
          <div class="loading-spinner-small"></div>
          <span>{{ statusMessage }}</span>
        </div>

        <!-- WeChat Work login button (normal mode, not in WeChat env) -->
        <button v-if="!isCollectPhoneMode && !isWeComEnv && !statusMessage" class="mobile-btn-primary wecom-login-btn" @click="wecomAutoLogin">
          <span class="wecom-icon">💬</span> 企业微信一键登录
        </button>

        <div v-if="!statusMessage && !isCollectPhoneMode" class="divider-line"><span>手机号登录</span></div>

        <!-- Phone input (both modes) -->
        <div class="input-group">
          <label>{{ $t('auth.phone') }}<span class="required">*</span></label>
          <input type="tel" v-model="phone" :placeholder="isCollectPhoneMode ? '请输入手机号' : $t('auth.phonePlaceholder')" />
        </div>

        <!-- Code input (both modes) -->
        <div class="input-group">
          <label>{{ isCollectPhoneMode ? '验证码（输入任意4-6位数字即可）' : $t('auth.code') }}<span class="required">*</span></label>
          <div class="code-input-wrap">
            <input type="text" v-model="code" :placeholder="isCollectPhoneMode ? '输入任意数字' : $t('auth.codePlaceholder')" />
            <span v-if="!isCollectPhoneMode" class="code-btn" @click="countdown === 0 && sendCode()">
              {{ countdown > 0 ? $t('auth.sent', { time: countdown }) : $t('auth.getCode') }}
            </span>
          </div>
        </div>

        <!-- Submit button -->
        <button class="mobile-btn-primary login-btn" :disabled="loading" @click="handleSubmit">
          {{ loading ? '提交中...' : (isCollectPhoneMode ? '确认补充手机号' : $t('auth.login')) }}
        </button>

        <!-- Normal mode extras -->
        <template v-if="!isCollectPhoneMode && !statusMessage">
          <div class="agreement-row">
            <input type="checkbox" id="agree" checked />
            <label for="agree">
              {{ $t('auth.agreePrefix') }}<a href="javascript:void(0)" @click="showPrivacyModal = true">{{ $t('auth.policy') }}</a>
            </label>
          </div>
          <div class="other-login">
            <a href="javascript:void(0)" @click="router.push('/login')">{{ $t('auth.pwdLogin') }}</a>
          </div>
        </template>

        <!-- Collect-phone mode skip option -->
        <div v-if="isCollectPhoneMode" class="skip-row">
          <a href="javascript:void(0)" @click="router.push('/m')">暂不填写，直接进入</a>
        </div>
      </div>

      <!-- Privacy Policy Modal (normal mode) -->
      <div v-if="!isCollectPhoneMode && showPrivacyModal" class="privacy-modal-overlay">
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
          <strong>Genesis</strong> 健适医疗
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

// Mode detection: collect-phone means user already has token (wecom login), just needs phone
const isCollectPhoneMode = computed(() => route.query.mode === 'collect-phone');

// WeChat Work environment detection
const isWeComEnv = computed(() => {
  const ua = navigator.userAgent.toLowerCase();
  return ua.includes('wxwork');
});

// Auto WeChat Work OAuth (normal mode only, when in WeChat env)
onMounted(async () => {
  const errorParam = route.query.error as string;
  if (errorParam === 'wecom_login_failed') {
    alert(route.query.message || t('auth.loginFailed'));
  }
  if (!isCollectPhoneMode.value && isWeComEnv.value) {
    wecomAutoLogin();
  }
});

const wecomAutoLogin = async () => {
  loading.value = true;
  statusMessage.value = '正在跳转企业微信登录...';
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

const switchLang = (lang: string) => {
  locale.value = lang;
  localStorage.setItem('locale', lang);
};

const handleBack = () => {
  if (isCollectPhoneMode.value) {
    router.push('/m');
  } else {
    router.back();
  }
};

const phone = ref('');
const code = ref('');
const countdown = ref(0);
const loading = ref(false);
const showPrivacyModal = ref(false);
const statusMessage = ref('');

const startCountdown = () => {
  countdown.value = 60;
  const timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) clearInterval(timer);
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

// Unified submit handler
const handleSubmit = async () => {
  if (!phone.value || !/^1[3-9]\d{9}$/.test(phone.value)) {
    alert('请输入正确的手机号');
    return;
  }
  if (!code.value) {
    alert('请输入验证码');
    return;
  }

  loading.value = true;
  try {
    if (isCollectPhoneMode.value) {
      // collect-phone mode: user already has token, call update-phone endpoint
      await api.post('/auth/update-phone', { phone: phone.value, code: code.value });
      alert('手机号已补充完成');
      router.push('/m');
    } else {
      // normal login mode: call mobile-login endpoint
      const res: any = await api.post('/auth/mobile-login', { phone: phone.value, code: code.value });
      localStorage.setItem('token', res.token);

      const user: any = await api.get('/sys/profile');
      const roles = (user.roles || []).map((r: any) => r.roleCode);
      const isAdmin = roles.includes('ROLE_ADMIN');
      const isUser = roles.includes('ROLE_USER');

      const redirect = route.query.redirect as string;
      if (redirect && redirect !== '/m/booking') {
        router.push(redirect);
        return;
      }
      if (isAdmin && isUser) router.push('/role-selection');
      else if (isAdmin) router.push('/admin');
      else if (isUser) router.push('/m');
      else router.push('/m');
    }
  } catch (error: any) {
    const msg = error.response?.data?.message || error.response?.data || '操作失败';
    alert(typeof msg === 'string' ? msg : '操作失败');
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

.collect-phone-hint {
  font-size: 14px;
  color: #666;
  margin-top: 8px;
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

.skip-row {
  margin-top: 20px;
  text-align: center;
}

.skip-row a {
  color: #999;
  font-size: 14px;
  text-decoration: none;
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

.divider-line {
  text-align: center;
  margin: 24px 0 0;
  position: relative;
  font-size: 13px;
  color: #999;
}

.divider-line::before,
.divider-line::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 35%;
  height: 1px;
  background: #e8e8e8;
}

.divider-line::before { left: 0; }
.divider-line::after { right: 0; }
.divider-line span { background: #fff; padding: 0 8px; }

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