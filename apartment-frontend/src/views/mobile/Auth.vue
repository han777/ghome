<template>
  <div class="auth-page">
    <header class="mobile-header no-border">
      <div class="header-left" @click="router.back()">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z" /></svg>
      </div>
      <div class="mobile-header-title">身份认证</div>
      <div class="header-right">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M12,16A2,2 0 0,1 14,18A2,2 0 0,1 12,20A2,2 0 0,1 10,18A2,2 0 0,1 12,16M12,10A2,2 0 0,1 14,12A2,2 0 0,1 12,14A2,2 0 0,1 10,12A2,2 0 0,1 12,10M12,4A2,2 0 0,1 14,6A2,2 0 0,1 12,8A2,2 0 0,1 10,6A2,2 0 0,1 12,4Z" /></svg>
      </div>
    </header>

    <div class="auth-content">
      <div class="welcome-section">
        <h1 class="welcome-title">您好，<br/>欢迎登录健适公寓预定系统</h1>
        <div class="lang-switch">简体中文 🌐</div>
      </div>

      <div class="auth-form">
        <div class="input-group">
          <label>手机号<span class="required">*</span></label>
          <input type="tel" v-model="phone" placeholder="请输入手机号" />
        </div>
        <div class="input-group">
          <label>验证码<span class="required">*</span></label>
          <div class="code-input-wrap">
            <input type="text" v-model="code" placeholder="请输入验证码" />
            <span class="code-btn" @click="countdown === 0 && sendCode()">
              {{ countdown > 0 ? `已发送 (${countdown}s)` : '获取验证码' }}
            </span>
          </div>
        </div>

        <button class="mobile-btn-primary login-btn" :disabled="loading" @click="handleLogin">
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <div class="agreement-row">
          <input type="checkbox" id="agree" checked />
          <label for="agree">
            我已阅读并同意 <a href="#">薪福通服务协议</a> 和 <a href="#">个人信息保护政策</a>
          </label>
        </div>

        <div class="footer-links">
          <span>没有账号？<a href="#" class="primary-link">立即注册</a></span>
          <a href="#" class="secondary-link">从掌上薪福通登录</a>
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
import api from '../../utils/api';

const router = useRouter();
const route = useRoute();

const phone = ref('');
const code = ref('');
const countdown = ref(0);
const loading = ref(false);

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
    alert('请输入正确的手机号');
    return;
  }
  try {
    await api.post('/auth/send-code', { phone: phone.value });
    startCountdown();
  } catch (error: any) {
    alert(error.response?.data?.message || '发送失败');
  }
};

const handleLogin = async () => {
  if (!phone.value || !code.value) {
    alert('请输入手机号和验证码');
    return;
  }
  loading.value = true;
  try {
    const res: any = await api.post('/auth/mobile-login', { 
      phone: phone.value, 
      code: code.value 
    });
    localStorage.setItem('token', res.token);
    const redirect = route.query.redirect as string;
    router.push(redirect || '/m/booking');
  } catch (error: any) {
    alert(error.response?.data?.message || '登录失败');
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
</style>
