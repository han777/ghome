<template>
  <div class="admin-page">
    <!-- Controls -->
    <div class="forecast-controls">
      <div class="date-picker-row">
        <label>开始日期</label>
        <input type="date" v-model="startDate" :min="todayStr" @change="clampDates(); fetchData()" />
        <label>结束日期</label>
        <input type="date" v-model="endDate" :min="startDate" :max="maxEndStr" @change="clampDates(); fetchData()" />
      </div>
      <div class="action-row">
        <button class="admin-btn" @click="prevWindow">◀ 上一页</button>
        <button class="admin-btn" @click="nextWindow">下一页 ▶</button>
        <button class="admin-btn primary" @click="fetchData">刷新</button>
        <button class="admin-btn export" @click="exportExcel">导出Excel</button>
      </div>
    </div>

    <!-- Period display -->
    <div class="period-info">
      时间窗口: {{ startDate }} → {{ endDate }} ({{ windowDays }} 天)
    </div>

    <!-- Table -->
    <div class="table-card scroll-wrapper" v-if="data">
      <table class="admin-table forecast-table">
        <thead>
          <tr>
            <th rowspan="2" class="sticky-col type-col">房型</th>
            <th rowspan="2" class="sticky-col total-col">总房间数</th>
            <th v-for="d in data.dates" :key="d" colspan="3" class="date-header">{{ formatDisplayDate(d) }}</th>
          </tr>
          <tr>
            <template v-for="d in data.dates" :key="d + '-sub'">
              <th class="sub-header booked">预订</th>
              <th class="sub-header maint">维修</th>
              <th class="sub-header avail">可订</th>
            </template>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in data.rows" :key="row.roomTypeId">
            <td class="sticky-col type-col">{{ row.roomTypeName }}</td>
            <td class="sticky-col total-col">{{ row.totalRooms }}</td>
            <template v-for="d in data.dates" :key="d + '-' + row.roomTypeId">
              <td class="num-cell booked">{{ row.bookedCount?.[d] ?? 0 }}</td>
              <td class="num-cell maint">{{ row.maintenanceCount?.[d] ?? 0 }}</td>
              <td class="num-cell avail" :class="{ 'low': (row.availableCount?.[d] ?? 0) <= 0 }">{{ row.availableCount?.[d] ?? 0 }}</td>
            </template>
          </tr>
          <!-- Total row -->
          <tr class="total-row" v-if="data.total">
            <td class="sticky-col type-col bold">合计</td>
            <td class="sticky-col total-col bold">{{ data.total.totalRooms }}</td>
            <template v-for="d in data.dates" :key="d + '-total'">
              <td class="num-cell booked bold">{{ data.total.bookedCount?.[d] ?? 0 }}</td>
              <td class="num-cell maint bold">{{ data.total.maintenanceCount?.[d] ?? 0 }}</td>
              <td class="num-cell avail bold" :class="{ 'low': (data.total.availableCount?.[d] ?? 0) <= 0 }">{{ data.total.availableCount?.[d] ?? 0 }}</td>
            </template>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-else class="loading-state">加载中...</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import api from '../../utils/api';

const todayStr = new Date().toISOString().split('T')[0];
const startDate = ref(todayStr);
const endDate = ref(addDays(todayStr, 9));
const data = ref<any>(null);

const maxEndStr = computed(() => addDays(startDate.value, 29));
const windowDays = computed(() => {
  const s = new Date(startDate.value);
  const e = new Date(endDate.value);
  return Math.max(1, Math.ceil((e.getTime() - s.getTime()) / 86400000) + 1);
});

function addDays(dateStr: string, days: number): string {
  const d = new Date(dateStr);
  d.setDate(d.getDate() + days);
  return d.toISOString().split('T')[0];
}

function clampDates() {
  if (endDate.value < startDate.value) endDate.value = startDate.value;
  if (endDate.value > maxEndStr.value) endDate.value = maxEndStr.value;
}

