# 外部团体免建账号预订 — 设计方案（v3）

## 1. 背景与目标

针对外部团体（如政府参观、合作单位来访等），当前系统要求每个房间入住人必须是已注册的 `SysUser`，导致管理员需要为临时访客逐个创建系统账号，流程繁琐且产生大量僵尸账号。

**目标**：
1. 团体预订时，预订人仍从系统用户中选择，入住人以团体名称展示，无需为外部人员逐个建号
2. 增加联系人信息（姓名、联系电话），与订房人电话区分
3. 增加财务对账字段（所属公司、成本中心、活动编码），方便财务核算

## 2. 现状分析

| 约束点 | 当前状态 |
|--------|----------|
| `RoomOrder.booker` | `@ManyToOne SysUser`，预订人必选注册用户 |
| `RoomOccupy.occupantUser` | `@ManyToOne SysUser`，入住人**强制关联注册用户** |
| `customerType` | 已有 1=个人 / 2=团体 区分 |
| `bookPhone` | 已有字段，含义为**订房人电话**（从 booker 自动填充） |
| `company` | 已有字段，所属公司，非必填 |
| `costCenter` | 已有字段，成本中心，非必填 |
| `coOccupants` | 文本字段，同行人姓名 |
| 房态卡片 `guestName` | 取自 `occupantUser.realName`，无团体名称展示逻辑 |

关键文件：

| 类别 | 文件路径 |
|------|----------|
| 订单实体 | `apartment-backend/src/main/java/com/apartment/entity/RoomOrder.java` |
| 入住实体 | `apartment-backend/src/main/java/com/apartment/entity/RoomOccupy.java` |
| 订单服务 | `apartment-backend/src/main/java/com/apartment/service/RoomOrderService.java` |
| 订单控制器 | `apartment-backend/src/main/java/com/apartment/controller/RoomOrderController.java` |
| 房态仪表盘 | `apartment-frontend/src/views/admin/Dashboard.vue` |
| 管理后台订单页 | `apartment-frontend/src/views/admin/Orders.vue` |
| 移动端预订页 | `apartment-frontend/src/views/mobile/Booking.vue` |

## 3. 设计方案

**核心思路**：团体预订时 `booker` 仍必选系统用户；新增团体信息区（团体名称、联系人姓名、联系电话）和财务对账区（所属公司、成本中心、活动编码）；`RoomOccupy.occupantUser` 对团体订单解耦为可选，`occupantName` 自动填入团体名称且不可编辑。

### 3.1 数据库变更

```sql
-- RoomOrder 新增字段
ALTER TABLE room_order ADD COLUMN group_name VARCHAR(100);       -- 团体名称（customerType=2 时必填）
ALTER TABLE room_order ADD COLUMN contact_name VARCHAR(100);     -- 联系人姓名（团体时必填）
ALTER TABLE room_order ADD COLUMN contact_phone VARCHAR(30);     -- 联系电话（团体时必填，区别于 bookPhone 订房人电话）
ALTER TABLE room_order ADD COLUMN activity_code VARCHAR(50);     -- 活动编码（财务对账用）

-- RoomOccupy 新增字段
ALTER TABLE room_occupy ADD COLUMN occupant_name VARCHAR(100);    -- 入住人姓名（团体时=团体名称）
ALTER TABLE room_occupy ALTER COLUMN occupant_user_id DROP NOT NULL; -- 团体订单时允许为空
```

> 注：`company`（所属公司）和 `costCenter`（成本中心）已存在于 `room_order` 表，无需新增列，仅需在团体模式下设为必填。

### 3.2 实体层改动

**RoomOrder.java**：

```java
// 已有字段，无需改动
@ManyToOne
@JoinColumn(name = "book_user_id")
private SysUser booker;          // 预订人，仍必选系统用户

private Integer customerType = 1; // 1: 个人, 2: 团体
private String bookPhone;         // 订房人电话（从 booker 自动填充）
private String company;           // 所属公司（已有，团体时必填）
private String costCenter;        // 成本中心（已有，团体时必填）

// 新增字段
private String groupName;         // 团体名称（customerType=2 时必填）
private String contactName;       // 联系人姓名（团体时必填，区别于 booker）
private String contactPhone;      // 联系电话（团体时必填，区别于 bookPhone）
private String activityCode;      // 活动编码（财务对账用）
```

**RoomOccupy.java**：

```java
@ManyToOne
@JoinColumn(name = "occupant_user_id")
private SysUser occupantUser;    // 个人订单时必填，团体订单时为 null

// 新增字段
private String occupantName;     // 入住人姓名：个人=用户realName，团体=团体名称
```

### 3.3 字段定义总览

