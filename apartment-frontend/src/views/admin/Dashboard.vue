<template>
  <div class="admin-page dashboard">
    <div class="stat-grid">
      <div class="stat-card arrival">
        <div class="stat-label">今日抵店</div>
        <div class="stat-value">{{ data.arrivingToday }}</div>
      </div>
      <div class="stat-card departure">
        <div class="stat-label">今日离店</div>
        <div class="stat-value">{{ data.departingToday }}</div>
      </div>
      <div class="stat-card upcoming">
        <div class="stat-label">未来3天抵达</div>
        <div class="stat-value">{{ data.arrivingInNDays }}</div>
      </div>
      <div class="stat-card status">
        <div class="stat-label">房态统计</div>
        <div class="status-summary">
          <span class="free">空闲: {{ data.statusCounts?.FREE }}</span>
          <span class="occupied">在住: {{ data.statusCounts?.OCCUPIED }}</span>
          <span class="repair">维保: {{ data.statusCounts?.REPAIR }}</span>
        </div>
      </div>
    </div>

    <div class="room-status-grid">
      <div 
        v-for="room in data.rooms" 
        :key="room.roomId"
        class="room-item"
        :class="getStatusClass(room.status)"
      >
        <div class="room-no">{{ room.roomNo }}</div>
        <div class="room-type">{{ room.roomTypeName }}</div>
        <div v-if="room.label" class="room-label" :class="room.label.toLowerCase()">
          {{ t('dashboard.' + room.label) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import api from '../../utils/api';

const { t } = useI18n();
const data = ref<any>({
  arrivingToday: 0,
  departingToday: 0,
  arrivingInNDays: 0,
  statusCounts: { FREE: 0, OCCUPIED: 0, REPAIR: 0 },
  rooms: []
});

const fetchData = async () => {
  try {
    const res = await api.get('/dashboard/room-status');
    data.value = res;
  } catch (e) {
    console.error('Failed to fetch dashboard data', e);
  }
};

const getStatusClass = (status: number) => {
  if (status === 0) return 'free-bg';
  if (status === 1) return 'occupied-bg';
  return 'repair-bg';
};

onMounted(fetchData);
</script>

<style scoped>
.dashboard { padding: 1.5rem; }
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}
.stat-card {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);
  border-left: 4px solid #cbd5e1;
}
.arrival { border-left-color: #10b981; }
.departure { border-left-color: #ef4444; }
.upcoming { border-left-color: #f59e0b; }
.status { border-left-color: #6366f1; }

.stat-label { font-size: 0.875rem; color: #64748b; font-weight: 600; margin-bottom: 0.5rem; }
.stat-value { font-size: 1.875rem; font-weight: 700; color: #1e293b; }
.status-summary { display: flex; flex-direction: column; gap: 4px; font-size: 14px; font-weight: 600; }
.status-summary .free { color: #10b981; }
.status-summary .occupied { color: #ef4444; }
.status-summary .repair { color: #64748b; }

.room-status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 1rem;
}
.room-item {
  aspect-ratio: 1/1;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  transition: transform 0.2s;
  cursor: pointer;
  background: white;
  box-shadow: 0 1px 3px rgb(0 0 0 / 0.1);
}
.room-item:hover { transform: scale(1.05); }

.room-no { font-size: 1.25rem; font-weight: 800; }
.room-type { font-size: 0.75rem; color: #64748b; }

.free-bg { background: #eff6ff; border: 1px solid #bfdbfe; color: #1e40af; } /* Blue for Empty */
.occupied-bg { background: #f0fdf4; border: 1px solid #bcf0da; color: #166534; } /* Green for Occupied */
.repair-bg { background: #fef2f2; border: 1px solid #fecaca; color: #991b1b; } /* Red for Repair */

.room-label {
  position: absolute;
  top: 4px;
  right: 4px;
  font-size: 10px;
  padding: 2px 4px;
  border-radius: 4px;
  color: white;
}
.arriving_today { background: #10b981; }
.departing_today { background: #ef4444; }
.arriving_soon { background: #f59e0b; }
</style>
