<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <label>Room Filter:</label>
        <select v-model="roomIdFilter" @change="fetchData">
          <option value="">All Rooms</option>
          <option v-for="room in rooms" :key="room.id" :value="room.id">
            {{ room.roomNo }}
          </option>
        </select>
      </div>
      <button class="add-btn" @click="openModal()">+ Add Maintenance</button>
    </div>

    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Room No.</th>
            <th>Start Time</th>
            <th>End Time</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in filteredMaintenances" :key="m.id">
            <td>{{ m.id }}</td>
            <td>{{ m.room?.roomNo }}</td>
            <td>{{ formatDateTime(m.startTime) }}</td>
            <td>{{ formatDateTime(m.endTime) }}</td>
            <td>
              <span class="status-badge" :class="getStatusClass(m.status)">
                {{ getStatusLabel(m.status) }}
              </span>
            </td>
            <td class="actions">
              <button class="edit-btn" v-if="m.status === 0" @click="openModal(m)">Edit</button>
              <button class="delete-btn" v-if="m.status === 2" @click="deleteMaintenance(m.id)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ form.id ? 'Edit Maintenance' : 'Add Maintenance' }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form">
            <div class="form-item">
              <label>Room</label>
              <select v-model="form.roomId" :disabled="!!form.id">
                <option v-for="room in rooms" :key="room.id" :value="room.id">
                  {{ room.roomNo }}
                </option>
              </select>
            </div>
            <div class="form-item">
              <label>Start Time</label>
              <input v-model="form.startTime" type="datetime-local">
            </div>
            <div class="form-item">
              <label>End Time</label>
              <input v-model="form.endTime" type="datetime-local">
            </div>
            <div class="form-item">
              <label>Status</label>
              <select v-model="form.status" :disabled="form.status === 1 || form.status === 2">
                <option :value="0">In Progress</option>
                <option :value="1">Completed</option>
                <option :value="2">Cancelled</option>
              </select>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showModal = false">Cancel</button>
          <button class="save-btn" @click="saveMaintenance">Save</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue';
import { useRoute } from 'vue-router';
import api from '../../utils/api';

const route = useRoute();
const maintenances = ref<any[]>([]);
const rooms = ref<any[]>([]);
const showModal = ref(false);

const roomIdFilter = ref<string | number>(route.query.roomId ? Number(route.query.roomId) : '');

const form = reactive<any>({
  id: null,
  roomId: null,
  startTime: '',
  endTime: '',
  status: 0
});

const fetchData = async () => {
  try {
    const roomRes = await api.get('/rooms/all') as any[];
    rooms.value = roomRes;
    
    if (roomIdFilter.value) {
      maintenances.value = await api.get(`/maintenances/room/${roomIdFilter.value}`) as any[];
    } else {
      // API doesn't have an 'all' maintenances yet, we'll fetch all rooms' maintenances if we had it, but for now let's just make sure we can handle this.
      // Assuming we need to add a GET /api/maintenances/all endpoint or just list for a specific room.
      // Wait, there is no /maintenances/all, I should add it. Let's do it in a sec.
      const res = await api.get('/maintenances/all') as any[];
      maintenances.value = res;
    }
  } catch (e) {
    console.error('Failed to fetch data', e);
  }
};

const filteredMaintenances = computed(() => {
  return maintenances.value;
});

const formatDateTime = (dtStr: string) => {
  if (!dtStr) return '-';
  const d = new Date(dtStr);
  return d.toLocaleString();
};

const getStatusLabel = (status: number) => {
  if (status === 0) return '维修中';
  if (status === 1) return '已完成';
  if (status === 2) return '已取消';
  return '未知';
};

const getStatusClass = (status: number) => {
  if (status === 0) return 'occupied'; // orange
  if (status === 1) return 'active'; // green
  if (status === 2) return 'repair'; // red
  return '';
};

const formatForInput = (dtStr: string) => {
  if (!dtStr) return '';
  return dtStr.slice(0, 16); // format to yyyy-MM-ddThh:mm
};

const openModal = (m?: any) => {
  if (m) {
    form.id = m.id;
    form.roomId = m.room?.id;
    form.startTime = formatForInput(m.startTime);
    form.endTime = formatForInput(m.endTime);
    form.status = m.status;
  } else {
    form.id = null;
    form.roomId = roomIdFilter.value || null;
    const now = new Date();
    // Default to +8 timezone simple hack or just use local ISO string
    form.startTime = formatForInput(new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString());
    const end = new Date(now.getTime());
    end.setFullYear(end.getFullYear() + 3);
    form.endTime = formatForInput(new Date(end.getTime() - end.getTimezoneOffset() * 60000).toISOString());
    form.status = 0;
  }
  showModal.value = true;
};

const saveMaintenance = async () => {
  try {
    const payload = {
      id: form.id,
      room: { id: form.roomId },
      startTime: form.startTime,
      endTime: form.endTime,
      status: form.status
    };
    await api.post('/maintenances', payload);
    showModal.value = false;
    fetchData();
  } catch (e: any) {
    alert(e.response?.data || 'Failed to save');
  }
};

const deleteMaintenance = async (id: number) => {
  if (!confirm('Delete this record?')) return;
  try {
    await api.delete(`/maintenances/${id}`);
    fetchData();
  } catch (e: any) {
    alert(e.response?.data || 'Failed to delete');
  }
};

onMounted(fetchData);
</script>

<style scoped>
@import "../../assets/admin.css";

.search-bar { display: flex; align-items: center; gap: 8px; font-weight: 600; }
.search-bar select { padding: 8px; border: 1px solid #e2e8f0; border-radius: 6px; }

.status-badge { padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }
.status-badge.active { background: #dcfce7; color: #166534; }
.status-badge.occupied { background: #fef9c3; color: #854d0e; }
.status-badge.repair { background: #fee2e2; color: #991b1b; }
</style>
