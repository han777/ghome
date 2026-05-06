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
            <th>Booker</th>
            <th>Creator</th>
            <th>Rooms</th>
            <th>Room List</th>
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
            <td>
              <div style="font-size: 12px;">
                👤 {{ order.booker?.realName || '-' }}<br>
                📞 {{ order.bookPhone || order.booker?.phone || '-' }}
              </div>
            </td>
            <td class="name">{{ order.user?.realName || order.user?.username || 'System' }}</td>
            <td><span class="room-count-badge">{{ order.roomOccupies?.length || 0 }}</span></td>
            <td>
              <div class="room-tags">
                <span v-for="ro in order.roomOccupies" :key="ro.id" class="room-tag">
                  {{ ro.room?.roomNo }}
                </span>
                <span v-if="!order.roomOccupies || order.roomOccupies.length === 0">-</span>
              </div>
            </td>
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
            <td>
              <div class="room-tags">
                <template v-for="ro in order.roomOccupies" :key="ro.id">
                  <code v-if="ro.doorCode">{{ ro.doorCode }}</code>
                </template>
                <span v-if="!order.roomOccupies || order.roomOccupies.length === 0 || !order.roomOccupies.some(ro => ro.doorCode)">-</span>
              </div>
            </td>
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
              <a href="#section-rooms" @click.prevent="scrollTo('section-rooms')">🛏️ Rooms</a>
              <a href="#section-products" @click.prevent="scrollTo('section-products')">📦 Services & Products</a>
              <a href="#section-accounting" @click.prevent="scrollTo('section-accounting')">💰 Accounting</a>
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
                <label class="required">Book User</label>
                <select v-model="form.bookerId" required @change="onBookerChange">
                  <option :value="null">-- Select Booker --</option>
                  <option v-for="u in users" :key="u.id" :value="u.id">
                    {{ u.realName }} ({{ u.username }})
                  </option>
                </select>
              </div>
              <div class="form-item">
                <label>Book Phone</label>
                <input v-model="form.bookPhone" placeholder="Booking phone">
              </div>

              <div class="form-item">
                <label class="required">Stay Start Date</label>
                <input type="date" v-model="form.startDate" required>
              </div>
              <div class="form-item">
                <label>Stay End Date</label>
                <input type="date" v-model="form.endDate" required>
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
                <label>Order Status</label>
                <select v-model="form.status">
                  <option v-for="opt in getDictOptions('ORDER_STATUS')" :key="opt.value" :value="parseInt(opt.value)">
                    {{ opt.label }}
                  </option>
                </select>
              </div>

              <div class="form-item span-2">
                <label>Remarks</label>
                <input v-model="form.remarks" placeholder="General remarks...">
              </div>
            </div>
          </section>

          <!-- Room Occupancy Section -->
          <section id="section-rooms" class="form-section">
            <div class="section-header">
              <h3 class="section-title">Room Occupancy</h3>
              <button class="add-btn small" @click="addRoomRow">+ Add Room</button>
            </div>
            <div class="room-cards-container">
              <div v-for="(occupy, index) in form.roomOccupies" :key="index" class="room-card">
                <div class="card-header">
                  <div class="card-title">
                    <span class="room-no">Room {{ getRoomNo(occupy.roomId) || 'NEW' }}</span>
                    <span class="room-type">{{ getRoomTypeLabel(occupy.roomId) }}</span>
                  </div>
                  <div class="card-actions">
                    <button class="card-action-btn change" v-if="occupy.id" @click="startChangeRoom(occupy, index)">换房</button>
                    <button class="card-action-btn delete" @click="removeRoomRow(index)">×</button>
                  </div>
                </div>
                <div class="card-grid">
                  <div class="card-item">
                    <label>Actual Check-in</label>
                    <input type="datetime-local" v-model="occupy.checkInTime">
                  </div>
                  <div class="card-item">
                    <label>Actual Check-out</label>
                    <input type="datetime-local" v-model="occupy.checkOutTime">
                  </div>
                  <div class="card-item">
                    <label>Room</label>
                    <select v-model="occupy.roomId" @change="onRoomChange(occupy)">
                      <option :value="null">-- Select Room --</option>
                      <option v-for="r in availableRooms" :key="r.id" :value="r.id" :disabled="isRoomTaken(r.id, occupy.roomId)">
                        {{ r.roomNo }} ({{ r.roomType?.typeCode }})
                      </option>
                    </select>
                  </div>
                  <div class="card-item">
                    <label>Occupant User</label>
                    <select v-model="occupy.occupantUserId">
                      <option :value="null">-- Select Occupant --</option>
                      <option v-for="u in users" :key="u.id" :value="u.id">
                        {{ u.realName }} ({{ u.username }})
                      </option>
                    </select>
                  </div>
                  <div class="card-item">
                    <label>Room Card No.</label>
                    <input v-model="occupy.roomCardNo">
                  </div>
                  <div class="card-item">
                    <label>Door Code (Key)</label>
                    <input v-model="occupy.doorCode">
                  </div>
                  <div class="card-item">
                    <label>Occupants</label>
                    <input type="number" v-model="occupy.occupantCount" min="1">
                  </div>
                  <div class="card-item">
                    <label>Status</label>
                    <select v-model="occupy.status">
                      <option :value="0">Current (当前)</option>
                      <option :value="1">Finish (完成)</option>
                    </select>
                  </div>
                  <div class="card-item span-4">
                    <label>Co-Occupants</label>
                    <input v-model="occupy.coOccupants" placeholder="Names of other guests">
                  </div>
                </div>
              </div>
              <div v-if="!form.roomOccupies || form.roomOccupies.length === 0" class="empty-rooms">
                No rooms assigned to this order yet.
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

          <!-- Accounting Section -->
          <section id="section-accounting" class="form-section">
            <h3 class="section-title">Accounting</h3>
            <div class="form-grid-4">
              <div class="form-item">
                <label>Room Fee</label>
                <div class="amount-display">
                  <span class="currency">¥</span>
                  <input type="number" v-model="form.roomFee" step="0.01" disabled>
                </div>
              </div>
              <div class="form-item">
                <label>Service Fee</label>
                <div class="amount-display">
                  <span class="currency">¥</span>
                  <input type="number" v-model="form.serviceFee" step="0.01" disabled>
                </div>
              </div>
              <div class="form-item span-2">
                <label>Total Amount (Auto)</label>
                <div class="amount-display highlight">
                  <span class="currency">¥</span>
                  <input type="number" v-model="form.totalAmount" step="0.01">
                </div>
              </div>
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
    
    <!-- Change Room Modal -->
    <div v-if="showChangeRoomModal" class="modal-overlay" style="z-index: 2000;">
      <div class="modal-content" style="max-width: 500px;">
        <div class="modal-header">
          <h3>换房流程 (Change Room)</h3>
          <button class="close-btn" @click="showChangeRoomModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="room-card" style="box-shadow: none; border-color: #38bdf8;">
            <div class="card-header">
              <div class="card-title">
                <span class="room-no">New Room Details</span>
                <span class="room-type">FIELDS COPIED FROM CURRENT ROOM</span>
              </div>
            </div>
            <div class="card-grid">
              <div class="card-item span-4">
                <label class="required">Select New Room</label>
                <select v-model="changeRoomNewRoomId" required>
                  <option :value="null">-- Choose Available Room --</option>
                  <option v-for="r in rooms" :key="r.id" :value="r.id" :disabled="r.status !== 0">
                    {{ r.roomNo }} ({{ r.roomType?.typeCode }}) - ¥{{ form.bizType === 1 ? r.roomType?.priceShortRent : r.roomType?.priceLongRent }}
                  </option>
                </select>
              </div>
              <div class="card-item span-2">
                <label>Occupant User</label>
                <input :value="users.find(u => u.id === changingOccupy?.occupantUserId)?.realName" disabled>
              </div>
              <div class="card-item span-2">
                <label>Room Card No.</label>
                <input :value="changingOccupy?.roomCardNo" disabled>
              </div>
              <div class="card-item span-4" v-if="changeRoomPriceDiff !== 0">
                <div class="amount-display highlight">
                  <span style="font-size: 13px; color: #64748b;">Price Difference:</span>
                  <span class="currency">¥</span>
                  <span style="font-weight: 800; color: #ef4444;">{{ changeRoomPriceDiff.toFixed(2) }}</span>
                </div>
                <p style="font-size: 11px; color: #94a3b8; margin-top: 5px;">* Calculated for remaining days until {{ form.endDate }}</p>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showChangeRoomModal = false">Cancel</button>
          <button class="save-btn" :disabled="!changeRoomNewRoomId" @click="confirmChangeRoom">Confirm Change</button>
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
const showChangeRoomModal = ref(false);
const changingOccupy = ref<any>(null);
const changeRoomNewRoomId = ref<number | null>(null);
const availableRooms = ref<any[]>([]);

