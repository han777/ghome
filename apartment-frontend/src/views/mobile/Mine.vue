<template>
  <div class="mine-page">

    <div class="content">
      <!-- Profile Card -->
      <div class="mobile-card profile-card" v-if="user">
        <div class="avatar-wrap">
          <div class="avatar">
            <svg viewBox="0 0 24 24" width="32" height="32" fill="#1677ff"><path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z" /></svg>
          </div>
        </div>
        <h2 class="user-name">{{ user.realName || user.username }}</h2>
        <p class="user-phone">{{ maskPhone(user.phone) }}</p>
      </div>

      <!-- Stats Grid -->
      <div class="stats-grid">
        <div class="mobile-card stat-item">
          <div class="stat-val">{{ stats.total }}</div>
          <div class="stat-label">{{ $t('mine.totalBookings') }}</div>
        </div>
        <div class="mobile-card stat-item">
          <div class="stat-val primary-text">{{ stats.pending }}</div>
          <div class="stat-label">{{ $t('mine.pendingCheckIn') }}</div>
        </div>
        <div class="mobile-card stat-item">
          <div class="stat-val">{{ stats.completed }}</div>
          <div class="stat-label">{{ $t('mine.completed') }}</div>
        </div>
      </div>

      <!-- List Rows -->
      <div class="mobile-card info-list" v-if="user">
        <div class="info-row">
          <div class="row-left">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="#999"><path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z" /></svg>
            <span class="row-label">{{ $t('mine.nameLabel') }}</span>
          </div>
          <span class="row-val">{{ user.realName || '-' }}</span>
        </div>
        <div class="divider"></div>
        <div class="info-row">
          <div class="row-left">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="#999"><path d="M6.62,10.79C8.06,13.62 10.38,15.94 13.21,17.38L15.41,15.18C15.69,14.9 16.08,14.82 16.43,14.93C17.55,15.3 18.75,15.5 20,15.5A1,1 0 0,1 21,16.5V20A1,1 0 0,1 20,21A17,17 0 0,1 3,4A1,1 0 0,1 4,3H7.5A1,1 0 0,1 8.5,4C8.5,5.25 8.7,6.45 9.07,7.57C9.18,7.92 9.1,8.31 8.82,8.59L6.62,10.79Z" /></svg>
            <span class="row-label">{{ $t('mine.phoneLabel') }}</span>
          </div>
          <span class="row-val">{{ user.phone || '-' }}</span>
        </div>
        <div class="divider"></div>
        <div class="info-row lang-row" @click="toggleLanguage">
          <div class="row-left">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="#999"><path d="M12.87 15.07l-2.54-2.51.03-.03c1.74-1.94 2.98-4.17 3.71-6.53H17V4h-7V2H8v2H1v1.99h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5-5 3.11 3.11.76-2.04zM18.5 10h-2L12 22h2l1.12-3h4.75L21 22h2l-4.5-12zm-2.62 7l1.62-4.33L19.12 17h-3.24z"/></svg>
            <span class="row-label">{{ $t('mine.language') }}</span>
          </div>
          <div class="lang-toggle">
            <span class="lang-pill" :class="{ 'lang-active': locale === 'zh' }">中</span>
            <span class="lang-pill" :class="{ 'lang-active': locale === 'en' }">EN</span>
            <span class="lang-pill" :class="{ 'lang-active': locale === 'ja' }">日</span>
          </div>
        </div>
      </div>

      <div class="logout-wrap">
        <button class="mobile-btn-outline" @click="handleLogout">{{ $t('mine.logout') }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '../../utils/api';

const router = useRouter();
const { locale } = useI18n();

const toggleLanguage = () => {
  if (locale.value === 'zh') {
    locale.value = 'en';
  } else if (locale.value === 'en') {
    locale.value = 'ja';
  } else {
    locale.value = 'zh';
  }
  localStorage.setItem('locale', locale.value);
};

const user = ref<any>(null);
const stats = ref({
  total: 0,
  pending: 0,
  completed: 0
});

const fetchProfile = async () => {
  try {
    user.value = await api.get('/sys/profile');
  } catch (e) {
    console.error('Failed to fetch profile', e);
  }
};

const fetchStats = async () => {
  try {
    const myOrders = await api.get('/orders/mine') as any[];
    
    stats.value = {
      total: myOrders.length,
      pending: myOrders.filter(o => [0, 1].includes(o.status)).length, // Cooling-off or Pending
      completed: myOrders.filter(o => o.status === 3).length // Out
    };
  } catch (e) {
    console.error('Failed to fetch stats', e);
  }
};

const maskPhone = (phone: string) => {
  if (!phone) return '-';
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
};

const handleLogout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('roles');
  router.push('/m/auth');
};

onMounted(async () => {
  await fetchProfile();
  await fetchStats();
});
</script>

<style scoped>
.mine-page {
  background: #f5f5f5;
  min-height: 100vh;
}

.profile-card {
  text-align: center;
  padding: 30px 20px;
}

.avatar-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}

.avatar {
  width: 64px;
  height: 64px;
  background: #e6f4ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-name {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 4px;
}

.user-phone {
  font-size: 14px;
  color: #999;
}

.stats-grid {
  display: flex;
  margin: 0 4px;
}

.stat-item {
  flex: 1;
  text-align: center;
  margin: 8px;
}

.stat-val {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #999;
}

.primary-text {
  color: var(--mobile-primary);
}

.info-list {
  padding: 0;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
}

.row-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.row-label {
  font-size: 15px;
  color: #666;
}

.row-val {
  font-size: 15px;
  color: #333;
}

.divider {
  height: 1px;
  background: #f0f0f0;
  margin-left: 48px;
}

.logout-wrap {
  padding: 24px 16px;
}

.lang-row {
  cursor: pointer;
  user-select: none;
}

.lang-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
}

.lang-pill {
  font-size: 13px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
  background: #f0f0f0;
  color: #888;
  transition: all 0.25s ease;
}

.lang-pill.lang-active {
  background: #1677ff;
  color: #fff;
}
</style>
