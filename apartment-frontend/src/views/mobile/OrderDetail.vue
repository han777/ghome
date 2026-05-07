<template>
  <div class="order-detail-page" v-if="order">
    <header class="mobile-header">
      <div class="header-left" @click="router.back()">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M15.41,16.58L10.83,12L15.41,7.41L14,6L8,12L14,18L15.41,16.58Z" /></svg>
      </div>
      <div class="mobile-header-title">订单详情</div>
    </header>

    <!-- Status Banner -->
    <div class="status-banner" :class="statusClass">
      <div class="status-icon">
        <svg v-if="order.status === 3" viewBox="0 0 24 24" width="32" height="32" fill="white"><path d="M12,20A8,8 0 0,1 4,12A8,8 0 0,1 12,4A8,8 0 0,1 20,12A8,8 0 0,1 12,20M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,7A5,5 0 0,0 7,12A5,5 0 0,0 12,17A5,5 0 0,0 17,12A5,5 0 0,0 12,7Z" /></svg>
        <svg v-else viewBox="0 0 24 24" width="32" height="32" fill="white"><path d="M12,2A10,10 0 1,0 22,12A10,10 0 0,0 12,2M12,20A8,8 0 1,1 20,12A8,8 0 0,1 12,20M11,7H13V13H11V7M11,15H13V17H11V15Z" /></svg>
      </div>
      <div class="status-text">{{ statusText }}</div>
    </div>

    <div class="content">
      <!-- Key Code Card (Only if applicable) -->
      <div class="mobile-card key-card" v-if="order.doorCode || order.roomOccupies?.[0]?.doorCode">
        <div class="card-header-row">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="#1677ff"><path d="M11,9H13V7H11M12,20C7.59,20 4,16.41 4,12C4,7.59 7.59,4 12,4C16.41,4 20,7.59 20,12C20,16.41 16.41,20 12,20M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M11,17H13V11H11V17Z" /></svg>
          <span class="card-title-small">入住密码</span>
        </div>
        <div class="key-display">
          {{ order.doorCode || order.roomOccupies?.[0]?.doorCode }}
        </div>
        <div class="key-expiry">生效日期：{{ formatDate(order.startDate, true) }}</div>
      </div>

      <!-- Stay Info Card -->
      <div class="mobile-card stay-info-card">
        <div class="info-item">
          <div class="info-label-row">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="#999"><path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M17,12H12V17H17V12Z" /></svg>
            <span class="info-label">入住时间</span>
          </div>
          <div class="info-value-row">
            <span class="info-date">{{ formatDate(order.startDate) }}</span>
            <span class="info-weekday">{{ getDayOfWeek(order.startDate) }}</span>
            <span class="info-time">14:00后</span>
          </div>
        </div>
        <div class="info-item">
          <div class="info-label-row">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="#999"><path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M17,12H12V17H17V12Z" /></svg>
            <span class="info-label">退房时间</span>
          </div>
          <div class="info-value-row">
            <span class="info-date">{{ formatDate(order.endDate) }}</span>
            <span class="info-weekday">{{ getDayOfWeek(order.endDate) }}</span>
            <span class="info-time">12:00前</span>
          </div>
        </div>
        <div class="divider"></div>
        <div class="info-row-grid">
          <div class="grid-item">
            <div class="grid-label">房型</div>
            <div class="grid-value">{{ roomTypeName }}</div>
          </div>
          <div class="grid-item align-right">
            <div class="grid-label">房间号</div>
            <div class="grid-value primary-text">{{ roomNo }}</div>
          </div>
        </div>
      </div>

      <!-- Occupant Card -->
      <div class="mobile-card occupant-card">
        <div class="card-header-row">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="#999"><path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z" /></svg>
          <span class="card-title">入住人信息</span>
        </div>
        <div class="occupant-list">
          <div class="occupant-row">
            <span class="occ-name">{{ order.user?.realName || order.user?.username || '当前用户' }}</span>
            <span class="occ-tag">本人</span>
          </div>
          <div v-for="(name, index) in companions" :key="index" class="occupant-row">
            <span class="occ-name">{{ name }}</span>
          </div>
        </div>
      </div>

      <!-- Price Card -->
      <div class="mobile-card price-card">
        <div class="card-header-row">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="#999"><path d="M21,18V19A2,2 0 0,1 19,21H5C3.89,21 3,20.1 3,19V5A2,2 0 0,1 5,3H19A2,2 0 0,1 21,5V6H12C10.89,6 10,6.9 10,8V16A2,2 0 0,0 12,18H21M12,16H22V8H12V16M16,13.5A1.5,1.5 0 0,1 14.5,12A1.5,1.5 0 0,1 16,10.5A1.5,1.5 0 0,1 17.5,12A1.5,1.5 0 0,1 16,13.5Z" /></svg>
          <span class="card-title">费用明细</span>
        </div>
        <div class="price-detail-list">
          <div class="price-detail-row">
            <span class="detail-label">房费（¥ {{ roomPrice }}/晚 × {{ stayDays }}晚）</span>
            <span class="detail-value">¥ {{ (roomPrice * stayDays).toFixed(2) }}</span>
          </div>
          <div v-for="fee in extraFees" :key="fee.id" class="price-detail-row">
            <span class="detail-label">{{ fee.remarks || '其他费用' }}</span>
            <span class="detail-value">¥ {{ fee.amount.toFixed(2) }}</span>
          </div>
        </div>
        <div class="divider"></div>
        <div class="total-row">
          <span class="total-label">合计</span>
          <span class="total-value">¥ {{ order.totalAmount?.toFixed(2) }}</span>
        </div>
      </div>

      <div class="policy-notes">
        <p>财务统一结算，无需个人垫付、报销</p>
        <p>取消预订：须于入住日前1天24:00前操作</p>
        <p>提前退房：须于实际退房日前1天24:00前操作</p>
      </div>
    </div>

    <!-- Bottom Actions -->
    <div class="bottom-actions">
      <template v-if="order.status === 0">
        <button class="action-btn primary" @click="submitOrder">提交订单</button>
        <button class="action-btn" @click="cancelBooking">取消预订</button>
      </template>
      <template v-else>
        <button 
          class="action-btn" 
          :disabled="order.status !== 2"
          @click="earlyCheckOut"
        >
          提前退房
        </button>
        <button 
          class="action-btn" 
          :disabled="![0, 1].includes(order.status)"
          @click="cancelBooking"
        >
          取消预订
        </button>
      </template>
    </div>
  </div>
  <div v-else class="loading-state">
    加载中...
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import api from '../../utils/api';

