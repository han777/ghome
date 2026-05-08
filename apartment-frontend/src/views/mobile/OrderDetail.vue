<template>
  <div class="order-detail-page" v-if="order">
    <header class="mobile-header">
      <div class="header-left" @click="router.back()">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M15.41,16.58L10.83,12L15.41,7.41L14,6L8,12L14,18L15.41,16.58Z" /></svg>
      </div>
      <div class="mobile-header-title">{{ $t('orderDetail.title') }}</div>
    </header>

    <!-- 1. Status Banner -->
    <div class="status-banner" :class="statusClass">
      <div class="status-icon">
        <svg v-if="order.status === 3" viewBox="0 0 24 24" width="32" height="32" fill="white"><path d="M12,20A8,8 0 0,1 4,12A8,8 0 0,1 12,4A8,8 0 0,1 20,12A8,8 0 0,1 12,20M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,7A5,5 0 0,0 7,12A5,5 0 0,0 12,17A5,5 0 0,0 17,12A5,5 0 0,0 12,7Z" /></svg>
        <svg v-else viewBox="0 0 24 24" width="32" height="32" fill="white"><path d="M12,2A10,10 0 1,0 22,12A10,10 0 0,0 12,2M12,20A8,8 1,1 20,12A8,8 0 0,1 12,20M11,7H13V13H11V7M11,15H13V17H11V15Z" /></svg>
      </div>
      <div class="status-text">{{ statusText }}</div>
    </div>

    <div class="content">
      <!-- 1a. Key Code Card -->
      <div class="mobile-card key-card" v-if="order.doorCode || order.roomOccupies?.[0]?.doorCode">
        <div class="card-header-row">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="#1677ff"><path d="M11,9H13V7H11M12,20C7.59,20 4,16.41 4,12C4,7.59 7.59,4 12,4C16.41,4 20,7.59 20,12C20,16.41 16.41,20 12,20M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M11,17H13V11H11V17Z" /></svg>
          <span class="card-title-small">{{ $t('orderDetail.doorPassword') }}</span>
        </div>
        <div class="key-display">{{ order.doorCode || order.roomOccupies?.[0]?.doorCode }}</div>
        <div class="key-expiry">{{ $t('orderDetail.effectiveDate') }}{{ formatDate(order.startDate, true) }}</div>
      </div>

      <!-- 1b. Stay Info Card -->
      <div class="mobile-card stay-info-card">
        <div class="info-row-grid top-row">
          <div class="grid-item">
            <div class="grid-label">{{ $t('orderDetail.orderNo') }}</div>
            <div class="grid-value order-no-text">{{ order.orderNo || order.id }}</div>
          </div>
          <div class="grid-item align-right">
            <div class="grid-label">{{ $t('orderDetail.status') }}</div>
            <div class="grid-value"><span class="status-chip" :class="statusClass">{{ statusText }}</span></div>
          </div>
        </div>
        <div class="divider"></div>
        <div class="info-item">
          <div class="info-label-row">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="#999"><path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M17,12H12V17H17V12Z" /></svg>
            <span class="info-label">{{ $t('booking.checkIn') }}</span>
          </div>
          <div class="info-value-row">
            <span class="info-date">{{ formatDate(order.startDate) }}</span>
            <span class="info-weekday">{{ getDayOfWeek(order.startDate) }}</span>
            <span class="info-time">{{ $t('orderDetail.after14') }}</span>
          </div>
        </div>
        <div class="info-item">
          <div class="info-label-row">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="#999"><path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M17,12H12V17H17V12Z" /></svg>
            <span class="info-label">{{ $t('booking.checkOut') }}</span>
          </div>
          <div class="info-value-row">
            <span class="info-date">{{ formatDate(order.endDate) }}</span>
            <span class="info-weekday">{{ getDayOfWeek(order.endDate) }}</span>
            <span class="info-time">{{ $t('orderDetail.before12') }}</span>
          </div>
        </div>
      </div>

      <!-- 2. Room Card List -->
      <div class="section-label">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="#1677ff"><path d="M10,20V14H14V20H19V12H22L12,3L2,12H5V20H10Z"/></svg>
        {{ $t('orderDetail.roomsSection') }}
      </div>
      <div v-for="occupy in order.roomOccupies" :key="occupy.id" class="mobile-card room-occupy-card">
        <div class="ro-row">
          <div class="ro-col">
            <div class="ro-label">{{ $t('orderDetail.roomNoLabel') }}</div>
            <div class="ro-value primary-text">{{ occupy.room?.roomNo || '-' }}</div>
          </div>
          <div class="ro-col">
            <div class="ro-label">{{ $t('orderDetail.roomTypeLabel') }}</div>
            <div class="ro-value">{{ getRoomTypeName(occupy.room?.roomType) }}</div>
          </div>
        </div>
        <div class="ro-divider"></div>
        <div class="ro-row">
          <div class="ro-col">
            <div class="ro-label">{{ $t('orderDetail.checkInTime') }}</div>
            <div class="ro-value-sm">{{ formatDate(occupy.checkInTime) }}</div>
          </div>
          <div class="ro-col">
            <div class="ro-label">{{ $t('orderDetail.checkOutTime') }}</div>
            <div class="ro-value-sm">{{ formatDate(occupy.checkOutTime) }}</div>
          </div>
        </div>
        <div class="ro-divider"></div>
        <div class="ro-occupants">
          <div class="ro-label mb6">{{ $t('orderDetail.occupantInfo') }}</div>
          <div class="occ-chip-row">
            <span class="occ-chip self">
              {{ order.booker?.realName || order.booker?.username || $t('confirm.currentUser') }}
              <span class="chip-tag">{{ $t('orderDetail.myself') }}</span>
            </span>
            <span v-for="(name, idx) in parseCoOccupants(occupy.coOccupants)" :key="idx" class="occ-chip">{{ name }}</span>
          </div>
        </div>
      </div>

      <!-- 3. Room Fee Card -->
      <div class="section-label">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="#1677ff"><path d="M21,18V19A2,2 0 0,1 19,21H5C3.89,21 3,20.1 3,19V5A2,2 0 0,1 5,3H19A2,2 0 0,1 21,5V6H12C10.89,6 10,6.9 10,8V16A2,2 0 0,0 12,18H21M12,16H22V8H12V16M16,13.5A1.5,1.5 0 0,1 14.5,12A1.5,1.5 0 0,1 16,10.5A1.5,1.5 0 0,1 17.5,12A1.5,1.5 0 0,1 16,13.5Z"/></svg>
        {{ $t('confirm.roomCharge') }}
      </div>
      <div class="mobile-card price-card">
        <div class="price-detail-list">
          <div class="price-detail-row">
            <span class="detail-label">{{ $t('confirm.roomCharge') }}{{ $t('confirm.perNightMath', { price: roomPrice, days: stayDays }) }}</span>
            <span class="detail-value">¥ {{ (roomPrice * stayDays).toFixed(2) }}</span>
          </div>
          <div v-for="fee in extraFees" :key="fee.id" class="price-detail-row">
            <span class="detail-label">{{ fee.remarks || t('orderDetail.otherFee') }}</span>
            <span class="detail-value">¥ {{ fee.amount.toFixed(2) }}</span>
          </div>
        </div>
        <div class="divider"></div>
        <div class="total-row">
          <span class="total-label">{{ $t('orderDetail.totalLabel') }}</span>
          <span class="total-value">¥ {{ order.roomFee?.toFixed(2) || (roomPrice * stayDays).toFixed(2) }}</span>
        </div>
      </div>

      <!-- 4. Services Card List -->
      <div class="section-label">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="#1677ff"><path d="M20,6H16V4A2,2 0 0,0 14,2H10A2,2 0 0,0 8,4V6H4C2.89,6 2.01,6.89 2.01,8L2,19A2,2 0 0,0 4,21H20A2,2 0 0,0 22,19V8A2,2 0 0,0 20,6M10,4H14V6H10V4Z"/></svg>
        {{ $t('orderDetail.servicesSection') }}
      </div>
      <div v-if="order.productDetails && order.productDetails.length > 0">
        <div v-for="svc in order.productDetails" :key="svc.id" class="mobile-card service-card">
          <div class="svc-name">{{ svc.product?.productName || svc.productTitle || $t('orderDetail.serviceName') }}</div>
          <div class="svc-meta-row">
            <div class="svc-meta-item">
              <span class="svc-meta-label">{{ $t('orderDetail.qty') }}</span>
              <span class="svc-meta-val">{{ svc.quantity || 1 }}</span>
            </div>
            <div class="svc-meta-item">
              <span class="svc-meta-label">{{ $t('orderDetail.unitPrice') }}</span>
              <span class="svc-meta-val">¥{{ Number(svc.actualPrice || 0).toFixed(2) }}</span>
            </div>
            <div class="svc-meta-item">
              <span class="svc-meta-label">{{ $t('orderDetail.subTotal') }}</span>
              <span class="svc-meta-val primary-text">¥{{ (Number(svc.actualPrice || 0) * (svc.quantity || 1)).toFixed(2) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="mobile-card no-service-card">
        <span class="no-service-text">{{ $t('orderDetail.noServices') }}</span>
      </div>

      <!-- 5. Service Fee Total -->
      <div class="fee-summary-row" v-if="order.serviceFee > 0">
        <span class="fee-summary-label">{{ $t('orderDetail.servicesSection') }}{{ $t('orderDetail.totalLabel') }}</span>
        <span class="fee-summary-val">¥ {{ Number(order.serviceFee || 0).toFixed(2) }}</span>
      </div>

      <!-- 6. Order Total Amount Banner -->
      <div class="total-amount-banner">
        <span class="tab-label">{{ $t('orderDetail.totalAmount') }}</span>
        <span class="tab-amount">¥ {{ Number(order.totalAmount || 0).toFixed(2) }}</span>
      </div>

      <!-- 7. Policy Notes -->
      <div class="policy-notes">
        <p>{{ $t('orderDetail.financeNote') }}</p>
        <p>{{ $t('orderDetail.cancelRule') }}</p>
        <p>{{ $t('orderDetail.checkoutRule') }}</p>
      </div>
    </div>

    <!-- Bottom Actions -->
    <div class="bottom-actions">
      <template v-if="order.status === 0">
        <button class="action-btn primary" @click="submitOrder">{{ $t('confirm.submit') }}</button>
        <button class="action-btn" @click="cancelBooking">{{ $t('orderDetail.cancelBtn') }}</button>
      </template>
      <template v-else>
        <button class="action-btn" :disabled="order.status !== 2" @click="earlyCheckOut">{{ $t('orderDetail.earlyCheckoutBtn') }}</button>
        <button class="action-btn" :disabled="![0, 1].includes(order.status)" @click="cancelBooking">{{ $t('orderDetail.cancelBtn') }}</button>
      </template>
    </div>
  </div>
  <div v-else class="loading-state">
    {{ $t('booking.loading') }}
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { translateField } from '../../utils/lang';
import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import api from '../../utils/api';

const router = useRouter();
const { t, locale } = useI18n();
const route = useRoute();
const orderId = route.params.id;

const order = ref<any>(null);
const extraFees = ref<any[]>([]);

// roomNo and roomTypeName are rendered per-occupy via getRoomTypeName()
const roomPrice = computed(() => order.value?.roomOccupies?.[0]?.room?.roomType?.priceShortRent || 0);

const stayDays = computed(() => {
  if (!order.value) return 0;
  const s = new Date(order.value.startDate);
  const e = new Date(order.value.endDate);
  return Math.ceil((e.getTime() - s.getTime()) / (1000 * 60 * 60 * 24));
});

const statusText = computed(() => {
  const map: any = {
    0: t('records.status0'), 1: t('records.status1'),
    2: t('records.status2'), 3: t('records.status3'), 4: t('records.status4')
  };
  return map[order.value?.status] || t('records.unknown');
});

const statusClass = computed(() => {
  const map: any = { 0: 'pending', 1: 'booking', 2: 'active', 3: 'completed', 4: 'cancelled' };
  return map[order.value?.status] || '';
});

// companions are rendered per-occupy via parseCoOccupants()

const getRoomTypeName = (roomType: any): string => {
  if (!roomType) return '-';
  return translateField(roomType.nameIntlJson, locale.value) || roomType.typeCode || '-';
};

const parseCoOccupants = (coStr: string): string[] =>
  coStr ? coStr.split(',').filter(Boolean) : [];

const fetchOrder = async () => {
  try {
    const res = await api.get('/orders/mine') as any[];
    const found = res.find((o: any) => o.id.toString() === orderId);
    if (found) {
      order.value = found;
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
  const datePart = `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`;
  if (includeTime) {
    const timePart = `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}:${d.getSeconds().toString().padStart(2, '0')}`;
    return `${datePart} ${timePart}`;
  }
  return datePart;
};

const getDayOfWeek = (dateStr: string) => {
  if (!dateStr) return '';
  const days = [t('booking.sun'), t('booking.mon'), t('booking.tue'), t('booking.wed'), t('booking.thu'), t('booking.fri'), t('booking.sat')];
  return days[new Date(dateStr).getDay()];
};

const cancelBooking = async () => {
  if (confirm(t('orderDetail.cancelConfirmMsg'))) {
    try {
      await api.post(`/orders/${orderId}/cancel`);
      alert(t('orderDetail.cancelSuccess'));
      fetchOrder();
    } catch (e: any) {
      alert(t('orderDetail.cancelFail') + (e.response?.data?.message || e.message));
    }
  }
};

const earlyCheckOut = async () => {
  if (confirm(t('orderDetail.checkoutConfirmMsg'))) {
    try {
      await api.post(`/orders/${orderId}/checkout`);
      alert(t('orderDetail.checkoutSuccess'));
      fetchOrder();
    } catch (e: any) {
      alert(t('orderDetail.checkoutFail') + (e.response?.data?.message || e.message));
    }
  }
};

const submitOrder = async () => {
  try {
    const updatedOrder = { ...order.value };
    updatedOrder.status = 1;
    await api.post('/orders', updatedOrder);
    alert(t('orderDetail.submitSuccess'));
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
  padding: 0 20px 20px;
}

.info-row-grid {
  display: flex;
  justify-content: space-between;
  padding: 16px 0;
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
  margin-bottom: 20px;
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

/* ── Section Label ── */
.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 8px 0 8px;
}

/* ── Room Occupy Card ── */
.room-occupy-card {
  padding: 16px 20px;
}

.ro-row {
  display: flex;
  gap: 12px;
}

.ro-col {
  flex: 1;
}

.ro-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.ro-label.mb6 { margin-bottom: 6px; }

.ro-value {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}

.ro-value-sm {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.ro-divider {
  height: 1px;
  background: #f0f0f0;
  margin: 12px 0;
}

.ro-occupants {
  margin-top: 4px;
}

.occ-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.occ-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #f4f6fb;
  border: 1px solid #e8ecf4;
  color: #333;
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 20px;
}

.occ-chip.self {
  background: #e6f4ff;
  border-color: #91caff;
  color: #1677ff;
}

.chip-tag {
  font-size: 10px;
  font-weight: 600;
  background: #1677ff;
  color: #fff;
  padding: 1px 5px;
  border-radius: 8px;
}

/* ── Service Card ── */
.service-card {
  padding: 16px 20px;
}

.svc-name {
  font-size: 15px;
  font-weight: 600;
  color: #222;
  margin-bottom: 12px;
}

.svc-meta-row {
  display: flex;
  gap: 8px;
}

.svc-meta-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.svc-meta-label {
  font-size: 11px;
  color: #999;
}

.svc-meta-val {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.no-service-card {
  padding: 20px;
  text-align: center;
}

.no-service-text {
  font-size: 13px;
  color: #bfbfbf;
}

/* ── Total Amount Banner ── */
.total-amount-banner {
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
  border-radius: 12px;
  padding: 18px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.tab-label {
  font-size: 26px;
  font-weight: 800;
  color: #fff;
}

.tab-amount {
  font-size: 26px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.5px;
}

/* ── Fee Summary Row ── */
.fee-summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 12px;
  padding: 14px 20px;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.fee-summary-label {
  font-size: 14px;
  color: #595959;
}

.fee-summary-val {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}

/* ── Status Chip (in stay info card) ── */
.status-chip {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
}
.status-chip.pending  { background: #fff7e6; color: #fa8c16; border: 1px solid #ffd591; }
.status-chip.booking  { background: #e6f4ff; color: #1677ff; border: 1px solid #91caff; }
.status-chip.active   { background: #f9f0ff; color: #722ed1; border: 1px solid #d3adf7; }
.status-chip.completed{ background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.status-chip.cancelled{ background: #fff1f0; color: #f5222d; border: 1px solid #ffa39e; }

/* ── Order No style ── */
.order-no-text {
  font-size: 14px;
  font-weight: 600;
  color: #595959;
  word-break: break-all;
}
</style>
