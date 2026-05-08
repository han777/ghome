<template>
  <div class="selection-container">
    <div class="selection-card">
      <div class="header">
        <h1>Welcome Back</h1>
        <p>Please select your workspace to continue</p>
      </div>
      
      <div class="options">
        <div class="option-card admin" @click="navigateTo('/admin')">
          <div class="icon-wrapper">
            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
          </div>
          <div class="content">
            <h3>Admin Portal</h3>
            <p>Manage rooms, users, and system settings</p>
          </div>
          <div class="arrow">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>
          </div>
        </div>

        <div class="option-card user" @click="navigateTo('/m')">
          <div class="icon-wrapper">
            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>
          </div>
          <div class="content">
            <h3>Booking System</h3>
            <p>Browse rooms and manage your bookings</p>
          </div>
          <div class="arrow">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>
          </div>
        </div>
      </div>

      <div class="footer">
        <button class="logout-btn" @click="handleLogout">Logout</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const navigateTo = (path: string) => {
  router.push(path);
};

const handleLogout = () => {
  localStorage.removeItem('token');
  router.push('/login');
};
</script>

<style scoped>
.selection-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at top left, #1e293b, #0f172a);
  padding: 20px;
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
}

.selection-card {
  width: 100%;
  max-width: 600px;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 48px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

.header {
  text-align: center;
  margin-bottom: 40px;
}

.header h1 {
  color: #fff;
  font-size: 32px;
  font-weight: 800;
  margin-bottom: 12px;
  letter-spacing: -0.025em;
}

.header p {
  color: #94a3b8;
  font-size: 16px;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.option-card {
  display: flex;
  align-items: center;
  padding: 24px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.option-card:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(56, 189, 248, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 10px 20px -5px rgba(0, 0, 0, 0.3);
}

.icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24px;
  transition: all 0.3s;
}

.admin .icon-wrapper {
  background: rgba(56, 189, 248, 0.1);
  color: #38bdf8;
}

.user .icon-wrapper {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.option-card:hover .icon-wrapper {
  transform: scale(1.1);
}

.content h3 {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

.content p {
  color: #94a3b8;
  font-size: 14px;
}

.arrow {
  margin-left: auto;
  color: #475569;
  transition: all 0.3s;
}

.option-card:hover .arrow {
  color: #38bdf8;
  transform: translateX(4px);
}

.footer {
  margin-top: 40px;
  text-align: center;
}

.logout-btn {
  background: transparent;
  color: #64748b;
  border: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s;
}

.logout-btn:hover {
  color: #fff;
  text-decoration: underline;
}

@media (max-width: 480px) {
  .selection-card {
    padding: 32px 20px;
  }
  
  .icon-wrapper {
    width: 48px;
    height: 48px;
    margin-right: 16px;
  }
  
  .icon-wrapper svg {
    width: 24px;
    height: 24px;
  }
}
</style>
