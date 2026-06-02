<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>清扫任务</h2>
      <div class="header-actions">
        <button class="add-btn" @click="openModal()">+ 新增任务</button>
        <button class="refresh-btn" @click="generateTasks" title="生成当日任务">🔄 生成任务</button>
      </div>
    </div>

    <!-- Search Panel -->
    <div class="search-panel card">
      <div class="search-grid">
        <div class="search-item">
          <label>房间号</label>
          <select v-model="filters.roomId" @change="page = 1">
            <option :value="null">-- 全部房间 --</option>
            <option v-for="r in rooms" :key="r.id" :value="r.id">{{ r.roomNo }}</option>
          </select>
        </div>
        <div class="search-item">
          <label>任务类型</label>
          <select v-model="filters.taskType" @change="page = 1">
            <option :value="null">-- 全部类型 --</option>
            <option v-for="opt in getDictOptions('CLEANING_TASK_TYPE')" :key="opt.value" :value="parseInt(opt.value)">
              {{ opt.label }}
            </option>
          </select>
        </div>
        <div class="search-item">
          <label>状态</label>
          <select v-model="filters.status" @change="page = 1">
            <option :value="null">-- 全部状态 --</option>
            <option v-for="opt in getDictOptions('CLEANING_TASK_STATUS')" :key="opt.value" :value="parseInt(opt.value)">
              {{ opt.label }}
            </option>
          </select>
        </div>
        <div class="search-item">
          <label>任务时间</label>
          <div class="range-input">
            <input type="date" v-model="filters.taskDateFrom">
            <span>至</span>
            <input type="date" v-model="filters.taskDateTo">
          </div>
        </div>
      </div>
      <div class="search-actions">
        <button class="reset-btn" @click="resetFilters">重置</button>
        <button class="search-btn" @click="page = 1">查询</button>
      </div>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <span>共 {{ filteredTasks.length }} 条记录</span>
          <div class="page-size-selector">
            每页显示:
            <select v-model="pageSize" @change="page = 1">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
              <option :value="100">100</option>
            </select>
          </div>
        </div>
        <div class="pagination mini" v-if="totalPages > 1">
          <button :disabled="page === 1" @click="page--">上一页</button>
          <span class="page-info">第 {{ page }} / {{ totalPages }} 页</span>
          <button :disabled="page === totalPages" @click="page++">下一页</button>
        </div>
      </div>
      <table class="admin-table">
        <thead>
          <tr>
            <th>序号</th>
            <th>房间号</th>
            <th>任务类型</th>
            <th>状态</th>
            <th>任务日期</th>
            <th>生成时间</th>
            <th>开始时间</th>
            <th>结束时间</th>
            <th>报工时间</th>
            <th>任务内容</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(task, index) in paginatedTasks" :key="task.id">
            <td>{{ (page - 1) * pageSize + index + 1 }}</td>
            <td>{{ task.room?.roomNo || '-' }}</td>
            <td>
              <span class="tag" :class="task.taskType === 1 ? 'daily' : 'deep'">
                {{ getTaskTypeLabel(task.taskType) }}
              </span>
            </td>
            <td>
              <span class="status-badge" :class="getStatusClass(task.status)">
                {{ getStatusLabel(task.status) }}
              </span>
            </td>
            <td>{{ task.taskDate }}</td>
            <td>{{ formatDateTime(task.generatedAt) }}</td>
            <td>{{ formatDateTime(task.startTime) }}</td>
            <td>{{ formatDateTime(task.endTime) }}</td>
            <td>{{ formatDateTime(task.completedAt) }}</td>
            <td>{{ task.content || '-' }}</td>
            <td class="actions">
              <button v-if="task.status === 0" class="complete-btn" @click="completeTask(task.id)">完成</button>
              <button class="edit-btn" @click="openModal(task)">编辑</button>
              <button class="delete-btn" @click="deleteTask(task.id)">删除</button>
            </td>
          </tr>
          <tr v-if="paginatedTasks.length === 0">
            <td colspan="11" class="empty-row">暂无清扫任务</td>
          </tr>
        </tbody>
      </table>

      <div class="pagination" v-if="totalPages > 1">
        <button :disabled="page === 1" @click="page--">上一页</button>
        <span class="page-info">第 {{ page }} / {{ totalPages }} 页</span>
        <button :disabled="page === totalPages" @click="page++">下一页</button>
        <div class="jump-to">
          跳转至: <input type="number" v-model.number="jumpPage" @keyup.enter="goToPage" min="1" :max="totalPages">
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content" style="max-width: 500px;">
        <div class="modal-header">
          <h2>{{ form.id ? '编辑任务' : '新增任务' }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="admin-form">
            <div class="form-item">
              <label>房间</label>
              <select v-model="form.roomId" :disabled="!!form.id">
                <option :value="null">-- 选择房间 --</option>
                <option v-for="r in rooms" :key="r.id" :value="r.id">
                  {{ r.roomNo }}
                </option>
              </select>
            </div>
            <div class="form-item">
              <label>任务类型</label>
              <select v-model="form.taskType" :disabled="!!form.id">
                <option v-for="opt in getDictOptions('CLEANING_TASK_TYPE')" :key="opt.value" :value="parseInt(opt.value)">
                  {{ opt.label }}
                </option>
              </select>
            </div>
            <div class="form-item">
              <label>任务日期</label>
              <input type="date" v-model="form.taskDate" :disabled="!!form.id">
            </div>
            <div class="form-item">
              <label>开始时间</label>
              <input type="datetime-local" v-model="form.startTime">
            </div>
            <div class="form-item">
              <label>结束时间</label>
              <input type="datetime-local" v-model="form.endTime">
            </div>
            <div class="form-item">
              <label>报工时间</label>
              <input type="datetime-local" v-model="form.completedAt">
            </div>
            <div class="form-item">
              <label>任务内容</label>
              <textarea v-model="form.content" rows="3"></textarea>
            </div>
            <div class="form-item">
              <label>状态</label>
              <select v-model="form.status">
                <option v-for="opt in getDictOptions('CLEANING_TASK_STATUS')" :key="opt.value" :value="parseInt(opt.value)">
                  {{ opt.label }}
                </option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showModal = false">取消</button>
          <button class="save-btn" @click="saveTask">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue';
import api from '../../utils/api';
import { getErrorMessageZh } from '../../utils/errorTranslate';

const tasks = ref<any[]>([]);
const rooms = ref<any[]>([]);
const dicts = ref<any[]>([]);
const showModal = ref(false);
const page = ref(1);
const pageSize = ref(20);
const jumpPage = ref(1);

const filters = reactive({
  roomId: null as number | null,
  taskType: null as number | null,
  status: 0 as number | null,
  taskDateFrom: '',
  taskDateTo: ''
});

const resetFilters = () => {
  filters.roomId = null;
  filters.taskType = null;
  filters.status = 0;
  filters.taskDateFrom = '';
  filters.taskDateTo = '';
  page.value = 1;
};

const form = ref<any>({
  id: null,
  roomId: null,
  taskType: 1,
  taskDate: new Date().toISOString().slice(0, 10),
  startTime: null,
  endTime: null,
  completedAt: null,
  content: '',
  status: 0
});

const fetchData = async () => {
  try {
    const [taskRes, roomRes, dictRes] = await Promise.all([
      api.get('/cleaning-tasks/all'),
      api.get('/rooms/all'),
      api.get('/sys/dict/all')
    ]) as any[];
    tasks.value = taskRes || [];
    rooms.value = roomRes || [];
    dicts.value = dictRes || [];
  } catch (e) {
    console.error('Failed to fetch data', e);
  }
};

const filteredTasks = computed(() => {
  if (!Array.isArray(tasks.value)) return [];
  return tasks.value.filter(t => {
    if (filters.roomId !== null && t.room?.id !== filters.roomId) return false;
    if (filters.taskType !== null && t.taskType !== filters.taskType) return false;
    if (filters.status !== null && t.status !== filters.status) return false;
    if (filters.taskDateFrom && t.taskDate < filters.taskDateFrom) return false;
    if (filters.taskDateTo && t.taskDate > filters.taskDateTo) return false;
    return true;
  });
});

const totalPages = computed(() => Math.ceil(filteredTasks.value.length / pageSize.value));

const paginatedTasks = computed(() => {
  const start = (page.value - 1) * pageSize.value;
  return filteredTasks.value.slice(start, start + pageSize.value);
});

const goToPage = () => {
  if (jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    page.value = jumpPage.value;
  }
};

const getDictLabel = (code: string, value: any) => {
  const dict = dicts.value.find(d => d.dictCode === code);
  if (!dict) return value;
  const item = dict.items.find((i: any) => i.itemValue === String(value));
  return item?.itemLabel || value;
};

const getDictOptions = (code: string) => {
  const dict = dicts.value.find(d => d.dictCode === code);
  if (!dict) return [];
  return dict.items.map((i: any) => ({
    label: i.itemLabel,
    value: i.itemValue
  }));
};

const getTaskTypeLabel = (type: number) => getDictLabel('CLEANING_TASK_TYPE', type);
const getStatusLabel = (status: number) => getDictLabel('CLEANING_TASK_STATUS', status);

const getStatusClass = (status: number) => {
  switch (status) {
    case 0: return 'pending';
    case 1: return 'cancelled';
    case 2: return 'completed';
    default: return '';
  }
};

const formatDateTime = (dt: string) => {
  if (!dt) return '-';
  return dt.replace('T', ' ').slice(0, 16);
};

const openModal = (task?: any) => {
  if (task) {
    form.value = {
      ...task,
      roomId: task.room?.id || task.roomId,
      startTime: task.startTime ? task.startTime.slice(0, 16) : null,
      endTime: task.endTime ? task.endTime.slice(0, 16) : null,
      completedAt: task.completedAt ? task.completedAt.slice(0, 16) : null
    };
  } else {
    form.value = {
      id: null,
      roomId: null,
      taskType: 1,
      taskDate: new Date().toISOString().slice(0, 10),
      startTime: null,
      endTime: null,
      completedAt: null,
      content: '',
      status: 0
    };
  }
  showModal.value = true;
};

const saveTask = async () => {
  try {
    const payload = { ...form.value };
    if (payload.roomId) {
      payload.room = { id: payload.roomId };
      delete payload.roomId;
    }
    await api.post('/cleaning-tasks', payload);
    showModal.value = false;
    fetchData();
  } catch (e: any) {
    alert('保存失败: ' + getErrorMessageZh(e));
  }
};

const completeTask = async (id: number) => {
  if (!confirm('确认完成此任务？')) return;
  try {
    await api.post(`/cleaning-tasks/${id}/complete`);
    fetchData();
  } catch (e: any) {
    alert('操作失败: ' + getErrorMessageZh(e));
  }
};

const deleteTask = async (id: number) => {
  if (!confirm('确认删除此任务？')) return;
  try {
    await api.delete(`/cleaning-tasks/${id}`);
    fetchData();
  } catch (e: any) {
    alert('删除失败: ' + getErrorMessageZh(e));
  }
};

const generateTasks = async () => {
  const dateStr = prompt('请输入要生成任务的日期（格式: YYYY-MM-DD）：', new Date().toISOString().slice(0, 10));
  if (!dateStr) return;
  if (!confirm(`确认生成 ${dateStr} 的清扫任务？`)) return;
  try {
    const count = await api.post(`/cleaning-tasks/generate/${dateStr}`) as number;
    alert(`成功生成 ${count} 个任务`);
    fetchData();
  } catch (e: any) {
    alert('生成失败: ' + getErrorMessageZh(e));
  }
};

onMounted(fetchData);
</script>

<style scoped>
@import "../../assets/admin.css";

.search-panel {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

.search-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.search-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-item label {
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
}

.search-item input[type="date"],
.search-item select {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
}

.range-input {
  display: flex;
  align-items: center;
  gap: 10px;
}

.range-input input {
  flex: 1;
}

.search-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  border-top: 1px solid #f1f5f9;
  padding-top: 20px;
}

.reset-btn, .search-btn {
  padding: 8px 24px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.reset-btn {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #64748b;
}

.reset-btn:hover {
  background: #e2e8f0;
}

.search-btn {
  background: #38bdf8;
  border: 1px solid #38bdf8;
  color: #fff;
}

.search-btn:hover {
  background: #0ea5e9;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  font-size: 14px;
  color: #64748b;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.page-size-selector select {
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 24px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.pagination.mini {
  padding: 10px;
  background: #fff;
  border-top: none;
  border-bottom: 1px solid #e2e8f0;
  justify-content: flex-end;
  gap: 10px;
  font-size: 13px;
}

.pagination.mini button {
  padding: 4px 10px;
  font-size: 12px;
}

.pagination button {
  padding: 6px 16px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #fff;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.jump-to input {
  width: 50px;
  padding: 4px;
  text-align: center;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
}

.tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}
.tag.daily {
  background: #dcfce7;
  color: #166534;
}
.tag.deep {
  background: #fee2e2;
  color: #991b1b;
}

.status-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}
.status-badge.pending {
  background: #fef3c7;
  color: #92400e;
}
.status-badge.cancelled {
  background: #f1f5f9;
  color: #64748b;
}
.status-badge.completed {
  background: #dcfce7;
  color: #166534;
}

.refresh-btn {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
  margin-right: 12px;
}
.refresh-btn:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #0f172a;
}

.complete-btn {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #bbf7d0;
  border-radius: 4px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.complete-btn:hover {
  background: #bbf7d0;
}
</style>
