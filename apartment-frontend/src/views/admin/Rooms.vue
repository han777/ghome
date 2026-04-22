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
        <button class="add-btn" @click="openModal()">+ Add Room</button>
      </div>
      
      <div class="table-card">
        <table class="admin-table">
          <thead>
            <tr>
              <th>Room No.</th>
              <th>Location</th>
              <th>Direction</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="room in filteredRooms" :key="room.id">
              <td class="name">{{ room.roomNo }}</td>
              <td>
                {{ room.floor?.building?.name }} / {{ room.floor?.name }}
              </td>
              <td>{{ room.direction }}</td>
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
                <optgroup v-for="b in buildings" :key="b.id" :label="b.name">
                  <option v-for="f in b.floors" :key="f.id" :value="f.id">
                    {{ f.name }}
                  </option>
                </optgroup>
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
const showModal = ref(false);
const searchQuery = ref('');
const selectedFloorId = ref<number | null>(null);

const form = reactive<any>({
  id: null,
  roomNo: '',
  direction: 'SOUTH',
  status: 0,
  floorId: null
});

const fetchData = async () => {
  try {
    const [roomRes, dictRes, buildRes] = await Promise.all([
      api.get('/rooms/all'),
      api.get('/sys/dict/all'),
      api.get('/buildings/all')
    ]);
    rooms.value = roomRes as any[];
    dicts.value = dictRes as any[];
    buildings.value = buildRes as any[];
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
  } else {
    Object.assign(form, { id: null, roomNo: '', direction: 'SOUTH', status: 0, floorId: null });
  }
  showModal.value = true;
};

const saveRoom = async () => {
  try {
    // Construct room object for backend (including floor object)
    const payload = { ...form };
    if (form.floorId) {
      payload.floor = { id: form.floorId };
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
.occupied { background: #fef9c3; color: #854d0e; }
.repair { background: #fee2e2; color: #991b1b; }
</style>