const form = reactive<any>({
  id: null,
  userId: null,
  bizType: 1,
  startDate: new Date().toISOString().split('T')[0],
  endDate: new Date(Date.now() + 86400000).toISOString().split('T')[0],
  roomFee: 0,
  serviceFee: 0,
  totalAmount: 0,
  status: 1,
  bookerId: null,
  bookPhone: '',
  remarks: '',
  company: '',
  costCenter: '',
  roomOccupies: [],
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
    // Initial fetch for available rooms if form dates are set
    if (form.startDate && form.endDate) fetchAvailableRooms();
  } catch (e) {
    console.error('Failed to fetch data', e);
  }
};

const fetchAvailableRooms = async () => {
  if (!form.startDate || !form.endDate) {
    availableRooms.value = [];
    return;
  }
  try {
    const res = await api.get(`/rooms/available?startDate=${form.startDate}&endDate=${form.endDate}`);
    availableRooms.value = res as any[];
    // Also include currently selected rooms in the available list so they don't show as empty/unknown
    const currentRoomIds = form.roomOccupies?.map((ro: any) => ro.roomId).filter(Boolean) || [];
    currentRoomIds.forEach((id: number) => {
      if (!availableRooms.value.find(r => r.id === id)) {
        const room = rooms.value.find(r => r.id === id);
        if (room) availableRooms.value.push(room);
      }
    });
  } catch (e) {
    console.error('Failed to fetch available rooms', e);
  }
};

