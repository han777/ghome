<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="search-bar">
        <input v-model="searchQuery" type="text" placeholder="搜索订房人、创建人、入住人...">
      </div>
      <button class="refresh-btn" @click="fetchData" title="刷新">🔄 刷新</button>
      <button class="add-btn" @click="openModal()">+ 新建订单</button>
    </div>

    <div class="filter-row">
      <div class="quick-filters">
        <button 
          class="filter-chip" 
          :class="{ active: filterTodayArrival }"
          @click="filterTodayArrival = !filterTodayArrival"
        >
          🛬 今日入住
        </button>
        <button 
          class="filter-chip" 
          :class="{ active: filterTodayDeparture }"
          @click="filterTodayDeparture = !filterTodayDeparture"
        >
          🛫 今日退房
        </button>
      </div>
      <div class="status-filters">
        <label v-for="opt in getDictOptions('ORDER_STATUS')" :key="opt.value" class="filter-checkbox">
          <input type="checkbox" :value="parseInt(opt.value)" v-model="statusFilter">
          {{ opt.label }}
        </label>
      </div>
    </div>
    
    <div class="table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>预订人</th>
            <th>创建人</th>
            <th>房数</th>
            <th>房间列表</th>
            <th>业务类型</th>
            <th>入住周期</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in filteredOrders" :key="order.id">
            <td class="code">{{ order.orderNo }}</td>
            <td>
              <div style="font-size: 12px;">
                👤 {{ order.booker?.realName || '-' }}<br>
                📞 {{ order.bookPhone || order.booker?.phone || '-' }}
              </div>
            </td>
            <td class="name">{{ order.createUser?.realName || order.createUser?.username || '-' }}</td>
            <td><span class="room-count-badge">{{ order.roomOccupies?.length || 0 }}</span></td>
            <td>
              <div class="room-tags">
                <span v-for="ro in order.roomOccupies" :key="ro.id" class="room-tag">
                  {{ ro.room?.roomNo }}
                </span>
                <span v-if="!order.roomOccupies || order.roomOccupies.length === 0">-</span>
              </div>
            </td>
            <td>
              <span class="tag">
                {{ getDictLabel('BIZ_TYPE', order.bizType) }}
              </span>
            </td>
            <td>
              <div style="font-size: 12px; white-space: nowrap;">
                📅 {{ order.startDate || '-' }}<br>
                🔚 {{ order.endDate || '-' }}
              </div>
            </td>
            <td>
              <span class="status-badge" :class="getOrderStatusClass(order.status)">
                {{ getDictLabel('ORDER_STATUS', order.status) }}
              </span>
            </td>
            <td class="actions">
              <button class="edit-btn" @click="openModal(order, 'basic')">详情</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Order Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content" :class="{ 'modal-maximized': isMaximized }">
        <div class="modal-header">
          <div class="header-left">
            <h2>{{ form.id ? '编辑订单' : '创建订单' }}</h2>
            <div class="modal-nav">
              <a href="#section-basic" @click.prevent="scrollTo('section-basic')">🏠 基础信息</a>
              <a href="#section-rooms" @click.prevent="scrollTo('section-rooms')">🛏️ 客房</a>
              <a href="#section-products" @click.prevent="scrollTo('section-products')">📦 服务与商品</a>
              <a href="#section-accounting" @click.prevent="scrollTo('section-accounting')">💰 财务结算</a>
              <a href="#section-logs" @click.prevent="scrollTo('section-logs')" v-if="form.id">📋 操作日志</a>
            </div>
          </div>
          <div class="header-actions">
            <button v-if="form.id && !isEditMode" class="edit-top-btn" @click="isEditMode = true">编辑</button>
            <button v-if="form.id && form.status <= 1" class="cancel-order-btn" @click="cancelOrderFromModal(form.id)">取消订单</button>
            <button v-if="form.id && form.status === 1" class="send-card-btn" @click="sendRoomCard(form.id)">📇 发送房卡</button>
            <button v-if="form.id && form.status < 3 && form.status > 1" class="checkout-btn" @click="handleCheckout">🔔 退房</button>
            <button class="maximize-btn" @click="isMaximized = !isMaximized">
              {{ isMaximized ? '🗗' : '🗖' }}
            </button>
            <button class="close-btn" @click="showModal = false">&times;</button>
          </div>
        </div>
        <div class="modal-body scrollable">
          <!-- Basic Info Section -->
           <section id="section-basic" class="form-section">
            <h3 class="section-title">基础信息</h3>
            <div class="form-grid-4">
              <div class="form-item">
                <label class="required">预订人</label>
                <select v-model="form.bookerId" required @change="onBookerChange" :disabled="!isEditMode">
                  <option :value="null">-- 选择预订人 --</option>
                  <option v-for="u in users" :key="u.id" :value="u.id">
                    {{ u.realName }} ({{ u.username }})
                  </option>
                </select>
              </div>
              <div class="form-item">
                <label>联系电话</label>
                <input v-model="form.bookPhone" placeholder="预订电话" :disabled="!isEditMode">
              </div>

              <div class="form-item">
                <label class="required">入住日期</label>
                <input type="datetime-local" v-model="form.startDate" required :disabled="!isEditMode">
              </div>
              <div class="form-item">
                <label>退房日期</label>
                <input type="datetime-local" v-model="form.endDate" required :disabled="!isEditMode">
              </div>

              <div class="form-item">
                <label>业务类型</label>
                <select v-model="form.bizType" :disabled="!isEditMode">
                  <option v-for="opt in getDictOptions('BIZ_TYPE')" :key="opt.value" :value="parseInt(opt.value)">
                    {{ opt.label }}
                  </option>
                </select>
              </div>

              <div class="form-item">
                <label>客户类型</label>
                <select v-model="form.customerType" :disabled="!isEditMode || !!form.id">
                  <option :value="1">个人</option>
                  <option :value="2">团体</option>
                </select>
              </div>

              <div class="form-item">
                <label>订单状态</label>
                <select v-model="form.status" :disabled="!isEditMode || !!form.id">
                  <option v-for="opt in getDictOptions('ORDER_STATUS')" :key="opt.value" :value="parseInt(opt.value)">
                    {{ opt.label }}
                  </option>
                </select>
              </div>

              <div class="form-item span-2">
                <label>备注</label>
                <input v-model="form.remarks" placeholder="订单备注信息..." :disabled="!isEditMode">
              </div>

              <div class="form-item">
                <label>创建时间</label>
                <input type="datetime-local" v-model="form.createdAt" readonly style="background-color: #f3f4f6; cursor: not-allowed;">
              </div>
              <div class="form-item">
                <label>创建人</label>
                <input :value="form.createUser?.realName || form.createUser?.username || '-'" readonly style="background-color: #f3f4f6; cursor: not-allowed;">
              </div>
            </div>
          </section>

          <!-- Room Occupancy Section -->
          <section id="section-rooms" class="form-section">
            <div class="section-header">
              <h3 class="section-title">客房入住</h3>
              <div class="header-actions">
                <span class="room-count-badge" v-if="form.roomOccupies?.length">
                  {{ form.roomOccupies.length }} 间
                </span>
                <button class="add-btn small" @click="addRoomRow" v-if="isEditMode">+ 添加房间</button>
              </div>
            </div>
            <div class="room-cards-container">
              <div v-for="(occupy, index) in form.roomOccupies" :key="index" class="room-card">
                <div class="card-header">
                  <div class="card-title">
                    <span class="room-no">房间 {{ getRoomNo(Number(occupy.roomId)) || '未设置' }}</span>
                    <span class="room-type">{{ getRoomTypeName(Number(occupy.roomId)) }}</span>
                  </div>
                  <div class="card-actions">
                    <button class="card-action-btn adjust" v-if="occupy.status === 0" @click="openTimeAdjustModal(occupy)">调整时间</button>
                    <button class="card-action-btn change" v-if="occupy.id" @click="startChangeRoom(occupy)">换房</button>
                    <button class="card-action-btn delete" v-if="isEditMode" @click="removeRoomRow(index)">×</button>
                  </div>
                </div>
                <div class="card-grid">
                  <div class="card-item">
                    <label>实际入住</label>
                    <input type="datetime-local" v-model="occupy.checkInTime" :disabled="!isEditMode">
                  </div>
                  <div class="card-item">
                    <label>实际退房</label>
                    <input type="datetime-local" v-model="occupy.checkOutTime" :disabled="!isEditMode">
                  </div>
                    <div class="card-item">
                      <label>房间</label>
                      <div class="room-selector-btn" @click="openRoomPicker(occupy, index)" v-if="isEditMode">
                        <span v-if="occupy.roomId" class="selected-room-info">
                          <strong>{{ getRoomNo(Number(occupy.roomId)) }}</strong>
                          <small>({{ getRoomTypeName(Number(occupy.roomId)) }})</small>
                        </span>
                        <span v-else class="placeholder">选择房间...</span>
                      </div>
                      <div class="room-selector-btn" v-else style="cursor: default; opacity: 0.8;">
                        <span v-if="occupy.roomId" class="selected-room-info">
                          <strong>{{ getRoomNo(Number(occupy.roomId)) }}</strong>
                          <small>({{ getRoomTypeName(Number(occupy.roomId)) }})</small>
                        </span>
                        <span v-else class="placeholder">未选择</span>
                      </div>
                    </div>
                  <div class="card-item">
                    <label>入住人</label>
                    <select v-model="occupy.occupantUserId" :disabled="!isEditMode">
                      <option :value="null">-- 选择入住人 --</option>
                      <option v-for="u in users" :key="u.id" :value="u.id">
                        {{ u.realName }} ({{ u.username }})
                      </option>
                    </select>
                  </div>
                  <div class="card-item">
                    <label>房卡号</label>
                    <input v-model="occupy.roomCardNo" :disabled="!isEditMode">
                  </div>
                  <div class="card-item">
                    <label>门锁密码</label>
                    <input v-model="occupy.doorCode" :disabled="!isEditMode">
                  </div>
                  <div class="card-item">
                    <label>入住人数</label>
                    <input type="number" v-model="occupy.occupantCount" min="1" :disabled="!isEditMode">
                  </div>
                  <div class="card-item">
                    <label>实际单价</label>
                    <input type="number" v-model="occupy.actualPrice" step="0.01" @input="refreshTotals" :disabled="!isEditMode">
                  </div>
                  <div class="card-item">
                    <label>数量 (天/次)</label>
                    <input type="number" v-model="occupy.quantity" min="1" @input="refreshTotals" :disabled="!isEditMode">
                  </div>
                  <div class="card-item">
                    <label>状态</label>
                    <select v-model="occupy.status" :disabled="!isEditMode">
                      <option :value="0">当前</option>
                      <option :value="1">完成</option>
                    </select>
                  </div>
                  <div class="card-item span-3">
                    <label>同住人</label>
                    <input v-model="occupy.coOccupants" placeholder="其他房客姓名">
                  </div>
                </div>
              </div>
              <div v-if="!form.roomOccupies || form.roomOccupies.length === 0" class="empty-rooms">
                此订单尚未分配客房。
              </div>
            </div>
          </section>

          <!-- Products & Services Section -->
          <section id="section-products" class="form-section">
            <div class="section-header">
              <h3 class="section-title">服务与商品</h3>
              <button class="add-btn small" @click="addProductDetail" v-if="isEditMode">+ 添加行</button>
            </div>
            <div class="table-wrapper">
              <table class="detail-table">
                <thead>
                  <tr>
                    <th>日期</th>
                    <th>产品/服务</th>
                    <th>单位</th>
                    <th>标准价</th>
                    <th>实际价</th>
                    <th>数量</th>
                    <th>总计</th>
                    <th>备注</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(detail, index) in form.productDetails" :key="index">
                    <td><input type="date" v-model="detail.consumeDate" class="table-input" style="width: 120px;" :disabled="!isEditMode"></td>
                    <td>
                      <select v-model="detail.productId" @change="onProductChange(detail)" class="table-input" :disabled="!isEditMode">
                        <option :value="null">-- 选择 --</option>
                        <option v-for="p in productPrices" :key="p.id" :value="p.id">
                          {{ p.productName }} ({{ p.category === 1 ? '出售' : '损坏' }})
                        </option>
                      </select>
                    </td>
                    <td>{{ getProductById(Number(detail.productId))?.unit || '-' }}</td>
                    <td>¥{{ getProductById(Number(detail.productId))?.price || 0 }}</td>
                    <td><input type="number" v-model="detail.actualPrice" class="table-input no-border" style="width: 70px;" :disabled="!isEditMode"></td>
                    <td><input type="number" v-model="detail.quantity" min="1" class="table-input no-border" style="width: 50px;" :disabled="!isEditMode"></td>
                    <td>¥{{ (detail.actualPrice || 0) * (detail.quantity || 1) }}</td>
                    <td><input v-model="detail.remarks" placeholder="..." class="table-input no-border" :disabled="!isEditMode"></td>
                    <td><button class="delete-btn" @click="removeProductDetail(index)" v-if="isEditMode">×</button></td>
                  </tr>
                  <tr v-if="form.productDetails?.length === 0">
                    <td colspan="9" class="empty-row">未添加服务或产品</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <!-- Accounting Section -->
          <section id="section-accounting" class="form-section">
            <h3 class="section-title">财务结算</h3>
            <div class="form-grid-4">
              <div class="form-item">
                <label>房费</label>
                <div class="amount-display">
                  <span class="currency">¥</span>
                  <input type="number" v-model="form.roomFee" step="0.01" disabled>
                </div>
              </div>
              <div class="form-item">
                <label>服务费</label>
                <div class="amount-display">
                  <span class="currency">¥</span>
                  <input type="number" v-model="form.serviceFee" step="0.01" disabled>
                </div>
              </div>
              <div class="form-item span-2">
                <label>总金额 (自动)</label>
                <div class="amount-display highlight">
                  <span class="currency">¥</span>
                  <input type="number" v-model="form.totalAmount" step="0.01">
                </div>
              </div>
            </div>
          </section>

          <!-- Order Logs Section -->
          <section id="section-logs" class="form-section" v-if="form.id">
            <h3 class="section-title">操作日志</h3>
            <div class="log-list">
              <div v-for="log in orderLogs" :key="log.id" class="log-item" :class="'log-' + log.operationType">
                <div class="log-header">
                  <span class="log-type-badge">{{ getLogTypeLabel(log.operationType) }}</span>
                  <span class="log-time">{{ formatLogTime(log.operationTime) }}</span>
                  <span class="log-operator">{{ log.operator?.realName || log.operator?.username || '-' }}</span>
                </div>
                <div class="log-content">{{ log.operationContent }}</div>
                <div v-if="log.changedFields" class="log-changes">
                  <div v-for="(change, ci) in parseChanges(log.changedFields)" :key="ci" class="log-change-row">
                    <span class="log-field">{{ change.field }}:</span>
                    <span class="log-old">{{ change.oldValue }}</span>
                    <span class="log-arrow">→</span>
                    <span class="log-new">{{ change.newValue }}</span>
                  </div>
                </div>
              </div>
              <div v-if="orderLogs.length === 0" class="empty-log">暂无操作日志</div>
            </div>
          </section>
        </div>
        <div class="modal-footer">
          <div class="footer-summary" v-if="isEditMode">
            总计: <span class="price-highlight">¥{{ form.totalAmount }}</span>
          </div>
          <div class="footer-btns">
            <button class="cancel-btn" @click="showModal = false">关闭</button>
            <button class="cancel-edit-btn" v-if="isEditMode && form.id" @click="cancelEdit">取消编辑</button>
            <button class="save-btn" v-if="isEditMode" @click="saveOrder">保存更改</button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Change Room Modal -->
    <div v-if="showChangeRoomModal" class="modal-overlay" style="z-index: 2000;">
      <div class="modal-content" style="max-width: 500px;">
        <div class="modal-header">
          <h3>换房流程</h3>
          <button class="close-btn" @click="showChangeRoomModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="room-card" style="box-shadow: none; border-color: #38bdf8;">
            <div class="card-header">
              <div class="card-title">
                <span class="room-no">换房设置</span>
                <span class="room-type">请确认生效日期及新房间</span>
              </div>
            </div>
            <div class="card-grid">
              <div class="card-item span-4">
                <label class="required">换房生效日期</label>
                <input type="date" v-model="changeRoomDate" @change="fetchChangeRoomAvailableRooms" :min="new Date().toISOString().split('T')[0]">
                <p style="font-size: 11px; color: #64748b; margin-top: 4px;">* 换房操作将从该日期起生效至 {{ form.endDate.slice(0, 10) }}</p>
              </div>

              <div class="card-item span-4">
                <label class="required">选择新房间 (仅显示可用)</label>
                <select v-model="changeRoomNewRoomId" required>
                  <option :value="null">-- 选择可用房间 --</option>
                  <option v-for="r in changeRoomAvailableRooms" :key="r.id" :value="r.id">
                    {{ r.roomNo }} ({{ getRoomTypeName(r.id) }}) - ¥{{ form.bizType === 1 ? r.roomType?.priceShortRent : r.roomType?.priceLongRent }}
                  </option>
                </select>
              </div>

              <div class="card-item span-2">
                <label>入住人</label>
                <input :value="users.find(u => u.id === changingOccupy?.occupantUserId)?.realName" disabled>
              </div>
              <div class="card-item span-2">
                <label>原房卡号</label>
                <input :value="changingOccupy?.roomCardNo" disabled>
              </div>

              <div class="card-item span-4" v-if="changeRoomPriceDiff !== 0">
                <div class="amount-display highlight" :style="{ borderColor: changeRoomPriceDiff > 0 ? '#ef4444' : '#10b981' }">
                  <span style="font-size: 13px; color: #64748b;">补缴/退还差价：</span>
                  <span class="currency">¥</span>
                  <span style="font-weight: 800; font-size: 18px;" :style="{ color: changeRoomPriceDiff > 0 ? '#ef4444' : '#10b981' }">
                    {{ Math.abs(changeRoomPriceDiff).toFixed(2) }}
                    <small style="font-size: 12px; font-weight: 400; margin-left: 4px;">
                      ({{ changeRoomPriceDiff > 0 ? '需补缴' : '需退还' }})
                    </small>
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div v-if="changeRoomAvailableRooms.length === 0 && changeRoomDate" style="text-align: center; color: #ef4444; font-size: 12px; margin-top: 10px;">
            ⚠️ 所选日期范围内暂无可用房间，请调整日期。
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showChangeRoomModal = false">不保存关闭</button>
          <button class="save-btn" :disabled="!changeRoomNewRoomId" @click="confirmChangeRoom">确认换房</button>
        </div>
      </div>
    </div>

    <!-- Room Picker Modal -->
    <div v-if="showRoomPicker" class="modal-overlay" style="z-index: 2100;">
      <div class="modal-content room-picker-modal">
        <div class="modal-header">
          <div class="header-left">
            <h2>选择可用房间</h2>
            <p class="subtitle">显示无锁定、无维修且无重叠预订的房间。</p>
          </div>
          <button class="close-btn" @click="showRoomPicker = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="picker-filter-bar">
            <div class="filter-group">
              <label>入住</label>
              <input type="date" v-model="roomPickerFilters.startDate" @change="fetchPickerAvailableRooms">
            </div>
            <div class="filter-group">
              <label>退房</label>
              <input type="date" v-model="roomPickerFilters.endDate" @change="fetchPickerAvailableRooms">
            </div>
            <div class="filter-group">
              <label>房型</label>
              <select v-model="roomPickerFilters.roomTypeId">
                <option :value="null">-- 全部房型 --</option>
                <option v-for="t in roomTypes" :key="t.id" :value="t.id">
                  {{ t.typeCode }} ({{ parseNameIntl(t.nameIntlJson, 'zh') || parseNameIntl(t.nameIntlJson, 'en') }})
                </option>
              </select>
            </div>
          </div>

          <div class="room-selection-grid">
            <div 
              v-for="r in filteredPickerRooms" 
              :key="r.id" 
              class="room-pick-card"
              @click="selectRoomFromPicker(r)"
            >
              <div class="room-badge">房间</div>
              <div class="room-no">{{ r.roomNo }}</div>
              <div class="room-meta">
                <span class="type-tag">{{ r.roomType ? (parseNameIntl(r.roomType.nameIntlJson, 'zh') || r.roomType.typeCode) : '-' }}</span>
                <span class="price">¥{{ form.bizType === 1 ? r.roomType?.priceShortRent : r.roomType?.priceLongRent }}</span>
              </div>
            </div>
            <div v-if="filteredPickerRooms.length === 0" class="empty-picker">
              <div class="empty-icon">📭</div>
              <p>在所选时段和房型下未找到可用房间。</p>
              <small>请尝试不同的日期或清除房型筛选。</small>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Time Adjust Modal -->
    <div v-if="showTimeAdjustModal" class="modal-overlay" style="z-index: 2200;">
      <div class="modal-content" style="max-width: 500px;">
        <div class="modal-header">
          <h3>调整入住时间</h3>
          <button class="close-btn" @click="showTimeAdjustModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="info-alert">
            <p><strong>房间:</strong> {{ getRoomNo(Number(adjustingOccupy?.roomId)) }}</p>
            <p><strong>入住人:</strong> {{ users.find(u => u.id === adjustingOccupy?.occupantUserId)?.realName || '-' }}</p>
          </div>
          
          <div class="booked-periods-view" v-if="bookedPeriods.length > 0">
            <label>该房间已有预订/维修时段 (不可选):</label>
            <ul class="period-list">
              <li v-for="(p, i) in bookedPeriods" :key="i" class="period-item" :class="p.type">
                {{ p.type === 'maintenance' ? '🔧 维修' : '📅 预订' }}: 
                {{ p.start.slice(0, 10) }} 至 {{ p.end.slice(0, 10) }}
              </li>
            </ul>
          </div>

          <div class="form-grid-2" style="margin-top: 20px;">
            <div class="form-item">
              <label class="required">新入住时间</label>
              <input type="datetime-local" v-model="adjustingDates.start" required>
            </div>
            <div class="form-item">
              <label class="required">新退房时间</label>
              <input type="datetime-local" v-model="adjustingDates.end" required>
            </div>
          </div>
          <p class="hint-text">* 系统将根据新时段重新计算房费。</p>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showTimeAdjustModal = false">不保存关闭</button>
          <button class="save-btn" @click="confirmTimeAdjust">确认调整</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed, watch } from 'vue';
