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
            <span class="dot dot-free"></span>空净 {{ data.statusCounts?.FREE || 0 }}
          </div>
          <div class="status-chip" :class="{ active: statusFilters.includes(1) }" @click="toggleStatusFilter(1)">
            <span class="dot dot-occupied"></span>住净 {{ data.statusCounts?.OCCUPIED || 0 }}
          </div>
          <div class="status-chip" :class="{ active: statusFilters.includes(4) }" @click="toggleStatusFilter(4)">
            <span class="dot dot-empty-dirty"></span>空脏 {{ data.statusCounts?.EMPTY_DIRTY || 0 }}
          </div>
          <div class="status-chip" :class="{ active: statusFilters.includes(5) }" @click="toggleStatusFilter(5)">
            <span class="dot dot-occupied-dirty"></span>住脏 {{ data.statusCounts?.OCCUPIED_DIRTY || 0 }}
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

      <!-- Arriving Filter - 按抵达 -->
      <div class="filter-group">
        <div class="filter-title"><span>|</span> 按抵达</div>
        <div class="days-list">
          <div
            v-for="item in arrivingByDaysList"
            :key="item.days"
            class="days-item"
            :class="{ active: arrivingDaysFilter === item.days }"
            @click="toggleArrivingDaysFilter(item.days)"
          >
            <span class="days-tag">{{ item.label }}</span>
            <span class="count">{{ item.count }}</span>
          </div>
        </div>
      </div>

      <!-- Departing Filter - 按离开 -->
      <div class="filter-group">
        <div class="filter-title"><span>|</span> 按离开</div>
        <div class="days-list">
          <div
            v-for="item in departingByDaysList"
            :key="item.days"
            class="days-item"
            :class="{ active: departingDaysFilter === item.days }"
            @click="toggleDepartingDaysFilter(item.days)"
          >
            <span class="days-tag depart">{{ item.label }}</span>
            <span class="count">{{ item.count }}</span>
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
            <!-- 房间头部 -->
            <div class="room-header">
              <div class="header-left">
                <span class="room-no">{{ room.roomNo }}</span>
                <span class="room-type-tag">{{ room.roomTypeName }}</span>
              </div>
              <div class="header-right">
                <span v-if="room.status === 2" class="status-icon locked">锁</span>
                <span v-if="room.status === 3" class="status-icon repair">修</span>
              </div>
            </div>

            <!-- 内容区 -->
            <div class="room-body">
              <!-- 清扫任务条 -->
              <div class="task-bar" :class="{ 'has-task': room.cleaningTask, 'task-completed': room.cleaningTask?.status === 2 }">
                <div class="task-content">
                  <span class="task-icon">🧹</span>
                  <span v-if="room.cleaningTask" class="task-type">
                    {{ room.cleaningTask.taskType === 1 ? '日常保洁' : '强打扫' }}
                    <span v-if="room.cleaningTask.status === 1" class="task-canceled">(已取消)</span>
                    <span v-if="room.cleaningTask.status === 2" class="task-done">(已完成)</span>
                  </span>
                  <span v-else class="task-type no-task">无清扫任务</span>
                </div>
                <div class="task-menu" @click.stop="toggleTaskMenu($event, room)">
                  <span>•••</span>
                </div>
              </div>

              <!-- 最近到达订单条 -->
              <div class="order-bar arriving" v-if="room.nearestArriving" @click.stop="navigateToOrder(room.nearestArriving.orderId)">
                <span class="order-icon">🛬</span>
                <span class="order-info">
                  <span class="guest-name">{{ room.nearestArriving.guestName }}</span>
                  <span class="days-label">{{ formatDaysLabel(room.arrivingDays, true) }}</span>
                </span>
              </div>
              <div class="order-bar empty" v-else>
                <span class="order-icon">🛬</span>
                <span class="order-info no-order">暂无预订</span>
              </div>

              <!-- 最近离开订单条 -->
              <div class="order-bar departing" v-if="room.nearestDeparting" @click.stop="navigateToOrder(room.nearestDeparting.orderId)">
                <span class="order-icon">🛫</span>
                <span class="order-info">
                  <span class="guest-name">{{ room.nearestDeparting.guestName }}</span>
                  <span class="days-label">{{ formatDaysLabel(room.departingDays, false) }}</span>
                </span>
              </div>
              <div class="order-bar empty" v-else>
                <span class="order-icon">🛫</span>
                <span class="order-info no-order">-</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="filteredFloors.length === 0" class="empty-state">
        没有符合条件的房间
      </div>
    </main>

    <!-- 任务菜单弹窗 -->
    <div v-if="showTaskMenu" class="task-menu-popup" :style="taskMenuStyle" @click.stop>
      <div v-if="selectedRoomForMenu?.cleaningTask && selectedRoomForMenu?.cleaningTask?.status === 0" @click="completeTask">完成清扫</div>
      <div v-if="selectedRoomForMenu?.cleaningTask" @click="editTask">编辑任务</div>
      <div @click="addTask">新增清扫</div>
    </div>

    <!-- 清扫任务弹窗 -->
    <div v-if="showTaskModal" class="modal-overlay" @click="showTaskModal = false">
      <div class="modal-content" style="max-width: 450px;" @click.stop>
        <div class="modal-header">
          <h3>{{ taskForm.id ? '编辑清扫任务' : '新增清扫任务' }}</h3>
          <button class="close-btn" @click="showTaskModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-item">
            <label>房间</label>
            <input :value="selectedRoomForMenu?.roomNo" disabled>
          </div>
          <div class="form-item">
            <label>任务类型</label>
            <select v-model="taskForm.taskType">
              <option :value="1">日常保洁</option>
              <option :value="2">强打扫</option>
            </select>
          </div>
          <div class="form-item">
            <label>任务内容</label>
            <textarea v-model="taskForm.content" rows="2"></textarea>
          </div>
          <div class="form-item" v-if="taskForm.id">
            <label>状态</label>
            <select v-model="taskForm.status">
              <option :value="0">计划</option>
              <option :value="1">取消</option>
              <option :value="2">完成</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showTaskModal = false">取消</button>
          <button class="save-btn" @click="saveTask">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import api from '../../utils/api';

