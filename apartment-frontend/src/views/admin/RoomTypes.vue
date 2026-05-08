<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input v-model="searchQuery" type="text" placeholder="搜索房型...">
      </div>
      <button class="add-btn" @click="openModal()">+ 添加房型</button>
    </div>
    
    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>代码</th>
            <th>名称 (多语言)</th>
            <th>短租</th>
            <th>长租</th>
            <th>备注</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="type in filteredTypes" :key="type.id">
            <td class="code">{{ type.typeCode }}</td>
            <td>
              <div class="name-intl-cell">
                <span class="tag">中</span> {{ parseName(type.nameIntlJson, 'zh') }}
              </div>
              <div class="name-intl-cell">
                <span class="tag">EN</span> {{ parseName(type.nameIntlJson, 'en') }}
              </div>
              <div class="name-intl-cell">
                <span class="tag">JA</span> {{ parseName(type.nameIntlJson, 'ja') }}
              </div>
            </td>
            <td>¥{{ type.priceShortRent }}</td>
            <td>¥{{ type.priceLongRent }}</td>
            <td>{{ type.remarks || '-' }}</td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(type)">编辑</button>
              <button class="delete-btn" @click="deleteType(type.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Type Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ form.id ? '编辑房型' : '添加房型' }}</h2>
          <button class="close-btn" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <form class="admin-form">
            <div class="form-item">
              <label>类型代码</label>
              <input v-model="form.typeCode" placeholder="如 KING, SUITE" required>
            </div>
            <div class="form-item">
              <label>中文名称</label>
              <input v-model="nameZh" placeholder="如 大床房">
            </div>
            <div class="form-item">
              <label>英文名称 (English)</label>
              <input v-model="nameEn" placeholder="e.g. King Room">
            </div>
            <div class="form-item">
              <label>日语名称 (日本語)</label>
              <input v-model="nameJa" placeholder="例：キングルーム">
            </div>
            <div class="form-group-row">
              <div class="form-item">
                <label>短租价格</label>
                <input type="number" v-model="form.priceShortRent">
              </div>
              <div class="form-item">
                <label>长租价格</label>
                <input type="number" v-model="form.priceLongRent">
              </div>
            </div>
            <div class="form-item span-2">
              <label>图片 (九宫格展示)</label>
              <div class="image-uploader">
                <div v-for="(img, idx) in form.images" :key="idx" class="image-preview-item">
                  <img :src="img.url" @click="previewImage = img.url">
                  <button type="button" class="remove-img" @click="form.images.splice(idx, 1)">&times;</button>
                </div>
                <label v-if="(form.images?.length || 0) < 9" class="upload-placeholder">
                  <input type="file" @change="handleFileUpload" multiple accept="image/*" hidden>
                  <span class="plus">+</span>
                </label>
              </div>
            </div>
            <div class="form-item span-2">
              <label>备注</label>
              <textarea v-model="form.remarks"></textarea>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showModal = false">取消</button>
          <button class="save-btn" @click="saveType">保存更改</button>
        </div>
      </div>
    </div>

    <!-- Big Image Preview -->
    <div v-if="previewImage" class="image-viewer-overlay" @click="previewImage = null">
      <img :src="previewImage" class="big-preview">
      <div class="close-viewer">&times;</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue';
import api from '../../utils/api';

const types = ref<any[]>([]);
const showModal = ref(false);
const searchQuery = ref('');
const nameZh = ref('');
const nameEn = ref('');
const nameJa = ref('');
const previewImage = ref<string | null>(null);

const form = reactive<any>({
  id: null,
  typeCode: '',
  nameIntlJson: '',
  priceShortRent: 0,
  priceLongRent: 0,
  images: [],
  remarks: ''
});

const fetchData = async () => {
  try {
    const res = await api.get('/room-types/all') as any;
    types.value = res;
  } catch (e) {
    console.error('Failed to fetch data', e);
  }
};