import api from '../../utils/api';

const orders = ref<any[]>([]);
const dicts = ref<any[]>([]);
const users = ref<any[]>([]);
const rooms = ref<any[]>([]);
const productPrices = ref<any[]>([]);
const activeTab = ref('basic');
const showModal = ref(false);
const isMaximized = ref(false);
const isEditMode = ref(false);
const searchQuery = ref('');
const filterTodayArrival = ref(false);
const filterTodayDeparture = ref(false);
const statusFilter = ref<number[]>([1, 2]); // Default: Pending (1), In (2)
const showChangeRoomModal = ref(false);
const changingOccupy = ref<any>(null);
const changeRoomNewRoomId = ref<number | null>(null);
const roomTypes = ref<any[]>([]);
const showRoomPicker = ref(false);
const pickingOccupyIndex = ref<number | null>(null);
const changeRoomAvailableRooms = ref<any[]>([]);
const showTimeAdjustModal = ref(false);
const adjustingOccupy = ref<any>(null);
const adjustingDates = reactive({ start: '', end: '' });
const bookedPeriods = ref<any[]>([]);
const roomPickerFilters = reactive({
  startDate: '',
  endDate: '',
  roomTypeId: null as number | null
});
const changeRoomDate = ref(new Date().toISOString().slice(0, 10));
const pickerAvailableRooms = ref<any[]>([]);
const availableRooms = ref<any[]>([]);
const orderLogs = ref<any[]>([]);

