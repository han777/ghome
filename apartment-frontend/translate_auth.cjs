const fs = require('fs');
const path = require('path');
const file = path.join(__dirname, 'src/views/mobile/Auth.vue');
let content = fs.readFileSync(file, 'utf-8');

const dict = {
  '身份认证': `{{ $t('auth.identity') }}`,
  '您好，欢迎登录<br/>健适公寓预定系统': `<span v-html="$t('auth.welcome')"></span>`,
  '简体中文 🌐': `{{ $i18n.locale === 'zh' ? 'English 🌐' : '简体中文 🌐' }}`,
  '<label>手机号<span class="required">\\*</span></label>': `<label>{{ $t('auth.phone') }}<span class="required">*</span></label>`,
  'placeholder="请输入手机号"': `:placeholder="$t('auth.phonePlaceholder')"`,
  '<label>验证码<span class="required">\\*</span></label>': `<label>{{ $t('auth.code') }}<span class="required">*</span></label>`,
  'placeholder="请输入验证码"': `:placeholder="$t('auth.codePlaceholder')"`,
  '\\{\\{ countdown > 0 \\? \\`已发送 \\(\\$\\{countdown\\}s\\)\\` : \\'获取验证码\\' \\}\\}': `{{ countdown > 0 ? $t('auth.sent', { time: countdown }) : $t('auth.getCode') }}`,
  '\\{\\{ loading \\? \\'登录中...\\' : \\'登录\\' \\}\\}': `{{ loading ? $t('auth.logining') : $t('auth.login') }}`,
  '我已阅读并同意<a': `{{ $t('auth.agreePrefix') }}<a`,
  '个人信息保护政策</a>': `{{ $t('auth.policy') }}</a>`,
  '账号密码登录</a>': `{{ $t('auth.pwdLogin') }}</a>`,
  '个人信息保护及隐私政策提示': `{{ $t('auth.privacyTitle') }}`,
  '<p class="highlight">欢迎使用 \\[G-HOME订房程序\\]！</p>': `<p class="highlight">{{ $t('auth.privacyP1') }}</p>`,
  '<p>在您开始使用前，请您务必仔细阅读并理解《隐私政策》与《用户服务协议》。</p>': `<p>{{ $t('auth.privacyP2') }}</p>`,
  '<p>我们非常重视您的隐私保护，为了向您提供基础服务及优化体验，我们需要通过此弹窗告知您：</p>': `<p>{{ $t('auth.privacyP3') }}</p>`,
  '<strong>核心信息收集：</strong>': `<strong>{{ $t('auth.privacyH1') }}</strong>`,
  '<span>我们仅在您注册/登录、使用核心功能时，收集必要的信息（如手机号、设备标识符等）。</span>': `<span>{{ $t('auth.privacyC1') }}</span>`,
  '<strong>权限说明：</strong>': `<strong>{{ $t('auth.privacyH2') }}</strong>`,
  '<span>为实现特定功能，我们可能会申请相机、地理位置或存储权限，您可以随时在系统设置中关闭。</span>': `<span>{{ $t('auth.privacyC2') }}</span>`,
  '<strong>第三方共享：</strong>': `<strong>{{ $t('auth.privacyH3') }}</strong>`,
  '<span>未经您的同意，我们不会向第三方分享您的个人信息，除非法律法规另有规定。</span>': `<span>{{ $t('auth.privacyC3') }}</span>`,
  '<p class="footer-note">您可以点击下方按钮开始使用。如果您不同意上述协议，将无法使用本 App 的相关服务。</p>': `<p class="footer-note">{{ $t('auth.privacyP4') }}</p>`,
  '>退出<': `>{{ $t('auth.exit') }}<`,
  '>同意并进入<': `>{{ $t('auth.agreeAndEnter') }}<`,
  "'请输入正确的手机号'": `t('auth.invalidPhone')`,
  "'请输入手机号和验证码'": `t('auth.requirePhoneCode')`,
  "'登录失败'": `t('auth.loginFailed')`,
  "'发送失败'": `t('auth.sendFailed')`
};

for (const [k, v] of Object.entries(dict)) {
  content = content.replace(new RegExp(k, 'g'), v);
}

// Add toggle lang logic
content = content.replace(
  `<div class="lang-switch">{{ $i18n.locale === 'zh' ? 'English 🌐' : '简体中文 🌐' }}</div>`,
  `<div class="lang-switch" @click="$i18n.locale = $i18n.locale === 'zh' ? 'en' : 'zh'">{{ $i18n.locale === 'zh' ? 'English 🌐' : '简体中文 🌐' }}</div>`
);

// add useI18n
if (!content.includes('useI18n')) {
  content = content.replace(`import { ref } from 'vue';`, `import { ref } from 'vue';\nimport { useI18n } from 'vue-i18n';`);
  content = content.replace(`const router = useRouter();`, `const router = useRouter();\nconst { t } = useI18n();`);
}

fs.writeFileSync(file, content);
