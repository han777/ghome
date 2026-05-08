const fs = require('fs');

const zhPath = 'src/locales/zh.json';
const enPath = 'src/locales/en.json';
const zh = JSON.parse(fs.readFileSync(zhPath, 'utf-8'));
const en = JSON.parse(fs.readFileSync(enPath, 'utf-8'));

// Add new keys to orderDetail
const zhNew = {
  roomsSection: "入住房间",
  servicesSection: "产品服务",
  totalAmount: "订单总金额",
  roomNoLabel: "房间号",
  roomTypeLabel: "房型",
  checkInTime: "入住时间",
  checkOutTime: "离开时间",
  occupantCount: "入住人数",
  coOccupants: "同住人",
  serviceName: "服务名称",
  qty: "数量",
  unitPrice: "单价",
  subTotal: "小计",
  noServices: "暂无产品服务",
  person: "人"
};
const enNew = {
  roomsSection: "Room(s)",
  servicesSection: "Services",
  totalAmount: "Total Amount",
  roomNoLabel: "Room No.",
  roomTypeLabel: "Room Type",
  checkInTime: "Check-in",
  checkOutTime: "Check-out",
  occupantCount: "Guests",
  coOccupants: "Co-occupants",
  serviceName: "Service",
  qty: "Qty",
  unitPrice: "Unit Price",
  subTotal: "Subtotal",
  noServices: "No services",
  person: " pax"
};

Object.assign(zh.orderDetail, zhNew);
Object.assign(en.orderDetail, enNew);

fs.writeFileSync(zhPath, JSON.stringify(zh, null, 2));
fs.writeFileSync(enPath, JSON.stringify(en, null, 2));
console.log('Done');
