<template>
  <div class="dashboard-layout">
    <aside class="sidebar-filter">
      <div class="filter-group">
        <select v-model="selectedFloor" class="floor-select">
          <option value="">全部楼层</option>
          <option v-for="floor in availableFloors" :key="floor.id" :value="floor.id">
            {{ floor.name }}
          </option>
        </select>
      </div>
      <div class="filter-group">
        <div class="search-box">
          <input type="text" v-model="searchQuery" placeholder="输入房号或姓名搜索">
        </div>
      </div>

      <!-- Status Filter -->
      <div class="filter-group">
        <div class="filter-title"><span>|</span> 按房态</div>
        <div class="status-grid">
          <div class="status-chip" :class="{ active: statusFilters.includes(0) }" @click="toggleStatusFilter(0)">
            <span class="dot dot-free"></span>空闲 {{ data.statusCounts?.FREE || 0 }}
          </div>
          <div class="status-chip" :class="{ active: statusFilters.includes(1) }" @click="toggleStatusFilter(1)">
            <span class="dot dot-occupied"></span>在住 {{ data.statusCounts?.OCCUPIED || 0 }}
          </div>
          <div class="status-chip" :class="{ active: statusFilters.includes(3) }" @click="toggleStatusFilter(3)">
            <span class="dot dot-repair"></span>维修 {{ data.statusCounts?.REPAIR || 0 }}
          </div>
          <div class="status-chip" :class="{ active: statusFilters.includes(2) }" @click="toggleStatusFilter(2)">
            <span class="dot dot-locked"></span>锁房 {{ data.statusCounts?.LOCKED || 0 }}
          </div>
        </div>
      </div>

      <!-- Room Type Filter -->
      <div class="filter-group">
        <div class="filter-title"><span>|</span> 按房型</div>
        <div class="type-list">
          <div 
            v-for="type in availableRoomTypes" 
            :key="type.name"
            class="type-item"
            :class="{ active: typeFilters.includes(type.name) }"
            @click="toggleTypeFilter(type.name)"
          >
            <span>{{ type.name }}</span>
            <span class="count">{{ type.count }}</span>
          </div>
        </div>
      </div>

      <!-- Label Filter -->
      <div class="filter-group">
        <div class="filter-title"><span>|</span> 按标签</div>
        <div class="label-list">
          <div class="label-item" :class="{ active: labelFilters.includes('ARRIVING_TODAY') }" @click="toggleLabelFilter('ARRIVING_TODAY')">
            <span class="label-tag blue">今日抵</span>
            <span class="count">{{ data.arrivingToday || 0 }}</span>
          </div>
          <div class="label-item" :class="{ active: labelFilters.includes('DEPARTING_TODAY') }" @click="toggleLabelFilter('DEPARTING_TODAY')">
            <span class="label-tag blue">今日离</span>
            <span class="count">{{ data.departingToday || 0 }}</span>
          </div>
          <div class="label-item" :class="{ active: labelFilters.includes('ARRIVING_SOON') }" @click="toggleLabelFilter('ARRIVING_SOON')">
            <span class="label-tag blue">未来3日抵达</span>
            <span class="count">{{ data.arrivingInNDays || 0 }}</span>
          </div>
        </div>
      </div>
    </aside>

    <main class="dashboard-content">
      <div v-for="floor in filteredFloors" :key="floor.id" class="floor-section">
        <div class="floor-title">{{ floor.name }}</div>
        <div class="room-grid">
          <div 
            v-for="room in floor.rooms" 
            :key="room.roomId"
            class="room-card"
            :class="getRoomStatusClass(room.status)"
          >
            <div class="room-header">
              <span class="room-no">{{ room.roomNo }}</span>
              <span v-if="room.status === 2" class="status-icon locked">锁</span>
              <span v-if="room.status === 3" class="status-icon repair">修</span>
            </div>
            <div class="room-type">{{ room.roomTypeName }}</div>
            <div class="room-guest" v-if="room.guestName">{{ room.guestName }}</div>
            
            <div class="room-tags" v-if="room.labels && room.labels.length > 0">
              <span v-if="room.labels.includes('ARRIVING_TODAY')" class="room-tag">今日抵</span>
              <span v-if="room.labels.includes('DEPARTING_TODAY')" class="room-tag">今日离</span>
              <span v-if="room.labels.includes('ARRIVING_SOON')" class="room-tag">即将抵</span>
            </div>
          </div>
        </div>
      </div>
      <div v-if="filteredFloors.length === 0" class="empty-state">
        没有符合条件的房间
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import api from '../../utils/api';