watch([() => form.startDate, () => form.endDate], fetchAvailableRooms);

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
    case 0: return 'repair'; // Cooling-off
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

watch([() => form.roomOccupies, () => form.startDate, () => form.endDate, () => form.bizType, () => form.productDetails], () => {
  let rFee = 0;
  let sFee = 0;
  // 1. Room Fee (Sum of all rooms)
  if (form.roomOccupies && form.startDate && form.endDate) {
    const days = calculateDays(form.startDate, form.endDate);
    form.roomOccupies.forEach((ro: any) => {
      if (ro.roomId) {
        const room = rooms.value.find(r => r.id === ro.roomId);
        if (room && room.roomType) {
          const price = form.bizType === 1 ? room.roomType.priceShortRent : room.roomType.priceLongRent;
          rFee += (price || 0) * days;
        }
      }
    });
  }
  // 2. Product/Service Fee
  if (form.productDetails && form.productDetails.length > 0) {
    form.productDetails.forEach((d: any) => {
      sFee += (d.actualPrice || 0) * (d.quantity || 1);
    });
  }
  form.roomFee = rFee;
  form.serviceFee = sFee;
  form.totalAmount = rFee + sFee;
}, { deep: true });

const getRoomNo = (id: number) => rooms.value.find(r => r.id === id)?.roomNo;
const getRoomTypeLabel = (id: number) => {
  const room = rooms.value.find(r => r.id === id);
  return room?.roomType?.typeCode || 'Unknown';
};

const isRoomTaken = (roomId: number, currentRoomId: number) => {
  if (roomId === currentRoomId) return false;
  return form.roomOccupies.some((ro: any) => ro.roomId === roomId);
};

const addRoomRow = () => {
  form.roomOccupies.push({
    roomId: null,
    occupantUserId: form.userId,
    occupantCount: 1,
    status: 0,
    roomCardNo: '',
    coOccupants: '',
    checkInTime: form.startDate ? `${form.startDate}T14:00` : null,
    checkOutTime: form.endDate ? `${form.endDate}T12:00` : null
  });
};

const removeRoomRow = (index: number) => {
  form.roomOccupies.splice(index, 1);
};

const changeRoomPriceDiff = computed(() => {
  if (!changingOccupy.value || !changeRoomNewRoomId.value) return 0;
  const newRoom = rooms.value.find(r => r.id === changeRoomNewRoomId.value);
  if (!newRoom) return 0;
  
  const oldRoom = rooms.value.find(r => r.id === changingOccupy.value.roomId);
  const oldPrice = form.bizType === 1 ? oldRoom?.roomType?.priceShortRent : oldRoom?.roomType?.priceLongRent;
  const newPrice = form.bizType === 1 ? newRoom.roomType?.priceShortRent : newRoom.roomType?.priceLongRent;
  
  const days = calculateDays(new Date().toISOString().split('T')[0], form.endDate);
  return ((newPrice || 0) - (oldPrice || 0)) * days;
});

const startChangeRoom = (occupy: any) => {
  changingOccupy.value = occupy;
  changeRoomNewRoomId.value = null;
  showChangeRoomModal.value = true;
};

const confirmChangeRoom = async () => {
  if (!changeRoomNewRoomId.value || !changingOccupy.value) return;
  
  try {
    await api.post(`/orders/occupy/${changingOccupy.value.id}/change-room?roomId=${changeRoomNewRoomId.value}`, {});
    alert('换房成功 (Change Room Success)');
    showChangeRoomModal.value = false;
    showModal.value = false;
    fetchData();
  } catch (e: any) {
    alert('换房失败: ' + (e.response?.data || e.message));
  }
};

