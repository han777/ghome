const fs = require('fs');
const file = 'src/views/mobile/OrderDetail.vue';
let content = fs.readFileSync(file, 'utf-8');

// Extract script and style blocks — they are correct
const scriptStart = content.indexOf('<script setup');
const styleStart = content.indexOf('<style scoped>');
const styleEnd = content.lastIndexOf('</style>') + '</style>'.length;

const scriptBlock = content.substring(scriptStart, styleStart).trim();
const styleBlock = content.substring(styleStart, styleEnd).trim();

const newTemplate = `<template>
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
        <svg v-else viewBox="0 0 24 24" width="32" height="32" fill="white"><path d="M12,2A10,10 0 1,0 22,12A10,10 0 0,0 12,2M12,20A8,8 0 1,1 20,12A8,8 0 0,1 12,20M11,7H13V13H11V7M11,15H13V17H11V15Z" /></svg>
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
        <div class="divider"></div>
        <div class="info-row-grid">
          <div class="grid-item">
            <div class="grid-label">{{ $t('orderDetail.orderNo') }}</div>
            <div class="grid-value order-no-text">{{ order.orderNo || order.id }}</div>
          </div>
          <div class="grid-item align-right">
            <div class="grid-label">{{ $t('orderDetail.status') }}</div>
            <div class="grid-value"><span class="status-chip" :class="statusClass">{{ statusText }}</span></div>
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
              {{ order.booker?.realName || order.user?.realName || order.user?.username || $t('confirm.currentUser') }}
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
          <div class="svc-name">{{ svc.productPrice?.title || svc.productTitle || $t('orderDetail.serviceName') }}</div>
          <div class="svc-meta-row">
            <div class="svc-meta-item">
              <span class="svc-meta-label">{{ $t('orderDetail.qty') }}</span>
              <span class="svc-meta-val">{{ svc.quantity || 1 }}</span>
            </div>
            <div class="svc-meta-item">
              <span class="svc-meta-label">{{ $t('orderDetail.unitPrice') }}</span>
              <span class="svc-meta-val">¥{{ Number(svc.unitPrice || svc.productPrice?.price || 0).toFixed(2) }}</span>
            </div>
            <div class="svc-meta-item">
              <span class="svc-meta-label">{{ $t('orderDetail.subTotal') }}</span>
              <span class="svc-meta-val primary-text">¥{{ (Number(svc.unitPrice || svc.productPrice?.price || 0) * (svc.quantity || 1)).toFixed(2) }}</span>
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
</template>`;

const result = newTemplate + '\n\n' + scriptBlock + '\n\n' + styleBlock + '\n';
fs.writeFileSync(file, result, 'utf-8');
console.log('File written. Lines:', result.split('\n').length);
