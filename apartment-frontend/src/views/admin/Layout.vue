<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="logo">
        <span class="logo-icon">🏢</span>
        <span class="logo-text">G-HOME Admin</span>
      </div>
      <div class="nav-menu">
        <div v-for="group in menuGroups" :key="group.title" class="menu-group">
          <div class="group-header" @click="toggleGroup(group.title)">
            <span class="icon">{{ group.icon }}</span>
            <span class="text">{{ group.title }}</span>
            <span class="arrow" :class="{ 'is-expanded': isExpanded(group.title) }">▼</span>
          </div>
          <transition name="expand">
            <div v-show="isExpanded(group.title)" class="group-items">
              <router-link 
                v-for="item in group.items" 
                :key="item.path" 
                :to="item.path" 
                class="nav-item"
              >
                <span class="icon">{{ item.icon }}</span>
                <span class="text">{{ item.name }}</span>
              </router-link>
            </div>
          </transition>
        </div>
      </div>
      <div class="user-info" v-if="currentUser">
        <div class="avatar">{{ currentUser.realName?.charAt(0) || currentUser.username?.charAt(0) }}</div>
        <div class="details">
          <p class="name">{{ currentUser.realName || currentUser.username }}</p>
          <p class="role">{{ currentUser.roles?.map((r: any) => r.roleName).join(', ') }}</p>
        </div>
        <button @click="logout" class="logout-btn" title="Logout">🚪</button>
      </div>
    </aside>
    <main class="content-area">
      <header class="top-header">
        <h1>{{ currentTitle }}</h1>
      </header>
      <div class="page-content">
        <router-view></router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '../../utils/api';

const route = useRoute();
const router = useRouter();

const menuGroups = [
  {
    title: '客房业务',
    icon: '🏨',
    items: [
      { name: '房态仪表盘', path: '/admin/dashboard', icon: '📊' },
      { name: '入住管理', path: '/admin/orders', icon: '📝' },
      { name: '历史订单查询', path: '/admin/history-orders', icon: '📜' },
      { name: '维修管理', path: '/admin/maintenances', icon: '🔧' },
      { name: '线性房态', path: '/admin/gantt', icon: '📅' },
      { name: '财务报表', path: '/admin/reports', icon: '📈' },
      { name: '房型预测报表', path: '/admin/room-type-forecast', icon: '📉' },
      { name: '房间费结算明细', path: '/admin/room-fee-detail', icon: '🧾' },
      { name: '商品服务费结算明细', path: '/admin/service-fee-detail', icon: '📋' },
      { name: '通知记录', path: '/admin/notification-records', icon: '🔔' },
    ]
  },
  {
    title: '数据管理',
    icon: '📊',
    items: [
      { name: '楼栋管理', path: '/admin/buildings', icon: '🏘️' },
      { name: '房型价格', path: '/admin/room-types', icon: '🛌' },
      { name: '房间列表', path: '/admin/rooms', icon: '🏠' },
      { name: '商品服务价格', path: '/admin/product-prices', icon: '🏷️' },
    ]
  },
  {
    title: '系统管理',
    icon: '⚙️',
    items: [
      { name: '用户管理', path: '/admin/accounts', icon: '👤' },
      { name: '角色管理', path: '/admin/roles', icon: '🛡️' },
      { name: '字典管理', path: '/admin/dicts', icon: '📖' },
      { name: '部门管理', path: '/admin/depts', icon: '🏢' },
    ]
  }
];

const expandedGroups = ref(menuGroups.map(g => g.title));

const toggleGroup = (title: string) => {
  const index = expandedGroups.value.indexOf(title);
  if (index > -1) {
    expandedGroups.value.splice(index, 1);
  } else {
    expandedGroups.value.push(title);
  }
};

const isExpanded = (title: string) => expandedGroups.value.includes(title);

