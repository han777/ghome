<template>
  <div class="booking-page">
    <header class="mobile-header">
      <div class="header-left">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M15.41,16.58L10.83,12L15.41,7.41L14,6L8,12L14,18L15.41,16.58Z" /></svg>
      </div>
      <div class="mobile-header-title">G-HOME公寓</div>
      <div class="header-right">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M12,16A2,2 0 0,1 14,18A2,2 0 0,1 12,20A2,2 0 0,1 10,18A2,2 0 0,1 12,16M12,10A2,2 0 0,1 14,12A2,2 0 0,1 12,14A2,2 0 0,1 10,12A2,2 0 0,1 12,10M12,4A2,2 0 0,1 14,6A2,2 0 0,1 12,8A2,2 0 0,1 10,6A2,2 0 0,1 12,4Z" /></svg>
      </div>
    </header>

    <div class="content">
      <h2 class="page-title">公寓预订</h2>

      <!-- Date Selection Card -->
      <div class="mobile-card date-card">
        <div class="date-item">
          <span class="date-label">入住日期</span>
          <span class="date-value">2026年04月10日</span>
          <span class="date-day">星期五</span>
        </div>
        <div class="date-duration">
          <span class="duration-badge">共1晚</span>
          <div class="duration-line"></div>
        </div>
        <div class="date-item">
          <span class="date-label">退房日期</span>
          <span class="date-value">2026年04月11日</span>
          <span class="date-day">星期六</span>
        </div>
      </div>

      <h3 class="section-title">房型选择</h3>

      <!-- Room Type List -->
      <div class="room-list">
        <div v-for="room in roomTypes" :key="room.id" class="mobile-card room-card" @click="selectRoom(room)">
          <div class="room-img" :style="{ backgroundImage: `url(${room.image})` }"></div>
          <div class="room-info">
            <div class="room-name">{{ room.name }}</div>
            <div class="room-desc">{{ room.desc }}</div>
            <div class="room-price-row">
              <span class="price-symbol">¥</span>
              <span class="price-value">{{ room.price }}</span>
              <span class="price-unit">/晚</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Notice Row -->
      <div class="mobile-card notice-row">
        <span>入住须知</span>
        <svg viewBox="0 0 24 24" width="20" height="20" fill="#ccc"><path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z" /></svg>
      </div>

      <div class="bottom-action">
        <button class="mobile-btn-primary" @click="goToRoomSelect">立即预订</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const roomTypes = ref([
  {
    id: 1,
    name: '行政套房',
    desc: '仅限副总监及以上预订',
    price: 400,
    image: 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&q=80&w=400'
  },
  {
    id: 2,
    name: '商务大床房',
    desc: '',
    price: 250,
    image: 'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&q=80&w=400'
  },
  {
    id: 3,
    name: '商务双床房',
    desc: '',
    price: 250,
    image: 'https://images.unsplash.com/photo-1595526114035-0d45ed16cfbf?auto=format&fit=crop&q=80&w=400'
  }
]);

const selectRoom = (room: any) => {
  console.log('Selected:', room.name);
};

const goToRoomSelect = () => {
  router.push('/m/room-select');
};
</script>

<style scoped>
.booking-page {
  padding-bottom: 80px;
}

.content {
  padding: 8px 4px;
}

.page-title {
  font-size: 20px;
  font-weight: bold;
  margin: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 20px 16px 8px;
}

.date-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 16px;
}

.date-item {
  display: flex;
  flex-direction: column;
}

.date-label {
  font-size: 12px;
  color: var(--mobile-text-secondary);
  margin-bottom: 4px;
}

.date-value {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.date-day {
  font-size: 12px;
  color: var(--mobile-text-secondary);
  margin-top: 2px;
}

.date-duration {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  flex: 1;
  padding: 0 10px;
}

.duration-badge {
  background: var(--mobile-primary);
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  z-index: 2;
}

.duration-line {
  height: 1px;
  background: var(--mobile-border);
  width: 100%;
  position: absolute;
  top: 50%;
}

.room-card {
  display: flex;
  padding: 0;
  overflow: hidden;
  height: 100px;
}

.room-img {
  width: 100px;
  height: 100%;
  background-size: cover;
  background-position: center;
}

.room-info {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.room-name {
  font-size: 15px;
  font-weight: 600;
}

.room-desc {
  font-size: 12px;
  color: var(--mobile-text-secondary);
  margin-top: 4px;
}

.room-price-row {
  text-align: right;
  color: var(--mobile-primary);
}

.price-symbol {
  font-size: 12px;
  margin-right: 2px;
}

.price-value {
  font-size: 18px;
  font-weight: bold;
}

.price-unit {
  font-size: 12px;
  color: var(--mobile-text-secondary);
  margin-left: 2px;
}

.notice-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
  font-weight: 500;
}

.bottom-action {
  padding: 16px;
  position: sticky;
  bottom: 0;
  background: rgba(245, 245, 245, 0.9);
  backdrop-filter: blur(10px);
}
</style>
