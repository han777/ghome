const fs = require('fs');
const path = require('path');

const zhPath = path.join(__dirname, 'src', 'locales', 'zh.json');
const enPath = path.join(__dirname, 'src', 'locales', 'en.json');

const zhDict = JSON.parse(fs.readFileSync(zhPath, 'utf8'));
const enDict = JSON.parse(fs.readFileSync(enPath, 'utf8'));

// Booking
zhDict.booking.checkInDate = "入住日期";
enDict.booking.checkInDate = "Check-in Date";
zhDict.booking.checkOutDate = "退房日期";
enDict.booking.checkOutDate = "Check-out Date";
zhDict.booking.selectRoomType = "选择";
enDict.booking.selectRoomType = "Select";
zhDict.booking.stayNotice = "入住须知";
enDict.booking.stayNotice = "Stay Notice";
zhDict.booking.sun = "星期日";
enDict.booking.sun = "Sun";
zhDict.booking.mon = "星期一";
enDict.booking.mon = "Mon";
zhDict.booking.tue = "星期二";
enDict.booking.tue = "Tue";
zhDict.booking.wed = "星期三";
enDict.booking.wed = "Wed";
zhDict.booking.thu = "星期四";
enDict.booking.thu = "Thu";
zhDict.booking.fri = "星期五";
enDict.booking.fri = "Fri";
zhDict.booking.sat = "星期六";
enDict.booking.sat = "Sat";

// Confirm
zhDict.confirm.perNightMath = "（¥ {price}/晚×{days}晚）";
enDict.confirm.perNightMath = "(¥ {price}/Night x {days} Nights)";
zhDict.confirm.perNightMath2 = "( ¥ {price}/晚×{days} )";
enDict.confirm.perNightMath2 = "( ¥ {price}/Night x {days} )";
zhDict.confirm.currentUser = "当前用户";
enDict.confirm.currentUser = "Current User";

// Mine
zhDict.mine.title2 = "我的";
enDict.mine.title2 = "Me";

// OrderDetail
zhDict.orderDetail.doorPassword = "入住密码";
enDict.orderDetail.doorPassword = "Door Password";
zhDict.orderDetail.effectiveDate = "生效日期：";
enDict.orderDetail.effectiveDate = "Effective Date: ";
zhDict.orderDetail.after14 = "14:00后";
enDict.orderDetail.after14 = "After 14:00";
zhDict.orderDetail.before12 = "12:00前";
enDict.orderDetail.before12 = "Before 12:00";
zhDict.orderDetail.myself = "本人";
enDict.orderDetail.myself = "Myself";
zhDict.orderDetail.otherFee = "其他费用";
enDict.orderDetail.otherFee = "Other Fees";
zhDict.orderDetail.totalLabel = "合计";
enDict.orderDetail.totalLabel = "Total";
zhDict.orderDetail.financeNote = "财务统一结算，无需个人垫付、报销";
enDict.orderDetail.financeNote = "Unified financial settlement, no personal advance payment or reimbursement required";
zhDict.orderDetail.cancelRule = "取消预订：须于入住日前1天24:00前操作";
enDict.orderDetail.cancelRule = "Cancellation: Must be done before 24:00, 1 day prior to check-in";
zhDict.orderDetail.checkoutRule = "提前退房：须于实际退房日前1天24:00前操作";
enDict.orderDetail.checkoutRule = "Early Checkout: Must be done before 24:00, 1 day prior to actual checkout";
zhDict.orderDetail.cancelBtn = "取消预订";
enDict.orderDetail.cancelBtn = "Cancel Booking";
zhDict.orderDetail.earlyCheckoutBtn = "提前退房";
enDict.orderDetail.earlyCheckoutBtn = "Early Checkout";
zhDict.orderDetail.year = "年";
enDict.orderDetail.year = "-";
zhDict.orderDetail.month = "月";
enDict.orderDetail.month = "-";
zhDict.orderDetail.day = "日";
enDict.orderDetail.day = "";
zhDict.orderDetail.cancelConfirmMsg = "确定要取消预订吗？";
enDict.orderDetail.cancelConfirmMsg = "Are you sure you want to cancel the booking?";
zhDict.orderDetail.cancelSuccess = "已取消预订";
enDict.orderDetail.cancelSuccess = "Booking cancelled";
zhDict.orderDetail.cancelFail = "取消失败: ";
enDict.orderDetail.cancelFail = "Cancellation failed: ";
zhDict.orderDetail.checkoutConfirmMsg = "确定要提前退房吗？";
enDict.orderDetail.checkoutConfirmMsg = "Are you sure you want to checkout early?";
zhDict.orderDetail.checkoutSuccess = "已办理提前退房";
enDict.orderDetail.checkoutSuccess = "Early checkout processed";
zhDict.orderDetail.checkoutFail = "退房失败: ";
enDict.orderDetail.checkoutFail = "Checkout failed: ";
zhDict.orderDetail.submitSuccess = "订单提交成功";
enDict.orderDetail.submitSuccess = "Order submitted successfully";

// RoomSelect
zhDict.roomSelect.title2 = "选择";
enDict.roomSelect.title2 = "Select ";
zhDict.roomSelect.selected = "已选";
enDict.roomSelect.selected = "Selected";
zhDict.roomSelect.available = "可选";
enDict.roomSelect.available = "Available";
zhDict.roomSelect.unavailable = "不可选";
enDict.roomSelect.unavailable = "Unavailable";
zhDict.roomSelect.floor = "楼";
enDict.roomSelect.floor = "F";
zhDict.roomSelect.cancel = "取消";
enDict.roomSelect.cancel = "Cancel";
zhDict.roomSelect.confirm = "确定";
enDict.roomSelect.confirm = "Confirm";
zhDict.roomSelect.unknown = "未知";
enDict.roomSelect.unknown = "Unknown";
zhDict.roomSelect.fail = "预订失败: ";
enDict.roomSelect.fail = "Booking failed: ";

