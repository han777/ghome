<template>
  <div class="records-page" ref="scrollContainer">
    <!-- Tab Bar -->
    <div class="tab-bar">
      <div class="tab-item" :class="{ active: activeTab === 'pending' }" @click="switchTab('pending')">
        {{ t('records.tabPending') }}
        <span v-if="pendingCount > 0" class="badge">{{ pendingCount }}</span>
      </div>
      <div class="tab-item" :class="{ active: activeTab === 'all' }" @click="switchTab('all')">
        {{ t('records.tabAll') }}
        <span v-if="totalCount > 0" class="badge">{{ totalCount }}</span>
      </div>
    </div>

    <!-- Record List -->
    <div class="content" ref="listContainer">
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
      <div v-else-if="!loading" class="empty-state">
        <p>{{ $t('records.noRecords2') }}</p>
      </div>

      <!-- Loading / No More -->
      <div v-if="loading" class="load-status">{{ t('records.loadingMore') }}</div>
      <div v-else-if="noMore && records.length > 0" class="load-status">{{ t('records.noMore') }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { translateField } from '../../utils/lang';
import { ref, onMounted, watch, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import api from '../../utils/api';

const router = useRouter();
const { t, locale } = useI18n();

const activeTab = ref<'pending' | 'all'>('pending');
const records = ref<any[]>([]);
const loading = ref(false);
const noMore = ref(false);
const page = ref(0);
const pageSize = 10;
const pendingCount = ref(0);
const totalCount = ref(0);
const listContainer = ref<HTMLElement | null>(null);

const mapOrder = (o: any) => ({
  ...o,
  roomTypeName: translateField(
    o.roomOccupies?.[0]?.room?.roomType?.nameIntlJson,
    locale.value
  ) || o.roomOccupies?.[0]?.room?.roomType?.typeCode || t('records.unknown'),
  roomNo: o.roomOccupies?.[0]?.room?.roomNo || '-',
  statusText: getStatusText(o.status),
  statusClass: getStatusClass(o.status)
});

const getStatusText = (status: number) => {
  const map: any = {
    0: t('records.status0'), 1: t('records.status1'),
    2: t('records.status2'), 3: t('records.status3'), 4: t('records.status4')
  };
  return map[status] || t('records.unknown');
};

const getStatusClass = (status: number) => {
  const map: any = { 0: 'pending', 1: 'booking', 2: 'active', 3: 'completed', 4: 'cancelled' };
  return map[status] || '';
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-';
  const d = new Date(dateStr);
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`;
};

const fetchPage = async () => {
  if (loading.value || noMore.value) return;
  loading.value = true;
  try {
    const endpoint = activeTab.value === 'pending'
      ? `/orders/mine/pending?page=${page.value}&size=${pageSize}`
      : `/orders/mine/paged?page=${page.value}&size=${pageSize}`;
    const res: any = await api.get(endpoint);
    const mapped = (res.content || []).map(mapOrder);
    if (page.value === 0) {
      records.value = mapped;
    } else {
      records.value = [...records.value, ...mapped];
    }
    noMore.value = mapped.length < pageSize || (res.last ?? true);
    page.value++;
  } catch (e) {
    console.error('Failed to fetch records', e);
  } finally {
    loading.value = false;
  }
};

const fetchCounts = async () => {
  try {
    // pending count from dedicated endpoint
    pendingCount.value = await api.get('/orders/mine/pending-count') as number;
    // total count from first page metadata
    const res: any = await api.get(`/orders/mine/paged?page=0&size=1`);
    totalCount.value = res.totalElements ?? 0;
  } catch (e) {
    console.error('Failed to fetch counts', e);
  }
};

const resetAndFetch = () => {
  records.value = [];
  page.value = 0;
  noMore.value = false;
  fetchPage();
};

const switchTab = (tab: 'pending' | 'all') => {
  if (activeTab.value === tab) return;
  activeTab.value = tab;
  resetAndFetch();
};

const goToDetail = (record: any) => {
  if (record.status === 0) {
    router.push({ path: '/m/confirm', query: { orderId: record.id.toString() } });
  } else {
    router.push(`/m/order-detail/${record.id}`);
  }
};

// Infinite scroll: detect when user scrolls near bottom
const onScroll = () => {
  const el = listContainer.value;
  if (!el) return;
  const threshold = 100;
  if (el.scrollHeight - el.scrollTop - el.clientHeight < threshold) {
    fetchPage();
  }
};

// Also listen to window scroll for non-fixed containers
const onWindowScroll = () => {
  const el = listContainer.value;
  if (!el) return;
  const rect = el.getBoundingClientRect();
  const threshold = 200;
  if (rect.bottom - window.innerHeight < threshold) {
    fetchPage();
  }
};

onMounted(async () => {
  await fetchCounts();
  resetAndFetch();
  nextTick(() => {
    window.addEventListener('scroll', onWindowScroll, { passive: true });
  });
});

// Re-map records when locale changes (status text, room type name)
watch(locale, () => {
  records.value = records.value.map(o => ({
    ...o,
    roomTypeName: translateField(
      o.roomOccupies?.[0]?.room?.roomType?.nameIntlJson,
      locale.value
    ) || o.roomOccupies?.[0]?.room?.roomType?.typeCode || t('records.unknown'),
    statusText: getStatusText(o.status),
    statusClass: getStatusClass(o.status)
  }));
});
</script>

<style scoped>
.records-page {
  background: #f5f5f5;
  min-height: 100vh;
}

.tab-bar {
  display: flex;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 14px 0;
  font-size: 15px;
  font-weight: 500;
  color: #666;
  position: relative;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-item.active {
  color: var(--mobile-primary);
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 30%;
  right: 30%;
  height: 3px;
  background: var(--mobile-primary);
  border-radius: 2px;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  font-size: 11px;
  font-weight: 600;
  background: var(--mobile-primary);
  color: #fff;
  border-radius: 10px;
  margin-left: 4px;
  line-height: 1;
}

.content {
  padding: 8px 0;
}

.record-card {
  padding: 16px;
  margin: 8px 12px;
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.load-status {
  text-align: center;
  padding: 20px 0;
  font-size: 13px;
  color: #999;
}
</style>