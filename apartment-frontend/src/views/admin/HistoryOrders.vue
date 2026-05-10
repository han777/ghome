<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>历史订单查询</h2>
    </div>

    <!-- Search Panel -->
    <div class="search-panel card">
      <div class="search-grid">
        <div class="search-item">
          <label>入住期间</label>
          <div class="range-input">
            <input type="date" v-model="filters.startDateFrom">
            <span>至</span>
            <input type="date" v-model="filters.startDateTo">
          </div>
        </div>
        <div class="search-item">
          <label>离店期间</label>
          <div class="range-input">
            <input type="date" v-model="filters.endDateFrom">
            <span>至</span>
            <input type="date" v-model="filters.endDateTo">
          </div>
        </div>
        <div class="search-item">
          <label>订房人</label>
          <input type="text" v-model="filters.bookerName" placeholder="搜索订房人姓名">
        </div>
        <div class="search-item">
          <label>入住人</label>
          <input type="text" v-model="filters.occupantName" placeholder="搜索入住人姓名">
        </div>
        <div class="search-item">
          <label>创建人</label>
          <input type="text" v-model="filters.creatorName" placeholder="搜索创建人姓名">
        </div>
        <div class="search-item span-2">
          <label>订单状态</label>
          <div class="status-options">
            <label v-for="opt in getDictOptions('ORDER_STATUS')" :key="opt.value" class="checkbox-label">
              <input type="checkbox" :value="parseInt(opt.value)" v-model="filters.statuses">
              {{ opt.label }}
            </label>
          </div>
        </div>
      </div>
      <div class="search-actions">
        <button class="reset-btn" @click="resetFilters">重置</button>
        <button class="search-btn" @click="page = 1">查询</button>
      </div>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <span>共 {{ filteredOrders.length }} 条记录</span>
          <div class="page-size-selector">
            每页显示:
            <select v-model="pageSize" @change="page = 1">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
              <option :value="100">100</option>
            </select>
          </div>
        </div>
        <!-- Top Pagination -->
        <div class="pagination mini" v-if="totalPages > 1">
          <button :disabled="page === 1" @click="page--">上一页</button>
          <span class="page-info">第 {{ page }} / {{ totalPages }} 页</span>
          <button :disabled="page === totalPages" @click="page++">下一页</button>
        </div>
      </div>
      <table class="admin-table">
        <thead>
          <tr>
            <th>序号</th>
            <th>订单编号</th>
            <th>订房人</th>
            <th>创建人</th>
            <th>房间数量</th>
            <th>房间列表</th>
            <th>业务类型</th>
            <th>居住期间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(order, index) in paginatedOrders" :key="order.id">
            <td>{{ (page - 1) * pageSize + index + 1 }}</td>
            <td class="code">{{ order.orderNo }}</td>
            <td>
              <div style="font-size: 12px;">
                👤 {{ order.booker?.realName || '-' }}<br>
                📞 {{ order.bookPhone || order.booker?.phone || '-' }}
              </div>
            </td>
            <td class="name">{{ order.createUser?.realName || order.createUser?.username || 'System' }}</td>
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
              <span class="status-badge" :class="getOrderStatusClass(order.status)">
                {{ getDictLabel('ORDER_STATUS', order.status) }}
              </span>
            </td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(order)">详情</button>
              <button class="delete-btn" @click="deleteOrder(order.id)" v-if="order.status === 4">删除</button>
            </td>
          </tr>
          <tr v-if="paginatedOrders.length === 0">
            <td colspan="10" class="empty-row">暂无数据</td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div class="pagination" v-if="totalPages > 1">
        <button :disabled="page === 1" @click="page--">上一页</button>
        <span class="page-info">第 {{ page }} / {{ totalPages }} 页</span>
        <button :disabled="page === totalPages" @click="page++">下一页</button>
        <div class="jump-to">
          跳转至: <input type="number" v-model.number="jumpPage" @keyup.enter="goToPage" min="1" :max="totalPages">
        </div>
      </div>
    </div>

    <!-- Read-only Detail Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content" :class="{ 'modal-maximized': isMaximized }">
        <div class="modal-header">
          <div class="header-left">
            <h2>订单详情 (只读)</h2>
            <div class="modal-nav">
              <a href="#section-basic" @click.prevent="scrollTo('section-basic')">🏠 基本信息</a>
              <a href="#section-rooms" @click.prevent="scrollTo('section-rooms')">🛏️ 房间信息</a>
              <a href="#section-products" @click.prevent="scrollTo('section-products')">📦 服务/商品</a>
              <a href="#section-accounting" @click.prevent="scrollTo('section-accounting')">💰 费用明细</a>
            </div>
          </div>
          <div class="header-actions">
            <button class="maximize-btn" @click="isMaximized = !isMaximized">
              {{ isMaximized ? '🗗' : '🗖' }}
            </button>
            <button class="close-btn" @click="showModal = false">&times;</button>
          </div>
        </div>
        <div class="modal-body scrollable">
           <section id="section-basic" class="form-section">
            <h3 class="section-title">基本信息</h3>
            <div class="form-grid-4 readonly-view">
              <div class="form-item">
                <label>订房用户</label>
                <div class="value">{{ form.booker?.realName }} ({{ form.booker?.username }})</div>
              </div>
              <div class="form-item">
                <label>联系电话</label>
                <div class="value">{{ form.bookPhone }}</div>
              </div>
              <div class="form-item">
                <label>入住时间</label>
                <div class="value">{{ form.startDate }}</div>
              </div>
              <div class="form-item">
                <label>离店时间</label>
                <div class="value">{{ form.endDate }}</div>
              </div>
              <div class="form-item">
                <label>业务类型</label>
                <div class="value">{{ getDictLabel('BIZ_TYPE', form.bizType) }}</div>
              </div>
              <div class="form-item">
                <label>客户类型</label>
                <div class="value">{{ form.customerType === 1 ? '个人' : '团体' }}</div>
              </div>
              <div class="form-item">
                <label>订单状态</label>
                <div class="value">{{ getDictLabel('ORDER_STATUS', form.status) }}</div>
              </div>
              <div class="form-item">
                <label>创建时间</label>
                <div class="value">{{ form.createdAt }}</div>
              </div>
              <div class="form-item">
                <label>创建人</label>
                <div class="value">{{ form.createUser?.realName || form.createUser?.username || 'System' }}</div>
              </div>
              <div class="form-item span-2">
                <label>备注</label>
                <div class="value">{{ form.remarks || '-' }}</div>
              </div>
            </div>
          </section>

          <section id="section-rooms" class="form-section">
            <h3 class="section-title">房间入住信息</h3>
            <div class="room-cards-container">
              <div v-for="(occupy, index) in form.roomOccupies" :key="index" class="room-card readonly">
                <div class="card-header">
                  <div class="card-title">
                    <span class="room-no">房间号: {{ occupy.room?.roomNo }}</span>
                    <span class="room-type">{{ getRoomTypeNameZh(occupy.room?.roomType) }}</span>
                  </div>
                </div>
                <div class="card-grid">
                  <div class="card-item">
                    <label>实际入住</label>
                    <div class="value">{{ occupy.checkInTime || '-' }}</div>
                  </div>
                  <div class="card-item">
                    <label>实际离店</label>
                    <div class="value">{{ occupy.checkOutTime || '-' }}</div>
                  </div>
                  <div class="card-item">
                    <label>入住人</label>
                    <div class="value">{{ occupy.occupantUser?.realName || '-' }}</div>
                  </div>
                  <div class="card-item">
                    <label>房卡号</label>
                    <div class="value">{{ occupy.roomCardNo || '-' }}</div>
                  </div>
                  <div class="card-item">
                    <label>门锁密码</label>
                    <div class="value">{{ occupy.doorCode || '-' }}</div>
                  </div>
                  <div class="card-item">
                    <label>入住人数</label>
                    <div class="value">{{ occupy.occupantCount }}</div>
                  </div>
                  <div class="card-item span-4">
                    <label>随行人员</label>
                    <div class="value">{{ occupy.coOccupants || '-' }}</div>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <section id="section-products" class="form-section">
            <h3 class="section-title">服务/商品消费</h3>
            <div class="table-wrapper">
              <table class="detail-table">
                <thead>
                  <tr>
                    <th>日期</th>
                    <th>项目</th>
                    <th>单位</th>
                    <th>单价</th>
                    <th>数量</th>
                    <th>小计</th>
                    <th>备注</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(detail, index) in form.productDetails" :key="index">
                    <td>{{ detail.consumeDate }}</td>
                    <td>{{ detail.product?.productName }}</td>
                    <td>{{ detail.product?.unit }}</td>
                    <td>¥{{ detail.actualPrice }}</td>
                    <td>{{ detail.quantity }}</td>
                    <td>¥{{ detail.actualPrice * detail.quantity }}</td>
                    <td>{{ detail.remarks }}</td>
                  </tr>
                  <tr v-if="!form.productDetails?.length">
                    <td colspan="7" class="empty-row">无消费记录</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section id="section-accounting" class="form-section">
            <h3 class="section-title">结算信息</h3>
            <div class="form-grid-4">
              <div class="form-item">
                <label>房费总计</label>
                <div class="amount-display">¥ {{ form.roomFee }}</div>
              </div>
              <div class="form-item">
                <label>服务费总计</label>
                <div class="amount-display">¥ {{ form.serviceFee }}</div>
              </div>
              <div class="form-item span-2">
                <label>订单总额</label>
                <div class="amount-display highlight">¥ {{ form.totalAmount }}</div>
              </div>
            </div>
          </section>
        </div>
        <div class="modal-footer">
          <button class="save-btn" @click="showModal = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue';
