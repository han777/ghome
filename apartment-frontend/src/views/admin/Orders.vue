<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input v-model="searchQuery" type="text" placeholder="Search orders by name...">
      </div>
      <button class="add-btn" @click="openModal()">+ New Order</button>
    </div>

    <div class="filter-row">
      <div class="quick-filters">
        <button 
          class="filter-chip" 
          :class="{ active: filterTodayArrival }"
          @click="filterTodayArrival = !filterTodayArrival"
        >
          🛬 Today Arrival
        </button>
        <button 
          class="filter-chip" 
          :class="{ active: filterTodayDeparture }"
          @click="filterTodayDeparture = !filterTodayDeparture"
        >
          🛫 Today Departure
        </button>
      </div>
    </div>
    
    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Order No.</th>
            <th>Guest</th>
            <th>Room</th>
            <th>Business Type</th>
            <th>Stay Period</th>
            <th>Guest Info</th>
            <th>Key Code</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in filteredOrders" :key="order.id">
            <td class="code">{{ order.orderNo }}</td>
            <td class="name">{{ order.user?.realName || order.user?.username || order.customerName || '-' }}</td>
            <td>{{ order.room?.roomNo || '-' }}</td>
            <td>
              <span class="tag">
                {{ getDictLabel('BIZ_TYPE', order.bizType) }}
              </span>
            </td>
            <td>
              <div style="font-size: 12px; white-space: nowrap;">
                📅 {{ order.startDate || '-' }}<br>
                🔚 {{ order.endDate || '-' }}
              </div>
            </td>
            <td>
              <div style="font-size: 12px; color: #64748b;">
                📞 {{ order.guestPhone || '-' }}<br>
                🏢 {{ order.company || '-' }}
              </div>
            </td>
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
            <div class="form-group-row">
              <div class="form-item">
                <label class="required">Room</label>
                <select v-model="form.roomId" required>
                  <option :value="null">-- Select Room --</option>
                  <option v-for="r in rooms" :key="r.id" :value="r.id">
                    {{ r.roomNo }} ({{ r.roomType?.typeCode }})
                  </option>
                </select>
              </div>
              <div class="form-item">
                <label>Room Type (Auto)</label>
                <input :value="selectedRoomType" disabled placeholder="Select a room first">
              </div>
            </div>

            <div class="form-group-row">
              <div class="form-item">
                <label class="required">Customer (System User)</label>
                <select v-model="form.userId" required>
                  <option :value="null">-- Select Customer --</option>
                  <option v-for="u in users" :key="u.id" :value="u.id">
                    {{ u.realName }} ({{ u.username }})
                  </option>
                </select>
              </div>
              <div class="form-item">
                <label>Guest Phone</label>
                <input v-model="form.guestPhone">
              </div>
            </div>

            <div class="form-group-row">
              <div class="form-item">
                <label class="required">Check-in Date</label>
                <input type="date" v-model="form.startDate" required>
              </div>
              <div class="form-item">
                <label>Check-out Date</label>
                <input type="date" v-model="form.endDate" required>
              </div>
            </div>

            <div class="form-group-row">
              <div class="form-item">
                <label>Check-in Time</label>
                <input type="time" v-model="form.checkInTime">
              </div>
              <div class="form-item">
                <label>Check-out Time</label>
                <input type="time" v-model="form.checkOutTime">
              </div>
            </div>

            <div class="form-group-row">
              <div class="form-item">
                <label>Room Card Code</label>
                <input v-model="form.roomCardCode" placeholder="Enter card code">
              </div>
              <div class="form-item">
                <label>Check-in Type</label>
                <input v-model="form.checkInType" placeholder="e.g. Normal, VIP">
              </div>
            </div>

            <div class="form-group-row">
              <div class="form-item">
                <label>Occupant Count</label>
                <input type="number" v-model="form.occupantCount" min="1">
              </div>
              <div class="form-item">
                <label>Business Type</label>
                <select v-model="form.bizType">
                  <option v-for="opt in getDictOptions('BIZ_TYPE')" :key="opt.value" :value="parseInt(opt.value)">
                    {{ opt.label }}
                  </option>
                </select>
              </div>
            </div>

            <div class="form-item">
              <label>Co-Occupants</label>
              <input v-model="form.coOccupants" placeholder="Names of other guests">
            </div>

            <div class="form-item">
              <label>Remarks</label>
              <textarea v-model="form.remarks" rows="2"></textarea>
            </div>

            <div class="form-group-row">
              <div class="form-item">
                <label>Total Amount (Auto Calculated)</label>
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span style="font-weight: 600; color: #0369a1;">¥</span>
                  <input type="number" v-model="form.totalAmount">
                </div>
              </div>
              <div class="form-item">
                <label>Order Status</label>
                <select v-model="form.status">
                  <option v-for="opt in getDictOptions('ORDER_STATUS')" :key="opt.value" :value="parseInt(opt.value)">
                    {{ opt.label }}
                  </option>
                </select>
              </div>
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
import { ref, onMounted, reactive, computed, watch } from 'vue';
import api from '../../utils/api';

