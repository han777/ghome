const fs = require('fs');
const file = 'src/views/mobile/Mine.vue';
let content = fs.readFileSync(file, 'utf-8');

// Insert language row after phone value span closing div
const oldSnippet = `          <span class="row-val">{{ user.phone || '-' }}</span>\r\n        </div>\r\n      </div>`;
const newSnippet = `          <span class="row-val">{{ user.phone || '-' }}</span>\r\n        </div>\r\n        <div class="divider"></div>\r\n        <div class="info-row lang-row" @click="toggleLanguage">\r\n          <div class="row-left">\r\n            <svg viewBox="0 0 24 24" width="20" height="20" fill="#999"><path d="M12.87 15.07l-2.54-2.51.03-.03c1.74-1.94 2.98-4.17 3.71-6.53H17V4h-7V2H8v2H1v1.99h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5-5 3.11 3.11.76-2.04zM18.5 10h-2L12 22h2l1.12-3h4.75L21 22h2l-4.5-12zm-2.62 7l1.62-4.33L19.12 17h-3.24z"/></svg>\r\n            <span class="row-label">{{ $t('mine.language') }}</span>\r\n          </div>\r\n          <div class="lang-toggle">\r\n            <span class="lang-pill" :class="{ 'lang-active': locale === 'zh' }">中</span>\r\n            <span class="lang-pill" :class="{ 'lang-active': locale === 'en' }">EN</span>\r\n          </div>\r\n        </div>\r\n      </div>`;

if (content.includes(oldSnippet)) {
  content = content.replace(oldSnippet, newSnippet);
  console.log('Replaced with CRLF version');
} else {
  // Try LF version
  const oldLF = `          <span class="row-val">{{ user.phone || '-' }}</span>\n        </div>\n      </div>`;
  const newLF = `          <span class="row-val">{{ user.phone || '-' }}</span>\n        </div>\n        <div class="divider"></div>\n        <div class="info-row lang-row" @click="toggleLanguage">\n          <div class="row-left">\n            <svg viewBox="0 0 24 24" width="20" height="20" fill="#999"><path d="M12.87 15.07l-2.54-2.51.03-.03c1.74-1.94 2.98-4.17 3.71-6.53H17V4h-7V2H8v2H1v1.99h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5-5 3.11 3.11.76-2.04zM18.5 10h-2L12 22h2l1.12-3h4.75L21 22h2l-4.5-12zm-2.62 7l1.62-4.33L19.12 17h-3.24z"/></svg>\n            <span class="row-label">{{ $t('mine.language') }}</span>\n          </div>\n          <div class="lang-toggle">\n            <span class="lang-pill" :class="{ 'lang-active': locale === 'zh' }">中</span>\n            <span class="lang-pill" :class="{ 'lang-active': locale === 'en' }">EN</span>\n          </div>\n        </div>\n      </div>`;
  if (content.includes(oldLF)) {
    content = content.replace(oldLF, newLF);
    console.log('Replaced with LF version');
  } else {
    console.log('NOT FOUND. Snippet chars:');
    const idx = content.indexOf("user.phone || '-'");
    console.log(JSON.stringify(content.substring(idx, idx + 80)));
  }
}

fs.writeFileSync(file, content, 'utf-8');
