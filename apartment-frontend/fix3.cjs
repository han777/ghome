const fs = require('fs');
const path = require('path');
const file = path.join(__dirname, 'src', 'views', 'mobile', 'OrderDetail.vue');
let content = fs.readFileSync(file, 'utf-8');

content = content.replace(/import \{ translateField \} from '\.\.\/\.\.\/utils\/lang';\n/, '');
content = content.replace(/const \{ t, locale \} = useI18n\(\);/, 'const { t } = useI18n();');
content = content.replace(/const days = \['星期', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'\];/, "const days = [t('booking.sun'), t('booking.mon'), t('booking.tue'), t('booking.wed'), t('booking.thu'), t('booking.fri'), t('booking.sat')];");
content = content.replace(/>\s*提前退房\s*<\/button>/, ">\n          {{ $t('orderDetail.earlyCheckoutBtn') }}\n        </button>");
content = content.replace(/>\s*取消预订\s*<\/button>/, ">\n          {{ $t('orderDetail.cancelBtn') }}\n        </button>");

fs.writeFileSync(file, content, 'utf-8');
