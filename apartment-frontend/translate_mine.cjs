const fs = require('fs');
const path = require('path');

const zhPath = path.join(__dirname, 'src', 'locales', 'zh.json');
const enPath = path.join(__dirname, 'src', 'locales', 'en.json');

const zhDict = JSON.parse(fs.readFileSync(zhPath, 'utf8'));
const enDict = JSON.parse(fs.readFileSync(enPath, 'utf8'));

zhDict.mine = { ...zhDict.mine,
  totalBookings: "总预定",
  pendingCheckIn: "待入住",
  completed: "已完成",
  nameLabel: "姓 名",
  phoneLabel: "手机号"
};

enDict.mine = { ...enDict.mine,
  totalBookings: "Total",
  pendingCheckIn: "Pending",
  completed: "Completed",
  nameLabel: "Name",
  phoneLabel: "Phone"
};

fs.writeFileSync(zhPath, JSON.stringify(zhDict, null, 2), 'utf8');
fs.writeFileSync(enPath, JSON.stringify(enDict, null, 2), 'utf8');

const minePath = path.join(__dirname, 'src', 'views', 'mobile', 'Mine.vue');
let mineContent = fs.readFileSync(minePath, 'utf8');

const replaces = [
  ['总预定', `{{ $t('mine.totalBookings') }}`],
  ['待入住', `{{ $t('mine.pendingCheckIn') }}`],
  ['已完成', `{{ $t('mine.completed') }}`],
  ['姓 名', `{{ $t('mine.nameLabel') }}`],
  ['手机号', `{{ $t('mine.phoneLabel') }}`]
];

for (const [k, v] of replaces) {
  mineContent = mineContent.replace(new RegExp(k, 'g'), v);
}

// Add lang switcher
if (!mineContent.includes('lang-switch-row')) {
  const langSwitchHtml = `
        <div class="divider"></div>
        <div class="info-row" @click="$i18n.locale = $i18n.locale === 'zh' ? 'en' : 'zh'">
          <div class="row-left">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="#999"><path d="M12.87,15.07L10.33,12.56L10.36,12.53C12.1,10.59 13.34,8.36 14.07,6H17V4H10V2H8V4H1V6H12.17C11.5,7.92 10.44,9.75 9,11.35C8.07,10.32 7.3,9.19 6.69,8H4.69C5.42,9.63 6.42,11.17 7.67,12.56L2.58,17.58L4,19L9,14L12.11,17.11L12.87,15.07M18.5,10H16.5L12,22H14L15.12,19H19.87L21,22H23L18.5,10M15.88,17L17.5,12.67L19.12,17H15.88Z" /></svg>
            <span class="row-label">{{ $t('mine.language') }}</span>
          </div>
          <span class="row-val" style="color:var(--mobile-primary)">{{ $i18n.locale === 'zh' ? '简体中文' : 'English' }}</span>
        </div>`;
  mineContent = mineContent.replace('</div>\n\n      <div class="logout-wrap">', `  ${langSwitchHtml}\n      </div>\n\n      <div class="logout-wrap">`);
}

fs.writeFileSync(minePath, mineContent, 'utf8');
