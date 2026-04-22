<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input type="text" placeholder="Search dictionary...">
      </div>
      <button class="add-btn" @click="openModal()">+ Create Dictionary</button>
    </div>
    
    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Dict Code</th>
            <th>Dict Name</th>
            <th>Description</th>
            <th>Items</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="dict in dicts" :key="dict.id">
            <td class="code">{{ dict.dictCode }}</td>
            <td class="name">{{ dict.dictName }}</td>
            <td>{{ dict.description || '-' }}</td>
            <td>
              <div class="item-tags">
                <span v-for="item in dict.items" :key="item.id" class="tag">
                  {{ item.itemLabel }}: {{ item.itemValue }}
                </span>
              </div>
            </td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(dict)">Edit</button>
              <button class="delete-btn" @click="deleteDict(dict.id)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Dict Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ form.id ? 'Edit Dictionary' : 'Add Dictionary' }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form">
            <div class="form-item">
              <label>Dict Code</label>
              <input v-model="form.dictCode" required>
            </div>
            <div class="form-item">
              <label>Dict Name</label>
              <input v-model="form.dictName" required>
            </div>
            <div class="form-item">
              <label>Description</label>
              <textarea v-model="form.description"></textarea>
            </div>
            <div class="form-item">
              <label>Items (Label:Value)</label>
              <div v-for="(item, index) in form.items" :key="index" style="display:flex; gap:8px; margin-bottom:8px;">
                <input v-model="item.itemLabel" placeholder="Label" style="flex:1">
                <input v-model="item.itemValue" placeholder="Value" style="flex:1">
                <button type="button" @click="form.items.splice(index, 1)" class="delete-btn">×</button>
              </div>
              <button type="button" @click="form.items.push({itemLabel:'', itemValue:''})" class="add-btn" style="padding:4px 8px; font-size:12px;">+ Add Item</button>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showModal = false">Cancel</button>
          <button class="save-btn" @click="saveDict">Save Changes</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import api from '../../utils/api';

const dicts = ref<any[]>([]);
const showModal = ref(false);
const form = reactive<any>({
  id: null,
  dictCode: '',
  dictName: '',
  description: '',
  items: []
});

const fetchDicts = async () => {
  try {
    const res: any = await api.get('/sys/dict/all');
    dicts.value = res;
  } catch (e) {
    console.error('Failed to fetch dicts', e);
  }
};

const openModal = (dict?: any) => {
  if (dict) {
    Object.assign(form, JSON.parse(JSON.stringify(dict)));
  } else {
    Object.assign(form, { id: null, dictCode: '', dictName: '', description: '', items: [] });
  }
  showModal.value = true;
};

const saveDict = async () => {
  try {
    await api.post('/sys/dict', form);
    showModal.value = false;
    fetchDicts();
  } catch (e) {
    alert('Failed to save dictionary');
  }
};

const deleteDict = async (id: number) => {
  if (!confirm('Delete this dictionary?')) return;
  try {
    await api.delete(`/sys/dict/${id}`);
    fetchDicts();
  } catch (e) {
    alert('Failed to delete');
  }
};

onMounted(fetchDicts);
</script>

<style scoped>
@import "../../assets/admin.css";

.code { font-family: 'Fira Code', monospace; font-weight: 600; color: #0ea5e9; }
.name { font-weight: 500; }
.item-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.tag { background: #f1f5f9; color: #475569; padding: 4px 10px; border-radius: 100px; font-size: 12px; font-weight: 500; }
</style>
