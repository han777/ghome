<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input type="date" v-model="filterDate" @change="fetchData">
      </div>
      <button class="add-btn" @click="openModal()">+ 新增任务</button>
      <button class="refresh-btn" @click="generateTasks" title="生成当日任务">🔄 生成任务</button>
    </div>

    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
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
          <tr v-for="task in tasks" :key="task.id">
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
          <tr v-if="tasks.length === 0">
            <td colspan="10" class="empty-row">暂无清扫任务</td>
          </tr>
        </tbody>
      </table>
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
import { ref, onMounted } from 'vue';
import api from '../../utils/api';
import { getErrorMessageZh } from '../../utils/errorTranslate';

const tasks = ref<any[]>([]);
const rooms = ref<any[]>([]);
const dicts = ref<any[]>([]);
const filterDate = ref(new Date().toISOString().slice(0, 10));
const showModal = ref(false);
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
      api.get(`/cleaning-tasks/date/${filterDate.value}`),
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
      taskDate: filterDate.value,
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
  if (!confirm(`确认生成 ${filterDate.value} 的清扫任务？`)) return;
  try {
    const count = await api.post(`/cleaning-tasks/generate/${filterDate.value}`) as number;
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