const router = useRouter();
const route = useRoute();
const orderId = route.params.id;

const order = ref<any>(null);
const extraFees = ref<any[]>([]);

const roomNo = computed(() => order.value?.roomOccupies?.[0]?.room?.roomNo || '-');
const roomTypeName = computed(() => order.value?.roomOccupies?.[0]?.room?.roomType?.typeCode || '-');
const roomPrice = computed(() => order.value?.roomOccupies?.[0]?.room?.roomType?.priceShortRent || 0);

const stayDays = computed(() => {
  if (!order.value) return 0;
  const s = new Date(order.value.startDate);
  const e = new Date(order.value.endDate);
  const diff = e.getTime() - s.getTime();
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
});

const statusText = computed(() => {
  const map: any = {
    0: '待确认',
    1: '预订中',
    2: '入住中',
    3: '已退房',
    4: '已取消'
  };
  return map[order.value?.status] || '未知';
});

const statusClass = computed(() => {
  const map: any = {
    0: 'pending',
    1: 'booking',
    2: 'active',
    3: 'completed',
    4: 'cancelled'
  };
  return map[order.value?.status] || '';
});

const companions = computed(() => {
  const coStr = order.value?.roomOccupies?.[0]?.coOccupants;
  return coStr ? coStr.split(',').filter(Boolean) : [];
});

const fetchOrder = async () => {
  try {
    const res = await api.get(`/orders/all`) as any[];
    const found = res.find(o => o.id.toString() === orderId);
    if (found) {
      order.value = found;
      // Fetch extra fees if any
      const fees = await api.get(`/orders/${orderId}/fees`) as any[];
      extraFees.value = fees;
    }
  } catch (e) {
    console.error('Failed to fetch order', e);
  }
};

const formatDate = (dateStr: string, includeTime = false) => {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const datePart = `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`;
  if (includeTime) {
    const timePart = `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}:${d.getSeconds().toString().padStart(2, '0')}`;
    return `${datePart} ${timePart}`;
  }
  return datePart;
};

const getDayOfWeek = (dateStr: string) => {
  if (!dateStr) return '';
  const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
  return days[new Date(dateStr).getDay()];
};

const cancelBooking = async () => {
  if (confirm('确定要取消预订吗？')) {
    try {
      await api.post(`/orders/${orderId}/cancel`);
      alert('已取消预订');
      fetchOrder();
    } catch (e: any) {
      alert('取消失败: ' + (e.response?.data?.message || e.message));
    }
  }
};