import api from '../../utils/api';

const orders = ref<any[]>([]);

// 后台管理固定显示中文
const dictLabelZh: Record<string, Record<string, string>> = {
  ROOM_STATUS: { 'Available': '可用', 'Locked': '锁定' },
  ORDER_STATUS: { 'Cooling-off': '冷静期', 'Pending': '待确认', 'In': '已入住', 'Out': '已退房', 'Canceled': '已取消' },
  BIZ_TYPE: { 'Short Rent': '短租', 'Long Rent': '长租' }
};

const parseNameIntl = (json: string, lang: string): string => {
  if (!json) return '';
  try {
    const obj = typeof json === 'string' ? JSON.parse(json) : json;
    return obj[lang] || obj['zh'] || '';
  } catch {
    return '';
  }
};

const getRoomTypeNameZh = (roomType: any): string => {
  if (!roomType) return '-';
  return parseNameIntl(roomType.nameIntlJson, 'zh') || roomType.typeCode || '-';
};
const dicts = ref<any[]>([]);
const showModal = ref(false);
const isMaximized = ref(false);
const page = ref(1);
const pageSize = ref(20);
const jumpPage = ref(1);

const filters = reactive({
  startDateFrom: '',
  startDateTo: '',
  endDateFrom: '',
  endDateTo: '',
  bookerName: '',
  occupantName: '',
  creatorName: '',
  statuses: [] as number[]
});

