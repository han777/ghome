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
            <th>Price</th>
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
            <td>¥{{ order.amount }}</td>
            <td>
              <span class="status-badge" :class="getOrderStatusClass(order.status)">
                {{ getDictLabel('ORDER_STATUS', order.status) }}
              </span>
            </td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(order)">Edit</button>
              <button class="delete-btn" @click="deleteOrder(order.id)">Delete</button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue';
import api from '../../utils/api';

const orders = ref<any[]>([]);
const dicts = ref<any[]>([]);
const showModal = ref(false);
const searchQuery = ref('');
const form = reactive<any>({
  id: null,
  customerName: '',
  bizType: 1,
  amount: 0,
  status: 1
});

const fetchData = async () => {
  try {
    const [orderRes, dictRes] = await Promise.all([
      api.get('/orders/all'),
      api.get('/sys/dict/all')
    ]);
    orders.value = orderRes as any[];
    dicts.value = dictRes as any[];
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
    Object.assign(form, { id: null, customerName: '', bizType: 1, amount: 0, status: 1 });
  }
  showModal.value = true;
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

const deleteOrder = async (id: number) => {
  if (!confirm('Delete this order?')) return;
  try {
    await api.delete(`/orders/${id}`);
    fetchData();
  } catch (e) {
    alert('Failed to delete');
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