const form = reactive<any>({
  id: null,
  userId: null,
  bizType: 1,
  startDate: new Date().toISOString().slice(0, 10) + 'T14:00',
  endDate: new Date(Date.now() + 86400000).toISOString().slice(0, 10) + 'T12:00',
  createdAt: new Date().toISOString().slice(0, 16),
  roomFee: 0,
  serviceFee: 0,
  totalAmount: 0,
  status: 1,
  customerType: 1,
  bookerId: null,
  bookPhone: '',
  remarks: '',
  company: '',
  costCenter: '',
  roomOccupies: [],
  productDetails: []
});

const parseNameIntl = (json: string, lang: string): string => {
  if (!json) return '';
  try {
    const obj = typeof json === 'string' ? JSON.parse(json) : json;
    return obj[lang] || obj['zh'] || '';
  } catch {
    return '';
  }
};

const fetchData = async () => {
  try {
    const [orderRes, dictRes, userRes, roomRes, productRes, typeRes] = await Promise.all([
      api.get('/orders/all'),
      api.get('/sys/dict/all'),
      api.get('/sys/users'),
      api.get('/rooms/all'),
      api.get('/product-prices/all'),
      api.get('/room-types/all')
    ]) as any[];
    // Sort by latest first
    orders.value = (orderRes || []).sort((a: any, b: any) => 
       new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
    );
    dicts.value = dictRes;
    users.value = userRes;
    rooms.value = roomRes;
    productPrices.value = productRes;
    roomTypes.value = typeRes;
    // Initial fetch for available rooms if form dates are set
    if (form.startDate && form.endDate) fetchAvailableRooms();
  } catch (e) {
    console.error('Failed to fetch data', e);
  }
};

