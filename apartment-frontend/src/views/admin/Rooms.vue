<template>
  <div class="admin-page-with-sidebar">
    <!-- Filter Sidebar -->
    <div class="filter-sidebar">
      <div class="sidebar-header">楼层筛选</div>
      <div class="sidebar-content">
        <div 
          class="filter-item" 
          :class="{ active: !selectedFloorId }" 
          @click="selectedFloorId = null"
        >
          <span class="icon">🏢</span> 全部楼栋
        </div>
        <template v-for="b in buildings" :key="b.id">
          <div class="filter-item" style="font-weight: 600; background: #f8fafc; cursor: default;">
            {{ b.name }}
          </div>
          <div 
            v-for="f in b.floors" 
            :key="f.id" 
            class="filter-item" 
            :class="{ active: selectedFloorId === f.id }"
            @click="selectedFloorId = f.id"
            style="padding-left: 32px;"
          >
            {{ f.name }}
          </div>
        </template>
      </div>
    </div>

    <!-- Main Content -->
    <div class="main-content">
      <div class="page-header">
        <div class="search-bar">
          <input v-model="searchQuery" type="text" placeholder="Search rooms...">
        </div>
        <div class="header-actions">
          <div class="view-toggle">
            <button :class="{ active: viewMode === 'card' }" @click="viewMode = 'card'">🎴 Card</button>
            <button :class="{ active: viewMode === 'table' }" @click="viewMode = 'table'">📋 Table</button>
          </div>
          <button class="add-btn" @click="openModal()">+ Add Room</button>
        </div>
      </div>
      
      <!-- Table View -->
      <div v-if="viewMode === 'table'" class="table-card">
        <table class="admin-table">
          <thead>
            <tr>
              <th>Room No.</th>
              <th>Type</th>
              <th>Location</th>
              <th>Direction</th>
              <th>Area (㎡)</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="room in filteredRooms" :key="room.id">
              <td class="name">{{ room.roomNo }}</td>
              <td>
                <span class="tag">{{ room.roomType?.typeCode || '-' }}</span>
              </td>
              <td>
                {{ room.floor?.building?.name }} / {{ room.floor?.name }}
              </td>
              <td>{{ room.direction }}</td>
              <td>{{ room.area || '-' }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(room.status)">
                  {{ getDictLabel('ROOM_STATUS', room.status) }}
                </span>
              </td>
              <td class="actions">
                <button class="edit-btn" @click="openModal(room)">Edit</button>
                <button class="delete-btn" @click="deleteRoom(room.id)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Card View -->
      <div v-else class="room-card-grid">
        <div v-for="room in filteredRooms" :key="room.id" class="room-card" :class="getStatusClass(room.status) + '-border'">
          <div class="card-header">
            <span class="room-number">{{ room.roomNo }}</span>
            <span class="status-dot" :class="getStatusClass(room.status)"></span>
          </div>
          <div class="card-body">
            <div class="info-row">
              <span class="label">Location:</span>
              <span class="value">{{ room.floor?.name }}</span>
            </div>
            <div class="info-row">
              <span class="label">Type:</span>
              <span class="value">{{ room.roomType?.typeCode || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="label">Area:</span>
              <span class="value">{{ room.area }}㎡</span>
            </div>
            <div class="info-row">
              <span class="label">Dir:</span>
              <span class="value">{{ room.direction }}</span>
            </div>
          </div>
          <div class="card-footer">
            <button class="icon-btn edit" @click="openModal(room)" title="Edit">✏️</button>
            <button class="icon-btn delete" @click="deleteRoom(room.id)" title="Delete">🗑️</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Room Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ form.id ? 'Edit Room' : 'Add Room' }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form">
            <div class="form-item">
              <label>Room Number</label>
              <input v-model="form.roomNo" required>
            </div>
            <div class="form-item">
              <label>Location (Floor)</label>
              <select v-model="form.floorId">
                <option :value="null">-- Select Floor --</option>
                <optgroup v-for="b in buildings" :key="b.id" :label="b.name">
                  <option v-for="f in b.floors" :key="f.id" :value="f.id">
                    {{ f.name }}
                  </option>
                </optgroup>
              </select>
            </div>
            <div class="form-item">
              <label>Room Type</label>
              <select v-model="form.roomTypeId">
                <option :value="null">-- Select Room Type --</option>
                <option v-for="t in roomTypes" :key="t.id" :value="t.id">
                  {{ t.typeCode }}
                </option>
              </select>
            </div>
            <div class="form-item">
              <label>Direction</label>
              <select v-model="form.direction">
                <option value="SOUTH">South</option>
                <option value="NORTH">North</option>
                <option value="EAST">East</option>
                <option value="WEST">West</option>
              </select>
            </div>
            <div class="form-item">
              <label>Area (㎡)</label>
              <input v-model.number="form.area" type="number" step="0.1">
            </div>
            <div class="form-item">
              <label>Status</label>
              <select v-model="form.status">
                <option v-for="opt in getDictOptions('ROOM_STATUS')" :key="opt.value" :value="parseInt(opt.value)">
                  {{ opt.label }}
                </option>
              </select>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showModal = false">Cancel</button>
          <button class="save-btn" @click="saveRoom">Save Changes</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue';
import api from '../../utils/api';

const rooms = ref<any[]>([]);
const dicts = ref<any[]>([]);
const buildings = ref<any[]>([]);
const roomTypes = ref<any[]>([]);
const showModal = ref(false);
const searchQuery = ref('');
const selectedFloorId = ref<number | null>(null);
const viewMode = ref<'card' | 'table'>('card');

const form = reactive<any>({
  id: null,
  roomNo: '',
  direction: 'SOUTH',
  status: 0,
  floorId: null,
  roomTypeId: null,
  area: null
});

const fetchData = async () => {
  try {
    const [roomRes, dictRes, buildRes, typeRes] = await Promise.all([
      api.get('/rooms/all'),
      api.get('/sys/dict/all'),
      api.get('/buildings/all'),
      api.get('/room-types/all')
    ]) as any[];
    rooms.value = roomRes;
    dicts.value = dictRes;
    buildings.value = buildRes;
    roomTypes.value = typeRes;
  } catch (e) {
    console.error('Failed to fetch data', e);
  }
};

const filteredRooms = computed(() => {
  let list = rooms.value;
  if (selectedFloorId.value) {
    list = list.filter(r => r.floor?.id === selectedFloorId.value);
  }
  if (searchQuery.value) {
    list = list.filter(r => r.roomNo.includes(searchQuery.value));
  }
  return list;
});

const getDictLabel = (code: string, value: any) => {
  const dict = dicts.value.find(d => d.dictCode === code);
  if (!dict) return value;
  const item = dict.items.find((i: any) => i.itemValue === String(value));
  return item ? item.itemLabel : value;
};

const getDictOptions = (code: string) => {
  const dict = dicts.value.find(d => d.dictCode === code);
  return dict ? dict.items.map((i: any) => ({ label: i.itemLabel, value: i.itemValue })) : [];
};

const getStatusClass = (status: number) => {
  if (status === 0) return 'active';
  if (status === 1) return 'occupied';
  return 'repair';
};

const openModal = (room?: any) => {
  if (room) {
    Object.assign(form, room);
    form.floorId = room.floor?.id;
    form.roomTypeId = room.roomType?.id;
    form.area = room.area;
  } else {
    Object.assign(form, { id: null, roomNo: '', direction: 'SOUTH', status: 0, floorId: null, roomTypeId: null, area: null });
  }
  showModal.value = true;
};

const saveRoom = async () => {
  try {
    // Construct room object for backend (including floor and roomType objects)
    const payload = { ...form };
    if (form.floorId) {
      payload.floor = { id: form.floorId };
    }
    if (form.roomTypeId) {
      payload.roomType = { id: form.roomTypeId };
    }
    await api.post('/rooms', payload);
    showModal.value = false;
    fetchData();
  } catch (e) {
    alert('Failed to save room');
  }
};

const deleteRoom = async (id: number) => {
  if (!confirm('Delete this room?')) return;
  try {
    await api.delete(`/rooms/${id}`);
    fetchData();
  } catch (e) {
    alert('Failed to delete');
  }
};

onMounted(fetchData);
</script>

<style scoped>
@import "../../assets/admin.css";

.filter-item {
  white-space: nowrap;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.view-toggle {
  display: flex;
  background: #f1f5f9;
  padding: 4px;
  border-radius: 8px;
  gap: 4px;
}

.view-toggle button {
  padding: 6px 12px;
  border: none;
  background: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.view-toggle button.active {
  background: #fff;
  color: #0f172a;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

/* Card View Styles */
.room-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.room-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  border: 2px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.room-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
}

.room-card.active-border { border-color: #dcfce7; }
.room-card.occupied-border { border-color: #fef9c3; }
.room-card.repair-border { border-color: #fee2e2; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.room-number {
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-dot.active { background: #10b981; box-shadow: 0 0 8px rgba(16, 185, 129, 0.5); }
.status-dot.occupied { background: #f59e0b; box-shadow: 0 0 8px rgba(245, 158, 11, 0.5); }
.status-dot.repair { background: #ef4444; box-shadow: 0 0 8px rgba(239, 68, 68, 0.5); }

.card-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.info-row .label { color: #64748b; }
.info-row .value { font-weight: 600; color: #334155; }

.card-footer {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.icon-btn {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-btn:hover { background: #f1f5f9; }
.icon-btn.delete:hover { background: #fee2e2; color: #ef4444; }

.occupied { background: #fef9c3; color: #854d0e; }
.repair { background: #fee2e2; color: #991b1b; }
.tag { background: #e0f2fe; color: #0369a1; padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }
</style>