const router = useRouter();
const route = useRoute();

const data = ref<any>({
  arrivingToday: 0,
  departingToday: 0,
  arrivingInNDays: 0,
  statusCounts: { FREE: 0, OCCUPIED: 0, REPAIR: 0, LOCKED: 0, EMPTY_DIRTY: 0, OCCUPIED_DIRTY: 0 },
  rooms: [],
  arrivingByDays: {},
  departingByDays: {}
});

const selectedFloor = ref<string>('');
const searchQuery = ref<string>('');
const statusFilters = ref<number[]>([]);
const typeFilters = ref<string[]>([]);
const arrivingDaysFilter = ref<number | null>(null);
const departingDaysFilter = ref<number | null>(null);

// 任务菜单相关
const showTaskMenu = ref(false);
const taskMenuStyle = ref<Record<string, string>>({});
const selectedRoomForMenu = ref<any>(null);

// 任务弹窗相关
const showTaskModal = ref(false);
const taskForm = ref<any>({
  id: null,
  roomId: null,
  taskType: 1,
  taskDate: new Date().toISOString().slice(0, 10),
  content: '',
  status: 0
});

// 同步筛选条件到 URL
const syncFiltersToUrl = () => {
  const query: Record<string, string> = {};
  if (selectedFloor.value) query.floor = selectedFloor.value;
  if (searchQuery.value) query.search = searchQuery.value;
  if (statusFilters.value.length > 0) query.status = statusFilters.value.join(',');
  if (typeFilters.value.length > 0) query.type = typeFilters.value.join(',');
  if (arrivingDaysFilter.value !== null) query.arrDays = String(arrivingDaysFilter.value);
  if (departingDaysFilter.value !== null) query.depDays = String(departingDaysFilter.value);
  router.replace({ path: '/admin/dashboard', query });
};

