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
              <button class="edit-btn" @click="openModal(order, 'products')">+ Service</button>
              <button class="edit-btn" @click="openModal(order)">Edit</button>
              <button class="delete-btn" @click="cancelOrder(order.id)" v-if="order.status < 3">Cancel</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Order Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content" :class="{ 'modal-maximized': isMaximized }">
        <div class="modal-header">
          <div class="header-left">
            <h2>{{ form.id ? 'Edit Order' : 'Create Order' }}</h2>
            <div class="modal-nav">
              <a href="#section-basic" @click.prevent="scrollTo('section-basic')">🏠 Basic Info</a>
              <a href="#section-products" @click.prevent="scrollTo('section-products')">📦 Services & Products</a>
            </div>
          </div>
          <div class="header-actions">
            <button v-if="form.id && form.status < 3" class="checkout-btn" @click="handleCheckout">🔔 退房 (Checkout)</button>
            <button class="maximize-btn" @click="isMaximized = !isMaximized">
              {{ isMaximized ? '🗗' : '🗖' }}
            </button>
            <button class="close-btn" @click="showModal = false">&times;</button>
          </div>
        </div>
        <div class="modal-body scrollable">
          <!-- Basic Info Section -->
          <section id="section-basic" class="form-section">
            <h3 class="section-title">Basic Information</h3>
            <div class="form-grid-4">
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
                <label>Room Type</label>
                <input :value="selectedRoomType" disabled>
              </div>
              <div class="form-item">
                <label class="required">Customer</label>
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

              <div class="form-item">
                <label class="required">Check-in Date</label>
                <input type="date" v-model="form.startDate" required>
              </div>
              <div class="form-item">
                <label>Check-out Date</label>
                <input type="date" v-model="form.endDate" required>
              </div>
              <div class="form-item">
                <label>Check-in Time</label>
                <input type="time" v-model="form.checkInTime">
              </div>
              <div class="form-item">
                <label>Check-out Time</label>
                <input type="time" v-model="form.checkOutTime">
              </div>

              <div class="form-item">
                <label>Room Card Code</label>
                <input v-model="form.roomCardCode" placeholder="Card code">
              </div>
              <div class="form-item">
                <label>Check-in Type</label>
                <input v-model="form.checkInType" placeholder="e.g. Normal">
              </div>
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

              <div class="form-item span-2">
                <label>Co-Occupants</label>
                <input v-model="form.coOccupants" placeholder="Names of other guests">
              </div>
              <div class="form-item span-2">
                <label>Total Amount (Auto)</label>
                <div class="amount-display">
                  <span class="currency">¥</span>
                  <input type="number" v-model="form.totalAmount" step="0.01">
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
              <div class="form-item span-3">
                <label>Remarks</label>
                <input v-model="form.remarks" placeholder="General remarks...">
              </div>
            </div>
          </section>

          <!-- Products & Services Section -->
          <section id="section-products" class="form-section">
            <div class="section-header">
              <h3 class="section-title">Services & Products</h3>
              <button class="add-btn small" @click="addProductDetail">+ Add Row</button>
            </div>
            <div class="table-wrapper">
              <table class="detail-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Product/Service</th>
                    <th>Unit</th>
                    <th>Std. Price</th>
                    <th>Act. Price</th>
                    <th>Qty</th>
                    <th>Total</th>
                    <th>Remarks</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(detail, index) in form.productDetails" :key="index">
                    <td><input type="date" v-model="detail.consumeDate" class="table-input" style="width: 120px;"></td>
                    <td>
                      <select v-model="detail.productId" @change="onProductChange(detail)" class="table-input">
                        <option :value="null">-- Select --</option>
                        <option v-for="p in productPrices" :key="p.id" :value="p.id">
                          {{ p.productName }} ({{ p.category === 1 ? 'Sale' : 'Damage' }})
                        </option>
                      </select>
                    </td>
                    <td>{{ getProductById(detail.productId)?.unit || '-' }}</td>
                    <td>¥{{ getProductById(detail.productId)?.price || 0 }}</td>
                    <td><input type="number" v-model="detail.actualPrice" class="table-input no-border" style="width: 70px;"></td>
                    <td><input type="number" v-model="detail.quantity" min="1" class="table-input no-border" style="width: 50px;"></td>
                    <td>¥{{ (detail.actualPrice || 0) * (detail.quantity || 1) }}</td>
                    <td><input v-model="detail.remarks" placeholder="..." class="table-input no-border"></td>
                    <td><button class="delete-btn" @click="removeProductDetail(index)">×</button></td>
                  </tr>
                  <tr v-if="form.productDetails?.length === 0">
                    <td colspan="9" class="empty-row">No services or products added</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>
        <div class="modal-footer">
          <div class="footer-summary">
            Total: <span class="price-highlight">¥{{ form.totalAmount }}</span>
          </div>
          <div class="footer-btns">
            <button class="cancel-btn" @click="showModal = false">Cancel</button>
            <button class="save-btn" @click="saveOrder">Save Changes</button>
          </div>
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
const productPrices = ref<any[]>([]);
const activeTab = ref('basic');
const showModal = ref(false);
const isMaximized = ref(false);
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
  costCenter: '',
  productDetails: []
});