fs.writeFileSync(zhPath, JSON.stringify(zhDict, null, 2), 'utf8');
fs.writeFileSync(enPath, JSON.stringify(enDict, null, 2), 'utf8');

const viewsDir = path.join(__dirname, 'src', 'views', 'mobile');

function replaceInFile(fileName, replaces) {
  const p = path.join(viewsDir, fileName);
  let content = fs.readFileSync(p, 'utf8');
  for (const [k, v] of replaces) {
    content = content.replace(k, v);
  }
  fs.writeFileSync(p, content, 'utf8');
}

// Booking.vue
replaceInFile('Booking.vue', [
  [/>入住日期</g, `>{{ $t('booking.checkInDate') }}<`],
  [/>退房日期</g, `>{{ $t('booking.checkOutDate') }}<`],
  [/>选择</g, `>{{ $t('booking.selectRoomType') }}<`],
  [/>入住须知</g, `>{{ $t('booking.stayNotice') }}<`],
  [/'星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'/g, `t('booking.sun'), t('booking.mon'), t('booking.tue'), t('booking.wed'), t('booking.thu'), t('booking.fri'), t('booking.sat')`]
]);

// Confirm.vue
replaceInFile('Confirm.vue', [
  [/\( ¥ \{\{ roomPrice \}\}\/晚×\{\{ stayDays \}\} \)/g, `{{ $t('confirm.perNightMath2', { price: roomPrice, days: stayDays }) }}`],
  [/'当前用户'/g, `t('confirm.currentUser')`],
  [/年/g, `-`],
  [/月/g, `-`],
  [/日/g, ``]
]);

// Mine.vue
replaceInFile('Mine.vue', [
  [/>我的</g, `>{{ $t('mine.title2') }}<`]
]);

// OrderDetail.vue
replaceInFile('OrderDetail.vue', [
  [/>入住密码</g, `>{{ $t('orderDetail.doorPassword') }}<`],
  [/生效日期：/g, `{{ $t('orderDetail.effectiveDate') }}`],
  [/>14:00后</g, `>{{ $t('orderDetail.after14') }}<`],
  [/>12:00前</g, `>{{ $t('orderDetail.before12') }}<`],
  [/'当前用户'/g, `t('confirm.currentUser')`],
  [/>本人</g, `>{{ $t('orderDetail.myself') }}<`],
  [/'其他费用'/g, `t('orderDetail.otherFee')`],
  [/>合计</g, `>{{ $t('orderDetail.totalLabel') }}<`],
  [/>财务统一结算，无需个人垫付、报销</g, `>{{ $t('orderDetail.financeNote') }}<`],
  [/>取消预订：须于入住日前1天24:00前操作</g, `>{{ $t('orderDetail.cancelRule') }}<`],
  [/>提前退房：须于实际退房日前1天24:00前操作</g, `>{{ $t('orderDetail.checkoutRule') }}<`],
  [/>取消预订</g, `>{{ $t('orderDetail.cancelBtn') }}<`],
  [/>提前退房</g, `>{{ $t('orderDetail.earlyCheckoutBtn') }}<`],
  [/年/g, `-`],
  [/月/g, `-`],
  [/日/g, ``],
  [/'确定要取消预订吗？'/g, `t('orderDetail.cancelConfirmMsg')`],
  [/'已取消预订'/g, `t('orderDetail.cancelSuccess')`],
  [/'取消失败: '/g, `t('orderDetail.cancelFail')`],
  [/'确定要提前退房吗？'/g, `t('orderDetail.checkoutConfirmMsg')`],
  [/'已办理提前退房'/g, `t('orderDetail.checkoutSuccess')`],
  [/'退房失败: '/g, `t('orderDetail.checkoutFail')`],
  [/'订单提交成功'/g, `t('orderDetail.submitSuccess')`],
  [/（¥ \{\{ roomPrice \}\}\/晚 × \{\{ stayDays \}\}晚）/g, `{{ $t('confirm.perNightMath', { price: roomPrice, days: stayDays }) }}`],
  [/'待确认'/g, `t('records.status0')`],
  [/'预订中'/g, `t('records.status1')`],
  [/'入住中'/g, `t('records.status2')`], // I used 入住中 here
  [/'已退房'/g, `t('records.status3')`],
  [/'已取消'/g, `t('records.status4')`],
  [/'未知'/g, `t('records.unknown')`]
]);

// RoomSelect.vue
replaceInFile('RoomSelect.vue', [
  [/>选择/g, `>{{ $t('roomSelect.title2') }}`],
  [/已选/g, `{{ $t('roomSelect.selected') }}`],
  [/可选/g, `{{ $t('roomSelect.available') }}`],
  [/不可选/g, `{{ $t('roomSelect.unavailable') }}`],
  [/楼/g, `{{ $t('roomSelect.floor') }}`],
  [/>取消</g, `>{{ $t('roomSelect.cancel') }}<`],
  [/>确定</g, `>{{ $t('roomSelect.confirm') }}<`],
  [/'未知'/g, `t('roomSelect.unknown')`],
  [/'预订失败: '/g, `t('roomSelect.fail')`]
]);