const fetchData = async () => {
  clampDates();
  try {
    data.value = await api.get(`/forecast/room-type?start=${startDate.value}&end=${endDate.value}`);
  } catch (e) {
    console.error('Failed to fetch forecast data', e);
  }
};

const prevWindow = () => {
  // Shift window back by windowDays, but not before today
  const days = windowDays.value;
  const newStart = addDays(startDate.value, -days);
  const newEnd = addDays(endDate.value, -days);
  startDate.value = newStart >= todayStr ? newStart : todayStr;
  endDate.value = newEnd >= todayStr ? newEnd : todayStr;
  clampDates();
  fetchData();
};

const nextWindow = () => {
  const days = windowDays.value;
  startDate.value = addDays(startDate.value, days);
  endDate.value = addDays(endDate.value, days);
  clampDates();
  fetchData();
};

const exportExcel = () => {
  const url = `/api/forecast/room-type/excel?start=${startDate.value}&end=${endDate.value}`;
  const token = localStorage.getItem('token');
  const link = document.createElement('a');
  link.href = url;
  if (token) {
    // Can't set Authorization header for direct download, so use query param approach
    // Actually, let's just open the URL — the browser will send cookies if any,
    // but since we use JWT in header, we need a different approach.
    // Fetch as blob with auth header:
    fetch(url, { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.blob())
      .then(blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = `room_type_forecast_${startDate.value}_to_${endDate.value}.xlsx`;
        a.click();
        URL.revokeObjectURL(a.href);
      })
      .catch(e => alert('Export failed: ' + e.message));
  }
};

const formatDisplayDate = (dateStr: string) => {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
  return `${d.getMonth() + 1}/${d.getDate()} ${weekdays[d.getDay()]}`;
};

onMounted(fetchData);
</script>

<style scoped>
@import "../../assets/admin.css";

.forecast-controls {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.date-picker-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.date-picker-row label {
  font-weight: 600;
  font-size: 14px;
  color: #333;
}

.date-picker-row input[type="date"] {
  padding: 6px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
}

.action-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.admin-btn {
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.admin-btn:hover {
  background: #f1f5f9;
}

.admin-btn.primary {
  background: #38bdf8;
  color: #fff;
  border-color: #38bdf8;
}

.admin-btn.primary:hover {
  background: #0ea5e9;
}

.admin-btn.export {
  background: #22c55e;
  color: #fff;
  border-color: #22c55e;
}

.admin-btn.export:hover {
  background: #16a34a;
}

.period-info {
  margin-bottom: 16px;
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.forecast-table {
  font-size: 13px;
  min-width: 100%;
}

.forecast-table th,
.forecast-table td {
  padding: 8px 10px;
  text-align: center;
  white-space: nowrap;
}

.type-col {
  text-align: left;
  min-width: 120px;
  position: sticky;
  left: 0;
  background: #fff;
  z-index: 2;
}

.total-col {
  min-width: 60px;
  position: sticky;
  left: 120px;
  background: #fff;
  z-index: 2;
  border-right: 2px solid #e2e8f0;
}

.date-header {
  font-size: 12px;
  color: #334155;
}

.sub-header {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 6px;
}

.sub-header.booked { color: #6366f1; }
.sub-header.maint { color: #f59e0b; }
.sub-header.avail { color: #22c55e; }

.num-cell {
  font-size: 13px;
  padding: 6px 8px;
}

.num-cell.booked { color: #6366f1; }
.num-cell.maint { color: #f59e0b; }
.num-cell.avail { color: #22c55e; }
.num-cell.avail.low { color: #ef4444; font-weight: 700; }

.total-row td {
  border-top: 2px solid #334155;
  background: #f1f5f9;
}

.bold { font-weight: 700; }

.loading-state {
  text-align: center;
  padding: 60px 0;
  color: #999;
}

.scroll-wrapper {
  overflow-x: auto;
}
</style>