const fetchData = async () => {
  try {
    const [orderRes, dictRes, userRes, roomRes, productRes] = await Promise.all([
      api.get('/orders/all'),
      api.get('/sys/dict/all'),
      api.get('/sys/users'),
      api.get('/rooms/all'),
      api.get('/product-prices/all')
    ]) as [any, any, any, any, any];
    orders.value = orderRes;
    dicts.value = dictRes;
    users.value = userRes;
    rooms.value = roomRes;
    productPrices.value = productRes;
  } catch (e) {
    console.error('Failed to fetch data', e);
  }
};

const filteredOrders = computed(() => {
  const today = new Date().toISOString().split('T')[0];
  if (!Array.isArray(orders.value)) return [];
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

watch([() => form.roomId, () => form.startDate, () => form.endDate, () => form.bizType, () => form.productDetails], () => {
  let total = 0;
  // 1. Room Fee
  if (form.roomId && form.startDate && form.endDate) {
    const room = rooms.value.find(r => r.id === form.roomId);
    if (room && room.roomType) {
      const days = calculateDays(form.startDate, form.endDate);
      const price = form.bizType === 1 ? room.roomType.priceShortRent : room.roomType.priceLongRent;
      total += (price || 0) * days;
    }
  }
  // 2. Product/Service Fee
  if (form.productDetails && form.productDetails.length > 0) {
    form.productDetails.forEach((d: any) => {
      total += (d.actualPrice || 0) * (d.quantity || 1);
    });
  }
  form.totalAmount = total;
}, { deep: true });

const getProductById = (id: number) => productPrices.value.find(p => p.id === id);

const addProductDetail = () => {
  if (!form.productDetails) form.productDetails = [];
  form.productDetails.push({
    productId: null,
    actualPrice: 0,
    quantity: 1,
    consumeDate: new Date().toISOString().split('T')[0],
    remarks: ''
  });
};

const removeProductDetail = (index: number) => {
  form.productDetails.splice(index, 1);
};

const onProductChange = (detail: any) => {
  const p = getProductById(detail.productId);
  if (p) {
    detail.actualPrice = p.price;
  }
};

const openModal = (order?: any, tab: string = 'basic') => {
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
    // Handle nested productDetails
    if (order.productDetails) {
      form.productDetails = order.productDetails.map((d: any) => ({
        ...d,
        productId: d.product?.id
      }));
    } else {
      form.productDetails = [];
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
      costCenter: '',
      productDetails: []
    });
  }
  activeTab.value = tab;
  showModal.value = true;
};

const scrollTo = (id: string) => {
  const el = document.getElementById(id);
  if (el) el.scrollIntoView({ behavior: 'smooth' });
};