// 从 URL 恢复筛选状态
const restoreFiltersFromUrl = () => {
  const q = route.query;
  if (q.floor) selectedFloor.value = q.floor as string;
  if (q.search) searchQuery.value = q.search as string;
  if (q.status) statusFilters.value = (q.status as string).split(',').map(Number).filter(n => !isNaN(n));
  if (q.type) typeFilters.value = (q.type as string).split(',');
  if (q.arrDays) arrivingDaysFilter.value = parseInt(q.arrDays as string);
  if (q.depDays) departingDaysFilter.value = parseInt(q.depDays as string);
};

// 监听筛选条件变化
watch([selectedFloor, searchQuery, statusFilters, typeFilters, arrivingDaysFilter, departingDaysFilter], syncFiltersToUrl, { deep: true });

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

// 按抵达天数列表
const arrivingByDaysList = computed(() => {
  const map = data.value.arrivingByDays || {};
  return Object.entries(map)
    .map(([days, count]) => ({
      days: parseInt(days),
      count: count as number,
      label: formatDaysLabel(parseInt(days), true)
    }))
    .sort((a, b) => a.days - b.days);
});

// 按离开天数列表
const departingByDaysList = computed(() => {
  const map = data.value.departingByDays || {};
  return Object.entries(map)
    .map(([days, count]) => ({
      days: parseInt(days),
      count: count as number,
      label: formatDaysLabel(parseInt(days), false)
    }))
    .sort((a, b) => a.days - b.days);
});

const formatDaysLabel = (days: number, isArriving: boolean) => {
  if (days === 0) return isArriving ? '今日达' : '今日离';
  return `${days}日${isArriving ? '达' : '离'}`;
};

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

const toggleArrivingDaysFilter = (days: number) => {
  arrivingDaysFilter.value = arrivingDaysFilter.value === days ? null : days;
};

const toggleDepartingDaysFilter = (days: number) => {
  departingDaysFilter.value = departingDaysFilter.value === days ? null : days;
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
      const matchArriving = room.nearestArriving?.guestName?.toLowerCase().includes(q);
      const matchDeparting = room.nearestDeparting?.guestName?.toLowerCase().includes(q);
      if (!matchNo && !matchName && !matchArriving && !matchDeparting) return false;
    }

    // Status filter
    if (statusFilters.value.length > 0 && !statusFilters.value.includes(room.status)) return false;

    // Room Type filter
    if (typeFilters.value.length > 0 && !typeFilters.value.includes(room.roomTypeName)) return false;

    // Arriving days filter
    if (arrivingDaysFilter.value !== null && room.arrivingDays !== arrivingDaysFilter.value) return false;

    // Departing days filter
    if (departingDaysFilter.value !== null && room.departingDays !== departingDaysFilter.value) return false;

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
  if (status === 0) return 'status-free';
  if (status === 1) return 'status-occupied';
  if (status === 2) return 'status-locked';
  if (status === 3) return 'status-repair';
  if (status === 4) return 'status-empty-dirty';
  if (status === 5) return 'status-occupied-dirty';
  return 'status-free';
};

// 导航到订单详情
const navigateToOrder = (orderId: number) => {
  if (!orderId) return;
  const returnQuery: Record<string, string> = {};
  if (selectedFloor.value) returnQuery.floor = selectedFloor.value;
  if (searchQuery.value) returnQuery.search = searchQuery.value;
  if (statusFilters.value.length > 0) returnQuery.status = statusFilters.value.join(',');
  if (typeFilters.value.length > 0) returnQuery.type = typeFilters.value.join(',');
  if (arrivingDaysFilter.value !== null) returnQuery.arrDays = String(arrivingDaysFilter.value);
  if (departingDaysFilter.value !== null) returnQuery.depDays = String(departingDaysFilter.value);

  const returnPath = Object.keys(returnQuery).length > 0
    ? `/admin/dashboard?${new URLSearchParams(returnQuery).toString()}`
    : '/admin/dashboard';

  router.push({ path: '/admin/orders', query: { orderId, returnPath } });
};

