<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input v-model="searchQuery" type="text" placeholder="Search room types...">
      </div>
      <button class="add-btn" @click="openModal()">+ Add Type</button>
    </div>
    
    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Code</th>
            <th>Name (Multi-lang)</th>
            <th>Short Rent</th>
            <th>Long Rent</th>
            <th>Remarks</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="type in filteredTypes" :key="type.id">
            <td class="code">{{ type.typeCode }}</td>
            <td>
              <div v-for="(val, lang) in type.nameIntl" :key="lang">
                <span class="tag">{{ lang }}</span> {{ val }}
              </div>
            </td>
            <td>¥{{ type.priceShortRent }}</td>
            <td>¥{{ type.priceLongRent }}</td>
            <td>{{ type.remarks || '-' }}</td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(type)">Edit</button>
              <button class="delete-btn" @click="deleteType(type.id)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Type Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ form.id ? 'Edit Room Type' : 'Add Room Type' }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form">
            <div class="form-item">
              <label>Type Code</label>
              <input v-model="form.typeCode" placeholder="e.g. KING, SUITE" required>
            </div>
            <div class="form-item">
              <label>Names (JSON Map)</label>
              <textarea v-model="nameIntlJson" placeholder='{"zh": "大床房", "en": "King Room"}'></textarea>
            </div>
            <div class="form-item">
              <label>Short Rent Price</label>
              <input type="number" v-model="form.priceShortRent">
            </div>
            <div class="form-item">
              <label>Long Rent Price</label>
              <input type="number" v-model="form.priceLongRent">
            </div>
            <div class="form-item">
              <label>Image URL</label>
              <input v-model="form.imageUrl">
            </div>
            <div class="form-item">
              <label>Remarks</label>
              <textarea v-model="form.remarks"></textarea>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showModal = false">Cancel</button>
          <button class="save-btn" @click="saveType">Save Changes</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue';
import api from '../../utils/api';

const types = ref<any[]>([]);
const showModal = ref(false);
const searchQuery = ref('');
const nameIntlJson = ref('');

const form = reactive<any>({
  id: null,
  typeCode: '',
  nameIntl: {},
  priceShortRent: 0,
  priceLongRent: 0,
  imageUrl: '',
  remarks: ''
});

const fetchData = async () => {
  try {
    const res = await api.get('/room-types/all') as any;
    types.value = res;
  } catch (e) {
    console.error('Failed to fetch data', e);
  }
};

const filteredTypes = computed(() => {
  if (!searchQuery.value) return types.value;
  return types.value.filter(t => t.typeCode.toLowerCase().includes(searchQuery.value.toLowerCase()));
});

const openModal = (type?: any) => {
  if (type) {
    Object.assign(form, type);
    nameIntlJson.value = JSON.stringify(type.nameIntl || {}, null, 2);
  } else {
    Object.assign(form, { id: null, typeCode: '', nameIntl: {}, priceShortRent: 0, priceLongRent: 0, imageUrl: '', remarks: '' });
    nameIntlJson.value = '{"zh": "", "en": ""}';
  }
  showModal.value = true;
};

const saveType = async () => {
  try {
    form.nameIntl = JSON.parse(nameIntlJson.value);
    await api.post('/room-types', form);
    showModal.value = false;
    fetchData();
  } catch (e) {
    alert('Invalid JSON in Names or failed to save');
  }
};

const deleteType = async (id: number) => {
  if (!confirm('Delete this type?')) return;
  try {
    await api.delete(`/room-types/${id}`);
    fetchData();
  } catch (e) {
    alert('Failed to delete');
  }
};

onMounted(fetchData);
</script>

<style scoped>
@import "../../assets/admin.css";
.tag { background: #f1f5f9; color: #475569; padding: 1px 4px; border-radius: 4px; font-size: 10px; margin-right: 4px; border: 1px solid #e2e8f0; }
textarea { width: 100%; min-height: 80px; padding: 8px; border-radius: 6px; border: 1px solid #e2e8f0; font-family: monospace; }
</style>