const fetchAvailableRooms = async () => {
  if (!form.startDate || !form.endDate) {
    availableRooms.value = [];
    return;
  }
  try {
    const start = form.startDate.length === 10 ? form.startDate + 'T14:00:00' : form.startDate;
    const end = form.endDate.length === 10 ? form.endDate + 'T12:00:00' : form.endDate;
    const res = await api.get(`/rooms/available?startDate=${start}&endDate=${end}`) as any;
    availableRooms.value = res;
    // Also include currently selected rooms in the available list so they don't show as empty/unknown
    const currentRoomIds = form.roomOccupies?.map((ro: any) => ro.roomId).filter(Boolean) || [];
    currentRoomIds.forEach((id: number) => {
      if (!availableRooms.value.find(r => r.id === id)) {
        const room = rooms.value.find(r => r.id === id);
        if (room) availableRooms.value.push(room);
      }
    });
  } catch (e) {
    console.error('Failed to fetch available rooms', e);
  }
};

watch([() => form.startDate, () => form.endDate], fetchAvailableRooms);

const filteredOrders = computed(() => {
  const today = new Date().toISOString().split('T')[0];
  if (!Array.isArray(orders.value)) return [];
  return orders.value.filter(o => {
    // Search filter
    const q = searchQuery.value.toLowerCase();
    const bookerName = o.booker?.realName?.toLowerCase() || '';
    const creatorName = o.createUser?.realName?.toLowerCase() || o.createUser?.username?.toLowerCase() || '';
    const occupants = o.roomOccupies?.map((ro: any) => 
      (ro.occupantUser?.realName || ro.coOccupants || '').toLowerCase()
    ).join(' ') || '';
    
    const matchSearch = !q || 
      bookerName.includes(q) || 
      creatorName.includes(q) || 
      occupants.includes(q);
    
    // Status filter
    const matchStatus = statusFilter.value.length === 0 || statusFilter.value.includes(o.status);
    
    // Today Arrival filter
    const matchArrival = !filterTodayArrival.value || o.startDate?.split('T')[0] === today;
    
    // Today Departure filter
    const matchDeparture = !filterTodayDeparture.value || o.endDate?.split('T')[0] === today;
    
    return matchSearch && matchStatus && matchArrival && matchDeparture;
  });
});

// 后台管理固定显示中文
const dictLabelZh: Record<string, Record<string, string>> = {
  ROOM_STATUS: { 'Available': '可用', 'Locked': '锁定' },
  ORDER_STATUS: { 'Cooling-off': '冷静期', 'Pending': '待确认', 'In': '已入住', 'Out': '已退房', 'Canceled': '已取消' },
  BIZ_TYPE: { 'Short Rent': '短租', 'Long Rent': '长租' }
};

const getDictLabel = (code: string, value: any) => {
  const dict = dicts.value.find(d => d.dictCode === code);
  if (!dict) return value;
  const item = dict.items.find((i: any) => i.itemValue === String(value));
  if (!item) return value;
  return dictLabelZh[code]?.[item.itemLabel] || item.itemLabel;
};

const getDictOptions = (code: string) => {
  const dict = dicts.value.find(d => d.dictCode === code);
  if (!dict) return [];
  return dict.items.map((i: any) => ({
    label: dictLabelZh[code]?.[i.itemLabel] || i.itemLabel,
    value: i.itemValue
  }));
};

const getOrderStatusClass = (status: number) => {
  switch(status) {
    case 0: return 'repair'; // Cooling-off
    case 1: return 'occupied'; // Pending
    case 2: return 'active'; // In
    case 3: return 'repair'; // Out
    default: return '';
  }
};


const calculateDays = (start: string, end: string) => {
  if (!start || !end) return 0;
  const s = new Date(start);
  const e = new Date(end);
  const diffTime = e.getTime() - s.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  return diffDays > 0 ? diffDays : 1;
};

watch([() => form.roomOccupies, () => form.startDate, () => form.endDate, () => form.bizType, () => form.productDetails], () => {
  let rFee = 0;
  let sFee = 0;
  // 1. Room Fee (Sum of all rooms)
  if (form.roomOccupies && form.startDate && form.endDate) {
    const days = calculateDays(form.startDate, form.endDate);
    form.roomOccupies.forEach((ro: any) => {
      if (ro.roomId) {
        const room = rooms.value.find(r => r.id === ro.roomId);
        if (room && room.roomType) {
          const price = form.bizType === 1 ? room.roomType.priceShortRent : room.roomType.priceLongRent;
          rFee += (price || 0) * days;
        }
      }
    });
  }
  // 2. Product/Service Fee
  if (form.productDetails && form.productDetails.length > 0) {
    form.productDetails.forEach((d: any) => {
      sFee += (d.actualPrice || 0) * (d.quantity || 1);
    });
  }
  form.roomFee = rFee;
  form.serviceFee = sFee;
  form.totalAmount = rFee + sFee;
}, { deep: true });

