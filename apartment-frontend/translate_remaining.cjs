const fs = require('fs');
const path = require('path');

const zhPath = path.join(__dirname, 'src', 'locales', 'zh.json');
const enPath = path.join(__dirname, 'src', 'locales', 'en.json');

const zhDict = JSON.parse(fs.readFileSync(zhPath, 'utf8'));
const enDict = JSON.parse(fs.readFileSync(enPath, 'utf8'));

// Update Booking
zhDict.booking = { ...zhDict.booking,
  title2: "公寓预订",
  nights: "共{n}晚",
  pics: "{n}张",
  perNight: "/晚",
  bookNow: "立即预订",
  ghome: "G-HOME公寓"
};

enDict.booking = { ...enDict.booking,
  title2: "Apartment Booking",
  nights: "{n} Nights",
  pics: "{n} Pics",
  perNight: "/Night",
  bookNow: "Book Now",
  ghome: "G-HOME Apartment"
};

// Update Records
zhDict.records = { ...zhDict.records,
  title2: "预订记录",
  orderNo: "单号：",
  checkIn: "入住：",
  checkOut: "退房：",
  roomNo: "房号：",
  noRecords2: "暂无预订记录",
  unknown: "未知",
  status0: "待确认",
  status1: "预订中",
  status2: "已入住",
  status3: "已退房",
  status4: "已取消"
};

enDict.records = { ...enDict.records,
  title2: "Booking Records",
  orderNo: "Order No: ",
  checkIn: "Check-in: ",
  checkOut: "Check-out: ",
  roomNo: "Room No: ",
  noRecords2: "No booking records",
  unknown: "Unknown",
  status0: "Pending",
  status1: "Booking",
  status2: "Active",
  status3: "Completed",
  status4: "Cancelled"
};

fs.writeFileSync(zhPath, JSON.stringify(zhDict, null, 2), 'utf8');
fs.writeFileSync(enPath, JSON.stringify(enDict, null, 2), 'utf8');

const viewsDir = path.join(__dirname, 'src', 'views', 'mobile');

// Fix Booking.vue
const bookingPath = path.join(viewsDir, 'Booking.vue');
let bookingContent = fs.readFileSync(bookingPath, 'utf8');
bookingContent = bookingContent.replace(/>G-HOME公寓</g, `>{{ $t('booking.ghome') }}<`);
bookingContent = bookingContent.replace(/>公寓预订</g, `>{{ $t('booking.title2') }}<`);
bookingContent = bookingContent.replace(/共\{\{\s*stayDays\s*\}\}晚/g, `{{ $t('booking.nights', { n: stayDays }) }}`);
bookingContent = bookingContent.replace(/\{\{\s*type\.images\.length\s*\}\}张/g, `{{ $t('booking.pics', { n: type.images.length }) }}`);
bookingContent = bookingContent.replace(/>\/晚</g, `>{{ $t('booking.perNight') }}<`);
bookingContent = bookingContent.replace(/>\s*立即预订\s*</g, `>\n          {{ $t('booking.bookNow') }}\n        <`);
fs.writeFileSync(bookingPath, bookingContent, 'utf8');

// Fix Records.vue
const recordsPath = path.join(viewsDir, 'Records.vue');
let recordsContent = fs.readFileSync(recordsPath, 'utf8');
recordsContent = recordsContent.replace(/>预订记录</g, `>{{ $t('records.title2') }}<`);
recordsContent = recordsContent.replace(/单号：/g, `{{ $t('records.orderNo') }}`);
recordsContent = recordsContent.replace(/入住：/g, `{{ $t('records.checkIn') }}`);
recordsContent = recordsContent.replace(/退房：/g, `{{ $t('records.checkOut') }}`);
recordsContent = recordsContent.replace(/房号：/g, `{{ $t('records.roomNo') }}`);
recordsContent = recordsContent.replace(/>暂无预订记录</g, `>{{ $t('records.noRecords2') }}<`);
recordsContent = recordsContent.replace(/'未知'/g, `t('records.unknown')`);
recordsContent = recordsContent.replace(/'待确认'/g, `t('records.status0')`);
recordsContent = recordsContent.replace(/'预订中'/g, `t('records.status1')`);
recordsContent = recordsContent.replace(/'已入住'/g, `t('records.status2')`);
recordsContent = recordsContent.replace(/'已退房'/g, `t('records.status3')`);
recordsContent = recordsContent.replace(/'已取消'/g, `t('records.status4')`);

// Records.vue needs `t` from `useI18n` for the JS part
if (!recordsContent.includes('useI18n')) {
  recordsContent = recordsContent.replace(/import \{ ref, onMounted \} from 'vue';/, "import { useI18n } from 'vue-i18n';\nimport { ref, onMounted } from 'vue';");
  recordsContent = recordsContent.replace(/const router = useRouter\(\);/, "const router = useRouter();\nconst { t } = useI18n();");
}
fs.writeFileSync(recordsPath, recordsContent, 'utf8');
