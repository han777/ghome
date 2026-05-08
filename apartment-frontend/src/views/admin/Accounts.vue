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
            <th>Email</th>
            <th>Phone</th>
            <th>Roles</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td class="name">{{ user.username }}</td>
            <td>{{ user.realName }}</td>
            <td>{{ user.email || '-' }}</td>
            <td>{{ user.phone || '-' }}</td>
            <td>
              <div class="role-tags">
                <span v-for="role in user.roles" :key="role.id" class="role-tag">
                  {{ role.roleName }}
                </span>
                <span v-if="!user.roles || user.roles.length === 0">-</span>
              </div>
            </td>
            <td>
              <span class="status-badge" :class="{ active: user.status === 1 }">
                {{ user.status === 1 ? 'Active' : 'Disabled' }}
              </span>
            </td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(user)">Edit</button>
              <button class="edit-btn" style="background-color: #f59e0b; color: white; border-color: #f59e0b;" @click="promptChangePassword(user)">Password</button>
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
              <label>Email</label>
              <input v-model="form.email" type="email">
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
            <div class="form-item">
              <label>Roles</label>
              <div class="roles-selection">
                <label v-for="role in allRoles" :key="role.id" class="role-checkbox">
                  <input type="checkbox" :value="role.id" v-model="selectedRoleIds">
                  {{ role.roleName }}
                </label>
              </div>
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
  email: '',
  phone: '',
  status: 1,
  roles: []
});
const allRoles = ref<any[]>([]);
const selectedRoleIds = ref<number[]>([]);

const promptChangePassword = async (user: any) => {
  const newPass = prompt(`Enter new password for ${user.username}:`, 'password123');
  if (newPass) {
    try {
      await api.post(`/sys/users/${user.id}/password`, { password: newPass });
      alert('Password updated successfully');
    } catch (e) {
      alert('Failed to update password');
    }
  }
};

const fetchRoles = async () => {
  try {
    const res: any = await api.get('/sys/roles');
    allRoles.value = res;
  } catch (e) {
    console.error('Failed to fetch roles', e);
  }
};

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
    selectedRoleIds.value = user.roles ? user.roles.map((r: any) => r.id) : [];
  } else {
    Object.assign(form, { id: null, username: '', realName: '', email: '', phone: '', status: 1, roles: [] });
    selectedRoleIds.value = [];
  }
  showModal.value = true;
};

const saveUser = async () => {
  try {
    // Map selected IDs back to role objects for the backend
    form.roles = selectedRoleIds.value.map(id => ({ id }));
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

onMounted(() => {
  fetchUsers();
  fetchRoles();
});
</script>

<style scoped>
@import "../../assets/admin.css";

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.role-tag {
  background: #e0f2fe;
  color: #0369a1;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 500;
}

.roles-selection {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 8px;
  background: #f8fafc;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.role-checkbox {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.9rem;
  cursor: pointer;
}

.role-checkbox input {
  width: auto;
  margin: 0;
}
</style>
