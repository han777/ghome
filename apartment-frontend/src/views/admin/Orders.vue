<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input v-model="searchQuery" type="text" placeholder="Search orders by name...">
      </div>
      <button class="add-btn" @click="openModal()">+ New Order</button>
    </div>
    
    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Order No.</th>
            <th>Guest</th>
            <th>Type</th>
            <th>Guest Info</th>
            <th>Total Amount</th>
            <th>Key Code</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in filteredOrders" :key="order.id">
            <td class="code">{{ order.orderNo }}</td>
            <td class="name">{{ order.customerName }}</td>
            <td>
              <span class="tag">
                {{ getDictLabel('BIZ_TYPE', order.bizType) }}
              </span>
            </td>
            <td>
              <div style="font-size: 12px; color: #64748b;">
                📞 {{ order.guestPhone || '-' }}<br>
                🏢 {{ order.company || '-' }}
              </div>
            </td>
            <td>¥{{ order.totalAmount || 0 }}</td>
            <td><code>{{ order.doorCode || '-' }}</code></td>
            <td>
              <span class="status-badge" :class="getOrderStatusClass(order.status)">
                {{ getDictLabel('ORDER_STATUS', order.status) }}
              </span>
            </td>
            <td class="actions">
              <button class="edit-btn" @click="sendCode(order.id)" v-if="order.status === 1 || order.status === 2">Send Code</button>
              <button class="edit-btn" @click="openFeeModal(order)">+ Fee</button>
              <button class="edit-btn" @click="openModal(order)">Edit</button>
              <button class="delete-btn" @click="cancelOrder(order.id)" v-if="order.status < 3">Cancel</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Order Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ form.id ? 'Edit Order' : 'Create Order' }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form">
            <div class="form-item">
              <label>Guest Name</label>
              <input v-model="form.customerName" required>
            </div>
            <div class="form-item">
              <label>Business Type</label>
              <select v-model="form.bizType">
                <option v-for="opt in getDictOptions('BIZ_TYPE')" :key="opt.value" :value="parseInt(opt.value)">
                  {{ opt.label }}
                </option>
              </select>
            </div>
            <div class="form-item">
              <label>Amount (Price)</label>
              <input type="number" v-model="form.amount">
            </div>
            <div class="form-item">
              <label>Order Status</label>
              <select v-model="form.status">
                <option v-for="opt in getDictOptions('ORDER_STATUS')" :key="opt.value" :value="parseInt(opt.value)">
                  {{ opt.label }}
                </option>
              </select>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showModal = false">Cancel</button>
          <button class="save-btn" @click="saveOrder">Save Changes</button>
        </div>
      </div>
    </div>

    <!-- Fee Modal -->
    <div v-if="showFeeModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>Add Extra Fee</h2>
          <button class="close-btn" @click="showFeeModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form">
            <div class="form-item">
              <label>Fee Type</label>
              <select v-model="feeForm.feeType">
                <option value="Breakfast">Breakfast</option>
                <option value="Damage">Damage</option>
                <option value="Laundry">Laundry</option>
                <option value="Other">Other</option>
              </select>
            </div>
            <div class="form-item">
              <label>Amount</label>
              <input type="number" v-model="feeForm.amount">
            </div>
            <div class="form-item">
              <label>Remarks</label>
              <input v-model="feeForm.remarks">
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showFeeModal = false">Cancel</button>
          <button class="save-btn" @click="saveFee">Add Fee</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue';
import api from '../../utils/api';

const orders = ref<any[]>([]);
const dicts = ref<any[]>([]);
const showModal = ref(false);
const showFeeModal = ref(false);
const searchQuery = ref('');
const form = reactive<any>({
  id: null,
  customerName: '',
  bizType: 1,
  totalAmount: 0,
  status: 1,
  guestPhone: '',
  company: '',
  costCenter: ''
});

const feeForm = reactive({
  orderId: null as number | null,
  feeType: 'Breakfast',
  amount: 0,
  remarks: ''
});

const fetchData = async () => {
  try {
    const [orderRes, dictRes] = await Promise.all([
      api.get('/orders/all'),
      api.get('/sys/dict/all')
    ]) as any[];
    orders.value = orderRes;
    dicts.value = dictRes;
  } catch (e) {
    console.error('Failed to fetch data', e);
  }
};

const filteredOrders = computed(() => {
  if (!searchQuery.value) return orders.value;
  return orders.value.filter(o => o.customerName.toLowerCase().includes(searchQuery.value.toLowerCase()));
});

const getDictLabel = (code: string, value: any) => {
  const dict = dicts.value.find(d => d.dictCode === code);
  if (!dict) return value;
  const item = dict.items.find((i: any) => i.itemValue === String(value));
  return item ? item.itemLabel : value;
};

const getDictOptions = (code: string) => {
  const dict = dicts.value.find(d => d.dictCode === code);
  return dict ? dict.items.map((i: any) => ({ label: i.itemLabel, value: i.itemValue })) : [];
};

const getOrderStatusClass = (status: number) => {
  switch(status) {
    case 0: return 'repair'; // Locking
    case 1: return 'occupied'; // Pending
    case 2: return 'active'; // In
    case 3: return 'repair'; // Out
    default: return '';
  }
};

const openModal = (order?: any) => {
  if (order) {
    Object.assign(form, order);
  } else {
    Object.assign(form, { id: null, customerName: '', bizType: 1, totalAmount: 0, status: 1, guestPhone: '', company: '', costCenter: '' });
  }
  showModal.value = true;
};

const sendCode = async (id: number) => {
  try {
    await api.post(`/orders/${id}/send-code`, {});
    fetchData();
    alert('Code sent successfully');
  } catch (e) {
    alert('Failed to send code');
  }
};

const cancelOrder = async (id: number) => {
  if (!confirm('Cancel this order?')) return;
  try {
    await api.post(`/orders/${id}/cancel`, {});
    fetchData();
  } catch (e) {
    alert('Failed to cancel');
  }
};

const openFeeModal = (order: any) => {
  feeForm.orderId = order.id;
  feeForm.feeType = 'Breakfast';
  feeForm.amount = 0;
  feeForm.remarks = '';
  showFeeModal.value = true;
};

const saveFee = async () => {
  try {
    await api.post(`/orders/${feeForm.orderId}/add-fee`, feeForm);
    showFeeModal.value = false;
    fetchData();
  } catch (e) {
    alert('Failed to add fee');
  }
};

const saveOrder = async () => {
  try {
    if (!form.orderNo && !form.id) {
      form.orderNo = 'ORD' + Date.now().toString().slice(-6);
    }
    await api.post('/orders', form);
    showModal.value = false;
    fetchData();
  } catch (e) {
    alert('Failed to save order');
  }
};

onMounted(fetchData);
</script>

<style scoped>
@import "../../assets/admin.css";
.tag { background: #e0f2fe; color: #0369a1; padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }
.occupied { background: #fef9c3; color: #854d0e; }
.repair { background: #fee2e2; color: #991b1b; }
</style>
