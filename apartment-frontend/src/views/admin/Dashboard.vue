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
        <button class="reset-btn" @click="resetFilters" title="清除全部筛选">重置</button>
      </div>

      <!-- Status Filter -->
      <div class="filter-group">
        <div class="filter-title"><span>|</span> 按房态</div>
        <div class="status-grid">
          <div class="status-chip chip-free" :class="{ active: statusFilters.includes(0) }" @click="toggleStatusFilter(0)">
            空净 <span class="chip-count">{{ data.statusCounts?.FREE || 0 }}</span>
          </div>
          <div class="status-chip chip-occupied" :class="{ active: statusFilters.includes(1) }" @click="toggleStatusFilter(1)">
            住净 <span class="chip-count">{{ data.statusCounts?.OCCUPIED || 0 }}</span>
          </div>
          <div class="status-chip chip-empty-dirty" :class="{ active: statusFilters.includes(4) }" @click="toggleStatusFilter(4)">
            空脏 <span class="chip-count">{{ data.statusCounts?.EMPTY_DIRTY || 0 }}</span>
          </div>
          <div class="status-chip chip-occupied-dirty" :class="{ active: statusFilters.includes(5) }" @click="toggleStatusFilter(5)">
            住脏 <span class="chip-count">{{ data.statusCounts?.OCCUPIED_DIRTY || 0 }}</span>
          </div>
          <div class="status-chip chip-repair" :class="{ active: maintTypeFilter === 1 }" @click="toggleMaintTypeFilter(1)">
            维修 <span class="chip-count">{{ data.statusCounts?.MAINT_REPAIR || 0 }}</span>
          </div>
          <div class="status-chip chip-locked" :class="{ active: maintTypeFilter === 2 }" @click="toggleMaintTypeFilter(2)">
            锁房 <span class="chip-count">{{ data.statusCounts?.MAINT_LOCKED || 0 }}</span>
          </div>
        </div>
      </div>

      <!-- Cleaning Type Filter -->
      <div class="filter-group" v-if="cleaningTypeCounts.daily + cleaningTypeCounts.deep > 0">
        <div class="filter-title"><span>|</span> 按清扫</div>
        <div class="cleaning-list">
          <div
            class="cleaning-item"
            :class="{ active: cleaningTypeFilter === 1 }"
            @click="toggleCleaningTypeFilter(1)"
          >
            <span class="cleaning-tag tag-daily">保洁</span>
            <span class="count">{{ cleaningTypeCounts.daily }}</span>
          </div>
          <div
            class="cleaning-item"
            :class="{ active: cleaningTypeFilter === 2 }"
            @click="toggleCleaningTypeFilter(2)"
          >
            <span class="cleaning-tag tag-deep">强打扫</span>
            <span class="count">{{ cleaningTypeCounts.deep }}</span>
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

      <!-- Purpose Filter - 按事由 -->
      <div class="filter-group" v-if="purposes.length > 0">
        <div class="filter-title"><span>|</span> 按事由</div>
        <div class="type-list">
          <div
            v-for="p in purposes"
            :key="p.id"
            class="type-item"
            :class="{ active: purposeFilter === p.id }"
            @click="togglePurposeFilter(p.id)"
          >
            <span>{{ p.name }}</span>
            <span class="count">{{ data.purposeCounts?.[p.name] || 0 }}</span>
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
                <div class="room-no-row">
                  <span class="room-no">{{ room.roomNo }}</span>
                  <span v-if="room.status === 2 && room.maintenanceType === 1" class="header-badge badge-repair">维修</span>
                  <span v-if="room.status === 2 && room.maintenanceType === 2" class="header-badge badge-locked">锁房</span>
                </div>
                <span class="room-type-tag">{{ room.roomTypeName }}</span>
              </div>
              <div class="header-right">
                <div class="room-header-menu" @click.stop="toggleRoomMenu($event, room)">
                  <span>⋯</span>
                </div>
              </div>
            </div>

            <!-- 内容区 -->
            <div class="room-body">
              <!-- 在住客人姓名（居中） -->
              <div v-if="room.guestName && (room.status === 1 || room.status === 5)" class="guest-name-center">
                {{ room.guestName }}
              </div>

              <!-- 彩色标签区 -->
              <div class="badge-area">
                <span v-if="room.arrivingDays != null" class="badge" :class="arrivingBadgeClass(room.arrivingDays)">{{ formatArrivingDays(room.arrivingDays) }}</span>
                <span v-if="room.departingDays != null && (room.status === 1 || room.status === 5)" class="badge" :class="departingBadgeClass(room.departingDays)">{{ formatDepartingDays(room.departingDays) }}</span>
                <template v-for="task in getPendingCleaningTasks(room)" :key="task.id">
                  <span class="badge" :class="task.taskType === 2 ? 'badge-deep-clean' : 'badge-daily-clean'">{{ task.taskType === 2 ? '强打扫' : '保洁' }}<span v-if="isTaskOverdue(task)" class="overdue-mark">!</span></span>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="filteredFloors.length === 0" class="empty-state">
        没有符合条件的房间
      </div>
    </main>

    <!-- 统一房间菜单弹窗 -->
    <div v-if="showRoomMenu" class="task-menu-popup" :style="roomMenuStyle" @click.stop>
      <div v-if="selectedRoomForMenu?.guestName" @click="viewCurrentOrder">查看当前订单</div>
      <div v-if="selectedRoomForMenu?.nearestArriving && !selectedRoomForMenu?.guestName" @click="viewArrivingOrder">查看预订订单</div>
      <div v-if="selectedRoomForMenu?.nearestDeparting" @click="viewDepartingOrder">查看离店订单</div>
      <div v-if="selectedRoomForMenu?.guestName || selectedRoomForMenu?.nearestArriving || selectedRoomForMenu?.nearestDeparting" class="menu-divider"></div>
      <div v-if="hasPendingCleaningTasks(selectedRoomForMenu)" @click="completeAllTasks">完成清扫</div>
      <div v-if="selectedRoomForMenu?.cleaningTasks?.length > 0" @click="editTask">编辑清扫任务</div>
      <div @click="addTask">新增清扫任务</div>
      <div class="menu-divider"></div>
      <div @click="navigateToCreateOrder">创建订单</div>
      <div v-if="selectedRoomForMenu?.status !== 2" @click="openQuickMaintenance">房间维护</div>
      <div v-if="selectedRoomForMenu?.status === 2" @click="openEditMaintenance">编辑维护</div>
    </div>

    <!-- 维护弹窗 -->
    <div v-if="showMaintenanceModal" class="modal-overlay" @click="showMaintenanceModal = false">
      <div class="modal-content" style="max-width: 450px;" @click.stop>
        <div class="modal-header">
          <h3>{{ maintenanceForm.id ? '编辑维护' : '房间维护' }} - {{ selectedRoomForMenu?.roomNo }}</h3>
          <button class="close-btn" @click="showMaintenanceModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-item">
            <label>维护类型</label>
            <select v-model="maintenanceForm.maintenanceType">
              <option :value="1">维修</option>
              <option :value="2">锁房</option>
            </select>
          </div>
          <div class="form-item">
            <label>开始时间</label>
            <input v-model="maintenanceForm.startTime" type="datetime-local">
          </div>
          <div class="form-item">
            <label>结束时间</label>
            <input v-model="maintenanceForm.endTime" type="datetime-local">
          </div>
          <div class="form-item">
            <label>维护内容</label>
            <textarea v-model="maintenanceForm.content" rows="2" placeholder="请输入维护详情..."></textarea>
          </div>
          <div class="form-item" v-if="maintenanceForm.id">
            <label>状态</label>
            <select v-model="maintenanceForm.status">
              <option :value="0">进行中</option>
              <option :value="1">已完成</option>
              <option :value="2">已取消</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showMaintenanceModal = false">取消</button>
          <button class="save-btn" @click="saveMaintenance">{{ maintenanceForm.id ? '保存' : '确认' }}</button>
        </div>
      </div>
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
  statusCounts: { FREE: 0, OCCUPIED: 0, MAINTENANCE: 0, MAINT_REPAIR: 0, MAINT_LOCKED: 0, EMPTY_DIRTY: 0, OCCUPIED_DIRTY: 0, OVERDUE_ARRIVING: 0, OVERDUE_DEPARTING: 0 },
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
const maintTypeFilter = ref<number | null>(null);
const cleaningTypeFilter = ref<number | null>(null);
const purposeFilter = ref<number | null>(null);
const purposes = ref<any[]>([]);

