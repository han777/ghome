<template>
  <div class="records-page">
    <header class="mobile-header">
      <div class="mobile-header-title">预订记录</div>
    </header>
    <div class="content">
      <div v-if="records.length > 0">
        <div v-for="record in records" :key="record.id" class="mobile-card record-card">
          <div class="record-header">
            <span class="room-type">{{ record.roomType }}</span>
            <span class="status-tag" :class="record.status">{{ record.statusText }}</span>
          </div>
          <div class="record-body">
            <p>入住：{{ record.checkIn }}</p>
            <p>退房：{{ record.checkOut }}</p>
            <p>房号：{{ record.roomNum }}</p>
          </div>
          <div class="record-footer">
            <span class="total-price">¥ {{ record.price }}</span>
            <button class="mobile-btn-outline small">查看详情</button>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <p>暂无预订记录</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const records = ref([
  {
    id: 1,
    roomType: '商务大床房',
    roomNum: '8412',
    checkIn: '2026-04-10',
    checkOut: '2026-04-11',
    price: 250,
    status: 'completed',
    statusText: '已完成'
  },
  {
    id: 2,
    roomType: '行政套房',
    roomNum: '9001',
    checkIn: '2026-03-15',
    checkOut: '2026-03-16',
    price: 400,
    status: 'completed',
    statusText: '已完成'
  }
]);
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
.status-tag.completed { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
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
