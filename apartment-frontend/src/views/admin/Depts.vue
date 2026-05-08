<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input type="text" placeholder="Search departments...">
      </div>
      <button class="add-btn" @click="openModal()">+ Add Department</button>
    </div>
    
    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Department Name</th>
            <th>Sort Order</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="dept in depts" :key="dept.id">
            <td :style="{ paddingLeft: (getDepth(dept) * 24 + 24) + 'px' }">
              <span class="icon">🏢</span> {{ dept.deptName }}
            </td>
            <td>{{ dept.sortOrder }}</td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(dept)">编辑</button>
              <button class="delete-btn" @click="deleteDept(dept.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Dept Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ form.id ? 'Edit Department' : 'Add Department' }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form">
            <div class="form-item">
              <label>Department Name</label>
              <input v-model="form.deptName" required>
            </div>
            <div class="form-item">
              <label>Parent Department</label>
              <select v-model="form.parentId">
                <option :value="null">None (Root)</option>
                <option v-for="d in depts" :key="d.id" :value="d.id" :disabled="d.id === form.id">
                  {{ d.deptName }}
                </option>
              </select>
            </div>
            <div class="form-item">
              <label>Sort Order</label>
              <input type="number" v-model="form.sortOrder">
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showModal = false">取消</button>
          <button class="save-btn" @click="saveDept">保存更改</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import api from '../../utils/api';

const depts = ref<any[]>([]);
const showModal = ref(false);
const form = reactive<any>({
  id: null,
  deptName: '',
  parentId: null,
  sortOrder: 0
});

const fetchDepts = async () => {
  try {
    const res: any = await api.get('/sys/depts');
    const flatList = res;
    
    // Build tree structure
    const map: Record<number, any> = {};
    const roots: any[] = [];
    
    flatList.forEach((item: any) => {
      item.children = [];
      map[item.id] = item;
    });
    
    flatList.forEach((item: any) => {
      if (item.parentId && map[item.parentId]) {
        map[item.parentId].children.push(item);
      } else {
        roots.push(item);
      }
    });

    // Flatten tree with DFS to ensure children follow parents
    const sorted: any[] = [];
    const traverse = (list: any[], depth: number) => {
      // Sort children by sortOrder
      list.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
      list.forEach(node => {
        node.depth = depth;
        sorted.push(node);
        if (node.children.length > 0) {
          traverse(node.children, depth + 1);
        }
      });
    };
    
    traverse(roots, 0);
    depts.value = sorted;
  } catch (e) {
    console.error('Failed to fetch departments', e);
  }
};

const getDepth = (dept: any) => {
  return dept.depth || 0;
};

const openModal = (dept?: any) => {
  if (dept) {
    Object.assign(form, dept);
  } else {
    Object.assign(form, { id: null, deptName: '', parentId: null, sortOrder: 0 });
  }
  showModal.value = true;
};

const saveDept = async () => {
  try {
    await api.post('/sys/depts', form);
    showModal.value = false;
    fetchDepts();
  } catch (e) {
    alert('Failed to save department');
  }
};

const deleteDept = async (id: number) => {
  if (!confirm('Delete this department?')) return;
  try {
    await api.delete(`/sys/depts/${id}`);
    fetchDepts();
  } catch (e) {
    alert('Failed to delete');
  }
};

onMounted(fetchDepts);
</script>

<style scoped>
@import "../../assets/admin.css";
.icon { margin-right: 8px; color: #94a3b8; }
</style>
