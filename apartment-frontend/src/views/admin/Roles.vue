<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input type="text" placeholder="Search roles...">
      </div>
      <button class="add-btn" @click="openModal()">+ Add Role</button>
    </div>
    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Role Code</th>
            <th>Role Name</th>
            <th>Description</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="role in roles" :key="role.id">
            <td class="code">{{ role.roleCode }}</td>
            <td class="name">{{ role.roleName }}</td>
            <td>{{ role.description || '-' }}</td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(role)">Edit</button>
              <button class="delete-btn" @click="deleteRole(role.id)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Role Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ form.id ? 'Edit Role' : 'Add Role' }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form">
            <div class="form-item">
              <label>Role Code</label>
              <input v-model="form.roleCode" required>
            </div>
            <div class="form-item">
              <label>Role Name</label>
              <input v-model="form.roleName" required>
            </div>
            <div class="form-item">
              <label>Description</label>
              <textarea v-model="form.description"></textarea>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showModal = false">Cancel</button>
          <button class="save-btn" @click="saveRole">Save Changes</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import api from '../../utils/api';

const roles = ref<any[]>([]);
const showModal = ref(false);
const form = reactive<any>({
  id: null,
  roleCode: '',
  roleName: '',
  description: ''
});

const fetchRoles = async () => {
  try {
    const res: any = await api.get('/sys/roles');
    roles.value = res;
  } catch (e) {
    console.error('Failed to fetch roles', e);
  }
};

const openModal = (role?: any) => {
  if (role) {
    Object.assign(form, role);
  } else {
    Object.assign(form, { id: null, roleCode: '', roleName: '', description: '' });
  }
  showModal.value = true;
};

const saveRole = async () => {
  try {
    await api.post('/sys/roles', form);
    showModal.value = false;
    fetchRoles();
  } catch (e) {
    alert('Failed to save role');
  }
};

const deleteRole = async (id: number) => {
  if (!confirm('Delete this role?')) return;
  try {
    await api.delete(`/sys/roles/${id}`);
    fetchRoles();
  } catch (e) {
    alert('Failed to delete role');
  }
};

onMounted(fetchRoles);
</script>

<style scoped>
@import "../../assets/admin.css";
.code { font-family: 'Fira Code', monospace; font-weight: 600; color: #0ea5e9; }
</style>
