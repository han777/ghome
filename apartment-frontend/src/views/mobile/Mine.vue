<template>
  <div class="mine-page">
    <header class="mobile-header">
      <div class="header-left" @click="router.back()">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M15.41,16.58L10.83,12L15.41,7.41L14,6L8,12L14,18L15.41,16.58Z" /></svg>
      </div>
      <div class="mobile-header-title">我的</div>
      <div class="header-right">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M12,16A2,2 0 0,1 14,18A2,2 0 0,1 12,20A2,2 0 0,1 10,18A2,2 0 0,1 12,16M12,10A2,2 0 0,1 14,12A2,2 0 0,1 12,14A2,2 0 0,1 10,12A2,2 0 0,1 12,10M12,4A2,2 0 0,1 14,6A2,2 0 0,1 12,8A2,2 0 0,1 10,6A2,2 0 0,1 12,4Z" /></svg>
      </div>
    </header>

    <div class="content">
      <!-- Profile Card -->
      <div class="mobile-card profile-card">
        <div class="avatar-wrap">
          <div class="avatar">
            <svg viewBox="0 0 24 24" width="32" height="32" fill="#1677ff"><path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z" /></svg>
          </div>
        </div>
        <h2 class="user-name">黄**</h2>
        <p class="user-phone">185*****200</p>
      </div>

      <!-- Stats Grid -->
      <div class="stats-grid">
        <div class="mobile-card stat-item">
          <div class="stat-val">2</div>
          <div class="stat-label">总预定</div>
        </div>
        <div class="mobile-card stat-item">
          <div class="stat-val primary-text">0</div>
          <div class="stat-label">待入住</div>
        </div>
        <div class="mobile-card stat-item">
          <div class="stat-val">2</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>

      <!-- List Rows -->
      <div class="mobile-card info-list">
        <div class="info-row">
          <div class="row-left">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="#999"><path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z" /></svg>
            <span class="row-label">姓 名</span>
          </div>
          <span class="row-val">黄**</span>
        </div>
        <div class="divider"></div>
        <div class="info-row">
          <div class="row-left">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="#999"><path d="M6.62,10.79C8.06,13.62 10.38,15.94 13.21,17.38L15.41,15.18C15.69,14.9 16.08,14.82 16.43,14.93C17.55,15.3 18.75,15.5 20,15.5A1,1 0 0,1 21,16.5V20A1,1 0 0,1 20,21A17,17 0 0,1 3,4A1,1 0 0,1 4,3H7.5A1,1 0 0,1 8.5,4C8.5,5.25 8.7,6.45 9.07,7.57C9.18,7.92 9.1,8.31 8.82,8.59L6.62,10.79Z" /></svg>
            <span class="row-label">手机号</span>
          </div>
          <span class="row-val">185*****200</span>
        </div>
      </div>

      <div class="logout-wrap">
        <button class="mobile-btn-outline" @click="handleLogout">退出登录</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
const router = useRouter();

const handleLogout = () => {
  localStorage.removeItem('token');
  router.push('/m/auth');
};
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
</style>