const handleCheckout = async () => {
  if (!confirm('Confirm checkout? This will set order status to OUT.')) return;
  form.status = 3; // OUT
  await saveOrder();
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
    // Handle nested productDetails payload
    if (form.productDetails) {
      payload.productDetails = form.productDetails.map((d: any) => ({
        ...d,
        product: d.productId ? { id: d.productId } : null
      })).filter((d: any) => d.product !== null);
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

/* Modal Tabs */
.modal-tabs { display: flex; gap: 20px; border-bottom: 1px solid #e2e8f0; margin-bottom: 20px; }
.modal-tabs button {
  background: none; border: none; padding: 10px 0; font-weight: 600; color: #64748b; cursor: pointer;
  border-bottom: 2px solid transparent; transition: all 0.2s;
}
.modal-tabs button.active { color: #38bdf8; border-bottom-color: #38bdf8; }

.tab-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.tab-header h3 { font-size: 16px; margin: 0; }
.add-btn.small { padding: 4px 10px; font-size: 12px; }

.detail-table { width: 100%; border-collapse: collapse; margin-top: 10px; }
.detail-table th { text-align: left; font-size: 12px; color: #64748b; padding: 10px; border-bottom: 1px solid #f1f5f9; }
.detail-table td { padding: 10px; border-bottom: 1px solid #f1f5f9; }
.small-input { width: 60px; padding: 4px 8px; border: 1px solid #e2e8f0; border-radius: 4px; }
.detail-table select { padding: 4px 8px; border: 1px solid #e2e8f0; border-radius: 4px; width: 100%; }

/* New Grid and Maximized Styles */
.modal-content {
  max-width: 1200px;
  width: 90%;
}

.modal-content.modal-maximized {
  width: 98vw;
  height: 98vh;
  max-width: none;
  margin: 1vh auto;
}

.modal-body.scrollable {
  overflow-y: auto;
  max-height: calc(90vh - 120px);
}
.modal-maximized .modal-body.scrollable {
  max-height: calc(98vh - 120px);
}

.modal-nav { display: flex; gap: 15px; margin-left: 20px; }
.modal-nav a { font-size: 14px; text-decoration: none; color: #64748b; font-weight: 600; }
.modal-nav a:hover { color: #38bdf8; }

.header-actions { display: flex; gap: 10px; align-items: center; }
.maximize-btn, .checkout-btn {
  background: #f1f5f9; border: 1px solid #e2e8f0; border-radius: 4px; padding: 4px 10px; cursor: pointer; font-size: 14px;
}
.checkout-btn { background: #fee2e2; color: #b91c1c; border-color: #fecaca; font-weight: 600; }
.checkout-btn:hover { background: #fecaca; }

.form-section { padding: 20px; border-bottom: 1px solid #f1f5f9; scroll-margin-top: 20px; }
.section-title { font-size: 16px; font-weight: 700; color: #1e293b; margin-bottom: 15px; border-left: 4px solid #38bdf8; padding-left: 10px; }

.form-grid-4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
}
.span-2 { grid-column: span 2; }
.span-3 { grid-column: span 3; }

.amount-display { display: flex; align-items: center; gap: 8px; background: #f8fafc; padding: 4px 12px; border-radius: 6px; border: 1px solid #e2e8f0; }
.currency { font-weight: 700; color: #0369a1; }
.amount-display input { background: transparent; border: none; font-weight: 700; font-size: 16px; width: 100%; color: #0369a1; }

.table-wrapper { overflow-x: auto; }
.table-input { padding: 4px; border: 1px solid #e2e8f0; border-radius: 4px; font-size: 13px; width: 100%; }
.table-input.no-border { border-color: transparent; background: #f8fafc; }
.empty-row { text-align: center; color: #94a3b8; padding: 30px; }

.footer-summary { font-size: 16px; font-weight: 600; }
.price-highlight { color: #ef4444; font-size: 20px; font-weight: 800; }
.footer-btns { display: flex; gap: 10px; }
</style>
