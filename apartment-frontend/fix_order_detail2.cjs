const fs = require('fs');
const path = require('path');

const mineFile = path.join(__dirname, 'src', 'views', 'mobile', 'Mine.vue');
let mineContent = fs.readFileSync(mineFile, 'utf-8');
mineContent = mineContent.replace(/import \{ translateField \} from '\.\.\/\.\.\/utils\/lang';\n/, '');
mineContent = mineContent.replace(/const \{ t, locale \} = useI18n\(\);\n/, '');
mineContent = mineContent.replace(/import \{ useI18n \} from 'vue-i18n';\n/, '');
fs.writeFileSync(mineFile, mineContent, 'utf-8');

const orderDetailFile = path.join(__dirname, 'src', 'views', 'mobile', 'OrderDetail.vue');
let orderDetailContent = fs.readFileSync(orderDetailFile, 'utf-8');
orderDetailContent = orderDetailContent.replace(/import \{ translateField \} from '\.\.\/\.\.\/utils\/lang';\n/, '');
orderDetailContent = orderDetailContent.replace(/const \{ t, locale \} = useI18n\(\);/, 'const { t } = useI18n();');

// Fix getDayOfWeek
orderDetailContent = orderDetailContent.replace(/const days = \['星期', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'\];/, "const days = [t('booking.sun'), t('booking.mon'), t('booking.tue'), t('booking.wed'), t('booking.thu'), t('booking.fri'), t('booking.sat')];");

// Fix buttons (since they span multiple lines, using a reliable regex)
orderDetailContent = orderDetailContent.replace(/>\s*提前退房\s*<\/button>/, ">\n          {{ $t('orderDetail.earlyCheckoutBtn') }}\n        </button>");
orderDetailContent = orderDetailContent.replace(/>\s*取消预订\s*<\/button>/, ">\n          {{ $t('orderDetail.cancelBtn') }}\n        </button>");

fs.writeFileSync(orderDetailFile, orderDetailContent, 'utf-8');
