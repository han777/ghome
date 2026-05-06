<template>
  <div class="mobile-app-container">
    <!-- Sidebar for PC -->
    <nav class="mobile-nav-sidebar">
      <div class="sidebar-logo">
        <span>G-HOME</span>
      </div>
      <router-link to="/m/booking" class="nav-item" :class="{ active: currentRoute === 'booking' }">
        <i class="icon-booking"></i>
        <span>预订</span>
      </router-link>
      <router-link to="/m/records" class="nav-item" :class="{ active: currentRoute === 'records' }">
        <i class="icon-records"></i>
        <span>记录</span>
      </router-link>
      <router-link to="/m/mine" class="nav-item" :class="{ active: currentRoute === 'mine' }">
        <i class="icon-mine"></i>
        <span>我的</span>
      </router-link>
    </nav>

    <!-- Main Content -->
    <main class="mobile-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
      <div class="bottom-spacing"></div>
    </main>

    <!-- Bottom Nav for Mobile -->
    <nav class="mobile-nav-bottom">
      <router-link to="/m/booking" class="nav-item" :class="{ active: currentRoute === 'booking' }">
        <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
          <path d="M19,4H18V2H16V4H8V2H6V4H5C3.89,4 3,4.9 3,6V20C3,21.1 3.89,22 5,22H19C20.11,22 21,21.1 21,20V6C21,4.9 20.11,4 19,4M19,20H5V10H19V20M19,8H5V6H19V8M7,12H17V14H7V12M7,16H14V18H7V16Z" />
        </svg>
        <span>预订</span>
      </router-link>
      <router-link to="/m/records" class="nav-item" :class="{ active: currentRoute === 'records' }">
        <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
          <path d="M14,10H19.5L14,4.5V10M5,2H15L21,8V20A2,2 0 0,1 19,22H5C3.89,22 3,21.1 3,20V4A2,2 0 0,1 5,2M7,12V14H17V12H7M7,16V18H14V16H7Z" />
        </svg>
        <span>记录</span>
      </router-link>
      <router-link to="/m/mine" class="nav-item" :class="{ active: currentRoute === 'mine' }">
        <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
          <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z" />
        </svg>
        <span>我的</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import '@/assets/mobile.css';

const route = useRoute();
const currentRoute = computed(() => {
  const path = route.path;
  if (path.includes('booking')) return 'booking';
  if (path.includes('records')) return 'records';
  if (path.includes('mine')) return 'mine';
  return 'booking';
});


</script>

<style scoped>
.mobile-main {
  flex: 1;
  width: 100%;
  max-width: 600px; /* Optional: limit width on PC for better H5 look */
  margin: 0 auto;
  position: relative;
}

@media (min-width: 768px) {
  .mobile-main {
    max-width: none;
    margin: 0;
  }
}

.bottom-spacing {
  height: var(--mobile-tab-height);
}

@media (min-width: 768px) {
  .bottom-spacing {
    display: none;
  }
}

.sidebar-logo {
  padding: 20px;
  font-size: 20px;
  font-weight: bold;
  color: var(--mobile-primary);
  text-align: center;
  border-bottom: 1px solid var(--mobile-border);
  margin-bottom: 10px;
}

.icon-booking::before { content: '📅'; }
.icon-records::before { content: '📄'; }
.icon-mine::before { content: '👤'; }
</style>