const onBookerChange = () => {
  const user = users.value.find(u => u.id === form.bookerId);
  if (user) {
    form.bookPhone = user.phone;
  }
};

const onRoomChange = (occupy: any) => {
  // Logic after room selection
};

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
    // Handle nested roomOccupies
    if (order.roomOccupies) {
      form.roomOccupies = order.roomOccupies.map((ro: any) => ({
        ...ro,
        roomId: ro.room?.id,
        occupantUserId: ro.occupantUser?.id,
        checkInTime: ro.checkInTime ? ro.checkInTime.slice(0, 16) : null,
        checkOutTime: ro.checkOutTime ? ro.checkOutTime.slice(0, 16) : null
      }));
    } else {
      form.roomOccupies = [];
    }
    form.bookerId = order.booker?.id || null;
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
      userId: null, 
      bizType: 1, 
      startDate: new Date().toISOString().split('T')[0],
      endDate: new Date(Date.now() + 86400000).toISOString().split('T')[0],
      roomFee: 0,
      serviceFee: 0,
      totalAmount: 0, 
      status: 1, 
      bookerId: null,
      bookPhone: '',
      remarks: '',
      company: '', 
      costCenter: '',
      roomOccupies: [],
      productDetails: []
    });
  }
  fetchAvailableRooms();
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
    if (!form.bookerId) {
      alert('⚠️ Please select a BOOKER');
      return;
    }
    if (!form.roomOccupies || form.roomOccupies.length === 0) {
      alert('⚠️ Please add at least one ROOM');
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
    
    if (form.bookerId) {
      payload.booker = { id: form.bookerId };
    }
    delete payload.bookerId;

    // Handle nested roomOccupies payload
    if (form.roomOccupies) {
      payload.roomOccupies = form.roomOccupies.map((ro: any) => ({
        ...ro,
        room: ro.roomId ? { id: ro.roomId } : null,
        occupantUser: ro.occupantUserId ? { id: ro.occupantUserId } : null
      })).filter((ro: any) => ro.room !== null);
    }

    // Handle nested productDetails payload
    if (form.productDetails) {
      payload.productDetails = form.productDetails.map((d: any) => ({
        ...d,
        product: d.productId ? { id: d.productId } : null
      })).filter((d: any) => d.product !== null);
    }
    delete payload.userId;
    
    await api.post('/orders', payload);
    showModal.value = false;
    fetchData();
  } catch (e: any) {
    const msg = e.response?.data?.message || e.response?.data || e.message || 'Failed to save order';
    alert('保存失败: ' + (typeof msg === 'string' ? msg : JSON.stringify(msg)));
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

/* Room Card Styles */
.room-cards-container { display: grid; grid-template-columns: 1fr; gap: 20px; margin-top: 15px; }
.room-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 15px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); transition: all 0.2s; }
.room-card:hover { box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border-color: #38bdf8; }
.card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 1px dashed #f1f5f9; }
.card-title { display: flex; flex-direction: column; gap: 2px; }
.room-no { font-size: 16px; font-weight: 800; color: #0f172a; }
.room-type { font-size: 11px; color: #64748b; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
.card-actions { display: flex; gap: 8px; }
.card-action-btn { padding: 4px 8px; border-radius: 6px; border: 1px solid #e2e8f0; background: #fff; font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.card-action-btn.change { color: #0369a1; border-color: #bae6fd; background: #f0f9ff; }
.card-action-btn.change:hover { background: #e0f2fe; }
.card-action-btn.delete { color: #94a3b8; border: none; font-size: 18px; line-height: 1; padding: 0 4px; }
.card-action-btn.delete:hover { color: #ef4444; }

.card-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; }
.card-item label { display: block; font-size: 11px; color: #94a3b8; margin-bottom: 4px; font-weight: 600; }
.card-item select, .card-item input { width: 100%; padding: 6px 8px; border: 1px solid #f1f5f9; border-radius: 6px; font-size: 13px; background: #f8fafc; }
.empty-rooms { grid-column: span 3; text-align: center; padding: 40px; color: #94a3b8; background: #f8fafc; border: 2px dashed #e2e8f0; border-radius: 12px; }

/* List View Styles */
.room-count-badge { background: #0f172a; color: #fff; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 700; }
.room-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.room-tag { background: #f1f5f9; color: #475569; padding: 1px 6px; border-radius: 4px; font-size: 11px; font-weight: 600; border: 1px solid #e2e8f0; }
.amount-display.highlight { border-color: #38bdf8; background: #f0f9ff; }
</style>
