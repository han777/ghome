<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input type="text" placeholder="Search accounts...">
      </div>
      <button class="add-btn" @click="openModal()">+ Add Account</button>
    </div>
    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Username</th>
            <th>Real Name</th>
            <th>Phone</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td class="name">{{ user.username }}</td>
            <td>{{ user.realName }}</td>
            <td>{{ user.phone || '-' }}</td>
            <td>
              <span class="status-badge" :class="{ active: user.status === 1 }">
                {{ user.status === 1 ? 'Active' : 'Disabled' }}
              </span>
            </td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(user)">Edit</button>
              <button class="delete-btn" @click="deleteUser(user.id)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- User Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ form.id ? 'Edit User' : 'Add User' }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form" @submit.prevent="saveUser">
            <div class="form-item">
              <label>Username</label>
              <input v-model="form.username" required>
            </div>
            <div class="form-item">
              <label>Real Name</label>
              <input v-model="form.realName">
            </div>
            <div class="form-item">
              <label>Phone</label>
              <input v-model="form.phone">
            </div>
            <div class="form-item">
              <label>Status</label>
              <select v-model="form.status">
                <option :value="1">Active</option>
                <option :value="0">Disabled</option>
              </select>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showModal = false">Cancel</button>
          <button class="save-btn" @click="saveUser">Save Changes</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import api from '../../utils/api';

const users = ref<any[]>([]);
const showModal = ref(false);
const form = reactive<any>({
  id: null,
  username: '',
  realName: '',
  phone: '',
  status: 1
});

const fetchUsers = async () => {
  try {
    const res: any = await api.get('/sys/users');
    users.value = res;
  } catch (e) {
    console.error('Failed to fetch users', e);
  }
};

const openModal = (user?: any) => {
  if (user) {
    Object.assign(form, user);
  } else {
    Object.assign(form, { id: null, username: '', realName: '', phone: '', status: 1 });
  }
  showModal.value = true;
};

const saveUser = async () => {
  try {
    await api.post('/sys/users', form);
    showModal.value = false;
    fetchUsers();
  } catch (e) {
    alert('Failed to save user');
  }
};

const deleteUser = async (id: number) => {
  if (!confirm('Are you sure you want to delete this user?')) return;
  try {
    await api.delete(`/sys/users/${id}`);
    fetchUsers();
  } catch (e) {
    alert('Failed to delete user');
  }
};

onMounted(fetchUsers);
</script>

<style scoped>
@import "../../assets/admin.css";
</style>
