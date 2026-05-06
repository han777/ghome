<template>
  <div class="confirm-page">
    <header class="mobile-header">
      <div class="header-left" @click="router.back()">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M15.41,16.58L10.83,12L15.41,7.41L14,6L8,12L14,18L15.41,16.58Z" /></svg>
      </div>
      <div class="mobile-header-title">预订确认</div>
      <div class="header-right">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z" /></svg>
      </div>
    </header>

    <div class="alert-banner">
      <div class="alert-icon">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="white"><path d="M11,9H13V7H11M12,20C7.59,20 4,16.41 4,12C4,7.59 7.59,4 12,4C16.41,4 20,7.59 20,12C20,16.41 16.41,20 12,20M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M11,17H13V11H11V17Z" /></svg>
      </div>
      <div class="alert-text">请于2026-04-10 14:21:23前提交订单，超时则自动取消预定</div>
    </div>

    <div class="content">
      <!-- Stay Details -->
      <div class="mobile-card stay-card">
        <div class="stay-row">
          <div class="stay-item">
            <div class="stay-label">入住日期</div>
            <div class="stay-val">2026年04月10日</div>
            <div class="stay-day">星期五</div>
          </div>
          <div class="stay-item align-right">
            <div class="stay-label">退房日期</div>
            <div class="stay-val">2026年04月11日</div>
            <div class="stay-day">星期六</div>
          </div>
        </div>
        <div class="divider"></div>
        <div class="stay-row">
          <div class="stay-item">
            <div class="stay-label">房型</div>
            <div class="stay-val-mid">商务大床房</div>
          </div>
          <div class="stay-item align-right">
            <div class="stay-label">房间号</div>
            <div class="stay-val-mid primary-text">{{ roomNumber }}</div>
          </div>
        </div>
      </div>

      <!-- Price Card -->
      <div class="mobile-card price-card">
        <div class="price-row">
          <span class="price-label">汇总金额</span>
          <div class="price-val-group">
            <span class="price-total">¥ 250.00</span>
            <span class="price-detail">( ¥ 250/晚×1 )</span>
          </div>
        </div>
      </div>

      <p class="billing-note">财务统一结算，无需个人垫付、报销</p>

      <!-- Occupant Card -->
      <div class="mobile-card occupant-card">
        <div class="card-header">
          <span class="card-title">入住人</span>
          <span class="add-btn">+ 新增同住人</span>
        </div>
        <div class="occupant-row">
          <span class="occ-name">黄冠读</span>
          <span class="occ-tag">本人</span>
        </div>
      </div>

      <!-- Notice Row -->
      <div class="mobile-card notice-row">
        <span>入住须知</span>
        <svg viewBox="0 0 24 24" width="20" height="20" fill="#ccc"><path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z" /></svg>
      </div>
    </div>

    <!-- Sticky Bottom -->
    <div class="bottom-bar">
      <div class="bar-total">
        <div class="total-label">合计</div>
        <div class="total-price">¥ 250.00</div>
      </div>
      <button class="bar-btn cancel" @click="router.back()">取消订单</button>
      <button class="bar-btn submit" @click="submitOrder">提交订单</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router';
const router = useRouter();
const route = useRoute();
const roomNumber = route.query.room || '8412';

const submitOrder = () => {
  alert('订单提交成功！');
  router.push('/m/booking');
};
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

.occupant-row {
  background: #f8f8f8;
  padding: 12px;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
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