const orders = ref<any[]>([]);
const dicts = ref<any[]>([]);
const users = ref<any[]>([]);
const rooms = ref<any[]>([]);
const showModal = ref(false);
const showFeeModal = ref(false);
const searchQuery = ref('');
const filterTodayArrival = ref(false);
const filterTodayDeparture = ref(false);
const form = reactive<any>({
  id: null,
  roomId: null,
  userId: null,
  bizType: 1,
  startDate: new Date().toISOString().split('T')[0],
  endDate: new Date(Date.now() + 86400000).toISOString().split('T')[0],
  totalAmount: 0,
  status: 1,
  guestPhone: '',
  checkInTime: '14:00',
  checkOutTime: '13:59',
  roomCardCode: '',
  checkInType: '',
  occupantCount: 1,
  coOccupants: '',
  remarks: '',
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
    const [orderRes, dictRes, userRes, roomRes] = await Promise.all([
      api.get('/orders/all'),
      api.get('/sys/dict/all'),
      api.get('/sys/users'),
      api.get('/rooms/all')
    ]) as any[];
    orders.value = orderRes;
    dicts.value = dictRes;
    users.value = userRes;
    rooms.value = roomRes;
  } catch (e) {
    console.error('Failed to fetch data', e);
  }
};

const filteredOrders = computed(() => {
  const today = new Date().toISOString().split('T')[0];
  return orders.value.filter(o => {
    // Search filter
    const name = o.user?.realName || o.user?.username || o.customerName || '';
    const matchSearch = !searchQuery.value || name.toLowerCase().includes(searchQuery.value.toLowerCase());
    
    // Today Arrival filter
    const matchArrival = !filterTodayArrival.value || o.startDate === today;
    
    // Today Departure filter
    const matchDeparture = !filterTodayDeparture.value || o.endDate === today;
    
    return matchSearch && matchArrival && matchDeparture;
  });
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

const selectedRoomType = computed(() => {
  if (!form.roomId) return '';
  const room = rooms.value.find(r => r.id === form.roomId);
  return room?.roomType?.typeCode || '';
});

const calculateDays = (start: string, end: string) => {
  if (!start || !end) return 0;
  const s = new Date(start);
  const e = new Date(end);
  const diffTime = e.getTime() - s.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  return diffDays > 0 ? diffDays : 1;
};

watch([() => form.roomId, () => form.startDate, () => form.endDate, () => form.bizType], () => {
  if (form.roomId && form.startDate && form.endDate) {
    const room = rooms.value.find(r => r.id === form.roomId);
    if (room && room.roomType) {
      const days = calculateDays(form.startDate, form.endDate);
      const price = form.bizType === 1 ? room.roomType.priceShortRent : room.roomType.priceLongRent;
      form.totalAmount = (price || 0) * days;
    }
  }
});

const openModal = (order?: any) => {
  if (order) {
    Object.assign(form, order);
    // Ensure roomId is set for select binding
    if (order.room) {
      form.roomId = order.room.id;
    }
    // Ensure userId is set for select binding
    if (order.user) {
      form.userId = order.user.id;
    }
  } else {
    Object.assign(form, { 
      id: null, 
      roomId: null,
      userId: null, 
      bizType: 1, 
      startDate: new Date().toISOString().split('T')[0],
      endDate: new Date(Date.now() + 86400000).toISOString().split('T')[0],
      totalAmount: 0, 
      status: 1, 
      guestPhone: '', 
      checkInTime: '14:00',
      checkOutTime: '13:59',
      roomCardCode: '',
      checkInType: '',
      occupantCount: 1,
      coOccupants: '',
      remarks: '',
      company: '', 
      costCenter: '' 
    });
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
    if (!form.roomId) {
      alert('⚠️ Please select a ROOM');
      return;
    }
    if (!form.userId) {
      alert('⚠️ Please select a CUSTOMER');
      return;
    }
    if (!form.startDate || !form.endDate) {
      alert('⚠️ Please select CHECK-IN and CHECK-OUT dates');
      return;
    }
    if (new Date(form.endDate) < new Date(form.startDate)) {
      alert('⚠️ Check-out date cannot be earlier than check-in date');
      return;
    }

    if (!form.orderNo && !form.id) {
      form.orderNo = 'ORD' + Date.now().toString().slice(-6);
    }
    // Map IDs to objects for backend
    const payload = { ...form };
    if (form.roomId) {
      payload.room = { id: form.roomId };
    }
    if (form.userId) {
      payload.user = { id: form.userId };
    }
    delete payload.roomId;
    delete payload.userId;
    
    await api.post('/orders', payload);
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

.filter-row { margin-bottom: -12px; }
.quick-filters { display: flex; gap: 10px; }
.filter-chip {
  padding: 6px 12px;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  background: #fff;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}
.filter-chip:hover { border-color: #cbd5e1; background: #f8fafc; }
.filter-chip.active {
  background: #0f172a;
  color: #fff;
  border-color: #0f172a;
}
</style>