// 任务菜单相关
const toggleTaskMenu = (event: MouseEvent, room: any) => {
  event.stopPropagation();
  selectedRoomForMenu.value = room;

  const rect = (event.target as HTMLElement).getBoundingClientRect();
  taskMenuStyle.value = {
    top: `${rect.bottom + 4}px`,
    left: `${rect.left - 80}px`
  };

  showTaskMenu.value = true;
};

const closeTaskMenu = () => {
  showTaskMenu.value = false;
};

const completeTask = async () => {
  if (!selectedRoomForMenu.value?.cleaningTask?.id) return;
  try {
    await api.post(`/cleaning-tasks/${selectedRoomForMenu.value.cleaningTask.id}/complete`);
    showTaskMenu.value = false;
    fetchData();
  } catch (e) {
    console.error('Failed to complete task', e);
  }
};

const editTask = () => {
  if (!selectedRoomForMenu.value) return;
  const task = selectedRoomForMenu.value.cleaningTask;
  taskForm.value = {
    id: task.id,
    roomId: selectedRoomForMenu.value.roomId,
    taskType: task.taskType,
    taskDate: selectedRoomForMenu.value.taskDate || new Date().toISOString().slice(0, 10),
    content: task.content || '',
    status: task.status
  };
  showTaskMenu.value = false;
  showTaskModal.value = true;
};

const addTask = () => {
  if (!selectedRoomForMenu.value) return;
  taskForm.value = {
    id: null,
    roomId: selectedRoomForMenu.value.roomId,
    taskType: 1,
    taskDate: new Date().toISOString().slice(0, 10),
    content: '',
    status: 0
  };
  showTaskMenu.value = false;
  showTaskModal.value = true;
};

const saveTask = async () => {
  try {
    const payload = { ...taskForm.value };
    if (payload.roomId) {
      payload.room = { id: payload.roomId };
      delete payload.roomId;
    }
    await api.post('/cleaning-tasks', payload);
    showTaskModal.value = false;
    fetchData();
  } catch (e) {
    console.error('Failed to save task', e);
  }
};

onMounted(() => {
  restoreFiltersFromUrl();
  fetchData();
  document.addEventListener('click', closeTaskMenu);
});

onUnmounted(() => {
  document.removeEventListener('click', closeTaskMenu);
});
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
  margin-bottom: 20px;
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
  gap: 6px;
}

.status-chip {
  display: flex;
  align-items: center;
  padding: 5px 6px;
  background: #f8fafc;
  border-radius: 4px;
  font-size: 12px;
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
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 5px;
  flex-shrink: 0;
}