| 字段 | 中文 | 类型 | 个人预订 | 团体预订 | 说明 |
|------|------|------|----------|----------|------|
| `booker` | 预订人 | SysUser FK | 必选 | 必选 | 系统用户，订单操作人 |
| `bookPhone` | 订房人电话 | String | 可空 | 可空 | 从 booker 自动填充 |
| `groupName` | 团体名称 | String | — | **必填** | 如「市规划局参观团」 |
| `contactName` | 联系人姓名 | String | — | **必填** | 外部团体对接人 |
| `contactPhone` | 联系电话 | String | — | **必填** | 联系人电话，非订房人电话 |
| `company` | 所属公司 | String | 可空 | **必填** | 已有字段，团体时升级为必填 |
| `costCenter` | 成本中心 | String | 可空 | **必填** | 已有字段，团体时升级为必填 |
| `activityCode` | 活动编码 | String | 可空 | **必填** | 新增，财务对账唯一标识 |
| `occupantUser` | 入住人 | SysUser FK | 必选 | **null** | 团体时不填 |
| `occupantName` | 入住人姓名 | String | null | **自动=groupName** | 团体时自动填充，不可编辑 |

### 3.4 业务规则

校验逻辑（`RoomOrderService.validateOrder()`）：

```java
if (order.getCustomerType() != null && order.getCustomerType() == 2) {
    // === 团体信息必填校验 ===
    if (isBlank(order.getGroupName())) {
        throw new BizException("团体名称不能为空");
    }
    if (isBlank(order.getContactName())) {
        throw new BizException("联系人姓名不能为空");
    }
    if (isBlank(order.getContactPhone())) {
        throw new BizException("联系电话不能为空");
    }

    // === 财务对账字段必填校验 ===
    if (isBlank(order.getCompany())) {
        throw new BizException("所属公司不能为空");
    }
    if (isBlank(order.getCostCenter())) {
        throw new BizException("成本中心不能为空");
    }
    if (isBlank(order.getActivityCode())) {
        throw new BizException("活动编码不能为空");
    }

    // === 入住人自动填充团体名称 ===
    for (RoomOccupy occupy : order.getRoomOccupies()) {
        occupy.setOccupantUser(null);                    // 清空系统用户关联
        occupy.setOccupantName(order.getGroupName());    // 自动填入团体名称
    }
}
```

### 3.5 控制器改动

`RoomOrderController.saveOrder()` 逻辑基本不变，`booker` 仍必选：

```java
// 现有逻辑保持不变
if (order.getBooker() == null) {
    order.setBooker(u);  // booker 仍默认为当前用户
}
// bookPhone = 订房人电话，从 booker 自动填充
if (order.getBookPhone() == null || order.getBookPhone().isBlank()) {
    order.setBookPhone(u.getPhone());
}
```

团体订单的 `occupantName` 填充在 service 层统一处理。

### 3.6 前端交互 — 管理后台

订单创建/编辑弹窗中，根据 `customerType` 切换表单：

```
┌──────────────────────────────────────────────────────┐
│ 客户类型： ○ 个人  ● 团体                              │
│                                                      │
│ ── 预订信息 ──                                        │
│ 预订人：     [搜索系统用户 ▼]  *                        │  ← 两种模式均必选
│ 订房人电话： [________________]                        │  ← 自动填充，可修改
│                                                      │
│ ── 团体信息 ──（仅团体时显示）                           │
│ 团体名称：   [________________]  *                     │
│ 联系人姓名： [________________]  *                     │
│ 联系电话：   [________________]  *                     │
│                                                      │
│ ── 财务信息 ──（仅团体时显示）                           │
│ 所属公司：   [________________]  *                     │
│ 成本中心：   [________________]  *                     │
│ 活动编码：   [________________]  *                     │
│                                                      │
│ ── 房间入住信息 ──                                     │
│ 房间：       [选择房间 ▼]                              │
│ 入住人：     市规划局参观团                             │  ← 自动=团体名称，灰色不可编辑
│ 入住人数：   [__]                                     │
└──────────────────────────────────────────────────────┘
```

交互要点：

- 选「个人」→ 现有逻辑不变：预订人=入住人，occupantUser 必选
- 选「团体」→ 展示「团体信息」和「财务信息」两个区块，字段均为必填；入住人字段自动显示团体名称，**禁用编辑**（disabled + 灰色背景）
- 预订人在两种模式下均从系统用户下拉框中选择
- `bookPhone` 标签改为「订房人电话」，明确语义；`contactPhone` 标签为「联系电话」

### 3.7 前端交互 — 移动端

移动端暂不支持团体自订（团体订单由管理员代订），无需改动。如后续需支持，另议。

### 3.8 房态卡片展示适配

