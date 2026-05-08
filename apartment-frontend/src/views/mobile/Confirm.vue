<template>
  <div class="confirm-page" v-if="order">
    <header class="mobile-header">
      <div class="header-left" @click="router.back()">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M15.41,16.58L10.83,12L15.41,7.41L14,6L8,12L14,18L15.41,16.58Z" /></svg>
      </div>
      <div class="mobile-header-title">{{ $t('confirm.title2') }}</div>
      <div class="header-right">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z" /></svg>
      </div>
    </header>

    <div class="alert-banner" v-if="!isExpired">
      <div class="alert-icon">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="white"><path d="M11,9H13V7H11M12,20C7.59,20 4,16.41 4,12C4,7.59 7.59,4 12,4C16.41,4 20,7.59 20,12C20,16.41 16.41,20 12,20M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M11,17H13V11H11V17Z" /></svg>
      </div>
      <div class="alert-text">{{ $t('confirm.timeoutMsg', { deadlineStr, countdownStr }) }}</div>
    </div>
    <div class="alert-banner expired" v-else>
      <div class="alert-text">{{ $t('confirm.expiredMsg') }}</div>
    </div>

    <div class="content">
      <!-- Stay Details -->
      <div class="mobile-card stay-card">
        <div class="stay-row">
          <div class="stay-item">
            <div class="stay-label">{{ $t('confirm.stayDate') }}</div>
            <div class="stay-val">{{ formatDate(order.startDate) }}</div>
          </div>
          <div class="stay-item align-right">
            <div class="stay-label">{{ $t('confirm.leaveDate') }}</div>
            <div class="stay-val">{{ formatDate(order.endDate) }}</div>
          </div>
        </div>
        <div class="divider"></div>
        <div class="stay-row">
          <div class="stay-item">
            <div class="stay-label">{{ $t('booking.roomType') }}</div>
            <div class="stay-val-mid">{{ roomTypeName }}</div>
          </div>
          <div class="stay-item align-right">
            <div class="stay-label">{{ $t('confirm.roomNo') }}</div>
            <div class="stay-val-mid primary-text">{{ roomNo }}</div>
          </div>
        </div>
      </div>

      <!-- Price Card -->
      <div class="mobile-card price-card">
        <div class="price-row">
          <span class="price-label">{{ $t('confirm.roomCharge') }}</span>
          <div class="price-val-group">
            <span class="price-total">¥ {{ (roomPrice * stayDays).toFixed(2) }}</span>
            <span class="price-detail">{{ $t('confirm.perNightMath2', { price: roomPrice, days: stayDays }) }}</span>
          </div>
        </div>
        <div class="divider"></div>
        <div class="price-row total-amount-row">
          <span class="price-label">{{ $t('confirm.totalAmount') }}</span>
          <span class="price-total large">¥ {{ (roomPrice * stayDays).toFixed(2) }}</span>
        </div>
      </div>

      <p class="billing-note">{{ $t('confirm.billingNote') }}</p>

      <!-- Occupant Card -->
      <div class="mobile-card occupant-card">
        <div class="card-header">
          <span class="card-title">{{ $t('confirm.occupant') }}</span>
          <span class="add-btn" @click="showCompanionInput = true">{{ $t('confirm.addCompanion') }}</span>
        </div>
        
        <div class="occupant-list">
          <div class="occupant-row">
            <span class="occ-name">{{ order.booker?.realName || order.booker?.username || t('confirm.currentUser') }}</span>
            <span class="occ-tag">{{ $t('confirm.myself') }}</span>
          </div>
          
          <!-- Existing Companions -->
          <div v-for="(name, index) in companions" :key="index" class="occupant-row secondary">
            <span class="occ-name">{{ name }}</span>
            <span class="remove-btn" @click="removeCompanion(index)">{{ $t('confirm.remove') }}</span>
          </div>

          <!-- New Companion Input -->
          <div v-if="showCompanionInput" class="companion-input-row">
            <input v-model="newCompanionName" :placeholder="$t('confirm.companionName')" class="occ-input" ref="nameInput">
            <button class="confirm-occ-btn" @click="addCompanion">{{ $t('confirm.confirmBtn') }}</button>
            <button class="cancel-occ-btn" @click="showCompanionInput = false">{{ $t('confirm.cancelBtn') }}</button>
          </div>
        </div>
      </div>

      <!-- Notice Row -->
      <div class="mobile-card notice-row">
        <span>{{ $t('confirm.notice') }}</span>
        <svg viewBox="0 0 24 24" width="20" height="20" fill="#ccc"><path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z" /></svg>
      </div>
    </div>

    <!-- Sticky Bottom -->
    <div class="bottom-bar">
      <div class="bar-total">
        <div class="total-label">{{ $t('confirm.totalLabel') }}</div>
        <div class="total-price">¥ {{ (roomPrice * stayDays).toFixed(2) }}</div>
      </div>
      <button class="bar-btn cancel" @click="cancelOrder">{{ $t('confirm.cancelOrder') }}</button>
      <button class="bar-btn submit" :disabled="isExpired" @click="submitOrder">{{ $t('confirm.submit') }}</button>
    </div>
  </div>
  <div v-else class="loading-state">
    {{ $t('booking.loading') }}
  </div>