const data = ref<any>({
  arrivingToday: 0,
  departingToday: 0,
  arrivingInNDays: 0,
  statusCounts: { FREE: 0, OCCUPIED: 0, REPAIR: 0, LOCKED: 0 },
  rooms: []
});

const selectedFloor = ref<string>('');
const searchQuery = ref<string>('');
const statusFilters = ref<number[]>([]);
const typeFilters = ref<string[]>([]);
const labelFilters = ref<string[]>([]);

const fetchData = async () => {
  try {
    const res = await api.get('/dashboard/room-status');
    data.value = res;
  } catch (e) {
    console.error('Failed to fetch dashboard data', e);
  }
};

const availableFloors = computed(() => {
  const floorsMap = new Map();
  if (data.value.rooms) {
    data.value.rooms.forEach((r: any) => {
      if (r.floorId) {
        floorsMap.set(r.floorId, { id: r.floorId, name: r.floorName });
      }
    });
  }
  return Array.from(floorsMap.values()).sort((a, b) => {
    // Basic numeric sort if floor names are numbers
    const numA = parseInt(a.name);
    const numB = parseInt(b.name);
    if (!isNaN(numA) && !isNaN(numB)) return numB - numA;
    return b.name.localeCompare(a.name);
  });
});

const availableRoomTypes = computed(() => {
  const typeMap = new Map();
  if (data.value.rooms) {
    data.value.rooms.forEach((r: any) => {
      if (r.roomTypeName) {
        const count = typeMap.get(r.roomTypeName) || 0;
        typeMap.set(r.roomTypeName, count + 1);
      }
    });
  }
  return Array.from(typeMap.entries()).map(([name, count]) => ({ name, count }));
});

const toggleStatusFilter = (status: number) => {
  const index = statusFilters.value.indexOf(status);
  if (index > -1) statusFilters.value.splice(index, 1);
  else statusFilters.value.push(status);
};

const toggleTypeFilter = (type: string) => {
  const index = typeFilters.value.indexOf(type);
  if (index > -1) typeFilters.value.splice(index, 1);
  else typeFilters.value.push(type);
};

const toggleLabelFilter = (label: string) => {
  const index = labelFilters.value.indexOf(label);
  if (index > -1) labelFilters.value.splice(index, 1);
  else labelFilters.value.push(label);
};

const filteredRooms = computed(() => {
  if (!data.value.rooms) return [];
  return data.value.rooms.filter((room: any) => {
    // Floor filter
    if (selectedFloor.value && room.floorId !== selectedFloor.value) return false;
    
    // Search filter
    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase();
      const matchNo = room.roomNo && room.roomNo.toLowerCase().includes(q);
      const matchName = room.guestName && room.guestName.toLowerCase().includes(q);
      if (!matchNo && !matchName) return false;
    }

    // Status filter
    if (statusFilters.value.length > 0 && !statusFilters.value.includes(room.status)) return false;

    // Room Type filter
    if (typeFilters.value.length > 0 && !typeFilters.value.includes(room.roomTypeName)) return false;

    // Label filter
    if (labelFilters.value.length > 0) {
      if (!room.labels) return false;
      const hasLabel = labelFilters.value.some(l => room.labels.includes(l));
      if (!hasLabel) return false;
    }

    return true;
  });
});

const filteredFloors = computed(() => {
  const rooms = filteredRooms.value;
  const groups = new Map();
  
  rooms.forEach((room: any) => {
    const fId = room.floorId || 'unknown';
    const fName = room.floorName || '未知楼层';
    if (!groups.has(fId)) {
      groups.set(fId, { id: fId, name: fName, rooms: [] });
    }
    groups.get(fId).rooms.push(room);
  });
  
  return Array.from(groups.values()).sort((a, b) => {
    const numA = parseInt(a.name);
    const numB = parseInt(b.name);
    if (!isNaN(numA) && !isNaN(numB)) return numB - numA;
    return b.name.localeCompare(a.name);
  });
});