const getRoomNo = (id: number) => rooms.value.find(r => r.id === id)?.roomNo;
const getRoomTypeName = (id: number) => {
  const room = rooms.value.find(r => r.id === id);
  if (!room || !room.roomType) return '未知房型';
  return parseNameIntl(room.roomType.nameIntlJson, 'zh') || room.roomType.typeCode || '未知房型';
};


const addRoomRow = () => {
  form.roomOccupies.push({
    roomId: null,
    occupantUserId: form.customerType === 1 ? form.bookerId : null,
    occupantCount: 1,
    status: 0,
    roomCardNo: '',
    coOccupants: '',
    checkInTime: form.startDate ? `${form.startDate}T14:00` : null,
    checkOutTime: form.endDate ? `${form.endDate}T12:00` : null
  });
};

const removeRoomRow = (index: any) => {
  form.roomOccupies.splice(index, 1);
  refreshTotals();
};

const changeRoomPriceDiff = computed(() => {
  if (!changingOccupy.value || !changeRoomNewRoomId.value) return 0;
  const newRoom = rooms.value.find(r => r.id === changeRoomNewRoomId.value);
  if (!newRoom) return 0;
  
  const oldRoom = rooms.value.find(r => r.id === changingOccupy.value.roomId);
  const oldPrice = form.bizType === 1 ? oldRoom?.roomType?.priceShortRent : oldRoom?.roomType?.priceLongRent;
  const newPrice = form.bizType === 1 ? newRoom.roomType?.priceShortRent : newRoom.roomType?.priceLongRent;
  
  const days = calculateDays(changeRoomDate.value, form.endDate);
  return ((newPrice || 0) - (oldPrice || 0)) * days;
});

const startChangeRoom = (occupy: any) => {
  changingOccupy.value = occupy;
  changeRoomNewRoomId.value = null;
  changeRoomDate.value = new Date().toISOString().slice(0, 10);
  fetchChangeRoomAvailableRooms();
  showChangeRoomModal.value = true;
};

const fetchChangeRoomAvailableRooms = async () => {
  if (!changeRoomDate.value || !form.endDate) return;
  try {
    const res = await api.get(`/rooms/available?startDate=${changeRoomDate.value}&endDate=${form.endDate}`) as any;
    // Filter out maintenance/locked rooms and rooms in THIS order
    changeRoomAvailableRooms.value = res.filter((r: any) => 
      r.status === 0 && !r.isMaintenance && r.id !== changingOccupy.value?.roomId
    );
  } catch (e) {
    console.error('Failed to fetch available rooms for change', e);
  }
};

const confirmChangeRoom = async () => {
  if (!changeRoomNewRoomId.value || !changingOccupy.value) return;
  
  const msg = changeRoomPriceDiff.value > 0 
    ? `换房将产生补缴金额 ¥${changeRoomPriceDiff.value.toFixed(2)}，确定继续？`
    : `换房将产生退还金额 ¥${Math.abs(changeRoomPriceDiff.value).toFixed(2)}，确定继续？`;
  
  if (!confirm(msg)) return;

  try {
    const switchDate = changeRoomDate.value + 'T' + new Date().toTimeString().slice(0, 8);
    await api.post(`/orders/occupy/${changingOccupy.value.id}/change-room?roomId=${changeRoomNewRoomId.value}&switchDate=${switchDate}`, {});
    alert('换房成功');
    showChangeRoomModal.value = false;
    showModal.value = false;
    fetchData();
  } catch (e: any) {
    alert('换房失败: ' + (e.response?.data || e.message));
  }
};

const onBookerChange = () => {
  const user = users.value.find(u => u.id === form.bookerId);
  if (user) {
    form.bookPhone = user.phone;
  }
};

const openRoomPicker = (occupy: any, index: any) => {
  pickingOccupyIndex.value = index;
  roomPickerFilters.startDate = occupy.checkInTime ? occupy.checkInTime.split('T')[0] : form.startDate;
  roomPickerFilters.endDate = occupy.checkOutTime ? occupy.checkOutTime.split('T')[0] : form.endDate;
  roomPickerFilters.roomTypeId = null; // Reset filter
  showRoomPicker.value = true;
  fetchPickerAvailableRooms();
};

const fetchPickerAvailableRooms = async () => {
  if (!roomPickerFilters.startDate || !roomPickerFilters.endDate) {
    pickerAvailableRooms.value = [];
    return;
  }
  try {
    const res = await api.get(`/rooms/available?startDate=${roomPickerFilters.startDate}&endDate=${roomPickerFilters.endDate}`) as any;
    pickerAvailableRooms.value = res;
  } catch (e) {
    console.error('Failed to fetch picker available rooms', e);
  }
};

const filteredPickerRooms = computed(() => {
  let list = pickerAvailableRooms.value;
  // Apply "not locked" and "not maintenance" logic
  // status 0 = Available, 1 = Locked
  list = list.filter(r => r.status === 0 && !r.isMaintenance);
  
  // Apply room type filter
  if (roomPickerFilters.roomTypeId) {
    list = list.filter(r => r.roomType?.id === roomPickerFilters.roomTypeId);
  }
  
  // Filter out rooms already selected in THIS order (except the one being edited if it was already set)
  const otherSelectedRoomIds = form.roomOccupies
    .map((ro: any, idx: number) => idx !== pickingOccupyIndex.value ? ro.roomId : null)
    .filter(Boolean);
    
  list = list.filter(r => !otherSelectedRoomIds.includes(r.id));
  
  return list;
});

const selectRoomFromPicker = (room: any) => {
  if (pickingOccupyIndex.value === null) return;
  const occupy = form.roomOccupies[pickingOccupyIndex.value];
  occupy.roomId = room.id;
  // Sync check-in/out times with the picker dates if they were changed
  occupy.checkInTime = `${roomPickerFilters.startDate}T14:00`;
  occupy.checkOutTime = `${roomPickerFilters.endDate}T12:00`;
  // Set default price and quantity
  const price = form.bizType === 2 ? room.roomType?.priceLongRent : room.roomType?.priceShortRent;
  occupy.actualPrice = price;
  occupy.quantity = calculateDays(roomPickerFilters.startDate, roomPickerFilters.endDate);
  
  showRoomPicker.value = false;
  onRoomChange(occupy);
  refreshTotals();
};

const onRoomChange = (_occupy: any) => {
  // Logic after room selection
};

const getProductById = (id: number) => productPrices.value.find(p => p.id === id);

const addProductDetail = () => {
  if (!form.productDetails) form.productDetails = [];
  form.productDetails.push({
    productId: null,
    actualPrice: 0,
    quantity: 1,
    consumeDate: new Date().toISOString().split('T')[0],
    remarks: ''
  });
};

const removeProductDetail = (index: any) => {
  form.productDetails.splice(index, 1);
  refreshTotals();
};

const onProductChange = (detail: any) => {
  const p = getProductById(detail.productId);
  if (p) {
    detail.actualPrice = p.price;
  }
  refreshTotals();
};

