<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input type="text" placeholder="账号搜索..." v-model="searchQuery" @input="fetchUsers(0)" @keyup.enter="fetchUsers(0)">
      </div>
      <button class="add-btn" @click="openModal()">+ 添加账号</button>
    </div>

    <!-- Page size selector -->
    <div class="size-row">
      <span>每页条数:</span>
      <select v-model="pageSize" @change="fetchUsers(0)">
        <option :value="20">20</option>
        <option :value="50">50</option>
        <option :value="100">100</option>
      </select>
      <span class="total-info">总记录数: {{ totalElements }}</span>
    </div>

    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>用户名</th>
            <th>姓名</th>
            <th>邮箱</th>
            <th>电话</th>
            <th>角色</th>
            <th>语言</th>
            <th>来源</th>
            <th>状态</th>
            <th>操作</th>
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
            <td>{{ localeLabel(user.locale) }}</td>
            <td>{{ getDictLabel('USER_SOURCE', user.source) }}</td>
            <td>
              <span class="status-badge" :class="{ active: user.status === 1 }">
                {{ user.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(user)">编辑</button>
              <button class="edit-btn" style="background-color: #f59e0b; color: white; border-color: #f59e0b;" @click="promptChangePassword(user)">密码</button>
              <button class="delete-btn" @click="deleteUser(user.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="totalPages > 0">
      <button class="page-btn" :disabled="currentPage === 0" @click="fetchUsers(0)">«</button>
      <button class="page-btn" :disabled="currentPage === 0" @click="fetchUsers(currentPage - 1)">‹</button>

      <template v-for="p in visiblePages" :key="p">
        <button v-if="p === '...'" class="page-btn ellipsis">...</button>
        <button v-else class="page-btn" :class="{ active: p === currentPage }" @click="fetchUsers(p)">{{ p + 1 }}</button>
      </template>

      <button class="page-btn" :disabled="currentPage >= totalPages - 1" @click="fetchUsers(currentPage + 1)">›</button>
      <button class="page-btn" :disabled="currentPage >= totalPages - 1" @click="fetchUsers(totalPages - 1)">»</button>

      <span class="jump-row">
        跳转到
        <input type="number" v-model.number="jumpPage" :min="1" :max="totalPages" class="jump-input" @keyup.enter="doJump" />
        <button class="page-btn" style="padding: 0 10px; min-width: auto; height: 28px; font-size: 12px; margin-left: 4px;" @click="doJump">跳转</button>
      </span>
    </div>

    <!-- User Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ form.id ? '编辑用户' : '添加用户' }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form" @submit.prevent="saveUser">
            <div class="form-item">
              <label>用户名</label>
              <input v-model="form.username" required>
            </div>
            <div class="form-item">
              <label>姓名</label>
              <input v-model="form.realName">
            </div>
            <div class="form-item">
              <label>邮箱</label>
              <input v-model="form.email" type="email">
            </div>
            <div class="form-item">
              <label>电话</label>
              <input v-model="form.phone">
            </div>
            <div class="form-item">
              <label>状态</label>
              <select v-model="form.status">
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
            </div>
            <div class="form-item">
              <label>语言</label>
              <select v-model="form.locale">
                <option value="zh">简体中文</option>
                <option value="en">English</option>
                <option value="ja">日本語</option>
              </select>
            </div>
            <div class="form-item">
              <label>角色</label>
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
          <button class="cancel-btn" @click="showModal = false">不保存关闭</button>
          <button class="save-btn" @click="saveUser">保存更改</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue';
import api from '../../utils/api';
import { getErrorMessageZh } from '../../utils/errorTranslate';

const users = ref<any[]>([]);
const dicts = ref<any[]>([]);
const showModal = ref(false);
const form = reactive<any>({
  id: null,
  username: '',
  realName: '',
  email: '',
  phone: '',
  status: 1,
  locale: 'zh',
  roles: []
});
const allRoles = ref<any[]>([]);
const selectedRoleIds = ref<number[]>([]);

// Pagination variables
const currentPage = ref(0);
const pageSize = ref(20);
const totalElements = ref(0);
const totalPages = ref(0);
const jumpPage = ref(1);
const searchQuery = ref('');

const localeLabel = (locale: string) => {
  const map: Record<string, string> = {
    zh: '简体中文',
    en: 'English',
    ja: '日本語'
  };
  return map[locale] || locale || '-';
};

const dictLabelZh: Record<string, Record<string, string>> = {
  USER_SOURCE: { 'Manual': '人工', 'WeCom': '企业微信' }
};

const getDictLabel = (code: string, value: any) => {
  const dict = dicts.value.find(d => d.dictCode === code);
  if (!dict) return value || '-';
  const item = dict.items.find((i: any) => i.itemValue === String(value));
  if (!item) return value || '-';
  return dictLabelZh[code]?.[item.itemLabel] || item.itemLabel;
};

const promptChangePassword = async (user: any) => {
  const newPass = prompt(`输入新密码 (${user.username}):`, 'password123');
  if (newPass) {
    try {
      await api.post(`/sys/users/${user.id}/password`, { password: newPass });
      alert('密码更新成功');
    } catch (e) {
      alert('密码更新失败');
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

const fetchUsers = async (page = 0) => {
  currentPage.value = page;
  try {
    let url = `/sys/users/paged?page=${page}&size=${pageSize.value}`;
    if (searchQuery.value) {
      url += `&search=${encodeURIComponent(searchQuery.value)}`;
    }
    const res: any = await api.get(url);
    users.value = res.content || [];
    totalElements.value = res.totalElements || 0;
    totalPages.value = res.totalPages || 0;
  } catch (e) {
    console.error('Failed to fetch users', e);
  }
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
    fetchUsers(jumpPage.value - 1);
  }
};

const openModal = (user?: any) => {
  if (user) {
    Object.assign(form, user);
    selectedRoleIds.value = user.roles ? user.roles.map((r: any) => r.id) : [];
  } else {
    delete form.source;
    Object.assign(form, { id: null, username: '', realName: '', email: '', phone: '', status: 1, locale: 'zh', roles: [] });
    selectedRoleIds.value = [];
  }
  showModal.value = true;
};

const saveUser = async () => {
  try {
    form.roles = selectedRoleIds.value.map(id => ({ id }));
    await api.post('/sys/users', form);
    showModal.value = false;
    fetchUsers(currentPage.value);
  } catch (e: any) {
    alert('保存失败: ' + getErrorMessageZh(e));
  }
};

const deleteUser = async (id: number) => {
  if (!confirm('确定要删除该用户吗？')) return;
  try {
    await api.delete(`/sys/users/${id}`);
    fetchUsers(currentPage.value);
  } catch (e: any) {
    alert('删除失败: ' + getErrorMessageZh(e));
  }
};

onMounted(async () => {
  const [, dictRes] = await Promise.all([
    fetchUsers(0),
    api.get('/sys/dict/all')
  ]) as any[];
  dicts.value = dictRes;
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

/* Pagination & Paging styles */
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
