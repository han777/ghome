const fs = require('fs');
const path = require('path');

const viewsDir = path.join(__dirname, 'src', 'views', 'mobile');

const dictionary = {
  // Booking.vue
  '公寓预定': `{{ $t('booking.title') }}`,
  '入住时间': `{{ $t('booking.checkIn') }}`,
  '退房时间': `{{ $t('booking.checkOut') }}`,
  '房型': `{{ $t('booking.roomType') }}`,
  '查询可用房间': `{{ $t('booking.search') }}`,
  '加载中...': `{{ $t('booking.loading') }}`,
  "'请选择日期范围'": `t('booking.selectDate')`,
  "'请选择'": `t('booking.pleaseSelect')`,
  
  // Confirm.vue
  '确认订单': `{{ $t('confirm.title') }}`,
  '客房信息': `{{ $t('confirm.roomInfo') }}`,
  '房间号': `{{ $t('confirm.roomNo') }}`,
  '入住人信息': `{{ $t('confirm.occupantInfo') }}`,
  '与预订人相同': `{{ $t('confirm.sameAsBooker') }}`,
  '<label>姓名</label>': `<label>{{ $t('confirm.name') }}</label>`,
  'placeholder="请输入姓名"': `:placeholder="$t('confirm.namePlaceholder')"`,
  '<label>手机号</label>': `<label>{{ $t('confirm.phone') }}</label>`,
  'placeholder="请输入手机号"': `:placeholder="$t('confirm.phonePlaceholder')"`,
  '身份证号': `{{ $t('confirm.idCard') }}`,
  'placeholder="请输入身份证号"': `:placeholder="$t('confirm.idCardPlaceholder')"`,
  '额外服务': `{{ $t('confirm.services') }}`,
  '数量': `{{ $t('confirm.quantity') }}`,
  '备注要求': `{{ $t('confirm.remark') }}`,
  'placeholder="如有特殊需求请填写"': `:placeholder="$t('confirm.remarkPlaceholder')"`,
  '费用明细': `{{ $t('confirm.payment') }}`,
  '房费': `{{ $t('confirm.roomCharge') }}`,
  '服务费': `{{ $t('confirm.serviceCharge') }}`,
  '总计': `{{ $t('confirm.total') }}`,
  '>提交订单<': `>{{ $t('confirm.submit') }}<`,
  '>提交中...<': `>{{ $t('confirm.submitting') }}<`,
  "'请填写完整的入住人信息'": `t('confirm.fillInfo')`,
  "'提交成功！'": `t('confirm.submitSuccess')`,
  "'提交失败！'": `t('confirm.submitFailed')`,

  // Records.vue
  '我的行程': `{{ $t('records.title') }}`,
  '当前行程': `{{ $t('records.current') }}`,
  '历史行程': `{{ $t('records.history') }}`,
  '暂无行程': `{{ $t('records.noRecords') }}`,
  '查看详情': `{{ $t('records.viewDetail') }}`,

  // OrderDetail.vue
  '订单详情': `{{ $t('orderDetail.title') }}`,
  '状态': `{{ $t('orderDetail.status') }}`,
  '订单信息': `{{ $t('orderDetail.orderInfo') }}`,
  '订单编号': `{{ $t('orderDetail.orderNo') }}`,
  '下单时间': `{{ $t('orderDetail.orderTime') }}`,
  '房间信息': `{{ $t('orderDetail.roomInfo') }}`,
  '房间': `{{ $t('orderDetail.roomNo') }}`,
  '>姓名<': `>{{ $t('orderDetail.name') }}<`,
  '>电话<': `>{{ $t('orderDetail.phone') }}<`,
  '增值服务': `{{ $t('orderDetail.services') }}`,
  '>费用<': `>{{ $t('orderDetail.cost') }}<`,
  
  // Mine.vue
  '个人中心': `{{ $t('mine.title') }}`,
  '登录/注册': `{{ $t('mine.login') }}`,
  '欢迎您': `{{ $t('mine.welcome') }}`,
  '我的订单': `{{ $t('mine.myOrders') }}`,
  '全部订单': `{{ $t('mine.allOrders') }}`,
  '语言设置': `{{ $t('mine.language') }}`,
  '退出登录': `{{ $t('mine.logout') }}`,

  // RoomSelect.vue
  '选择房间': `{{ $t('roomSelect.title') }}`,
  '>入住<': `>{{ $t('roomSelect.checkIn') }}<`,
  '>退房<': `>{{ $t('roomSelect.checkOut') }}<`,
  '>预订<': `>{{ $t('roomSelect.book') }}<`,
  '暂无可用房间': `{{ $t('roomSelect.noRooms') }}`,
  
  // Common
  '朝南': `{{ $t('booking.south') }}`,
  '朝北': `{{ $t('booking.north') }}`
};

function translateFile(filePath) {
  let content = fs.readFileSync(filePath, 'utf-8');
  let original = content;

  for (const [key, value] of Object.entries(dictionary)) {
    const regex = new RegExp(key, 'g');
    content = content.replace(regex, value);
  }

  // Inject translateField usage for roomTypeName and dictName, and nameIntlJson
  // e.g. room.roomTypeName -> translateField(room.roomTypeName, $i18n.locale)
  // We need to import it first
  if (!content.includes('useI18n') && content.includes('<script setup')) {
    content = content.replace(/<script setup(.*?)>/, `<script setup$1>\nimport { useI18n } from 'vue-i18n';\nimport { translateField } from '../../utils/lang';`);
    if (content.includes('const router = useRouter()')) {
        content = content.replace('const router = useRouter();', 'const router = useRouter();\nconst { t, locale } = useI18n();');
    } else {
        content = content.replace(/import \{.*?\} from 'vue';/, `$& \nconst { t, locale } = useI18n();`);
    }
  }

  // Find places where {{ type.name }} or {{ room.roomTypeName }} is used and replace with {{ translateField(...) }}
  content = content.replace(/\{\{\s*type\.name\s*\}\}/g, `{{ translateField(type.nameIntlJson || type.name, locale) }}`);
  content = content.replace(/\{\{\s*room\.roomTypeName\s*\}\}/g, `{{ translateField(room.roomTypeName, locale) }}`);
  content = content.replace(/\{\{\s*roomInfo\.roomTypeName\s*\}\}/g, `{{ translateField(roomInfo.roomTypeName, locale) }}`);
  content = content.replace(/\{\{\s*item\.productName\s*\}\}/g, `{{ translateField(item.productName, locale) }}`);

  if (content !== original) {
    fs.writeFileSync(filePath, content, 'utf-8');
    console.log(`Translated ${path.basename(filePath)}`);
  }
}

const files = ['Booking.vue', 'Confirm.vue', 'Mine.vue', 'OrderDetail.vue', 'Records.vue', 'RoomSelect.vue'];

for (const file of files) {
  translateFile(path.join(viewsDir, file));
}