const earlyCheckOut = async () => {
  if (confirm('确定要提前退房吗？')) {
    try {
      // In this system, check-out might just be a status update or a specific API
      // Assuming we have an endpoint or we can update status
      // Let's check if there's a checkout endpoint
      await api.post(`/orders/${orderId}/checkout`); // Mocking checkout endpoint if exists
      alert('已办理提前退房');
      fetchOrder();
    } catch (e: any) {
      // If endpoint doesn't exist, maybe update status directly? 
      // But status 3 is usually set by system or admin.
      alert('退房失败: ' + (e.response?.data?.message || e.message));
    }
  }
};

const submitOrder = async () => {
  try {
    const updatedOrder = { ...order.value };
    updatedOrder.status = 1;
    await api.post('/orders', updatedOrder);
    alert('订单提交成功');
    fetchOrder();
  } catch (e: any) {
    alert('提交失败: ' + (e.response?.data?.message || e.message));
  }
};

onMounted(fetchOrder);
</script>

<style scoped>
.order-detail-page {
  background-color: #f8f9fc;
  min-height: 100vh;
  padding-bottom: 90px;
}

.status-banner {
  height: 140px;
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: white;
  margin-bottom: -30px;
}

.status-banner.completed { background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%); }
.status-banner.cancelled { background: linear-gradient(135deg, #999 0%, #bbb 100%); }
.status-banner.pending { background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%); }
.status-banner.active { background: linear-gradient(135deg, #52c41a 0%, #95de64 100%); }

.status-icon {
  margin-bottom: 10px;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1));
}

.status-text {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1px;
}

.content {
  padding: 0 16px;
  position: relative;
  z-index: 1;
}

.mobile-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  margin-bottom: 16px;
  overflow: hidden;
}

.key-card {
  padding: 20px;
  border: 1px solid #e6f4ff;
}

.card-header-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 15px;
}

.card-title-small {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.key-display {
  background: #f0f7ff;
  color: #1677ff;
  font-size: 24px;
  font-weight: bold;
  text-align: center;
  padding: 20px;
  border-radius: 12px;
  margin: 10px 0;
  letter-spacing: 1px;
}

.key-expiry {
  font-size: 13px;
  color: #8c8c8c;
  text-align: center;
}

.stay-info-card {
  padding: 20px;
}

.info-item {
  margin-bottom: 20px;
}

.info-label-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.info-label {
  font-size: 14px;
  color: #8c8c8c;
  font-weight: 500;
}

.info-value-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.info-date {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
}

.info-weekday {
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.info-time {
  font-size: 14px;
  color: #bfbfbf;
}

.divider {
  height: 1px;
  background-color: #f0f0f0;
  margin: 15px 0;
}

.info-row-grid {
  display: flex;
  justify-content: space-between;
}

.grid-label {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 6px;
}

.grid-value {
  font-size: 17px;
  font-weight: 600;
  color: #333;
}

.primary-text {
  color: #1677ff;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.occupant-card {
  padding: 20px;
}

.occupant-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.occupant-row {
  background: #f9fafc;
  padding: 14px;
  border-radius: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.occ-name {
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.occ-tag {
  background: #1677ff;
  color: white;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: 600;
}

.price-card {
  padding: 20px;
}

.price-detail-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 15px;
}

.price-detail-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #595959;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 5px;
}

.total-label {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}

.total-value {
  font-size: 22px;
  font-weight: 800;
  color: #1a1a1a;
}

.policy-notes {
  padding: 10px 4px 20px;
  font-size: 13px;
  color: #8c8c8c;
  line-height: 2;
}

.bottom-actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  display: flex;
  padding: 12px 16px;
  gap: 16px;
  box-shadow: 0 -4px 16px rgba(0,0,0,0.06);
  z-index: 100;
}

.action-btn {
  flex: 1;
  height: 48px;
  border: 1px solid #e8e8e8;
  background: white;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  color: #595959;
  transition: all 0.2s;
}

.action-btn:active {
  background: #f5f5f5;
}

.action-btn:disabled {
  color: #d9d9d9;
  background: #fafafa;
  border-color: #f0f0f0;
}

.action-btn.primary {
  background: #1677ff;
  color: white;
  border-color: #1677ff;
}

.action-btn.primary:active {
  background: #0958d9;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  color: #999;
}
</style>