const resetFilters = () => {
  selectedFloor.value = '';
  searchQuery.value = '';
  statusFilters.value = [];
  typeFilters.value = [];
  arrivingDaysFilter.value = null;
  departingDaysFilter.value = null;
  maintTypeFilter.value = null;
  cleaningTypeFilter.value = null;
  purposeFilter.value = null;
};

// 房间菜单相关
const showRoomMenu = ref(false);
const roomMenuStyle = ref<Record<string, string>>({});
const selectedRoomForMenu = ref<any>(null);

// 维护弹窗相关
const showMaintenanceModal = ref(false);
const maintenanceForm = ref<any>({
  id: null,
  maintenanceType: 1,
  startTime: '',
  endTime: '',
  content: '',
  status: 0
});

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
  if (maintTypeFilter.value !== null) query.maintType = String(maintTypeFilter.value);
  if (cleaningTypeFilter.value !== null) query.cleanType = String(cleaningTypeFilter.value);
  if (purposeFilter.value !== null) query.purpose = String(purposeFilter.value);
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
  if (q.maintType) maintTypeFilter.value = parseInt(q.maintType as string);
  if (q.cleanType) cleaningTypeFilter.value = parseInt(q.cleanType as string);
  if (q.purpose) purposeFilter.value = parseInt(q.purpose as string);
};