const form = ref<any>({});

const resetFilters = () => {
  const now = new Date();
  const threeMonthsAgo = new Date();
  threeMonthsAgo.setMonth(now.getMonth() - 3);
  
  filters.startDateFrom = threeMonthsAgo.toISOString().split('T')[0];
  filters.startDateTo = now.toISOString().split('T')[0];
  filters.endDateFrom = '';
  filters.endDateTo = '';
  filters.bookerName = '';
  filters.occupantName = '';
  filters.creatorName = '';
  filters.statuses = [];
  page.value = 1;
};

const fetchData = async () => {
  try {
    const [orderRes, dictRes] = await Promise.all([
      api.get('/orders/all'),
      api.get('/sys/dict/all')
    ]) as any[];
    orders.value = (orderRes || []).sort((a: any, b: any) => 
       new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
    );
    dicts.value = dictRes;
  } catch (e) {
    console.error('Failed to fetch data', e);
  }
};

const filteredOrders = computed(() => {
  if (!Array.isArray(orders.value)) return [];
  return orders.value.filter(o => {
    // 1. Stay Period Filter (Start Date)
    if (filters.startDateFrom && o.startDate < filters.startDateFrom) return false;
    if (filters.startDateTo && o.startDate > (filters.startDateTo + 'T23:59:59')) return false;

    // 2. Departure Period Filter (End Date)
    if (filters.endDateFrom && o.endDate < filters.endDateFrom) return false;
    if (filters.endDateTo && o.endDate > (filters.endDateTo + 'T23:59:59')) return false;

    // 3. Booker
    if (filters.bookerName && !o.booker?.realName?.toLowerCase().includes(filters.bookerName.toLowerCase())) return false;

    // 4. Occupant
    if (filters.occupantName) {
      const occupantsStr = o.roomOccupies?.map((ro: any) => 
        (ro.occupantUser?.realName || ro.coOccupants || '')
      ).join(',').toLowerCase() || '';
      if (!occupantsStr.includes(filters.occupantName.toLowerCase())) return false;
    }

    // 5. Creator
    if (filters.creatorName) {
      const creatorName = o.createUser?.realName?.toLowerCase() || o.createUser?.username?.toLowerCase() || '';
      if (!creatorName.includes(filters.creatorName.toLowerCase())) return false;
    }

    // 6. Status
    if (filters.statuses.length > 0 && !filters.statuses.includes(o.status)) return false;

    return true;
  });
});

