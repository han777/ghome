<template>
  <div class="admin-page">
    <div class="page-header">
      <h1>全局设置</h1>
    </div>

    <div class="settings-card">
      <div class="card-section">
        <h3 class="section-title">通知设置</h3>

        <div class="form-item">
          <label>订单通知邮箱</label>
          <p class="hint">订单生成后（待入住），系统将向这些邮箱发送订单通知邮件，包含订单号、订房人、入离日期、房间信息等。多个邮箱请用逗号分隔。</p>
          <textarea
            v-model="form.notificationEmails"
            rows="4"
            placeholder="输入通知邮箱，多个邮箱用逗号分隔..."
          ></textarea>
        </div>
      </div>

      <div class="card-actions">
        <button class="save-btn" @click="saveConfig" :disabled="saving">
          {{ saving ? '保存中...' : '保存设置' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import api from '../../utils/api';

const form = reactive({
  id: null as number | null,
  notificationEmails: ''
});

const saving = ref(false);

const fetchConfig = async () => {
  try {
    const res = await api.get('/sys/config') as any;
    if (res) {
      form.id = res.id;
      form.notificationEmails = res.notificationEmails || '';
    }
  } catch (e) {
    console.error('Failed to fetch config', e);
  }
};

const saveConfig = async () => {
  saving.value = true;
  try {
    await api.post('/sys/config', {
      id: form.id,
      notificationEmails: form.notificationEmails
    });
    alert('设置保存成功');
    fetchConfig();
  } catch (e: any) {
    alert('保存失败: ' + (e.response?.data?.message || e.message));
  } finally {
    saving.value = false;
  }
};

onMounted(fetchConfig);
</script>

<style scoped>
@import "../../assets/admin.css";

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.settings-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 24px;
  max-width: 600px;
}

.card-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 16px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #e2e8f0;
}

.form-item {
  margin-bottom: 16px;
}

.form-item label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}

.form-item .hint {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
}

.form-item textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  transition: border-color 0.2s;
}

.form-item textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.card-actions {
  display: flex;
  justify-content: flex-end;
}

.save-btn {
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.save-btn:hover:not(:disabled) {
  background: #2563eb;
}

.save-btn:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}
</style>