// 监听筛选条件变化
watch([selectedFloor, searchQuery, statusFilters, typeFilters, arrivingDaysFilter, departingDaysFilter, maintTypeFilter, cleaningTypeFilter, purposeFilter], syncFiltersToUrl, { deep: true });

const fetchData = async () => {
  try {
    const [res, purposeRes] = await Promise.all([
      api.get('/dashboard/room-status'),
      api.get('/booking-purposes/all')
    ]) as any[];
    data.value = res;
    purposes.value = purposeRes || [];
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
  if (days === 0) return isArriving ? '今日抵' : '今日离';
  if (days < 0) return `${days}日${isArriving ? '抵' : '离'}`;
  return `${days}日${isArriving ? '抵' : '离'}`;
};

const formatArrivingDays = (days: number) => {
  if (days === 0) return '今日抵';
  return `${days}日抵`;
};

const arrivingBadgeClass = (days: number) => {
  if (days < 0) return 'badge-overdue-arriving';
  if (days === 0) return 'badge-arriving-today';
  if (days <= 3) return 'badge-arriving-soon';
  return 'badge-arriving-future';
};

const formatDepartingDays = (days: number) => {
  if (days === 0) return '今日离';
  return `${days}日离`;
};

const departingBadgeClass = (days: number) => {
  if (days < 0) return 'badge-overdue-departing';
  if (days === 0) return 'badge-departing-today';
  return 'badge-departing';
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

const toggleMaintTypeFilter = (type: number) => {
  maintTypeFilter.value = maintTypeFilter.value === type ? null : type;
};

const toggleCleaningTypeFilter = (type: number) => {
  cleaningTypeFilter.value = cleaningTypeFilter.value === type ? null : type;
};

const togglePurposeFilter = (id: number) => {
  purposeFilter.value = purposeFilter.value === id ? null : id;
};

const cleaningTypeCounts = computed(() => {
  let daily = 0;
  let deep = 0;
  if (data.value.rooms) {
    data.value.rooms.forEach((r: any) => {
      const pending = getPendingCleaningTasks(r);
      // 每个房间只计一次最高优先级类型（强打扫 > 保洁）
      if (pending.some((t: any) => t.taskType === 2)) deep++;
      else if (pending.some((t: any) => t.taskType === 1)) daily++;
    });
  }
  return { daily, deep };
});

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

    // Maintenance type filter
    if (maintTypeFilter.value !== null) {
      if (room.status !== 2 || room.maintenanceType !== maintTypeFilter.value) return false;
    }

    // Cleaning type filter
    if (cleaningTypeFilter.value !== null) {
      const pending = getPendingCleaningTasks(room);
      if (!pending.some((t: any) => t.taskType === cleaningTypeFilter.value)) return false;
    }

    // Purpose filter
    if (purposeFilter.value !== null && room.purposeId !== purposeFilter.value) return false;

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

  // Sort rooms within each floor by roomNo ascending
  groups.forEach((floor) => {
    floor.rooms.sort((a: any, b: any) => {
      const numA = parseInt(a.roomNo);
      const numB = parseInt(b.roomNo);
      if (!isNaN(numA) && !isNaN(numB)) return numA - numB;
      return (a.roomNo || '').localeCompare(b.roomNo || '');
    });
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
  if (status === 2) return 'status-maintenance';
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
  if (cleaningTypeFilter.value !== null) returnQuery.cleanType = String(cleaningTypeFilter.value);

  const returnPath = Object.keys(returnQuery).length > 0
    ? `/admin/dashboard?${new URLSearchParams(returnQuery).toString()}`
    : '/admin/dashboard';

  router.push({ path: '/admin/orders', query: { orderId, returnPath } });
};

// 房间菜单相关
const toggleRoomMenu = (event: MouseEvent, room: any) => {
  event.stopPropagation();
  selectedRoomForMenu.value = room;

  const rect = (event.target as HTMLElement).getBoundingClientRect();
  roomMenuStyle.value = {
    top: `${rect.bottom + 4}px`,
    left: `${rect.left - 100}px`
  };

  showRoomMenu.value = true;
};

const closeRoomMenu = () => {
  showRoomMenu.value = false;
};

const viewCurrentOrder = () => {
  if (!selectedRoomForMenu.value?.orderId) return;
  showRoomMenu.value = false;
  navigateToOrder(selectedRoomForMenu.value.orderId);
};

const viewArrivingOrder = () => {
  if (!selectedRoomForMenu.value?.nearestArriving?.orderId) return;
  showRoomMenu.value = false;
  navigateToOrder(selectedRoomForMenu.value.nearestArriving.orderId);
};

const viewDepartingOrder = () => {
  if (!selectedRoomForMenu.value?.nearestDeparting?.orderId) return;
  showRoomMenu.value = false;
  navigateToOrder(selectedRoomForMenu.value.nearestDeparting.orderId);
};

const todayStr = new Date().toISOString().slice(0, 10);

const getPendingCleaningTasks = (room: any) => {
  if (!room?.cleaningTasks) return [];
  return room.cleaningTasks.filter((t: any) => t.status === 0);
};

const hasPendingCleaningTasks = (room: any) => {
  return getPendingCleaningTasks(room).length > 0;
};

const isTaskOverdue = (task: any) => {
  return task.taskDate && task.taskDate < todayStr;
};

const completeAllTasks = async () => {
  if (!selectedRoomForMenu.value?.roomId) return;
  try {
    await api.post(`/cleaning-tasks/room/${selectedRoomForMenu.value.roomId}/complete-all`);
    showRoomMenu.value = false;
    fetchData();
  } catch (e) {
    console.error('Failed to complete tasks', e);
  }
};

const editTask = () => {
  if (!selectedRoomForMenu.value) return;
  // 优先编辑第一条未完成任务，如果没有则编辑第一条任务
  const tasks = selectedRoomForMenu.value.cleaningTasks || [];
  const task = tasks.find((t: any) => t.status === 0) || tasks[0];
  if (!task) return;
  taskForm.value = {
    id: task.id,
    roomId: selectedRoomForMenu.value.roomId,
    taskType: task.taskType,
    taskDate: task.taskDate || new Date().toISOString().slice(0, 10),
    content: task.content || '',
    status: task.status
  };
  showRoomMenu.value = false;
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
  showRoomMenu.value = false;
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

// 维护相关
const openQuickMaintenance = () => {
  if (!selectedRoomForMenu.value) return;
  const now = new Date();
  maintenanceForm.value = {
    id: null,
    maintenanceType: 1,
    startTime: new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 16),
    endTime: new Date(new Date(now.getTime() + 3 * 365 * 24 * 60 * 60 * 1000).getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 16),
    content: '',
    status: 0
  };
  showRoomMenu.value = false;
  showMaintenanceModal.value = true;
};

const openEditMaintenance = async () => {
  if (!selectedRoomForMenu.value) return;
  showRoomMenu.value = false;
  try {
    const res = await api.get(`/maintenances/room/${selectedRoomForMenu.value.roomId}`) as any[];
    const activeMaintenance = res.find(m => m.status === 0);
    if (!activeMaintenance) {
      alert('未找到进行中的维护记录');
      return;
    }
    maintenanceForm.value = {
      id: activeMaintenance.id,
      maintenanceType: activeMaintenance.maintenanceType || 1,
      startTime: activeMaintenance.startTime?.slice(0, 16) || '',
      endTime: activeMaintenance.endTime?.slice(0, 16) || '',
      content: activeMaintenance.content || '',
      status: activeMaintenance.status
    };
    showMaintenanceModal.value = true;
  } catch (e) {
    console.error('Failed to fetch maintenance', e);
    alert('获取维护记录失败');
  }
};

const saveMaintenance = async () => {
  try {
    const payload = {
      id: maintenanceForm.value.id,
      room: { id: selectedRoomForMenu.value?.roomId },
      maintenanceType: maintenanceForm.value.maintenanceType,
      startTime: maintenanceForm.value.startTime,
      endTime: maintenanceForm.value.endTime,
      content: maintenanceForm.value.content,
      status: maintenanceForm.value.status
    };
    await api.post('/maintenances', payload);
    showMaintenanceModal.value = false;
    fetchData();
  } catch (e: any) {
    console.error('Failed to save maintenance', e);
    alert(e.response?.data || '保存维护记录失败');
  }
};

// 快速创建订单 - 导航到入住管理页面
const navigateToCreateOrder = () => {
  if (!selectedRoomForMenu.value) return;
  showRoomMenu.value = false;

  const returnQuery: Record<string, string> = {};
  if (selectedFloor.value) returnQuery.floor = selectedFloor.value;
  if (searchQuery.value) returnQuery.search = searchQuery.value;
  if (statusFilters.value.length > 0) returnQuery.status = statusFilters.value.join(',');
  if (typeFilters.value.length > 0) returnQuery.type = typeFilters.value.join(',');
  if (arrivingDaysFilter.value !== null) returnQuery.arrDays = String(arrivingDaysFilter.value);
  if (departingDaysFilter.value !== null) returnQuery.depDays = String(departingDaysFilter.value);
  if (cleaningTypeFilter.value !== null) returnQuery.cleanType = String(cleaningTypeFilter.value);

  const returnPathValue = Object.keys(returnQuery).length > 0
    ? `/admin/dashboard?${new URLSearchParams(returnQuery).toString()}`
    : '/admin/dashboard';

  router.push({
    path: '/admin/orders',
    query: {
      autoCreate: 'true',
      roomId: String(selectedRoomForMenu.value.roomId),
      roomNo: selectedRoomForMenu.value.roomNo || '',
      returnPath: returnPathValue
    }
  });
};

onMounted(() => {
  restoreFiltersFromUrl();
  fetchData();
  document.addEventListener('click', closeRoomMenu);
});

onUnmounted(() => {
  document.removeEventListener('click', closeRoomMenu);
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

.reset-btn {
  margin-top: 6px;
  width: 100%;
  padding: 6px 0;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}
.reset-btn:hover {
  background: #f1f5f9;
  border-color: #94a3b8;
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
  justify-content: space-between;
  padding: 5px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #1e293b;
  cursor: pointer;
  border: 2px solid transparent;
  font-weight: 500;
}

.status-chip.active {
  border-color: #1e293b;
  font-weight: 700;
}

.chip-count {
  font-weight: 700;
}

.chip-free { background: #86efac; }
.chip-occupied { background: #bfdbfe; }
.chip-repair { background: #ef4444; color: #fff; }
.chip-locked { background: #94a3b8; color: #fff; }
.chip-empty-dirty { background: #a3d9a3; }
.chip-occupied-dirty { background: #c4915a; }

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
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  transition: all 0.2s;
  min-height: 120px;
}

.room-card:hover {
  box-shadow: 0 4px 6px rgba(0,0,0,0.15);
  transform: translateY(-2px);
}

/* Status header & body colors - solid fill */
.status-free .room-header { background: #86efac; }
.status-free .room-body { background: #86efac; }

.status-occupied .room-header { background: #bfdbfe; }
.status-occupied .room-body { background: #bfdbfe; }

.status-maintenance .room-header { background: #ef4444; }
.status-maintenance .room-body { background: #ef4444; }

.status-empty-dirty .room-header { background: #a3d9a3; }
.status-empty-dirty .room-body { background: #a3d9a3; }

.status-occupied-dirty .room-header { background: #c4915a; }
.status-occupied-dirty .room-body { background: #c4915a; }

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.room-header-menu {
  cursor: pointer;
  padding: 2px 6px;
  font-weight: 700;
  color: rgba(0,0,0,0.5);
  user-select: none;
  font-size: 16px;
  line-height: 1;
  border-radius: 4px;
}

.room-header-menu:hover {
  color: rgba(0,0,0,0.8);
  background: rgba(0,0,0,0.08);
}

.room-no {
  font-size: 26px;
  font-weight: 900;
  color: #1e293b;
  line-height: 1.1;
}

.room-no-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.room-type-tag {
  font-size: 10px;
  color: rgba(0,0,0,0.5);
  white-space: nowrap;
  line-height: 1;
}

.header-badge {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
  line-height: 1.4;
}

.room-body {
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  justify-content: center;
}

.guest-name-center {
  text-align: center;
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.badge-area {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
}

.badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  line-height: 1.4;
}

.badge-arriving-today {
  background: #3b82f6;
  color: #fff;
}
.badge-departing-today {
  background: #ec4899;
  color: #fff;
}
.badge-arriving-soon {
  background: #93c5fd;
  color: #1e40af;
}
.badge-arriving-future {
  background: #dbeafe;
  color: #3b82f6;
}
.badge-deep-clean {
  background: #f97316;
  color: #fff;
}
.badge-daily-clean {
  background: #94a3b8;
  color: #fff;
}
.overdue-mark {
  margin-left: 2px;
  font-weight: 900;
  color: #dc2626;
}
.badge-canceled {
  background: #e2e8f0;
  color: #64748b;
}
.badge-repair {
  background: #b91c1c;
  color: #fff;
}
.badge-locked {
  background: #64748b;
  color: #fff;
}
.badge-departing {
  background: rgba(236, 72, 153, 0.2);
  color: #be185d;
}
.badge-overdue-arriving {
  background: #dc2626;
  color: #fff;
  animation: pulse-badge 2s ease-in-out infinite;
}
.badge-overdue-departing {
  background: #d97706;
  color: #fff;
  animation: pulse-badge 2s ease-in-out infinite;
}
@keyframes pulse-badge {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

/* Cleaning type sidebar filter */
.cleaning-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.cleaning-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  color: #64748b;
}
.cleaning-item:hover {
  background: #f8fafc;
}
.cleaning-item.active {
  background: #fff7ed;
  color: #9a3412;
  font-weight: 600;
}
.cleaning-tag {
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 600;
}
.cleaning-tag.tag-daily {
  background: #e2e8f0;
  color: #475569;
}
.cleaning-tag.tag-deep {
  background: #ffedd5;
  color: #9a3412;
}

/* Task menu popup */
.task-menu-popup {
  position: fixed;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  padding: 4px 0;
  z-index: 1000;
  min-width: 140px;
}

.task-menu-popup div:not(.menu-divider) {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 13px;
  color: #374151;
}

.task-menu-popup div:not(.menu-divider):hover {
  background: #f3f4f6;
}

.menu-divider {
  height: 1px;
  background: #e2e8f0;
  margin: 4px 8px;
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