const openModal = (order?: any, tab: string = 'basic') => {
  isEditMode.value = !order; // new order = edit mode, existing order = read-only
  if (order) {
    Object.assign(form, order);
    // Format dates for datetime-local input (YYYY-MM-DDTHH:mm)
    if (order.createdAt) form.createdAt = order.createdAt.slice(0, 16);
    if (order.startDate) form.startDate = order.startDate.slice(0, 16);
    if (order.endDate) form.endDate = order.endDate.slice(0, 16);
    
    // Handle nested roomOccupies
    if (order.roomOccupies) {
      form.roomOccupies = order.roomOccupies.map((ro: any) => ({
        ...ro,
        roomId: ro.room?.id,
        occupantUserId: ro.occupantUser?.id,
        checkInTime: ro.checkInTime ? ro.checkInTime.slice(0, 16) : null,
        checkOutTime: ro.checkOutTime ? ro.checkOutTime.slice(0, 16) : null
      }));
    } else {
      form.roomOccupies = [];
    }
    form.bookerId = order.booker?.id || null;
    // Handle nested productDetails
    if (order.productDetails) {
      form.productDetails = order.productDetails.map((d: any) => ({
        ...d,
        productId: d.product?.id
      }));
    } else {
      form.productDetails = [];
    }
  } else {
    Object.assign(form, { 
      id: null, 
      userId: null, 
      bizType: 1, 
      startDate: new Date().toISOString().slice(0, 10) + 'T14:00',
      endDate: new Date(Date.now() + 86400000).toISOString().slice(0, 10) + 'T12:00',
      createdAt: new Date().toISOString().slice(0, 16),
      roomFee: 0,
      serviceFee: 0,
      totalAmount: 0, 
      status: 1, 
      customerType: 1,
      bookerId: null,
      bookPhone: '',
      remarks: '',
      company: '', 
      costCenter: '',
      roomOccupies: [],
      productDetails: []
    });
  }
  fetchAvailableRooms();
  // Fetch order logs for existing orders
  if (order && order.id) {
    fetchOrderLogs(order.id);
  } else {
    orderLogs.value = [];
  }
  activeTab.value = tab;
  showModal.value = true;
};

const openTimeAdjustModal = async (occupy: any) => {
  adjustingOccupy.value = occupy;
  adjustingDates.start = occupy.checkInTime ? occupy.checkInTime.slice(0, 16) : form.startDate;
  adjustingDates.end = occupy.checkOutTime ? occupy.checkOutTime.slice(0, 16) : form.endDate;
  
  try {
    const res = await api.get(`/rooms/${occupy.roomId}/booked-periods`) as any;
    // Exclude THIS order's current periods if any
    bookedPeriods.value = res.filter((p: any) => p.orderId !== form.id);
    showTimeAdjustModal.value = true;
  } catch (e) {
    console.error('Failed to fetch booked periods', e);
    alert('无法获取房间预订信息');
  }
};

const confirmTimeAdjust = async () => {
  if (!adjustingDates.start || !adjustingDates.end) {
    alert('请选择完整的时间段');
    return;
  }
  try {
    const res = await api.post(`/orders/occupy/${adjustingOccupy.value.id}/adjust-time?startDate=${adjustingDates.start}&endDate=${adjustingDates.end}`, {}) as any;
    alert('时间调整成功，费用已更新');
    // Refresh modal data
    Object.assign(form, res);
    // Format dates again
    if (res.createdAt) form.createdAt = res.createdAt.slice(0, 16);
    if (res.startDate) form.startDate = res.startDate.slice(0, 16);
    if (res.endDate) form.endDate = res.endDate.slice(0, 16);
    if (res.roomOccupies) {
      form.roomOccupies = res.roomOccupies.map((ro: any) => ({
        ...ro,
        roomId: ro.room?.id,
        occupantUserId: ro.occupantUser?.id,
        checkInTime: ro.checkInTime ? ro.checkInTime.slice(0, 16) : null,
        checkOutTime: ro.checkOutTime ? ro.checkOutTime.slice(0, 16) : null
      }));
    }
    showTimeAdjustModal.value = false;
    fetchData(); // Refresh list background
  } catch (e: any) {
    const msg = e.response?.data?.message || e.response?.data || e.message;
    alert('调整失败: ' + (typeof msg === 'string' ? msg : JSON.stringify(msg)));
  }
};

const refreshTotals = () => {
  let roomTotal = 0;
  if (form.roomOccupies) {
    form.roomOccupies.forEach((ro: any) => {
      roomTotal += (ro.actualPrice || 0) * (ro.quantity || 0);
    });
  }
  form.roomFee = roomTotal;
  
  let serviceTotal = 0;
  if (form.productDetails) {
    form.productDetails.forEach((pd: any) => {
      serviceTotal += (pd.actualPrice || 0) * (pd.quantity || 0);
    });
  }
  form.serviceFee = serviceTotal;
  form.totalAmount = roomTotal + serviceTotal;
};

const deleteOrder = async (id: number) => {
  if (!confirm('确定要永久删除此作废订单吗？')) return;
  try {
    await api.delete(`/orders/${id}`);
    fetchData();
    alert('删除成功');
  } catch (e: any) {
    alert('删除失败: ' + (e.response?.data || e.message));
  }
};

const fetchOrderLogs = async (orderId: number) => {
  try {
    const res = await api.get(`/orders/${orderId}/logs`) as any;
    orderLogs.value = res || [];
  } catch (e) {
    orderLogs.value = [];
  }
};

const getLogTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    SAVE: '保存',
    SEND_CARD: '发房卡',
    CANCEL: '取消',
    CHECKOUT: '退房',
    CHANGE_ROOM: '换房',
    ADJUST_TIME: '调时间'
  };
  return map[type] || type;
};

const formatLogTime = (time: string) => {
  if (!time) return '';
  return time.replace('T', ' ').slice(0, 16);
};

const parseChanges = (jsonStr: string) => {
  try {
    return JSON.parse(jsonStr);
  } catch {
    return [];
  }
};

const scrollTo = (id: string) => {
  const el = document.getElementById(id);
  if (el) el.scrollIntoView({ behavior: 'smooth' });
};

const handleCheckout = async () => {
  if (!confirm('确定退房？这将把订单状态设置为已退房。')) return;
  form.status = 3; // OUT
  await saveOrder();
};

const sendRoomCard = async (id: number) => {
  try {
    // If we are in the modal, we might want to save the form first if it's dirty,
    // but the requirement says "validation in form", so we assume the backend handles it.
    await api.post(`/orders/${id}/send-card`, {});
    fetchData();
    if (showModal.value) showModal.value = false;
    alert('房卡发送成功，订单状态已更新为已入住');
  } catch (e: any) {
    alert('发送失败: ' + (e.response?.data || e.message));
  }
};

const cancelOrder = async (id: number) => {
  if (!confirm('确定取消此订单？')) return;
  try {
    await api.post(`/orders/${id}/cancel`, {});
    fetchData();
  } catch (e) {
    alert('Failed to cancel');
  }
};

const cancelOrderFromModal = async (id: number) => {
  if (!confirm('确定取消此订单？')) return;
  try {
    await api.post(`/orders/${id}/cancel`, {});
    showModal.value = false;
    fetchData();
    alert('订单已取消');
  } catch (e: any) {
    alert('取消失败: ' + (e.response?.data || e.message));
  }
};