.dot-free { background: #22c55e; }
.dot-occupied { background: #38bdf8; }
.dot-repair { background: #ef4444; }
.dot-locked { background: #94a3b8; }
.dot-empty-dirty { background: #f97316; }
.dot-occupied-dirty { background: #eab308; }

.type-list, .days-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.type-item, .days-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 8px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  color: #64748b;
}

.type-item:hover, .days-item:hover {
  background: #f8fafc;
}

.type-item.active, .days-item.active {
  background: #e0f2fe;
  color: #0284c7;
  font-weight: 600;
}

.days-tag {
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 12px;
  background: #dbeafe;
  color: #1d4ed8;
}

.days-tag.depart {
  background: #fce7f3;
  color: #be185d;
}

.count {
  font-size: 12px;
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
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.room-card {
  border-radius: 6px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  transition: all 0.2s;
  min-height: 140px;
  background: #fff;
}

.room-card:hover {
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}

/* Status header colors */
.status-free .room-header { background: #86efac; }
.status-occupied .room-header { background: #7dd3fc; }
.status-locked .room-header { background: #64748b; }
.status-repair .room-header { background: #ef4444; }
.status-empty-dirty .room-header {
  background: repeating-linear-gradient(
    -45deg,
    #fdba74,
    #fdba74 8px,
    #fff 8px,
    #fff 16px
  );
}
.status-occupied-dirty .room-header {
  background: repeating-linear-gradient(
    -45deg,
    #fde047,
    #fde047 8px,
    #fff 8px,
    #fff 16px
  );
}

/* Status body border colors */
.status-free .room-body { border: 2px solid #86efac; border-top: none; }
.status-occupied .room-body { border: 2px solid #7dd3fc; border-top: none; }
.status-locked .room-body { border: 2px solid #64748b; border-top: none; }
.status-repair .room-body { border: 2px solid #ef4444; border-top: none; }
.status-empty-dirty .room-body { border: 2px solid #fdba74; border-top: none; }
.status-occupied-dirty .room-body { border: 2px solid #fde047; border-top: none; }

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.room-no {
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
}

.room-type-tag {
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 3px;
  background: rgba(0,0,0,0.15);
  color: #374151;
}

.status-icon {
  font-size: 10px;
  padding: 2px 4px;
  border-radius: 2px;
  font-weight: 600;
}
.status-icon.locked { background: #475569; color: white; }
.status-icon.repair { background: #b91c1c; color: white; }

.room-body {
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

/* Task bar */
.task-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 6px;
  border-radius: 4px;
  background: rgba(0,0,0,0.05);
  font-size: 11px;
}

.task-bar.has-task {
  background: rgba(59, 130, 246, 0.2);
}

.task-bar.task-completed {
  background: rgba(34, 197, 94, 0.2);
}

.task-content {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
}

.task-icon {
  font-size: 12px;
}

.task-type {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
}

.task-type.no-task {
  color: #94a3b8;
}

.task-canceled { color: #94a3b8; }
.task-done { color: #16a34a; }

.task-menu {
  cursor: pointer;
  padding: 0 4px;
  font-weight: 700;
  color: #64748b;
  user-select: none;
}

.task-menu:hover {
  color: #1e293b;
}

/* Order bar */
.order-bar {
  display: flex;
  align-items: center;
  padding: 4px 6px;
  border-radius: 4px;
  font-size: 11px;
  gap: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.order-bar:hover:not(.empty) {
  background: rgba(0,0,0,0.1);
}

.order-bar.arriving {
  background: rgba(59, 130, 246, 0.15);
}

.order-bar.departing {
  background: rgba(236, 72, 153, 0.15);
}

.order-bar.empty {
  background: rgba(0,0,0,0.03);
  cursor: default;
}

.order-icon {
  font-size: 12px;
}

.order-info {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
  flex: 1;
}

.guest-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
}

.days-label {
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 3px;
  background: rgba(0,0,0,0.1);
  white-space: nowrap;
}

.no-order {
  color: #94a3b8;
}

/* Task menu popup */
.task-menu-popup {
  position: fixed;
  background: white;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  padding: 4px 0;
  z-index: 1000;
  min-width: 120px;
}

.task-menu-popup div {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 13px;
  color: #374151;
}

.task-menu-popup div:hover {
  background: #f3f4f6;
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #1e293b;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #64748b;
}

.modal-body {
  padding: 20px;
}

.form-item {
  margin-bottom: 16px;
}

.form-item label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}

.form-item input,
.form-item select,
.form-item textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
}

.form-item textarea {
  resize: vertical;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
}

.cancel-btn {
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: white;
  cursor: pointer;
  font-size: 14px;
}

.save-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  background: #3b82f6;
  color: white;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}

.save-btn:hover {
  background: #2563eb;
}

.empty-state {
  text-align: center;
  color: #94a3b8;
  padding: 40px;
  font-size: 16px;
}
</style>
