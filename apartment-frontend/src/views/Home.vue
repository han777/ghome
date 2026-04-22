<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '../utils/api'

const { t, locale } = useI18n()
const rooms = ref<any[]>([])

onMounted(async () => {
  try {
    const res: any = await api.get('/rooms/all');
    rooms.value = res;
  } catch (e) {
    console.error('Failed to fetch rooms', e);
  }
});

const toggleLocale = () => {
  locale.value = locale.value === 'zh' ? 'en' : 'zh'
}
</script>

<template>
  <div class="glass-container">
    <header class="header">
      <h1 class="glow-text">{{ t('title') }}</h1>
      <div class="actions">
        <router-link to="/admin" class="admin-link">Admin Dashboard</router-link>
        <button @click="toggleLocale" class="lang-btn">{{ locale === 'zh' ? 'EN' : '中文' }}</button>
      </div>
    </header>

    <main class="content">
      <div class="card-hero">
        <h2>{{ t('booking.title') }}</h2>
        <p class="subtitle">Premium Apartment Living Experience</p>
      </div>

      <div class="room-grid">
        <div 
          v-for="room in rooms" 
          :key="room.id"
          class="room-card"
          :class="[room.direction?.toLowerCase(), room.status === 0 ? 'free' : room.status === 1 ? 'occupied' : 'repair']"
        >
          <div class="room-info">
            <span class="room-no">{{ room.roomNo }}</span>
            <span class="room-tag">{{ room.direction === 'SOUTH' ? t('booking.south') : t('booking.north') }}</span>
          </div>
          <div class="room-footer">
            <span class="status-indicator"></span>
            {{ room.status === 0 ? 'FREE' : room.status === 1 ? 'OCCUPIED' : 'REPAIR' }}
          </div>
          <div v-if="room.status === 0" class="hover-overlay">
            <button class="book-btn">{{ t('booking.submit') }}</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.glass-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 2rem;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 3rem;
}

.actions {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.admin-link {
  color: #94a3b8;
  text-decoration: none;
  font-size: 0.9rem;
  transition: color 0.3s;
}

.admin-link:hover {
  color: #38bdf8;
}

.glow-text {
  font-size: 2rem;
  background: linear-gradient(to right, #818cf8, #c084fc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: 800;
}

.lang-btn {
  padding: 0.5rem 1rem;
  border-radius: 20px;
  border: 1px solid rgba(255,255,255,0.2);
  background: rgba(255, 255, 255, 0.05);
  color: white;
  cursor: pointer;
  backdrop-filter: blur(10px);
}

.card-hero {
  margin-bottom: 2rem;
}

.subtitle {
  color: #94a3b8;
  letter-spacing: 1px;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1.5rem;
}

.room-card {
  position: relative;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  padding: 1.5rem;
  border: 1px solid rgba(255,255,255,0.1);
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.room-card:hover {
  transform: translateY(-8px);
  border-color: #6366f1;
  background: rgba(255,255,255,0.08);
}

.room-no {
  font-size: 2rem;
  font-weight: 700;
  display: block;
}

.room-tag {
  font-size: 0.8rem;
  background: rgba(255,255,255,0.1);
  padding: 2px 8px;
  border-radius: 10px;
}

.room-footer {
  margin-top: 2rem;
  display: flex;
  align-items: center;
  font-size: 0.9rem;
  color: #94a3b8;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
}

.free .status-indicator { background: #10b981; box-shadow: 0 0 10px #10b981; }
.occupied .status-indicator { background: #ef4444; }

.south { border-bottom: 4px solid #f59e0b; }
.north { border-bottom: 4px solid #3b82f6; }

.hover-overlay {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(99, 102, 241, 0.9);
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.room-card:hover .hover-overlay {
  opacity: 1;
}

.book-btn {
  background: white;
  color: #6366f1;
  border: none;
  padding: 0.8rem 1.5rem;
  border-radius: 12px;
  font-weight: 700;
  cursor: pointer;
}
</style>