const saveOrder = async () => {
  try {
    if (!form.bookerId) {
      alert('⚠️ 请选择预订人');
      return;
    }
    if (!form.roomOccupies || form.roomOccupies.length === 0) {
      alert('⚠️ 请添加至少一个房间');
      return;
    }
    if (!form.startDate || !form.endDate) {
      alert('⚠️ 请选择入住和退房日期');
      return;
    }
    if (new Date(form.endDate) < new Date(form.startDate)) {
      alert('⚠️ 退房日期不能早于入住日期');
      return;
    }


    // Map IDs to objects for backend
    const payload = { ...form };
    
    if (form.bookerId) {
      payload.booker = { id: form.bookerId };
    }
    delete payload.bookerId;

    // Handle nested roomOccupies payload
    if (form.roomOccupies) {
      payload.roomOccupies = form.roomOccupies.map((ro: any) => ({
        ...ro,
        room: ro.roomId ? { id: ro.roomId } : null,
        occupantUser: ro.occupantUserId ? { id: ro.occupantUserId } : null
      })).filter((ro: any) => ro.room !== null);
    }

    // Handle nested productDetails payload
    if (form.productDetails) {
      payload.productDetails = form.productDetails.map((d: any) => ({
        ...d,
        product: d.productId ? { id: d.productId } : null
      })).filter((d: any) => d.product !== null);
    }
    delete payload.userId;
    
    await api.post('/orders', payload);
    isEditMode.value = false;
    showModal.value = false;
    fetchData();
  } catch (e: any) {
    const msg = e.response?.data?.message || e.response?.data || e.message || 'Failed to save order';
    alert('保存失败: ' + (typeof msg === 'string' ? msg : JSON.stringify(msg)));
  }
};

const cancelEdit = async () => {
  if (form.id) {
    try {
      const all = (await api.get('/orders/all')) as any[];
      const fresh = all.find(o => o.id === form.id);
      if (fresh) {
        Object.assign(form, fresh);
        if (fresh.createdAt) form.createdAt = fresh.createdAt.slice(0, 16);
        if (fresh.startDate) form.startDate = fresh.startDate.slice(0, 16);
        if (fresh.endDate) form.endDate = fresh.endDate.slice(0, 16);
        if (fresh.roomOccupies) {
          form.roomOccupies = fresh.roomOccupies.map((ro: any) => ({
            ...ro,
            roomId: ro.room?.id,
            occupantUserId: ro.occupantUser?.id,
            checkInTime: ro.checkInTime ? ro.checkInTime.slice(0, 16) : null,
            checkOutTime: ro.checkOutTime ? ro.checkOutTime.slice(0, 16) : null
          }));
        } else {
          form.roomOccupies = [];
        }
        form.bookerId = fresh.booker?.id || null;
        if (fresh.productDetails) {
          form.productDetails = fresh.productDetails.map((d: any) => ({
            ...d,
            productId: d.product?.id
          }));
        } else {
          form.productDetails = [];
        }
        fetchOrderLogs(fresh.id);
      }
      isEditMode.value = false;
    } catch (e) {
      alert('重新获取订单失败');
      isEditMode.value = false;
    }
  }
};

onMounted(fetchData);
</script>

