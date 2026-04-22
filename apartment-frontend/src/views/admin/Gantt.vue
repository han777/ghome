<template>
  <div class="admin-page">
    <div class="gantt-header">
      <div class="date-selector">
        <button @click="prevMonth">&lt;</button>
        <span>{{ currentYear }}年{{ currentMonth + 1 }}月</span>
        <button @click="nextMonth">&gt;</button>
      </div>
    </div>

    <div class="gantt-container">
      <div class="gantt-y-axis">
        <div class="y-header">房间</div>
        <div v-for="room in rooms" :key="room.id" class="y-item">
          {{ room.roomNo }}
        </div>
      </div>
      <div class="gantt-grid" ref="gridRef">
        <div class="gantt-x-axis">
          <div v-for="day in daysInMonth" :key="day" class="x-item">
            {{ day }}
          </div>
        </div>
        <div class="gantt-body">
          <div v-for="room in rooms" :key="room.id" class="room-row">
            <div 
              v-for="order in getOrdersForRoom(room.id)" 
              :key="order.id"
              class="order-bar"
              :style="getOrderBarStyle(order)"
              :title="order.customerName"
            >
              {{ order.customerName }}
            </div>
            <div v-for="day in daysInMonth" :key="day" class="grid-cell"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import api from '../../utils/api';

const rooms = ref<any[]>([]);
const orders = ref<any[]>([]);
const currentYear = ref(new Date().getFullYear());
const currentMonth = ref(new Date().getMonth());

const daysInMonth = computed(() => {
  return new Date(currentYear.value, currentMonth.value + 1, 0).getDate();
});

const fetchData = async () => {
  try {
    const [roomRes, orderRes] = await Promise.all([
      api.get('/rooms/all'),
      api.get('/orders/all')
    ]) as any[];
    rooms.value = roomRes;
    orders.value = orderRes;
  } catch (e) {
    console.error('Failed to fetch data', e);
  }
};

const prevMonth = () => {
  if (currentMonth.value === 0) {
    currentMonth.value = 11;
    currentYear.value--;
  } else {
    currentMonth.value--;
  }
};

const nextMonth = () => {
  if (currentMonth.value === 11) {
    currentMonth.value = 0;
    currentYear.value++;
  } else {
    currentMonth.value++;
  }
};

const getOrdersForRoom = (roomId: number) => {
  return orders.value.filter(o => o.room?.id === roomId && o.status !== 4);
};

const getOrderBarStyle = (order: any) => {
  const start = new Date(order.startDate);
  const end = new Date(order.endDate);
  
  const monthStart = new Date(currentYear.value, currentMonth.value, 1);
  const monthEnd = new Date(currentYear.value, currentMonth.value + 1, 0);
  
  if (end < monthStart || start > monthEnd) return { display: 'none' };
  
  const effectiveStart = start < monthStart ? 1 : start.getDate();
  const effectiveEnd = end > monthEnd ? daysInMonth.value : end.getDate();
  
  const width = (effectiveEnd - effectiveStart + 1) * 40;
  const left = (effectiveStart - 1) * 40;
  
  return {
    left: left + 'px',
    width: width + 'px',
    background: order.status === 2 ? '#3b82f6' : '#94a3b8'
  };
};

onMounted(fetchData);
</script>

<style scoped>
.gantt-header { margin-bottom: 1rem; display: flex; justify-content: center; }
.date-selector { display: flex; align-items: center; gap: 1rem; font-weight: 700; }
.date-selector button { padding: 4px 12px; border-radius: 4px; border: 1px solid #e2e8f0; background: white; cursor: pointer; }

.gantt-container {
  display: flex;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.gantt-y-axis { width: 100px; border-right: 1px solid #e2e8f0; background: #f8fafc; }
.y-header { height: 40px; border-bottom: 1px solid #e2e8f0; display: flex; align-items: center; justify-content: center; font-weight: 700; }
.y-item { height: 40px; border-bottom: 1px solid #e2e8f0; display: flex; align-items: center; padding: 0 12px; font-size: 14px; }

.gantt-grid { flex: 1; overflow-x: auto; position: relative; }
.gantt-x-axis { display: flex; height: 40px; border-bottom: 1px solid #e2e8f0; background: #f8fafc; }
.x-item { min-width: 40px; width: 40px; border-right: 1px solid #e2e8f0; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; }

.gantt-body { position: relative; }
.room-row { height: 40px; border-bottom: 1px solid #e2e8f0; display: flex; position: relative; }
.grid-cell { min-width: 40px; width: 40px; border-right: 1px solid #f1f5f9; }

.order-bar {
  position: absolute;
  top: 6px;
  height: 28px;
  border-radius: 6px;
  color: white;
  display: flex;
  align-items: center;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  z-index: 10;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}
</style>
