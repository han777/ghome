<template>
  <div class="records-page">
    <div class="content">
      <div v-if="records.length > 0">
        <div v-for="record in records" :key="record.id" class="mobile-card record-card" @click="goToDetail(record)">
          <div class="record-header">
            <span class="room-type">{{ record.roomTypeName }}</span>
            <span class="status-tag" :class="record.statusClass">{{ record.statusText }}</span>
          </div>
          <div class="record-body">
            <p>{{ $t('records.orderNo') }}{{ record.orderNo }}</p>
            <p>{{ $t('records.checkIn') }}{{ formatDate(record.startDate) }}</p>
            <p>{{ $t('records.checkOut') }}{{ formatDate(record.endDate) }}</p>
            <p>{{ $t('records.roomNo') }}{{ record.roomNo }}</p>
          </div>
          <div class="record-footer">
            <span class="total-price">¥ {{ (record.totalAmount || 0).toFixed(2) }}</span>
            <button class="mobile-btn-outline small" @click="goToDetail(record)">{{ $t('records.viewDetail') }}</button>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <p>{{ $t('records.noRecords2') }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">

import { useI18n } from 'vue-i18n';
import { translateField } from '../../utils/lang';
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '../../utils/api';

const router = useRouter();
const { t, locale } = useI18n();

const records = ref<any[]>([]);

const fetchRecords = async () => {
  try {
    const res = await api.get('/orders/mine') as any[];
    records.value = res.map(o => ({
      ...o,
      roomTypeName: translateField(
        o.roomOccupies?.[0]?.room?.roomType?.nameIntlJson,
        locale.value
      ) || o.roomOccupies?.[0]?.room?.roomType?.typeCode || t('records.unknown'),
      roomNo: o.roomOccupies?.[0]?.room?.roomNo || '-',
      statusText: getStatusText(o.status),
      statusClass: getStatusClass(o.status)
    }));
  } catch (e) {
    console.error('Failed to fetch records', e);
  }
};

const getStatusText = (status: number) => {
  const map: any = {
    0: t('records.status0'),
    1: t('records.status1'),
    2: t('records.status2'),
    3: t('records.status3'),
    4: t('records.status4')
  };
  return map[status] || t('records.unknown');
};

const getStatusClass = (status: number) => {
  const map: any = {
    0: 'pending',
    1: 'booking',
    2: 'active',
    3: 'completed',
    4: 'cancelled'
  };
  return map[status] || '';
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-';
  const d = new Date(dateStr);
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`;
};

const goToDetail = (record: any) => {
  if (record.status === 0) {
    router.push({ path: '/m/confirm', query: { orderId: record.id.toString() } });
  } else {
    router.push(`/m/order-detail/${record.id}`);
  }
};

onMounted(fetchRecords);
</script>

<style scoped>
.record-card {
  padding: 16px;
}
.record-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}
.room-type {
  font-weight: bold;
  font-size: 16px;
}
.status-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}
.status-tag.pending { background: #fff7e6; color: #fa8c16; border: 1px solid #ffd591; }
.status-tag.booking { background: #e6f4ff; color: #1677ff; border: 1px solid #91caff; }
.status-tag.active { background: #f9f0ff; color: #722ed1; border: 1px solid #d3adf7; }
.status-tag.completed { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.status-tag.cancelled { background: #fff1f0; color: #f5222d; border: 1px solid #ffa39e; }
.record-body p {
  font-size: 14px;
  color: #666;
  margin: 4px 0;
}
.record-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.total-price {
  font-weight: bold;
  color: var(--mobile-primary);
}
.mobile-btn-outline.small {
  padding: 4px 12px;
  font-size: 12px;
  width: auto;
}
.empty-state {
  text-align: center;
  padding: 100px 0;
  color: #999;
}
</style>
