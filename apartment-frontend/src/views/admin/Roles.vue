<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input type="text" placeholder="搜索角色...">
      </div>
      <button class="add-btn" @click="openModal()">+ 添加角色</button>
    </div>
    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>角色代码</th>
            <th>角色名</th>
            <th>描述</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="role in roles" :key="role.id">
            <td class="code">{{ role.roleCode }}</td>
            <td class="name">{{ role.roleName }}</td>
            <td>{{ role.description || '-' }}</td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(role)">编辑</button>
              <button class="delete-btn" @click="deleteRole(role.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Role Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ form.id ? '编辑角色' : '添加角色' }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form">
            <div class="form-item">
              <label>角色代码</label>
              <input v-model="form.roleCode" required>
            </div>
            <div class="form-item">
              <label>角色名</label>
              <input v-model="form.roleName" required>
            </div>
            <div class="form-item">
              <label>描述</label>
              <textarea v-model="form.description"></textarea>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showModal = false">不保存关闭</button>
          <button class="save-btn" @click="saveRole">保存更改</button>
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
  if (!confirm('确定删除此角色？')) return;
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