const totalPages = computed(() => Math.ceil(filteredOrders.value.length / pageSize.value));

const paginatedOrders = computed(() => {
  const start = (page.value - 1) * pageSize.value;
  return filteredOrders.value.slice(start, start + pageSize.value);
});

const goToPage = () => {
  if (jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    page.value = jumpPage.value;
  }
};

const getDictLabel = (code: string, value: any) => {
  const dict = dicts.value.find(d => d.dictCode === code);
  if (!dict) return value;
  const item = dict.items.find((i: any) => i.itemValue === String(value));
  if (!item) return value;
  return dictLabelZh[code]?.[item.itemLabel] || item.itemLabel;
};

const getDictOptions = (code: string) => {
  const dict = dicts.value.find(d => d.dictCode === code);
  if (!dict) return [];
  return dict.items.map((i: any) => ({
    label: dictLabelZh[code]?.[i.itemLabel] || i.itemLabel,
    value: i.itemValue
  }));
};

const getOrderStatusClass = (status: number) => {
  switch(status) {
    case 0: return 'repair';
    case 1: return 'occupied';
    case 2: return 'active';
    case 3: return 'repair';
    default: return '';
  }
};

const openModal = (order: any) => {
  form.value = { ...order };
  showModal.value = true;
};

const deleteOrder = async (id: number) => {
  if (!confirm('确定要永久删除此作废订单吗？')) return;
  try {
    await api.delete(`/orders/${id}`);
    fetchData();
    alert('删除成功');
  } catch (e: any) {
    alert('删除失败: ' + (e.response?.data || e.message));
  }
};

const scrollTo = (id: string) => {
  const el = document.getElementById(id);
  if (el) el.scrollIntoView({ behavior: 'smooth' });
};

onMounted(() => {
  resetFilters();
  fetchData();
});
</script>

<style scoped>
@import "../../assets/admin.css";