**当前逻辑**（`Dashboard.vue`）：

```vue
<div v-if="room.guestName && (room.status === 1 || room.status === 5)"
     class="guest-name-center">
  {{ room.guestName }}
</div>
```

**后端 guestName 取值逻辑调整**：

```
if (occupy.occupantName != null) {
    guestName = occupy.occupantName;          // 团体订单 → 显示团体名称
} else if (occupy.occupantUser != null) {
    guestName = occupy.occupantUser.realName; // 个人订单 → 显示用户姓名（原逻辑）
}
```

效果示例：

| 订单类型 | 房态卡片入住人位置 |
|----------|-------------------|
| 个人预订 | 张三 |
| 团体预订 | 市规划局参观团 |

前端 `Dashboard.vue` 模板无需改动，只需后端 `/dashboard/room-status` 接口调整 `guestName` 取值来源即可。

### 3.9 订单列表/详情展示适配

- 预订人列：统一显示 `booker.realName`（逻辑不变）
- 入住人列：个人订单显示 `occupantUser.realName`，团体订单显示 `occupantName`（= groupName）
- 团体订单详情中增加展示区块：

```
── 团体信息 ──
团体名称：市规划局参观团
联系人：  李四
联系电话：138-xxxx-xxxx

── 财务信息 ──
所属公司：XX市规划局
成本中心：CC-2024-001
活动编码：ACT-GOV-20240315
```

### 3.10 电话字段语义区分

| 字段 | 中文 | 语义 | 填充方式 |
|------|------|------|----------|
| `bookPhone` | 订房人电话 | 系统中操作预订的内部人员电话 | 从 `booker.phone` 自动填充，可修改 |
| `contactPhone` | 联系电话 | 外部团体的对接联系人电话 | 手动填写 |

两者共存，互不干扰。个人订单仅展示 `bookPhone`；团体订单同时展示两者。

## 4. 不推荐的替代方案

| 方案 | 问题 |
|------|------|
| 自动创建临时 SysUser 账号 | 污染用户表、产生僵尸账号、需要清理机制 |
| 新建 Guest 实体表 | 过度设计，外部团体预订频率低，不值得单独建表 |
| 复用 `company` 字段当团体名称 | 语义不同（company=所属公司，groupName=团体名称），且 `company` 对财务对账有独立用途 |
| booker 允许为空 | 打破了预订人必选系统用户的约束，影响审计和操作追溯 |
| 复用 `bookPhone` 当联系人电话 | 语义不同（订房人 vs 联系人），团体场景下两者均需保留 |

## 5. 影响范围评估

| 改动点 | 范围 |
|--------|------|
| 数据库迁移 | 1 个 migration（4 个 ADD COLUMN + 1 个 DROP NOT NULL） |
| 后端实体 | `RoomOrder` 加 `groupName`/`contactName`/`contactPhone`/`activityCode`；`RoomOccupy` 加 `occupantName`、`occupantUser` 可空 |
| 后端校验 | `RoomOrderService.validateOrder()` 增加团体模式校验（6 个必填字段） |
| 后端接口 | `/dashboard/room-status` 的 `guestName` 取值逻辑调整 |
| 管理后台前端 | `Orders.vue` 表单：团体时展开团体信息+财务信息区块，入住人自动填充且禁用 |
| 房态卡片 | 后端返回 `guestName` 兼容 `occupantName`，前端无需改动 |
| 移动端前端 | 暂不影响（外部团体由管理员代订） |
| 全局搜索 | 检查所有引用 `occupantUser.getRealName()` 的地方，改为优先取 `occupantName` |
| 订单详情 | 团体订单增加团体信息+财务信息展示区块 |

## 6. 向后兼容性

- 个人预订逻辑完全不受影响（`customerType=1` 时走原有分支）
- 所有新增字段均为可空，不影响现有数据
- `company` 和 `costCenter` 已有字段，仅在团体模式下校验必填，不影响个人订单
- `occupantUser` 改为可空，现有数据已有值不受影响
- 房态卡片 `guestName` 取值逻辑向后兼容：`occupantName` 为空时回退到 `occupantUser.realName`

## 7. 新增字段汇总

| 字段名 | 表 | 类型 | 说明 |
|--------|-----|------|------|
| `group_name` | `room_order` | VARCHAR(100) | 团体名称 |
| `contact_name` | `room_order` | VARCHAR(100) | 联系人姓名 |
| `contact_phone` | `room_order` | VARCHAR(30) | 联系电话 |
| `activity_code` | `room_order` | VARCHAR(50) | 活动编码 |
| `occupant_name` | `room_occupy` | VARCHAR(100) | 入住人姓名（团体时=团体名称） |