</template>

<script setup lang="ts">

import { useI18n } from 'vue-i18n';
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import api from '../../utils/api';

const router = useRouter();
const { t } = useI18n();

const route = useRoute();
const orderId = route.query.orderId;

const order = ref<any>(null);
const companions = ref<string[]>([]);
const showCompanionInput = ref(false);
const newCompanionName = ref('');
const countdown = ref(900); // 15 mins in seconds
const isExpired = ref(false);
let timer: any = null;

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

const deadlineStr = computed(() => {
  if (!order.value?.createdAt) return '-';
  const created = new Date(order.value.createdAt);
  const deadline = new Date(created.getTime() + 15 * 60000);
  return deadline.toLocaleTimeString();
});

const countdownStr = computed(() => {
  const m = Math.floor(countdown.value / 60);
  const s = countdown.value % 60;
  return `${m}:${s < 10 ? '0' : ''}${s}`;
});

const fetchOrder = async () => {
  try {
    const res = await api.get(`/orders/mine`) as any[];
    const found = res.find(o => o.id.toString() === orderId);
    if (found) {
      order.value = found;
      // Initialize companions from DB if any
      const coStr = found.roomOccupies?.[0]?.coOccupants;
      if (coStr) {
        companions.value = coStr.split(',').filter(Boolean);
      }
      
      // Start countdown based on createdAt
      const created = new Date(found.createdAt || Date.now());
      const elapsed = Math.floor((Date.now() - created.getTime()) / 1000);
      countdown.value = Math.max(0, 900 - elapsed);
      if (countdown.value <= 0) isExpired.value = true;
      
      startTimer();
    }
  } catch (e) {
    console.error('Failed to fetch order', e);
  }
};

