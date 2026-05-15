<template>
  <div class="wecom-callback-page">
    <div class="callback-loading">
      <div class="loading-spinner"></div>
      <p>{{ statusMessage }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import api from '../../utils/api';

const router = useRouter();
const route = useRoute();
const statusMessage = ref('正在处理企业微信登录...');

onMounted(async () => {
  const token = route.query.token as string;
  const redirect = (route.query.redirect as string) || '/m';
  const error = route.query.error as string;
  const message = route.query.message as string;

  if (error) {
    statusMessage.value = message || '企业微信登录失败';
    setTimeout(() => {
      router.push('/m/auth');
    }, 2000);
    return;
  }

  if (!token) {
    statusMessage.value = '未收到登录凭证，正在跳转登录页...';
    setTimeout(() => {
      router.push('/m/auth');
    }, 2000);
    return;
  }

  // Store the JWT token
  localStorage.setItem('token', token);

  // Fetch profile to determine redirect
  try {
    const user: any = await api.get('/sys/profile');
    const roles = (user.roles || []).map((r: any) => r.roleCode);
    localStorage.setItem('roles', JSON.stringify(roles));

    const isAdmin = roles.includes('ROLE_ADMIN');
    const isUser = roles.includes('ROLE_USER');

    // If phone is missing, redirect to collect-phone mode first
    if (!user.phone || user.phone.trim() === '') {
      router.push('/m/auth?mode=collect-phone');
      return;
    }

    if (redirect && redirect !== '/m/booking' && redirect !== '/m') {
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
      router.push('/m');
    }
  } catch (profileErr) {
    console.error('Failed to fetch profile after wecom login', profileErr);
    router.push('/m/booking');
  }
});
</script>

<style scoped>
.wecom-callback-page {
  background: #fff;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.callback-loading {
  text-align: center;
  color: #666;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f0f0f0;
  border-top-color: var(--mobile-primary, #2b579a);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

p {
  font-size: 16px;
  color: #333;
}
</style>