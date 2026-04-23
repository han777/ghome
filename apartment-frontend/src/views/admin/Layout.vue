<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="logo">
        <span class="logo-icon">🏢</span>
        <span class="logo-text">Genesis Admin</span>
      </div>
      <nav class="nav-menu">
        <router-link to="/admin/dashboard" class="nav-item">
          <span class="icon">📊</span>
          <span class="text">房态仪表盘</span>
        </router-link>
        <router-link to="/admin/rooms" class="nav-item">
          <span class="icon">🏠</span>
          <span class="text">房间管理</span>
        </router-link>
        <router-link to="/admin/orders" class="nav-item">
          <span class="icon">📝</span>
          <span class="text">订单管理</span>
        </router-link>
        <router-link to="/admin/buildings" class="nav-item">
          <span class="icon">🏘️</span>
          <span class="text">楼栋管理</span>
        </router-link>
        <router-link to="/admin/room-types" class="nav-item">
          <span class="icon">🛌</span>
          <span class="text">房型管理</span>
        </router-link>
        <router-link to="/admin/gantt" class="nav-item">
          <span class="icon">📅</span>
          <span class="text">线性房态</span>
        </router-link>
        <router-link to="/admin/reports" class="nav-item">
          <span class="icon">📈</span>
          <span class="text">财务报表</span>
        </router-link>
        <div style="height: 1px; background: rgba(255,255,255,0.1); margin: 8px 16px;"></div>
        <router-link to="/admin/accounts" class="nav-item">
          <span class="icon">👤</span>
          <span class="text">账户管理</span>
        </router-link>
        <router-link to="/admin/roles" class="nav-item">
          <span class="icon">🛡️</span>
          <span class="text">角色管理</span>
        </router-link>
        <router-link to="/admin/menus" class="nav-item">
          <span class="icon">📜</span>
          <span class="text">菜单管理</span>
        </router-link>
        <router-link to="/admin/dicts" class="nav-item">
          <span class="icon">📖</span>
          <span class="text">字典管理</span>
        </router-link>
        <router-link to="/admin/depts" class="nav-item">
          <span class="icon">🏢</span>
          <span class="text">部门管理</span>
        </router-link>
      </nav>
      <div class="user-info">
        <div class="avatar">A</div>
        <div class="details">
          <p class="name">Admin User</p>
          <p class="role">Administrator</p>
        </div>
        <button @click="logout" class="logout-btn">🚪</button>
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
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

const titles: Record<string, string> = {
  'Dashboard': '房态仪表盘',
  'Accounts': '账户管理',
  'Roles': '角色管理',
  'Menus': '菜单管理',
  'Dicts': '字典管理',
  'Depts': '部门管理',
  'Rooms': '房间管理',
  'Orders': '订单管理',
  'Buildings': '楼栋管理',
  'RoomTypes': '房型管理',
  'Gantt': '线性房态',
  'Reports': '财务报表'
};

const currentTitle = computed(() => titles[route.name as string] || 'Dashboard');

const logout = () => {
  localStorage.removeItem('token');
  router.push('/login');
};
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background-color: #f8fafc;
}

.sidebar {
  width: 260px;
  flex-shrink: 0;
  background: #1e293b;
  color: #fff;
  display: flex;
  flex-direction: column;
  padding: 20px 0;
  box-shadow: 4px 0 10px rgba(0,0,0,0.1);
}

.logo {
  display: flex;
  align-items: center;
  padding: 0 24px;
  margin-bottom: 40px;
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
  gap: 8px;
  padding: 0 12px;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  color: #94a3b8;
  text-decoration: none;
  transition: all 0.3s ease;
}

.nav-item:hover {
  background: rgba(255,255,255,0.05);
  color: #fff;
}

.nav-item.router-link-active {
  background: #38bdf8;
  color: #fff;
  box-shadow: 0 4px 12px rgba(56, 189, 248, 0.3);
}

.nav-item .icon {
  font-size: 18px;
  margin-right: 12px;
  width: 24px;
  text-align: center;
}

.nav-item .text {
  font-size: 15px;
  font-weight: 500;
}

.user-info {
  margin-top: auto;
  padding: 20px 16px;
  border-top: 1px solid rgba(255,255,255,0.1);
  display: flex;
  align-items: center;
  gap: 12px;
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
</style>