.search-panel {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

.search-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.search-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-item label {
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
}

.search-item input[type="text"],
.search-item input[type="date"] {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
}

.range-input {
  display: flex;
  align-items: center;
  gap: 10px;
}

.range-input input {
  flex: 1;
}

.status-options {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  background: #f8fafc;
  padding: 10px;
  border-radius: 8px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  cursor: pointer;
}

.search-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  border-top: 1px solid #f1f5f9;
  padding-top: 20px;
}

.reset-btn, .search-btn {
  padding: 8px 24px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.reset-btn {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #64748b;
}

.search-btn {
  background: #38bdf8;
  border: 1px solid #38bdf8;
  color: #fff;
}

 .table-toolbar {
   display: flex;
   justify-content: space-between;
   align-items: center;
   padding: 12px 20px;
   background: #f8fafc;
   border-bottom: 1px solid #e2e8f0;
   font-size: 14px;
   color: #64748b;
 }
 .toolbar-left {
   display: flex;
   align-items: center;
   gap: 24px;
 }

.page-size-selector select {
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 24px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}
.pagination.mini {
  padding: 10px;
  background: #fff;
  border-top: none;
  border-bottom: 1px solid #e2e8f0;
  justify-content: flex-end;
  gap: 10px;
  font-size: 13px;
}
.pagination.mini button {
  padding: 4px 10px;
  font-size: 12px;
}

.pagination button {
  padding: 6px 16px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #fff;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.jump-to input {
  width: 50px;
  padding: 4px;
  text-align: center;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
}

/* Readonly Modal Styles */
.readonly-view .value {
  padding: 8px 12px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 6px;
  font-size: 14px;
  color: #0f172a;
  min-height: 38px;
  display: flex;
  align-items: center;
}

.room-card.readonly {
  background: #fdfdfd;
  border-color: #f1f5f9;
}

.amount-display {
  font-size: 18px;
  font-weight: 700;
  color: #0369a1;
}

.amount-display.highlight {
  color: #ef4444;
}

.empty-row {
  padding: 40px;
  text-align: center;
  color: #94a3b8;
}

.span-2 { grid-column: span 2; }
.span-4 { grid-column: span 4; }

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 1200px;
  max-height: 95vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden;
  position: relative;
}

.modal-content.modal-maximized {
  max-width: none;
  width: 98vw;
  height: 98vh;
  max-height: none;
}

.modal-header {
  padding: 20px 32px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-left h2 {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.modal-nav {
  display: flex;
  gap: 16px;
}

.modal-nav a {
  font-size: 14px;
  color: #64748b;
  text-decoration: none;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.modal-nav a:hover {
  background: #f1f5f9;
  color: #38bdf8;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.maximize-btn, .close-btn {
  background: #f1f5f9;
  border: none;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  color: #64748b;
  font-size: 18px;
  transition: all 0.2s;
}

.maximize-btn:hover { background: #e2e8f0; color: #1e293b; }
.close-btn:hover { background: #fee2e2; color: #ef4444; }

.modal-body {
  padding: 0;
  overflow-y: auto;
  flex: 1;
  background: #fff;
}

.modal-body.scrollable {
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 #f8fafc;
}

.modal-footer {
  padding: 20px 32px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background: #f8fafc;
  flex-shrink: 0;
}

.form-section {
  padding: 32px;
  border-bottom: 1px solid #f1f5f9;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-title::before {
  content: '';
  width: 4px;
  height: 18px;
  background: #38bdf8;
  border-radius: 2px;
}

.form-grid-4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 12px;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.room-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}

.card-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #e2e8f0;
}

.card-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.room-no {
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
}

.room-type {
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.table-wrapper {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
}

.detail-table th {
  background: #f8fafc;
  padding: 12px 16px;
  text-align: left;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  border-bottom: 1px solid #e2e8f0;
}

.detail-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 14px;
}
</style>