const getRoomStatusClass = (status: number) => {
  // 0: Available, 1: Occupied, 2: Locked, 3: Maintenance
  if (status === 0) return 'status-free';
  if (status === 1) return 'status-occupied';
  if (status === 2) return 'status-locked';
  if (status === 3) return 'status-repair';
  return 'status-free';
};

onMounted(fetchData);
</script>

<style scoped>
.dashboard-layout {
  display: flex;
  height: calc(100vh - 64px);
  background: #f1f5f9;
}

.sidebar-filter {
  width: 240px;
  background: white;
  padding: 16px;
  overflow-y: auto;
  border-right: 1px solid #e2e8f0;
}

.dashboard-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.filter-group {
  margin-bottom: 24px;
}

.floor-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  outline: none;
}

.search-box input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  outline: none;
}

.filter-title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 12px;
  color: #1e293b;
  display: flex;
  align-items: center;
}

.filter-title span {
  color: #3b82f6;
  margin-right: 6px;
  font-weight: 900;
}

.status-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.status-chip {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  background: #f8fafc;
  border-radius: 4px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  border: 1px solid transparent;
}

.status-chip.active {
  background: #e0f2fe;
  border-color: #7dd3fc;
  color: #0284c7;
  font-weight: 600;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 6px;
}

.dot-free { background: #cbd5e1; }
.dot-occupied { background: #38bdf8; }
.dot-repair { background: #ef4444; }
.dot-locked { background: #94a3b8; }

.type-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.type-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 8px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  color: #64748b;
}

.type-item:hover {
  background: #f8fafc;
}

.type-item.active {
  background: #e0f2fe;
  color: #0284c7;
  font-weight: 600;
}

.label-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.label-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
}

.label-item:hover {
  background: #f8fafc;
}

.label-item.active {
  background: #e0f2fe;
}

.label-tag {
  padding: 2px 8px;
  border-radius: 4px;
  color: white;
  font-size: 12px;
}
.label-tag.blue { background: #3b82f6; }

.count {
  font-size: 13px;
  color: #94a3b8;
}

/* Room Grid Section */
.floor-section {
  margin-bottom: 32px;
}

.floor-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 8px;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.room-card {
  height: 120px;
  border-radius: 4px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  position: relative;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  transition: all 0.2s;
}

.room-card:hover {
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}

/* Status colors based on prompt */
/* available卡片白色显示 */
.status-free { background: #ffffff; color: #64748b; }
.status-free .room-no { color: #3b82f6; }

/* occupied浅蓝色底色 */
.status-occupied { background: #7dd3fc; color: #ffffff; }
.status-occupied .room-no { color: #ffffff; }
.status-occupied .room-type { color: rgba(255,255,255,0.9); }

/* locked深灰色显示 */
.status-locked { background: #64748b; color: #ffffff; }
.status-locked .room-no { color: #ffffff; }

/* 维修状态加锁图示 (Redish background is common, but prompt doesn't specify color, let's use a distinct color like red-500) */
.status-repair { background: #ef4444; color: #ffffff; }
.status-repair .room-no { color: #ffffff; }


.room-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}

.room-no {
  font-size: 18px;
  font-weight: 800;
}

.status-icon {
  font-size: 10px;
  padding: 2px 4px;
  border-radius: 2px;
  font-weight: 600;
}
.status-icon.locked { background: #475569; color: white; }
.status-icon.repair { background: #b91c1c; color: white; }

.room-type {
  font-size: 12px;
  margin-bottom: 4px;
}

.room-guest {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.room-tags {
  margin-top: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.room-tag {
  background: rgba(255,255,255,0.3);
  color: #1e3a8a;
  border: 1px solid #bfdbfe;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
}

/* For tags on dark backgrounds, make them look nice */
.status-occupied .room-tag,
.status-repair .room-tag,
.status-locked .room-tag {
  background: #2563eb;
  color: white;
  border: none;
}

.empty-state {
  text-align: center;
  color: #94a3b8;
  padding: 40px;
  font-size: 16px;
}
</style>