<style scoped>
@import "../../assets/admin.css";
.tag { background: #e0f2fe; color: #0369a1; padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }
.occupied { background: #fef9c3; color: #854d0e; }
.repair { background: #fee2e2; color: #991b1b; }

.refresh-btn {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
  margin-right: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.refresh-btn:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #0f172a;
}

.status-filters {
  display: flex;
  gap: 15px;
  align-items: center;
  margin-left: auto;
  padding: 10px;
}
.filter-checkbox {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  user-select: none;
}
.filter-checkbox input {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.filter-row { margin-bottom: -12px; display: flex; align-items: center; }
.quick-filters { display: flex; gap: 10px; }
.filter-chip {
  padding: 6px 12px;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  background: #fff;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}
.filter-chip:hover { border-color: #cbd5e1; background: #f8fafc; }
.filter-chip.active { background: #3b82f6; color: #fff; border-color: #3b82f6; }

/* Room Selector Button */
.room-selector-btn {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 8px 12px;
  cursor: pointer;
  min-height: 38px;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}
.room-selector-btn:hover { border-color: #3b82f6; background: #f8fafc; }
.selected-room-info strong { color: #0f172a; margin-right: 4px; }
.selected-room-info small { color: #64748b; font-size: 11px; }
.placeholder { color: #94a3b8; font-size: 13px; }

/* Room Picker Modal */
.room-picker-modal { max-width: 800px !important; }
.subtitle { font-size: 12px; color: #64748b; margin-top: 4px; }
.picker-filter-bar {
  display: flex;
  gap: 20px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 10px;
  margin-bottom: 20px;
}
.filter-group { display: flex; flex-direction: column; gap: 4px; flex: 1; }
.filter-group label { font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; }
.filter-group input, .filter-group select {
  padding: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
}

.room-selection-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
  max-height: 400px;
  overflow-y: auto;
  padding: 4px;
}

.room-pick-card {
  background: #fff;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.room-pick-card:hover {
  border-color: #3b82f6;
  background: #eff6ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}
.room-badge { font-size: 10px; font-weight: 800; color: #94a3b8; text-transform: uppercase; }
.room-no { font-size: 18px; font-weight: 800; color: #1e293b; margin: 4px 0; }
.room-meta { display: flex; flex-direction: column; gap: 4px; }
.type-tag { font-size: 11px; font-weight: 600; color: #3b82f6; background: #eff6ff; padding: 2px 6px; border-radius: 4px; align-self: flex-start; }
.price { font-size: 13px; font-weight: 700; color: #10b981; }

.empty-picker {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px;
  color: #94a3b8;
}
.empty-icon { font-size: 48px; margin-bottom: 12px; }

.modal-tabs { display: flex; gap: 20px; border-bottom: 1px solid #e2e8f0; margin-bottom: 20px; }
.modal-tabs button {
  background: none; border: none; padding: 10px 0; font-weight: 600; color: #64748b; cursor: pointer;
  border-bottom: 2px solid transparent; transition: all 0.2s;
}
.modal-tabs button.active { color: #38bdf8; border-bottom-color: #38bdf8; }

.tab-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.tab-header h3 { font-size: 16px; margin: 0; }
.add-btn.small { padding: 4px 10px; font-size: 12px; }

.detail-table { width: 100%; border-collapse: collapse; margin-top: 10px; }
.detail-table th { text-align: left; font-size: 12px; color: #64748b; padding: 10px; border-bottom: 1px solid #f1f5f9; }
.detail-table td { padding: 10px; border-bottom: 1px solid #f1f5f9; }
.small-input { width: 60px; padding: 4px 8px; border: 1px solid #e2e8f0; border-radius: 4px; }
.detail-table select { padding: 4px 8px; border: 1px solid #e2e8f0; border-radius: 4px; width: 100%; }

/* New Grid and Maximized Styles */
.modal-content {
  max-width: 1200px;
  width: 90%;
}

.modal-content.modal-maximized {
  width: 98vw;
  height: 98vh;
  max-width: none;
  margin: 1vh auto;
}

.modal-body.scrollable {
  overflow-y: auto;
  max-height: calc(90vh - 120px);
}
.modal-maximized .modal-body.scrollable {
  max-height: calc(98vh - 120px);
}

.modal-nav { display: flex; gap: 15px; margin-left: 20px; }
.modal-nav a { font-size: 14px; text-decoration: none; color: #64748b; font-weight: 600; }
.modal-nav a:hover { color: #38bdf8; }

.header-actions { display: flex; gap: 10px; align-items: center; }
.maximize-btn, .checkout-btn, .cancel-order-btn, .edit-top-btn {
  background: #f1f5f9; border: 1px solid #e2e8f0; border-radius: 4px; padding: 4px 10px; cursor: pointer; font-size: 14px;
}
.edit-top-btn { background: #f0fdf4; color: #15803d; border-color: #bbf7d0; font-weight: 600; }
.edit-top-btn:hover { background: #dcfce7; }
.cancel-order-btn { background: #fef3c7; color: #92400e; border-color: #fde68a; font-weight: 600; }
.cancel-order-btn:hover { background: #fde68a; }
.checkout-btn { background: #fee2e2; color: #b91c1c; border-color: #fecaca; font-weight: 600; }
.checkout-btn:hover { background: #fecaca; }

.send-card-btn { 
  background: #f0fdf4; color: #15803d; border: 1px solid #bbf7d0; 
  border-radius: 4px; padding: 4px 10px; cursor: pointer; font-size: 14px; font-weight: 600;
}
.send-card-btn:hover { background: #dcfce7; }

.form-section { padding: 20px; border-bottom: 1px solid #f1f5f9; scroll-margin-top: 20px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.section-title { font-size: 16px; font-weight: 700; color: #1e293b; margin-bottom: 0; border-left: 4px solid #38bdf8; padding-left: 10px; }

.form-grid-4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
}
.span-2 { grid-column: span 2; }
.span-3 { grid-column: span 3; }

.amount-display { display: flex; align-items: center; gap: 8px; background: #f8fafc; padding: 4px 12px; border-radius: 6px; border: 1px solid #e2e8f0; }
.currency { font-weight: 700; color: #0369a1; }
.amount-display input { background: transparent; border: none; font-weight: 700; font-size: 16px; width: 100%; color: #0369a1; }

.table-wrapper { overflow-x: auto; }
.table-input { padding: 4px; border: 1px solid #e2e8f0; border-radius: 4px; font-size: 13px; width: 100%; }
.table-input.no-border { border-color: transparent; background: #f8fafc; }
.empty-row { text-align: center; color: #94a3b8; padding: 30px; }

.footer-summary { font-size: 16px; font-weight: 600; }
.price-highlight { color: #ef4444; font-size: 20px; font-weight: 800; }
.footer-btns { display: flex; gap: 10px; }
.cancel-edit-btn { background: #f1f5f9; border: 1px solid #e2e8f0; border-radius: 6px; padding: 8px 16px; font-weight: 600; color: #64748b; cursor: pointer; transition: all 0.2s; }
.cancel-edit-btn:hover { background: #fee2e2; border-color: #fecaca; color: #b91c1c; }

/* Room Card Styles */
.room-cards-container { display: grid; grid-template-columns: 1fr; gap: 20px; margin-top: 15px; }
.room-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 15px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); transition: all 0.2s; }
.room-card:hover { box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border-color: #38bdf8; }
.card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 1px dashed #f1f5f9; }
.card-title { display: flex; flex-direction: column; gap: 2px; }
.room-no { font-size: 16px; font-weight: 800; color: #0f172a; }
.room-type { font-size: 11px; color: #64748b; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
.card-actions { display: flex; gap: 8px; }
.card-action-btn { padding: 4px 8px; border-radius: 6px; border: 1px solid #e2e8f0; background: #fff; font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.card-action-btn.change { color: #0369a1; border-color: #bae6fd; background: #f0f9ff; }
.card-action-btn.change:hover { background: #e0f2fe; }
.card-action-btn.adjust { color: #8b5cf6; border-color: #ddd6fe; background: #f5f3ff; }
.card-action-btn.adjust:hover { background: #ede9fe; }
.card-action-btn.delete { color: #94a3b8; border: none; font-size: 18px; line-height: 1; padding: 0 4px; }
.card-action-btn.delete:hover { color: #ef4444; }

.card-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; }
.card-item label { display: block; font-size: 11px; color: #94a3b8; margin-bottom: 4px; font-weight: 600; }
.card-item select, .card-item input { width: 100%; padding: 6px 8px; border: 1px solid #f1f5f9; border-radius: 6px; font-size: 13px; background: #f8fafc; }
.empty-rooms { grid-column: span 3; text-align: center; padding: 40px; color: #94a3b8; background: #f8fafc; border: 2px dashed #e2e8f0; border-radius: 12px; }

/* List View Styles */
.room-count-badge { background: #0f172a; color: #fff; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 700; }
.room-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.room-tag { background: #f1f5f9; color: #475569; padding: 1px 6px; border-radius: 4px; font-size: 11px; font-weight: 600; border: 1px solid #e2e8f0; }
.amount-display.highlight { border-color: #38bdf8; background: #f0f9ff; }

.info-alert { background: #f0f9ff; border: 1px solid #bae6fd; border-radius: 8px; padding: 12px; margin-bottom: 20px; }
.info-alert p { margin: 4px 0; font-size: 14px; color: #0369a1; }
.booked-periods-view label { font-size: 12px; font-weight: 700; color: #64748b; margin-bottom: 8px; display: block; }
.period-list { list-style: none; padding: 0; margin: 0; max-height: 150px; overflow-y: auto; }
.period-item { font-size: 12px; padding: 6px 10px; border-radius: 4px; margin-bottom: 4px; border: 1px solid #e2e8f0; }
.period-item.maintenance { background: #fef2f2; color: #991b1b; border-color: #fecaca; }
.period-item.order { background: #f0f9ff; color: #0369a1; border-color: #bae6fd; }
.hint-text { font-size: 12px; color: #94a3b8; margin-top: 10px; font-style: italic; }
.form-grid-2 { display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; }

/* Order Logs */
.log-list { max-height: 400px; overflow-y: auto; }
.log-item { padding: 12px 16px; border: 1px solid #e2e8f0; border-radius: 8px; margin-bottom: 10px; background: #fafbfc; }
.log-item.log-SAVE { border-left: 3px solid #3b82f6; }
.log-item.log-SEND_CARD { border-left: 3px solid #10b981; }
.log-item.log-CANCEL { border-left: 3px solid #ef4444; }
.log-item.log-CHECKOUT { border-left: 3px solid #f59e0b; }
.log-item.log-CHANGE_ROOM { border-left: 3px solid #8b5cf6; }
.log-item.log-ADJUST_TIME { border-left: 3px solid #06b6d4; }
.log-header { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.log-type-badge { font-size: 11px; font-weight: 700; padding: 2px 8px; border-radius: 10px; background: #e2e8f0; color: #475569; text-transform: uppercase; }
.log-item.log-SAVE .log-type-badge { background: #dbeafe; color: #1d4ed8; }
.log-item.log-SEND_CARD .log-type-badge { background: #d1fae5; color: #047857; }
.log-item.log-CANCEL .log-type-badge { background: #fee2e2; color: #b91c1c; }
.log-item.log-CHECKOUT .log-type-badge { background: #fef3c7; color: #b45309; }
.log-item.log-CHANGE_ROOM .log-type-badge { background: #ede9fe; color: #6d28d9; }
.log-item.log-ADJUST_TIME .log-type-badge { background: #cffafe; color: #0891b2; }
.log-time { font-size: 12px; color: #94a3b8; }
.log-operator { font-size: 12px; color: #64748b; font-weight: 600; }
.log-content { font-size: 13px; color: #334155; margin-bottom: 6px; }
.log-changes { background: #f1f5f9; border-radius: 6px; padding: 8px 12px; }
.log-change-row { font-size: 12px; color: #475569; padding: 2px 0; display: flex; align-items: center; gap: 6px; }
.log-field { font-weight: 700; min-width: 60px; }
.log-old { color: #ef4444; text-decoration: line-through; }
.log-arrow { color: #94a3b8; }
.log-new { color: #10b981; font-weight: 600; }
.empty-log { text-align: center; padding: 30px; color: #94a3b8; font-size: 13px; }
</style>
