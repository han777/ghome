<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1>Genesis</h1>
        <p>Apartment Management System</p>
      </div>
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label>Username</label>
          <input v-model="username" type="text" placeholder="Enter your username" required>
        </div>
        <div class="form-group">
          <label>Password</label>
          <input v-model="password" type="password" placeholder="Enter your password" required>
        </div>
        <div v-if="error" class="error-msg">{{ error }}</div>
        <button type="submit" :disabled="loading" class="login-btn">
          <span v-if="loading">Logging in...</span>
          <span v-else>Login</span>
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import api from '../utils/api';

const router = useRouter();
const route = useRoute();
const username = ref('admin');
const password = ref('admin');
const loading = ref(false);
const error = ref('');

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
      const redirect = route.query.redirect as string;
      router.push(redirect || '/admin');
    }
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Invalid username or password';
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
</style>
