<template>
  <div class="room-select-page">
    <header class="mobile-header">
      <div class="header-left" @click="router.back()">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M15.41,16.58L10.83,12L15.41,7.41L14,6L8,12L14,18L15.41,16.58Z" /></svg>
      </div>
      <div class="mobile-header-title">选择房间</div>
      <div class="header-right">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z" /></svg>
      </div>
    </header>

    <div class="sub-header">
      <div class="selection-info">2026.04.10-2026.04.11 商务大床房</div>
    </div>

    <div class="legend">
      <div class="legend-item"><span class="dot selected"></span> 已选</div>
      <div class="legend-item"><span class="dot available"></span> 可选</div>
      <div class="legend-item"><span class="dot unavailable"></span> 不可选</div>
    </div>

    <div class="content">
      <div v-for="floor in floors" :key="floor.level" class="floor-section">
        <div class="floor-title">{{ floor.level }}楼</div>
        <div class="room-grid">
          <div 
            v-for="room in floor.rooms" 
            :key="room.number" 
            class="room-item"
            :class="[room.status, { selected: selectedRoom === room.number }]"
            @click="handleSelect(room)"
          >
            <div class="room-num">{{ room.number }}</div>
            <div class="room-dir">{{ room.dir }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="footer-actions">
      <button class="mobile-btn-outline" @click="router.back()">取消</button>
      <button class="mobile-btn-primary" @click="confirmSelection">确定</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const selectedRoom = ref('8412');

const floors = ref([
  {
    level: 4,
    rooms: [
      { number: '8418', dir: '南', status: 'unavailable' },
      { number: '8416', dir: '南', status: 'unavailable' },
      { number: '8412', dir: '南', status: 'available' },
      { number: '8411', dir: '北', status: 'available' },
      { number: '8410', dir: '南', status: 'unavailable' },
      { number: '8409', dir: '北', status: 'unavailable' },
      { number: '8408', dir: '南', status: 'unavailable' },
      { number: '8407', dir: '北', status: 'available' },
      { number: '8406', dir: '南', status: 'available' },
      { number: '8405', dir: '北', status: 'available' },
      { number: '8403', dir: '北', status: 'available' },
      { number: '8402', dir: '南', status: 'available' },
      { number: '8401', dir: '北', status: 'available' },
    ]
  },
  {
    level: 3,
    rooms: [
      { number: '8320', dir: '南', status: 'available' },
      { number: '8318', dir: '南', status: 'unavailable' },
      { number: '8316', dir: '南', status: 'unavailable' },
      { number: '8312', dir: '南', status: 'unavailable' },
      { number: '8311', dir: '北', status: 'unavailable' },
      { number: '8310', dir: '南', status: 'unavailable' },
      { number: '8309', dir: '北', status: 'available' },
      { number: '8308', dir: '南', status: 'unavailable' },
      { number: '8307', dir: '北', status: 'available' },
      { number: '8306', dir: '南', status: 'unavailable' },
      { number: '8305', dir: '北', status: 'available' },
      { number: '8303', dir: '北', status: 'available' },
      { number: '8302', dir: '南', status: 'available' },
      { number: '8301', dir: '北', status: 'available' },
    ]
  }
]);

const handleSelect = (room: any) => {
  if (room.status === 'available') {
    selectedRoom.value = room.number;
  }
};

const confirmSelection = () => {
  router.push({
    path: '/m/confirm',
    query: { room: selectedRoom.value }
  });
};
</script>

<style scoped>
.room-select-page {
  padding-bottom: 100px;
  background: #f8f8f8;
  min-height: 100vh;
}

.sub-header {
  background: #fff;
  padding: 8px 16px;
  border-bottom: 1px solid var(--mobile-border);
}

.selection-info {
  font-size: 13px;
  color: var(--mobile-text-secondary);
}

.legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding: 16px;
  background: #fff;
  font-size: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.dot.selected { background: #ff9500; }
.dot.available { background: #52c41a; }
.dot.unavailable { background: #f0f0f0; }

.content {
  padding: 12px;
}

.floor-section {
  margin-bottom: 20px;
}

.floor-title {
  font-size: 14px;
  color: var(--mobile-text-secondary);
  margin-bottom: 12px;
  padding-left: 4px;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.room-item {
  background: #fff;
  border-radius: 8px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.room-item.available {
  border-color: #52c41a;
  color: #52c41a;
}

.room-item.unavailable {
  color: #999;
  background: #f0f0f0;
}

.room-item.selected {
  background: #ff9500 !important;
  border-color: #ff9500 !important;
  color: #fff !important;
}

.room-num {
  font-size: 15px;
  font-weight: 600;
}

.room-dir {
  font-size: 11px;
  margin-top: 2px;
}

.footer-actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 16px;
  display: flex;
  gap: 12px;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.05);
}

.footer-actions button {
  flex: 1;
}
</style>
