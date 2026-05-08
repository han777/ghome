const fs = require('fs');
const path = require('path');
const file = path.join(__dirname, 'src', 'views', 'mobile', 'OrderDetail.vue');
let content = fs.readFileSync(file, 'utf-8');

// Ensure useI18n is imported
if (!content.includes('useI18n')) {
  content = content.replace(/import \{ ref, onMounted, computed \} from 'vue';/, "import { useI18n } from 'vue-i18n';\nimport { ref, onMounted, computed } from 'vue';");
  content = content.replace(/const route = useRoute\(\);/, "const route = useRoute();\nconst { t } = useI18n();");
}

// Fix getDayOfWeek
content = content.replace(/const days = \['星期', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'\];/, "const days = [t('booking.sun'), t('booking.mon'), t('booking.tue'), t('booking.wed'), t('booking.thu'), t('booking.fri'), t('booking.sat')];");

// Fix buttons (since they span multiple lines, using a reliable regex)
content = content.replace(/>\s*提前退房\s*<\/button>/, ">\n          {{ $t('orderDetail.earlyCheckoutBtn') }}\n        </button>");
content = content.replace(/>\s*取消预订\s*<\/button>/, ">\n          {{ $t('orderDetail.cancelBtn') }}\n        </button>");

fs.writeFileSync(file, content, 'utf-8');
