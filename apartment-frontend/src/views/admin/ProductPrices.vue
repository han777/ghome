<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input v-model="searchQuery" type="text" placeholder="搜索产品名称...">
      </div>
      <button class="add-btn" @click="openModal()">+ 新建产品/价格</button>
    </div>
    
    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>产品名称</th>
            <th>类别</th>
            <th>单位</th>
            <th>价格</th>
            <th>生效日期</th>
            <th>失效日期</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredProducts" :key="item.id">
            <td>{{ item.productName }}</td>
            <td>
              <span class="tag" :class="item.category === 2 ? 'damage' : 'sale'">
                {{ item.category === 1 ? '出售' : '损坏' }}
              </span>
            </td>
            <td>{{ item.unit }}</td>
            <td>¥{{ item.price }}</td>
            <td>{{ item.effectiveDate || '-' }}</td>
            <td>{{ item.expiryDate || '-' }}</td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(item)">编辑</button>
              <button class="delete-btn" @click="deleteProduct(item.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Product Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ form.id ? '编辑产品' : '创建产品' }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form">
            <div class="form-item">
              <label class="required">产品名称</label>
              <input v-model="form.productName" required>
            </div>
            <div class="form-group-row">
              <div class="form-item">
                <label>类别</label>
                <select v-model="form.category">
                  <option :value="1">出售</option>
                  <option :value="2">损坏</option>
                </select>
              </div>
              <div class="form-item">
                <label>单位</label>
                <input v-model="form.unit" placeholder="例如：个，小时">
              </div>
            </div>
            <div class="form-item">
              <label class="required">标准价格</label>
              <input type="number" v-model="form.price" required>
            </div>
            <div class="form-group-row">
              <div class="form-item">
                <label>生效日期</label>
                <input type="date" v-model="form.effectiveDate">
              </div>
              <div class="form-item">
                <label>失效日期</label>
                <input type="date" v-model="form.expiryDate">
              </div>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showModal = false">取消</button>
          <button class="save-btn" @click="saveProduct">保存更改</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue';
import api from '../../utils/api';

const products = ref<any[]>([]);
const showModal = ref(false);
const searchQuery = ref('');
const form = reactive<any>({
  id: null,
  productName: '',
  category: 1,
  unit: '',
  price: 0,
  effectiveDate: '',
  expiryDate: ''
});

const fetchData = async () => {
  try {
    const res = await api.get('/product-prices/all') as any;
    products.value = res;
  } catch (e) {
    console.error('Failed to fetch products', e);
  }
};

const filteredProducts = computed(() => {
  if (!searchQuery.value) return products.value;
  return products.value.filter(p => 
    p.productName.toLowerCase().includes(searchQuery.value.toLowerCase())
  );
});

const openModal = (item?: any) => {
  if (item) {
    Object.assign(form, item);
  } else {
    Object.assign(form, {
      id: null,
      productName: '',
      category: 1,
      unit: '',
      price: 0,
      effectiveDate: new Date().toISOString().split('T')[0],
      expiryDate: ''
    });
  }
  showModal.value = true;
};

const saveProduct = async () => {
  try {
    await api.post('/product-prices', form);
    showModal.value = false;
    fetchData();
  } catch (e) {
    alert('Failed to save product');
  }
};

const deleteProduct = async (id: number) => {
  if (!confirm('确定要删除吗？')) return;
  try {
    await api.delete(`/product-prices/${id}`);
    fetchData();
  } catch (e) {
    alert('Failed to delete');
  }
};

onMounted(fetchData);
</script>

<style scoped>
@import "../../assets/admin.css";
.tag.sale { background: #dcfce7; color: #166534; }
.tag.damage { background: #fee2e2; color: #991b1b; }
</style>
