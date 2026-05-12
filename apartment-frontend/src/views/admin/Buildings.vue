<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input type="text" placeholder="搜索楼栋...">
      </div>
      <button class="add-btn" @click="openModal()">+ 添加楼栋</button>
    </div>
    
    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>楼栋名称</th>
            <th>描述</th>
            <th>楼层</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="b in buildings" :key="b.id">
            <td class="name">{{ b.name }}</td>
            <td>{{ b.description || '-' }}</td>
            <td>
              <div class="item-tags">
                <span v-for="f in b.floors" :key="f.id" class="tag">
                  {{ f.name }}
                </span>
                <button class="add-tag-btn" @click="addFloor(b)">+</button>
              </div>
            </td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(b)">编辑</button>
              <button class="delete-btn" @click="deleteBuilding(b.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Building Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ form.id ? '编辑楼栋' : '添加楼栋' }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form">
            <div class="form-item">
              <label>楼栋名称</label>
              <input v-model="form.name" required>
            </div>
            <div class="form-item">
              <label>描述</label>
              <textarea v-model="form.description"></textarea>
            </div>
            <div class="form-item" v-if="form.id">
              <label>楼层</label>
              <div v-for="(f, index) in form.floors" :key="index" v-show="f._editFlag !== 'deleted'" style="display:flex; gap:8px; margin-bottom:8px;">
                <input v-model="f.name" placeholder="楼层名称" style="flex:1" @input="markFloorModified(f)">
                <span v-if="f._editFlag === 'added'" class="edit-badge add-badge">新</span>
                <span v-if="f._editFlag === 'modified'" class="edit-badge modify-badge">改</span>
                <button type="button" @click="deleteFloor(f)" class="delete-btn">×</button>
              </div>
              <button type="button" @click="addNewFloor()" class="add-btn" style="padding:4px 8px; font-size:12px;">+ 添加楼层</button>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showModal = false">不保存关闭</button>
          <button class="save-btn" @click="saveBuilding">保存更改</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import api from '../../utils/api';

const buildings = ref<any[]>([]);
const showModal = ref(false);
const form = reactive<any>({
  id: null,
  name: '',
  description: '',
  floors: []
});

const fetchBuildings = async () => {
  try {
    const res: any = await api.get('/buildings/all');
    buildings.value = res;
  } catch (e) {
    console.error('Failed to fetch buildings', e);
  }
};

const openModal = (building?: any) => {
  if (building) {
    const copy = JSON.parse(JSON.stringify(building));
    // 标记已有楼层为 'unchanged'，保存原始名称用于变更检测
    copy.floors = copy.floors.map((f: any) => ({ ...f, _editFlag: 'unchanged', _originalName: f.name }));
    Object.assign(form, copy);
  } else {
    Object.assign(form, { id: null, name: '', description: '', floors: [] });
  }
  showModal.value = true;
};

const saveBuilding = async () => {
  try {
    await api.post('/buildings', form);
    showModal.value = false;
    fetchBuildings();
  } catch (e) {
    alert('Failed to save building');
  }
};

const deleteFloor = (f: any) => {
  if (f.id) {
    // 已有楼层标记为删除，保留在数组中以便后端识别
    f._editFlag = 'deleted';
  } else {
    // 新增的楼层直接从数组移除（后端无需处理）
    const index = form.floors.indexOf(f);
    if (index !== -1) form.floors.splice(index, 1);
  }
};

const addNewFloor = () => {
  form.floors.push({ name: '', _editFlag: 'added' });
};

const markFloorModified = (f: any) => {
  if (f._editFlag === 'unchanged' && f.name !== f._originalName) {
    f._editFlag = 'modified';
  } else if (f._editFlag === 'modified' && f.name === f._originalName) {
    f._editFlag = 'unchanged';
  }
};

const deleteBuilding = async (id: number) => {
  if (!confirm('确定要删除此楼栋吗？所有关联楼层也将被移除。')) return;
  try {
    await api.delete(`/buildings/${id}`);
    fetchBuildings();
  } catch (e) {
    alert('Failed to delete');
  }
};

const addFloor = (b: any) => {
  const name = prompt('请输入楼层名称：');
  if (name) {
    api.post(`/buildings/${b.id}/floors`, { name }).then(() => fetchBuildings());
  }
};

onMounted(fetchBuildings);
</script>

<style scoped>
@import "../../assets/admin.css";
.item-tags { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.tag { background: #f1f5f9; color: #475569; padding: 4px 10px; border-radius: 100px; font-size: 12px; font-weight: 500; }
.add-tag-btn { background: #f1f5f9; border: 1px dashed #cbd5e1; border-radius: 50%; width: 24px; height: 24px; cursor: pointer; color: #64748b; }
.edit-badge { font-size: 10px; padding: 2px 6px; border-radius: 4px; font-weight: 600; line-height: 1; }
.add-badge { background: #dcfce7; color: #166534; }
.modify-badge { background: #fef3c7; color: #92400e; }
</style>
