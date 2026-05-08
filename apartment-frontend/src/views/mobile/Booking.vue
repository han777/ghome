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
          <input type="date" v-model="startDate" class="date-input" :min="minDate" @change="validateDates">
          <span class="date-day">{{ getDayOfWeek(startDate) }}</span>
        </div>
        <div class="date-duration">
          <span class="duration-badge">共{{ stayDays }}晚</span>
          <div class="duration-line"></div>
        </div>
        <div class="date-item">
          <span class="date-label">退房日期</span>
          <input type="date" v-model="endDate" class="date-input" :min="startDate" @change="validateDates">
          <span class="date-day">{{ getDayOfWeek(endDate) }}</span>
        </div>
      </div>

      <h3 class="section-title">房型选择</h3>

      <!-- Room Type List -->
      <div class="room-list">
        <div 
          v-for="type in roomTypes" 
          :key="type.id" 
          class="mobile-card room-card" 
          :class="{ selected: selectedTypeId === type.id }"
          @click="selectedTypeId = type.id"
        >
          <div 
            class="room-img" 
            :style="{ backgroundImage: `url(${type.images?.[0]?.url || defaultRoomImage})` }"
            @click.stop="openGallery(type)"
          >
            <div v-if="type.images?.length > 1" class="img-count-tag">
              {{ type.images.length }}张
            </div>
          </div>
          <div class="room-info">
            <div class="room-name">{{ type.nameIntl?.zh || type.typeCode }}</div>
            <div class="room-desc">{{ type.remarks }}</div>
            <div class="room-price-row">
              <span class="price-symbol">¥</span>
              <span class="price-value">{{ type.priceShortRent }}</span>
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
        <button 
          class="mobile-btn-primary" 
          :disabled="!selectedTypeId || stayDays <= 0"
          @click="goToRoomSelect"
        >
          立即预订
        </button>
      </div>
    </div>

    <!-- Mobile Gallery Modal -->
    <div v-if="showGallery" class="mobile-gallery-overlay" @click="showGallery = false">
      <div class="gallery-container" @click.stop>
        <div class="gallery-header">
          <span class="gallery-count">{{ currentGalleryIndex + 1 }} / {{ currentGalleryImages.length }}</span>
          <button class="gallery-close" @click="showGallery = false">&times;</button>
        </div>
        <div class="gallery-main">
          <button class="nav-btn prev" @click="prevImage" v-if="currentGalleryImages.length > 1">
            <svg viewBox="0 0 24 24" width="24" height="24"><path d="M15.41,16.58L10.83,12L15.41,7.41L14,6L8,12L14,18L15.41,16.58Z" fill="white"/></svg>
          </button>
          <img :src="currentGalleryImages[currentGalleryIndex].url" class="gallery-img">
          <button class="nav-btn next" @click="nextImage" v-if="currentGalleryImages.length > 1">
            <svg viewBox="0 0 24 24" width="24" height="24"><path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z" fill="white"/></svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '../../utils/api';

const router = useRouter();

const minDate = new Date().toISOString().split('T')[0];
const startDate = ref(new Date().toISOString().split('T')[0]);
const endDate = ref(new Date(Date.now() + 86400000).toISOString().split('T')[0]);
const roomTypes = ref<any[]>([]);
const selectedTypeId = ref<number | null>(null);
const defaultRoomImage = 'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&q=80&w=400';

const showGallery = ref(false);
const currentGalleryImages = ref<any[]>([]);
const currentGalleryIndex = ref(0);

const stayDays = computed(() => {
  if (!startDate.value || !endDate.value) return 0;
  const s = new Date(startDate.value);
  const e = new Date(endDate.value);
  const diff = e.getTime() - s.getTime();
  const days = Math.ceil(diff / (1000 * 60 * 60 * 24));
  return days > 0 ? days : 0;
});

const getDayOfWeek = (dateStr: string) => {
  if (!dateStr) return '';
  const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
  return days[new Date(dateStr).getDay()];
};

const validateDates = () => {
  if (endDate.value <= startDate.value) {
    const nextDay = new Date(new Date(startDate.value).getTime() + 86400000);
    endDate.value = nextDay.toISOString().split('T')[0];
  }
};

const fetchRoomTypes = async () => {
  try {
    const res = await api.get('/room-types/all') as any;
    roomTypes.value = res;
    if (res.length > 0) {
      selectedTypeId.value = res[0].id;
    }
  } catch (e) {
    console.error('Failed to fetch room types', e);
  }
};

const openGallery = (type: any) => {
  if (type.images && type.images.length > 0) {
    currentGalleryImages.value = type.images;
    currentGalleryIndex.value = 0;
    showGallery.value = true;
  }
};

const nextImage = () => {
  currentGalleryIndex.value = (currentGalleryIndex.value + 1) % currentGalleryImages.value.length;
};

const prevImage = () => {
  currentGalleryIndex.value = (currentGalleryIndex.value - 1 + currentGalleryImages.value.length) % currentGalleryImages.value.length;
};

onMounted(fetchRoomTypes);

const goToRoomSelect = () => {
  router.push({
    path: '/m/room-select',
    query: {
      start: startDate.value,
      end: endDate.value,
      typeId: selectedTypeId.value?.toString()
    }
  });
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

.date-input {
  border: none;
  font-size: 15px;
  font-weight: bold;
  color: #333;
  background: transparent;
  width: 100%;
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
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.room-card.selected {
  border-color: var(--mobile-primary);
  box-shadow: 0 4px 12px rgba(var(--mobile-primary-rgb), 0.2);
}

.room-img {
  width: 100px;
  height: 100%;
  background-size: cover;
  background-position: center;
  position: relative;
}

.img-count-tag {
  position: absolute;
  bottom: 4px;
  right: 4px;
  background: rgba(0,0,0,0.6);
  color: #fff;
  font-size: 10px;
  padding: 2px 4px;
  border-radius: 4px;
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

/* Mobile Gallery */
.mobile-gallery-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #000;
  z-index: 2000;
  display: flex;
  flex-direction: column;
}

.gallery-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.gallery-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
}

.gallery-count {
  font-size: 14px;
}

.gallery-close {
  background: transparent;
  border: none;
  color: #fff;
  font-size: 28px;
  line-height: 1;
}

.gallery-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.gallery-img {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
}

.nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255,255,255,0.1);
  border: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-btn.prev { left: 10px; }
.nav-btn.next { right: 10px; }
</style>
