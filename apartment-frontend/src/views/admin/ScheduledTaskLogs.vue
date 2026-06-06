<template>
  <div class="admin-page">
    <!-- Filters -->
    <div class="detail-controls">
      <div class="filter-row">
        <label>任务名称</label>
        <input type="text" v-model="taskNameFilter" placeholder="输入任务名称" />
        <label>执行开始</label>
        <input type="date" v-model="executeFrom" />
        <label>执行结束</label>
        <input type="date" v-model="executeTo" />
        <button class="admin-btn primary" @click="fetchData(0)">查询</button>
        <button class="admin-btn" @click="resetFilters">复位</button>
      </div>
    </div>

    <!-- Page size selector -->
    <div class="size-row">
      <span>每页条数:</span>
      <select v-model="pageSize" @change="fetchData(0)">
        <option value="20">20</option>
        <option value="50">50</option>
        <option value="100">100</option>
      </select>
      <span class="total-info">总记录数: {{ totalElements }}</span>
    </div>

    <!-- Table -->
    <div class="table-card">
      <table class="admin-table detail-table">
        <thead>
          <tr>
            <th>#</th>
            <th>任务名称</th>
            <th>执行时间</th>
            <th>耗时(ms)</th>
            <th>状态</th>
            <th>失败原因</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, idx) in rows" :key="row.id">
            <td>{{ rowNumber(idx) }}</td>
            <td>{{ row.taskName }}</td>
            <td>{{ formatDT(row.executeTime) }}</td>
            <td>{{ row.duration }}</td>
            <td>{{ statusLabel(row.status) }}</td>
            <td>{{ row.failReason || '-' }}</td>
          </tr>
          <tr v-if="rows.length === 0">
            <td colspan="6" class="empty-cell">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="totalPages > 0">
      <button class="page-btn" :disabled="currentPage === 0" @click="fetchData(0)">&#171;</button>
      <button class="page-btn" :disabled="currentPage === 0" @click="fetchData(currentPage - 1)">&#8249;</button>

      <template v-for="p in visiblePages" :key="p">
        <button v-if="p === '...'" class="page-btn ellipsis">...</button>
        <button v-else class="page-btn" :class="{ active: p === currentPage }" @click="fetchData(p)">{{ p + 1 }}</button>
      </template>

      <button class="page-btn" :disabled="currentPage >= totalPages - 1" @click="fetchData(currentPage + 1)">&#8250;</button>
      <button class="page-btn" :disabled="currentPage >= totalPages - 1" @click="fetchData(totalPages - 1)">&#187;</button>

      <span class="jump-row">
        跳转到
        <input type="number" v-model.number="jumpPage" :min="1" :max="totalPages" class="jump-input" @keyup.enter="doJump" />
        <button class="admin-btn small" @click="doJump">跳转</button>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import api from '../../utils/api';

const rows = ref<any[]>([]);
const currentPage = ref(0);
const pageSize = ref(20);
const totalElements = ref(0);
const totalPages = ref(0);
const jumpPage = ref(1);

const taskNameFilter = ref('');
const executeFrom = ref('');
const executeTo = ref('');

const rowNumber = (idx: number) => currentPage.value * pageSize.value + idx + 1;

const statusLabel = (st: string) => {
  if (st === 'success') return '成功';
  if (st === 'fail') return '失败';
  return st || '-';
};

const fetchData = async (page: number) => {
  currentPage.value = page;
  let url = `/scheduled-task-logs/paged?page=${page}&size=${pageSize.value}`;
  if (taskNameFilter.value) url += `&taskName=${encodeURIComponent(taskNameFilter.value)}`;
  if (executeFrom.value) url += `&executeFrom=${executeFrom.value}T00:00:00`;
  if (executeTo.value) url += `&executeTo=${executeTo.value}T23:59:59`;
  try {
    const res: any = await api.get(url);
    rows.value = res.content || [];
    totalElements.value = res.totalElements || 0;
    totalPages.value = res.totalPages || 0;
  } catch (e) {
    console.error('Failed to fetch scheduled task logs', e);
  }
};

const resetFilters = () => {
  taskNameFilter.value = '';
  executeFrom.value = '';
  executeTo.value = '';
  fetchData(0);
};

const visiblePages = computed(() => {
  const total = totalPages.value;
  const current = currentPage.value;
  const pages: (number | string)[] = [];
  if (total <= 7) {
    for (let i = 0; i < total; i++) pages.push(i);
  } else {
    pages.push(0);
    if (current > 2) pages.push('...');
    const start = Math.max(1, current - 1);
    const end = Math.min(total - 2, current + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    if (current < total - 3) pages.push('...');
    pages.push(total - 1);
  }
  return pages;
});

const doJump = () => {
  if (jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    fetchData(jumpPage.value - 1);
  }
};

const formatDT = (dt: string) => {
  if (!dt) return '-';
  const d = new Date(dt);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`;
};

fetchData(0);
</script>

<style scoped>
@import "../../assets/admin.css";

.detail-controls {
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  margin-bottom: 12px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-row label {
  font-weight: 600;
  font-size: 14px;
  color: #333;
}

.filter-row input[type="text"],
.filter-row input[type="date"] {
  padding: 6px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
}

.admin-btn {
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.admin-btn:hover { background: #f1f5f9; }

.admin-btn.primary {
  background: #38bdf8;
  color: #fff;
  border-color: #38bdf8;
}

.admin-btn.primary:hover { background: #0ea5e9; }

.admin-btn.small {
  padding: 4px 10px;
  font-size: 12px;
}

.size-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #64748b;
}

.size-row select {
  padding: 4px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-size: 14px;
}

.total-info {
  font-weight: 600;
}

.detail-table {
  font-size: 14px;
}

.detail-table th,
.detail-table td {
  padding: 10px 12px;
  text-align: left;
}

.empty-cell {
  text-align: center;
  color: #999;
  padding: 40px 0;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 16px 0;
  flex-wrap: wrap;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) { background: #f1f5f9; }

.page-btn.active {
  background: #38bdf8;
  color: #fff;
  border-color: #38bdf8;
}

.page-btn:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.page-btn.ellipsis {
  cursor: default;
  border: none;
  min-width: 24px;
}

.jump-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: 12px;
  font-size: 14px;
  color: #64748b;
}

.jump-input {
  width: 60px;
  padding: 4px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-size: 14px;
  text-align: center;
}
</style>
