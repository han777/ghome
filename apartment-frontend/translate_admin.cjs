const fs = require('fs');
const path = require('path');

const adminDir = path.join(__dirname, 'src', 'views', 'admin');

const dictionary = {
  // Common Buttons & Actions
  '\\+ Add Type': '+ 添加房型',
  '\\+ Add Room': '+ 添加房间',
  '\\+ Add Role': '+ 添加角色',
  '\\+ Add Account': '+ 添加账号',
  '\\+ Add Product': '+ 添加产品',
  '\\+ Add Maintenance': '+ 添加维护',
  '\\+ Add Order': '+ 添加订单',
  '\\+ Add Row': '+ 添加行',
  '>Edit<': '>编辑<',
  '>Delete<': '>删除<',
  '>Cancel<': '>取消<',
  '>Save Changes<': '>保存更改<',
  '>Search<': '>搜索<',
  '>Reset<': '>重置<',
  '>Password<': '>密码<',

  // Placeholders
  'Search room types...': '搜索房型...',
  'Search rooms...': '搜索房间...',
  'Search roles...': '搜索角色...',
  'Search accounts...': '搜索账号...',
  'Search product name...': '搜索产品名称...',

  // Modals & Titles
  "'Edit Room Type' : 'Add Room Type'": "'编辑房型' : '添加房型'",
  "'Edit Room' : 'Add Room'": "'编辑房间' : '添加房间'",
  "'Edit Role' : 'Add Role'": "'编辑角色' : '添加角色'",
  "'Edit User' : 'Add User'": "'编辑用户' : '添加用户'",
  "'Edit Product' : 'Create Product'": "'编辑产品' : '创建产品'",
  "'Edit Maintenance' : 'Add Maintenance'": "'编辑维护' : '添加维护'",
  "'Edit Order' : 'Create Order'": "'编辑订单' : '创建订单'",

  // Table Headers
  '>Username<': '>用户名<',
  '>Real Name<': '>真实姓名<',
  '>Email<': '>邮箱<',
  '>Phone<': '>电话<',
  '>Roles<': '>角色<',
  '>Status<': '>状态<',
  '>Actions<': '>操作<',
  '>Room Number<': '>房间号<',
  '>Room Type<': '>房型<',
  '>Description<': '>描述<',
  '>Floor<': '>楼层<',
  '>Price<': '>价格<',
  '>Role Name<': '>角色名<',
  '>Role Code<': '>角色代码<',
  '>Product Name<': '>产品名称<',
  '>Price<': '>价格<',
  '>Unit<': '>单位<',
  '>Maintenance Type<': '>维护类型<',
  '>Cost<': '>费用<',
  '>Created At<': '>创建时间<',
  '>Updated At<': '>更新时间<',
  '>Type<': '>类型<',
  '>Order ID<': '>订单号<',
  '>Customer<': '>客户<',
  '>Amount<': '>金额<',
  '>Arrival<': '>入住日期<',
  '>Departure<': '>离店日期<',

  // Status & Badges
  '>Active<': '>启用<',
  '>Disabled<': '>禁用<',
  '>Available<': '>空闲<',
  '>Occupied<': '>已入住<',
  '>Maintenance<': '>维护中<',
  '>Locked<': '>锁定<',
  '>Clean<': '>干净<',
  '>Dirty<': '>脏房<',
  '>Pending<': '>待处理<',
  '>Completed<': '>已完成<',
  '>Cancelled<': '>已取消<',
  '>Paid<': '>已支付<',
  '>Unpaid<': '>未支付<',
  '>Partially Paid<': '>部分支付<',
  
  // Forms & Labels
  '>Name<': '>名称<',
  '>Description<': '>描述<',
  '>Remarks<': '>备注<',

  // Prompts & Alerts
  "'Delete this type\\?'": "'确定删除此房型？'",
  "'Delete this room\\?'": "'确定删除此房间？'",
  "'Delete this role\\?'": "'确定删除此角色？'",
  "'Are you sure you want to delete this user\\?'": "'确定要删除此用户吗？'",
  "'Delete this product\\?'": "'确定删除此产品？'",
  "'Delete this maintenance\\?'": "'确定删除此维护记录？'",
  "'Cancel this order\\?'": "'确定取消此订单？'",
  
  // Orders.vue specific
  '>Check In<': '>办理入住<',
  '>Check Out<': '>办理退房<',
  '>Change Room<': '>换房<',
  '>Print<': '>打印<',
  '>Detail<': '>详情<'
};

function translateFile(filePath) {
  let content = fs.readFileSync(filePath, 'utf-8');
  let original = content;

  for (const [key, value] of Object.entries(dictionary)) {
    const regex = new RegExp(key, 'g');
    content = content.replace(regex, value);
  }

  // Some more replacements
  content = content.replace(/label>(\w+)<\/label>/g, (match, p1) => {
    const map = {
      'Username': '用户名',
      'Password': '密码',
      'Status': '状态',
      'Roles': '角色',
      'Email': '邮箱',
      'Phone': '电话'
    };
    if (map[p1]) return `label>${map[p1]}</label>`;
    return match;
  });

  if (content !== original) {
    fs.writeFileSync(filePath, content, 'utf-8');
    console.log(`Translated ${path.basename(filePath)}`);
  }
}

const files = fs.readdirSync(adminDir).filter(f => f.endsWith('.vue'));
for (const file of files) {
  translateFile(path.join(adminDir, file));
}
