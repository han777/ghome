<template>
  <div class="admin-page">
    <div class="report-stats">
      <div class="stat-card">
        <div class="stat-label">总费用收入 (累计)</div>
        <div class="stat-value">¥{{ totalRevenue }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">订单总数</div>
        <div class="stat-value">{{ orders.length }}</div>
      </div>
    </div>

    <div class="table-card">
      <div class="card-header">费用明细明细</div>
      <table class="admin-table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>客户</th>
            <th>费用类型</th>
            <th>金额</th>
            <th>日期</th>
            <th>备注</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="order in orders" :key="order.id">
            <tr v-for="fee in order.fees" :key="fee.id">
              <td>{{ order.orderNo }}</td>
              <td>{{ order.customerName }}</td>
              <td><span class="tag">{{ fee.feeType }}</span></td>
              <td>¥{{ fee.amount }}</td>
              <td>{{ formatDate(fee.createdAt) }}</td>
              <td>{{ fee.remarks }}</td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import api from '../../utils/api';

const orders = ref<any[]>([]);

const fetchData = async () => {
  try {
    const res = await api.get('/orders/all') as any[];
    // For each order, fetch its fees
    const ordersWithFees = await Promise.all(res.map(async (o) => {
      const fees = await api.get(`/orders/${o.id}/fees`) as any[];
      return { ...o, fees };
    }));
    orders.value = ordersWithFees;
  } catch (e) {
    console.error('Failed to fetch report data', e);
  }
};

const totalRevenue = computed(() => {
  return orders.value.reduce((sum, o) => sum + (o.totalAmount || 0), 0);
});

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleString();
};

onMounted(fetchData);
</script>

<style scoped>
@import "../../assets/admin.css";
.report-stats { display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; margin-bottom: 2rem; }
.card-header { padding: 1.5rem; font-weight: 700; border-bottom: 1px solid #e2e8f0; }
.tag { background: #fef9c3; color: #854d0e; padding: 2px 6px; border-radius: 4px; font-size: 11px; }
</style>
