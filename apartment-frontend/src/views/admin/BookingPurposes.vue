<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input v-model="searchQuery" type="text" placeholder="搜索事由名称...">
      </div>
      <button class="add-btn" @click="openModal()">+ 新建事由</button>
    </div>

    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>事由名称</th>
            <th>业务类型</th>
            <th>系统预设</th>
            <th>排序</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredPurposes" :key="item.id">
            <td>{{ item.name }}</td>
            <td>
              <span class="tag" :class="item.bizType === 1 ? 'short-rent' : 'long-rent'">
                {{ item.bizType === 1 ? '短租' : '长租' }}
              </span>
            </td>
            <td>
              <span v-if="item.isSystem" class="tag system">系统预设</span>
              <span v-else class="tag custom">自定义</span>
            </td>
            <td>{{ item.sortOrder }}</td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(item)">编辑</button>
              <button v-if="!item.isSystem" class="delete-btn" @click="deletePurpose(item.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Purpose Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ form.id ? '编辑事由' : '创建事由' }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form">
            <div class="form-item">
              <label class="required">事由名称</label>
              <input v-model="form.name" required placeholder="输入事由名称">
            </div>
            <div class="form-item">
              <label>业务类型</label>
              <select v-model="form.bizType" :disabled="form.isSystem">
                <option :value="1">短租</option>
                <option :value="2">长租</option>
              </select>
              <small v-if="form.isSystem" class="hint">系统预设事由不可修改业务类型</small>
            </div>
            <div class="form-item">
              <label>排序</label>
              <input type="number" v-model="form.sortOrder" placeholder="数字越小越靠前">
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showModal = false">不保存关闭</button>
          <button class="save-btn" @click="savePurpose">保存更改</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue';
import api from '../../utils/api';

const purposes = ref<any[]>([]);
const showModal = ref(false);
const searchQuery = ref('');
const form = reactive<any>({
  id: null,
  name: '',
  bizType: 1,
  isSystem: false,
  sortOrder: 0
});

const fetchData = async () => {
  try {
    const res = await api.get('/booking-purposes/all') as any;
    purposes.value = res;
  } catch (e) {
    console.error('Failed to fetch booking purposes', e);
  }
};

const filteredPurposes = computed(() => {
  if (!searchQuery.value) return purposes.value;
  return purposes.value.filter(p =>
    p.name.toLowerCase().includes(searchQuery.value.toLowerCase())
  );
});

const openModal = (item?: any) => {
  if (item) {
    Object.assign(form, {
      id: item.id,
      name: item.name,
      bizType: item.bizType,
      isSystem: item.isSystem,
      sortOrder: item.sortOrder
    });
  } else {
    Object.assign(form, {
      id: null,
      name: '',
      bizType: 1,
      isSystem: false,
      sortOrder: purposes.value.length
    });
  }
  showModal.value = true;
};

const savePurpose = async () => {
  if (!form.name.trim()) {
    alert('请输入事由名称');
    return;
  }
  try {
    await api.post('/booking-purposes', { ...form });
    showModal.value = false;
    fetchData();
  } catch (e: any) {
    const msg = e?.response?.data?.error || '保存失败';
    alert(msg);
  }
};

const deletePurpose = async (id: number) => {
  if (!confirm('确定要删除该事由吗？')) return;
  try {
    await api.delete(`/booking-purposes/${id}`);
    fetchData();
  } catch (e: any) {
    const msg = e?.response?.data?.error || '删除失败';
    alert(msg);
  }
};

onMounted(fetchData);
</script>

<style scoped>
@import "../../assets/admin.css";
.tag.short-rent { background: #dbeafe; color: #1e40af; }
.tag.long-rent { background: #fef3c7; color: #92400e; }
.tag.system { background: #e0e7ff; color: #3730a3; }
.tag.custom { background: #f1f5f9; color: #64748b; }
.hint { color: #94a3b8; font-size: 12px; margin-top: 4px; display: block; }
</style>