const startTimer = () => {
  if (timer) clearInterval(timer);
  timer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--;
    } else {
      isExpired.value = true;
      clearInterval(timer);
    }
  }, 1000);
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`;
};

const addCompanion = () => {
  if (newCompanionName.value.trim()) {
    companions.value.push(newCompanionName.value.trim());
    newCompanionName.value = '';
    showCompanionInput.value = false;
  }
};

const removeCompanion = (index: number) => {
  companions.value.splice(index, 1);
};

const cancelOrder = async () => {
  if (confirm(t('confirm.cancelConfirm'))) {
    try {
      await api.post(`/orders/${orderId}/cancel`);
      router.push('/m/booking');
    } catch (e) {
      console.error('Cancel failed', e);
    }
  }
};

const submitOrder = async () => {
  try {
    const updatedOrder = JSON.parse(JSON.stringify(order.value));
    updatedOrder.status = 1; // Pending
    if (updatedOrder.roomOccupies && updatedOrder.roomOccupies[0]) {
      updatedOrder.roomOccupies[0].coOccupants = companions.value.join(',');
      // Clean up to avoid recursion issues if any, but keep ID for mapping
      updatedOrder.roomOccupies[0].order = { id: Number(orderId) };
    }
    
    await api.post('/orders', updatedOrder);
    
    alert(t('confirm.bookSuccess'));
    router.push('/m/records');
  } catch (e: any) {
    alert(t('confirm.submitFail') + (e.response?.data?.message || e.message));
  }
};

onMounted(fetchOrder);
onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<style scoped>
.confirm-page {
  padding-bottom: 100px;
}

.alert-banner {
  background: #e6f4ff;
  border: 1px solid #91caff;
  margin: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.alert-icon {
  background: #1677ff;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.alert-text {
  font-size: 13px;
  color: #000;
  line-height: 1.4;
}

.alert-banner.expired {
  background: #fff1f0;
  border-color: #ffa39e;
}

.alert-banner.expired .alert-text {
  color: #cf1322;
}

.stay-card {
  padding: 16px;
}

.stay-row {
  display: flex;
  justify-content: space-between;
}

.stay-item {
  display: flex;
  flex-direction: column;
}

.align-right {
  text-align: right;
}

.stay-label {
  font-size: 12px;
  color: var(--mobile-text-secondary);
  margin-bottom: 6px;
}

.stay-val {
  font-size: 16px;
  font-weight: bold;
}

.stay-val-mid {
  font-size: 15px;
  font-weight: 500;
}

.stay-day {
  font-size: 12px;
  color: var(--mobile-text-secondary);
}

.primary-text {
  color: var(--mobile-primary);
}

.divider {
  height: 1px;
  background: #f0f0f0;
  margin: 16px 0;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price-label {
  font-size: 15px;
  font-weight: 600;
}

.price-total {
  font-size: 16px;
  font-weight: bold;
}

.price-total.large {
  font-size: 20px;
  color: var(--mobile-primary);
}

.total-amount-row {
  margin-top: 12px;
}

.price-detail {
  font-size: 12px;
  color: var(--mobile-text-secondary);
  margin-left: 4px;
}

.billing-note {
  font-size: 12px;
  color: var(--mobile-text-secondary);
  margin: 0 16px 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
}

.add-btn {
  color: var(--mobile-primary);
  font-size: 14px;
}

.occupant-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.occupant-row {
  background: #f8f8f8;
  padding: 12px;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.occupant-row.secondary {
  background: #fff;
  border: 1px dashed #ddd;
}

.occ-name {
  font-size: 14px;
}

.occ-tag {
  background: var(--mobile-primary);
  color: #fff;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
}

.remove-btn {
  color: #ff4d4f;
  font-size: 12px;
}

.companion-input-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.occ-input {
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 8px;
  font-size: 14px;
}

.confirm-occ-btn, .cancel-occ-btn {
  border: none;
  border-radius: 4px;
  padding: 0 12px;
  font-size: 12px;
  cursor: pointer;
}

.confirm-occ-btn {
  background: var(--mobile-primary);
  color: #fff;
}

.cancel-occ-btn {
  background: #eee;
  color: #666;
}

.notice-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  height: 60px;
  display: flex;
  align-items: stretch;
  border-top: 1px solid #f0f0f0;
  z-index: 1001;
}

.bar-total {
  flex: 1;
  padding-left: 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.total-label {
  font-size: 11px;
  color: #999;
}

.total-price {
  font-size: 16px;
  font-weight: bold;
}

.bar-btn {
  border: none;
  font-size: 15px;
  font-weight: 500;
  padding: 0 24px;
  cursor: pointer;
}

.bar-btn.cancel {
  background: #fff;
  color: #333;
}

.bar-btn.submit {
  background: var(--mobile-primary);
  color: #fff;
}
</style>
