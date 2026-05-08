const fs = require('fs');
const file = 'src/views/mobile/OrderDetail.vue';
let content = fs.readFileSync(file, 'utf-8');

const scriptStart = content.indexOf('<script setup lang="ts">');
const scriptEnd = content.indexOf('</script>') + '</script>'.length;

const newScript = `<script setup lang="ts">
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

const roomNo = computed(() => order.value?.roomOccupies?.[0]?.room?.roomNo || '-');
const roomTypeName = computed(() => {
  const rt = order.value?.roomOccupies?.[0]?.room?.roomType;
  if (!rt) return '-';
  return translateField(rt.nameIntlJson, locale.value) || rt.typeCode || '-';
});
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

const companions = computed(() => {
  const coStr = order.value?.roomOccupies?.[0]?.coOccupants;
  return coStr ? coStr.split(',').filter(Boolean) : [];
});

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
      const fees = await api.get(\`/orders/\${orderId}/fees\`) as any[];
      extraFees.value = fees;
    }
  } catch (e) {
    console.error('Failed to fetch order', e);
  }
};

const formatDate = (dateStr: string, includeTime = false) => {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const datePart = \`\${d.getFullYear()}-\${d.getMonth() + 1}-\${d.getDate()}\`;
  if (includeTime) {
    const timePart = \`\${d.getHours().toString().padStart(2, '0')}:\${d.getMinutes().toString().padStart(2, '0')}:\${d.getSeconds().toString().padStart(2, '0')}\`;
    return \`\${datePart} \${timePart}\`;
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
      await api.post(\`/orders/\${orderId}/cancel\`);
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
      await api.post(\`/orders/\${orderId}/checkout\`);
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
</script>`;

content = content.substring(0, scriptStart) + newScript + content.substring(scriptEnd);
fs.writeFileSync(file, content, 'utf-8');
console.log('Script section replaced, total lines:', content.split('\n').length);
