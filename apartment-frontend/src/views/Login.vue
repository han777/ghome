<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1>{{ $t('login.title') }}</h1>
        <p>{{ $t('login.subtitle') }}</p>
        <div class="lang-switch-bar">
          <span class="lang-pill" :class="{ 'lang-active': locale === 'zh' }" @click="switchLang('zh')">中</span>
          <span class="lang-pill" :class="{ 'lang-active': locale === 'en' }" @click="switchLang('en')">EN</span>
          <span class="lang-pill" :class="{ 'lang-active': locale === 'ja' }" @click="switchLang('ja')">日</span>
        </div>
      </div>
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label>{{ $t('login.username') }}</label>
          <input v-model="username" type="text" :placeholder="$t('login.usernamePlaceholder')" required>
        </div>
        <div class="form-group">
          <label>{{ $t('login.password') }}</label>
          <input v-model="password" type="password" :placeholder="$t('login.passwordPlaceholder')" required>
        </div>
        <div v-if="error" class="error-msg">{{ error }}</div>
        <button type="submit" :disabled="loading" class="login-btn">
          <span v-if="loading">{{ $t('login.submitting') }}</span>
          <span v-else>{{ $t('login.submit') }}</span>
        </button>
        <div class="other-login">
          <a href="javascript:void(0)" @click="router.push('/m/auth')">{{ $t('login.phoneLogin') }}</a>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import api from '../utils/api';

const router = useRouter();
const route = useRoute();
const { t, locale } = useI18n();

const username = ref('admin');
const password = ref('admin');
const loading = ref(false);
const error = ref('');

const switchLang = (lang: string) => {
  locale.value = lang;
  localStorage.setItem('locale', lang);
};

const handleLogin = async () => {
  loading.value = true;
  error.value = '';

  try {
    const res: any = await api.post('/auth/login', {
      username: username.value,
      password: password.value
    });

    if (res.token) {
      localStorage.setItem('token', res.token);

      try {
        const user: any = await api.get('/sys/profile');
        const roles = (user.roles || []).map((r: any) => r.roleCode);
        localStorage.setItem('roles', JSON.stringify(roles));

        const isAdmin = roles.includes('ROLE_ADMIN');
        const isUser = roles.includes('ROLE_USER');

        const redirect = route.query.redirect as string;
        if (redirect && redirect !== '/admin') {
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
          router.push('/admin');
        }
      } catch (profileErr) {
        console.error('Failed to fetch profile', profileErr);
        router.push('/admin');
      }
    }
  } catch (e: any) {
    error.value = e.response?.data?.message || t('login.errorInvalid');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  padding: 40px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-header h1 {
  color: #38bdf8;
  font-size: 32px;
  font-weight: 800;
  margin-bottom: 8px;
}

.login-header p {
  color: #94a3b8;
  font-size: 14px;
  margin-bottom: 16px;
}

.lang-switch-bar {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.lang-pill {
  font-size: 13px;
  font-weight: 600;
  padding: 4px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.1);
  color: #94a3b8;
  transition: all 0.25s ease;
  cursor: pointer;
}

.lang-pill.lang-active {
  background: #38bdf8;
  color: #fff;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  color: #cbd5e1;
  font-size: 14px;
  font-weight: 500;
}

.form-group input {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 12px 16px;
  color: #fff;
  transition: all 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: #38bdf8;
  box-shadow: 0 0 0 2px rgba(56, 189, 248, 0.2);
}

.error-msg {
  color: #ef4444;
  font-size: 13px;
  background: rgba(239, 68, 68, 0.1);
  padding: 8px 12px;
  border-radius: 6px;
}

.login-btn {
  background: #38bdf8;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 12px;
}

.login-btn:hover {
  background: #0ea5e9;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(56, 189, 248, 0.4);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.other-login {
  margin-top: 24px;
  text-align: center;
}

.other-login a {
  color: #94a3b8;
  font-size: 14px;
  text-decoration: none;
  transition: all 0.3s;
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.other-login a:hover {
  color: #38bdf8;
  background: rgba(56, 189, 248, 0.1);
  border-color: rgba(56, 189, 248, 0.2);
}
</style>