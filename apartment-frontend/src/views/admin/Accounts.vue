<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input type="text" :placeholder="$t('accounts.searchPlaceholder')">
      </div>
      <button class="add-btn" @click="openModal()">+ {{ $t('accounts.addAccount').replace('+ ', '') }}</button>
    </div>
    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>{{ $t('accounts.username') }}</th>
            <th>{{ $t('accounts.realName') }}</th>
            <th>{{ $t('accounts.email') }}</th>
            <th>{{ $t('accounts.phone') }}</th>
            <th>{{ $t('accounts.roles') }}</th>
            <th>{{ $t('accounts.locale') }}</th>
            <th>{{ $t('accounts.source') }}</th>
            <th>{{ $t('accounts.status') }}</th>
            <th>{{ $t('accounts.actions') }}</th>
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
            <td>{{ sourceLabel(user.source) }}</td>
            <td>
              <span class="status-badge" :class="{ active: user.status === 1 }">
                {{ user.status === 1 ? $t('accounts.active') : $t('accounts.disabled') }}
              </span>
            </td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(user)">{{ $t('accounts.edit') }}</button>
              <button class="edit-btn" style="background-color: #f59e0b; color: white; border-color: #f59e0b;" @click="promptChangePassword(user)">{{ $t('accounts.password') }}</button>
              <button class="delete-btn" @click="deleteUser(user.id)">{{ $t('accounts.delete') }}</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- User Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ form.id ? $t('accounts.editUser') : $t('accounts.addUser') }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form" @submit.prevent="saveUser">
            <div class="form-item">
              <label>{{ $t('accounts.username') }}</label>
              <input v-model="form.username" required>
            </div>
            <div class="form-item">
              <label>{{ $t('accounts.realName') }}</label>
              <input v-model="form.realName">
            </div>
            <div class="form-item">
              <label>{{ $t('accounts.email') }}</label>
              <input v-model="form.email" type="email">
            </div>
            <div class="form-item">
              <label>{{ $t('accounts.phone') }}</label>
              <input v-model="form.phone">
            </div>
            <div class="form-item">
              <label>{{ $t('accounts.status') }}</label>
              <select v-model="form.status">
                <option :value="1">{{ $t('accounts.statusEnabled') }}</option>
                <option :value="0">{{ $t('accounts.statusDisabled') }}</option>
              </select>
            </div>
            <div class="form-item">
              <label>{{ $t('accounts.locale') }}</label>
              <select v-model="form.locale">
                <option value="zh">{{ $t('accounts.localeZh') }}</option>
                <option value="en">{{ $t('accounts.localeEn') }}</option>
                <option value="ja">{{ $t('accounts.localeJa') }}</option>
              </select>
            </div>
            <div class="form-item">
              <label>{{ $t('accounts.roles') }}</label>
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
          <button class="cancel-btn" @click="showModal = false">{{ $t('accounts.cancel') }}</button>
          <button class="save-btn" @click="saveUser">{{ $t('accounts.save') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import { useI18n } from 'vue-i18n';
import api from '../../utils/api';

const { t } = useI18n();

const users = ref<any[]>([]);
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

const localeLabel = (locale: string) => {
  const map: Record<string, string> = {
    zh: t('accounts.localeZh'),
    en: t('accounts.localeEn'),
    ja: t('accounts.localeJa')
  };
  return map[locale] || locale || '-';
};

const sourceLabel = (source: string) => {
  const map: Record<string, string> = {
    system: t('accounts.sourceSystem'),
    wecom: t('accounts.sourceWecom')
  };
  return map[source] || source || '-';
};

const promptChangePassword = async (user: any) => {
  const newPass = prompt(`${t('accounts.passwordPrompt')} (${user.username}):`, 'password123');
  if (newPass) {
    try {
      await api.post(`/sys/users/${user.id}/password`, { password: newPass });
      alert(t('accounts.passwordSuccess'));
    } catch (e) {
      alert(t('accounts.passwordFail'));
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
    fetchUsers();
  } catch (e) {
    alert(t('accounts.saveFail'));
  }
};

const deleteUser = async (id: number) => {
  if (!confirm(t('accounts.deleteConfirm'))) return;
  try {
    await api.delete(`/sys/users/${id}`);
    fetchUsers();
  } catch (e) {
    alert(t('accounts.saveFail'));
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