const filteredTypes = computed(() => {
  if (!searchQuery.value) return types.value;
  return types.value.filter(t => t.typeCode.toLowerCase().includes(searchQuery.value.toLowerCase()));
});

const openModal = (type?: any) => {
  // Reset form
  form.id = null;
  form.typeCode = '';
  form.nameIntlJson = '';
  form.priceShortRent = 0;
  form.priceLongRent = 0;
  form.images = [];
  form.remarks = '';

  if (type) {
    const cloned = JSON.parse(JSON.stringify(type));
    form.id = cloned.id;
    form.typeCode = cloned.typeCode;
    form.nameIntlJson = cloned.nameIntlJson || '{}';
    form.priceShortRent = cloned.priceShortRent;
    form.priceLongRent = cloned.priceLongRent;
    form.images = cloned.images || [];
    form.remarks = cloned.remarks;
    // Parse existing JSON into separate inputs
    try {
      const parsed = JSON.parse(cloned.nameIntlJson || '{}');
      nameZh.value = parsed.zh || '';
      nameEn.value = parsed.en || '';
      nameJa.value = parsed.ja || '';
    } catch {
      nameZh.value = '';
      nameEn.value = '';
      nameJa.value = '';
    }
  } else {
    nameZh.value = '';
    nameEn.value = '';
    nameJa.value = '';
  }
  showModal.value = true;
};

const handleFileUpload = async (event: any) => {
  const files = event.target.files;
  if (!files.length) return;
  
  for (let file of files) {
    const formData = new FormData();
    formData.append('file', file);
    try {
      const res = await api.post('/files/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      }) as any;
      
      if (!form.images) form.images = [];
      form.images.push({ url: res.url, sortOrder: form.images.length });
    } catch (e) {
      console.error('Upload failed', e);
    }
  }
};

const saveType = async () => {
  try {
    // Build nameIntlJson from the input fields
    form.nameIntlJson = JSON.stringify({ 
      zh: nameZh.value, 
      en: nameEn.value, 
      ja: nameJa.value 
    });
    await api.post('/room-types', form);
    showModal.value = false;
    fetchData();
  } catch (e) {
    alert('保存失败，请检查表单内容');
  }
};

// Helper: parse nameIntlJson and return specified lang
const parseName = (json: string, lang: string): string => {
  if (!json) return '';
  try {
    const obj = typeof json === 'string' ? JSON.parse(json) : json;
    return obj[lang] || obj['zh'] || '';
  } catch {
    return '';
  }
};

const deleteType = async (id: number) => {
  if (!confirm('确定删除此房型？')) return;
  try {
    await api.delete(`/room-types/${id}`);
    fetchData();
  } catch (e) {
    alert('Failed to delete');
  }
};

onMounted(fetchData);
</script>

<style scoped>
@import "../../assets/admin.css";
.tag { background: #f1f5f9; color: #475569; padding: 1px 4px; border-radius: 4px; font-size: 10px; margin-right: 4px; border: 1px solid #e2e8f0; }
textarea { width: 100%; min-height: 80px; padding: 8px; border-radius: 6px; border: 1px solid #e2e8f0; font-family: monospace; }

.image-uploader {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 8px;
}

.image-preview-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.image-preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
}

.remove-img {
  position: absolute;
  top: 4px;
  right: 4px;
  background: rgba(0,0,0,0.5);
  color: #fff;
  border: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
}

.upload-placeholder {
  aspect-ratio: 1;
  border: 2px dashed #cbd5e1;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #f8fafc;
  transition: all 0.2s;
}

.upload-placeholder:hover {
  border-color: #38bdf8;
  background: #f0f9ff;
}

.upload-placeholder .plus {
  font-size: 32px;
  color: #94a3b8;
}

.image-viewer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.big-preview {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
}

.close-viewer {
  position: absolute;
  top: 20px;
  right: 20px;
  color: #fff;
  font-size: 40px;
  cursor: pointer;
}

.span-2 { grid-column: span 2; }
</style>
