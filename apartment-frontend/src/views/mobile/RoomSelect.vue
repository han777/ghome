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
      <div class="selection-info">{{ startDate }} - {{ endDate }} {{ roomTypeName }}</div>
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
            :key="room.id" 
            class="room-item"
            :class="[room.statusClass, { selected: selectedRoomId === room.id }]"
            @click="handleSelect(room)"
          >
            <div class="room-num">{{ room.roomNo }}</div>
            <div class="room-dir">{{ room.roomType?.typeCode }}</div>
          </div>
        </div>
      </div>
      <div v-if="floors.length === 0" class="empty-rooms">
        所选时段暂无可用房间
      </div>
    </div>

    <div class="footer-actions">
      <button class="mobile-btn-outline" @click="router.back()">取消</button>
      <button class="mobile-btn-primary" :disabled="!selectedRoomId" @click="confirmSelection">确定</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import api from '../../utils/api';

const router = useRouter();
const route = useRoute();

const startDate = route.query.start as string;
const endDate = route.query.end as string;
const typeId = route.query.typeId as string;

const selectedRoomId = ref<number | null>(null);
const roomTypeName = ref('加载中...');
const floors = ref<any[]>([]);

const fetchRooms = async () => {
  try {
    // Backend expects LocalDateTime, so we append times
    const startDT = startDate + 'T14:00:00';
    const endDT = endDate + 'T12:00:00';
    
    const [roomsRes, typesRes] = await Promise.all([
      api.get(`/rooms/available?startDate=${startDT}&endDate=${endDT}`),
      api.get('/room-types/all')
    ]) as any[];

    const roomType = typesRes.find((t: any) => t.id.toString() === typeId);
    roomTypeName.value = roomType ? (roomType.nameIntl?.zh || roomType.typeCode) : '未知房型';

    // Filter rooms by type
    const filteredRooms = roomsRes.filter((r: any) => r.roomType?.id.toString() === typeId);

    // Group by floor
    const grouped = filteredRooms.reduce((acc: any, room: any) => {
      const floorLevel = room.floor?.level || 0;
      if (!acc[floorLevel]) {
        acc[floorLevel] = { level: floorLevel, rooms: [] };
      }
      room.statusClass = 'available';
      acc[floorLevel].rooms.push(room);
      return acc;
    }, {});

    floors.value = Object.values(grouped).sort((a: any, b: any) => b.level - a.level);
  } catch (e) {
    console.error('Failed to fetch rooms', e);
  }
};

onMounted(fetchRooms);

const handleSelect = (room: any) => {
  selectedRoomId.value = room.id;
};

const confirmSelection = async () => {
  if (!selectedRoomId.value) return;
  
  try {
    const startDT = startDate + 'T14:00:00';
    const endDT = endDate + 'T12:00:00';

    // Generate Order
    const orderData = {
      startDate: startDT,
      endDate: endDT,
      bizType: 1, // Short term
      status: 0,  // Cooling-off (犹豫期)
      createdAt: new Date().toISOString().slice(0, 16),
      roomOccupies: [
        {
          room: { id: selectedRoomId.value },
          occupantCount: 1,
          checkInTime: startDT,
          checkOutTime: endDT,
          status: 0 // Current
        }
      ]
    };

    const res = await api.post('/orders', orderData) as any;
    
    router.push({
      path: '/m/confirm',
      query: { orderId: res.id.toString() }
    });
  } catch (e: any) {
    alert('预订失败: ' + (e.response?.data?.message || e.message));
  }
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
