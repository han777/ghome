const fs = require('fs');
const path = require('path');

const zhPath = path.join(__dirname, 'src', 'locales', 'zh.json');
const enPath = path.join(__dirname, 'src', 'locales', 'en.json');

const zhDict = JSON.parse(fs.readFileSync(zhPath, 'utf8'));
const enDict = JSON.parse(fs.readFileSync(enPath, 'utf8'));

zhDict.confirm = { ...zhDict.confirm, 
  title2: "预订确认",
  timeoutMsg: "请于 {deadlineStr} 前提交订单，超时则自动取消预定。剩余时间：{countdownStr}",
  expiredMsg: "订单已失效，请重新预订。",
  stayDate: "入住日期",
  leaveDate: "退房日期",
  totalAmount: "汇总金额",
  billingNote: "财务统一结算，无需个人垫付、报销",
  occupant: "入住人",
  addCompanion: "+ 新增同住人",
  myself: "本人",
  remove: "移除",
  companionName: "输入同住人姓名",
  confirmBtn: "确认",
  cancelBtn: "取消",
  notice: "入住须知",
  totalLabel: "合计",
  cancelOrder: "取消订单",
  cancelConfirm: "确定要取消预订吗？",
  bookSuccess: "预订成功！",
  submitFail: "提交失败: "
};

enDict.confirm = { ...enDict.confirm,
  title2: "Booking Confirmation",
  timeoutMsg: "Please submit the order before {deadlineStr}. Overtime will automatically cancel. Remaining: {countdownStr}",
  expiredMsg: "Order expired. Please rebook.",
  stayDate: "Check-in Date",
  leaveDate: "Check-out Date",
  totalAmount: "Total Amount",
  billingNote: "Unified financial settlement, no personal advance payment or reimbursement required",
  occupant: "Occupant",
  addCompanion: "+ Add Companion",
  myself: "Myself",
  remove: "Remove",
  companionName: "Enter companion's name",
  confirmBtn: "Confirm",
  cancelBtn: "Cancel",
  notice: "Stay Notice",
  totalLabel: "Total",
  cancelOrder: "Cancel Order",
  cancelConfirm: "Are you sure you want to cancel?",
  bookSuccess: "Booking successful!",
  submitFail: "Submit failed: "
};

fs.writeFileSync(zhPath, JSON.stringify(zhDict, null, 2), 'utf8');
fs.writeFileSync(enPath, JSON.stringify(enDict, null, 2), 'utf8');

const confirmPath = path.join(__dirname, 'src', 'views', 'mobile', 'Confirm.vue');
let confirmContent = fs.readFileSync(confirmPath, 'utf8');

const replaces = [
  ['预订确认', `{{ $t('confirm.title2') }}`],
  ['请于 {{ deadlineStr }} 前提交订单，超时则自动取消预定。剩余时间：{{ countdownStr }}', `{{ $t('confirm.timeoutMsg', { deadlineStr, countdownStr }) }}`],
  ['订单已失效，请重新预订。', `{{ $t('confirm.expiredMsg') }}`],
  ['>入住日期<', `>{{ $t('confirm.stayDate') }}<`],
  ['>退房日期<', `>{{ $t('confirm.leaveDate') }}<`],
  ['>汇总金额<', `>{{ $t('confirm.totalAmount') }}<`],
  ['财务统一结算，无需个人垫付、报销', `{{ $t('confirm.billingNote') }}`],
  ['>入住人<', `>{{ $t('confirm.occupant') }}<`],
  ['\\+ 新增同住人', `{{ $t('confirm.addCompanion') }}`],
  ['>本人<', `>{{ $t('confirm.myself') }}<`],
  ['>移除<', `>{{ $t('confirm.remove') }}<`],
  ['placeholder="输入同住人姓名"', `:placeholder="$t('confirm.companionName')"`],
  ['>确认<', `>{{ $t('confirm.confirmBtn') }}<`],
  ['>取消<', `>{{ $t('confirm.cancelBtn') }}<`],
  ['>入住须知<', `>{{ $t('confirm.notice') }}<`],
  ['>合计<', `>{{ $t('confirm.totalLabel') }}<`],
  ['>取消订单<', `>{{ $t('confirm.cancelOrder') }}<`],
  ["'确定要取消预订吗？'", `t('confirm.cancelConfirm')`],
  ["'预订成功！'", `t('confirm.bookSuccess')`],
  ["'提交失败: '", `t('confirm.submitFail')`]
];

for (const [k, v] of replaces) {
  confirmContent = confirmContent.replace(new RegExp(k, 'g'), v);
}

fs.writeFileSync(confirmPath, confirmContent, 'utf8');