const titles: Record<string, string> = {
  'Dashboard': '房态仪表盘',
  'Accounts': '用户管理',
  'Roles': '角色管理',
  'Dicts': '字典管理',
  'Depts': '部门管理',
  'Rooms': '房间管理',
  'Orders': '入住管理',
  'HistoryOrders': '历史订单查询',
  'Buildings': '楼栋管理',
  'RoomTypes': '房型价格',
  'Gantt': '线性房态',
  'Maintenances': '维修管理',
  'Reports': '财务报表',
  'RoomTypeForecast': '房型预测报表',
  'RoomFeeDetail': '房间费结算明细',
  'ServiceFeeDetail': '商品服务费结算明细',
  'ProductPrices': '商品服务价格',
  'NotificationRecords': '通知记录'
};

const currentTitle = computed(() => titles[route.name as string] || 'Dashboard');

const currentUser = ref<any>(null);

onMounted(async () => {
  try {
    currentUser.value = await api.get('/sys/profile');
  } catch (e) {
    console.error('Failed to fetch profile', e);
    // If unauthorized, redirect to login might be handled by api interceptor
  }
});

const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('roles');
  router.push('/login');
};
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  background-color: #f8fafc;
  overflow: hidden;
}

.sidebar {
  width: 260px;
  flex-shrink: 0;
  background: #1e293b;
  color: #fff;
  display: flex;
  flex-direction: column;
  padding: 0;
  box-shadow: 4px 0 10px rgba(0,0,0,0.1);
  height: 100%;
}

.logo {
  display: flex;
  align-items: center;
  padding: 24px;
  margin-bottom: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}

.logo-icon {
  font-size: 24px;
  margin-right: 12px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #38bdf8;
}

.nav-menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow-y: auto;
  /* Hide scrollbar but keep functionality */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}

.nav-menu::-webkit-scrollbar {
  display: none; /* Chrome, Safari and Opera */
}

.menu-group {
  margin-bottom: 4px;
}

.group-header {
  display: flex;
  align-items: center;
  padding: 12px 24px;
  cursor: pointer;
  color: #94a3b8;
  transition: all 0.3s ease;
  user-select: none;
}

.group-header:hover {
  background: rgba(255,255,255,0.05);
  color: #fff;
}

.group-header .icon {
  margin-right: 12px;
  font-size: 18px;
  width: 24px;
  text-align: center;
}

.group-header .text {
  flex: 1;
  font-weight: 600;
  font-size: 15px;
}

.group-header .arrow {
  font-size: 10px;
  transition: transform 0.3s ease;
  opacity: 0.5;
}

.group-header .arrow.is-expanded {
  transform: rotate(180deg);
}

.group-items {
  background: rgba(0,0,0,0.1);
  overflow: hidden;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 10px 24px 10px 52px;
  color: #94a3b8;
  text-decoration: none;
  transition: all 0.3s ease;
  font-size: 14px;
}

.nav-item:hover {
  background: rgba(255,255,255,0.05);
  color: #fff;
}

.nav-item.router-link-active {
  background: #38bdf8;
  color: #fff;
  box-shadow: inset 4px 0 0 #fff;
}

.nav-item .icon {
  margin-right: 12px;
  font-size: 16px;
  width: 20px;
  text-align: center;
}

.user-info {
  margin-top: auto;
  padding: 20px 24px;
  border-top: 1px solid rgba(255,255,255,0.05);
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(0,0,0,0.2);
}

.avatar {
  width: 40px;
  height: 40px;
  background: #38bdf8;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: #fff;
}

.details .name {
  font-size: 14px;
  font-weight: 600;
  margin: 0;
  color: #fff;
}

.details .role {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

.logout-btn {
  margin-left: auto;
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 18px;
  padding: 4px;
  transition: color 0.3s;
}

.logout-btn:hover {
  color: #ef4444;
}

.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.top-header {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  padding: 0 32px;
  flex-shrink: 0;
}

.top-header h1 {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.page-content {
  flex: 1;
  padding: 32px;
  overflow-y: auto;
}

/* Accordion Transition */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease-in-out;
  max-height: 1000px;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
}
</style>
