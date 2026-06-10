<template>
  <div class="notice-page">
    <header class="mobile-header">
      <div class="header-left" @click="goBack">
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M15.41,16.58L10.83,12L15.41,7.41L14,6L8,12L14,18L15.41,16.58Z" /></svg>
      </div>
      <div class="mobile-header-title">{{ $t('notice.title') }}</div>
      <div class="header-right"></div>
    </header>

    <div class="notice-content">
      <div class="notice-section">
        <h2 class="section-heading">{{ $t('notice.section1Title') }}</h2>
        <div class="notice-item">
          <span class="item-num">1.</span>
          <span>{{ $t('notice.s1.p1') }}</span>
        </div>
        <div class="notice-item">
          <span class="item-num">2.</span>
          <span>{{ $t('notice.s1.p2') }}</span>
        </div>
        <div class="notice-item">
          <span class="item-num">3.</span>
          <span>{{ $t('notice.s1.p3') }}</span>
        </div>
        <div class="notice-item">
          <span class="item-num">4.</span>
          <span>{{ $t('notice.s1.p4') }}</span>
          <div class="sub-items">
            <div class="sub-item">{{ $t('notice.s1.p4a') }}</div>
            <div class="sub-item">{{ $t('notice.s1.p4b') }}</div>
          </div>
        </div>
        <div class="notice-item">
          <span class="item-num">5.</span>
          <span>{{ $t('notice.s1.p5') }}</span>
          <div class="sub-items">
            <div class="sub-item">{{ $t('notice.s1.p5a') }}</div>
            <div class="sub-item">{{ $t('notice.s1.p5b') }}</div>
            <div class="sub-item">{{ $t('notice.s1.p5c') }}</div>
          </div>
        </div>
      </div>

      <div class="notice-section">
        <h2 class="section-heading">{{ $t('notice.section2Title') }}</h2>
        <div class="notice-item">
          <span class="item-num">6.</span>
          <span>{{ $t('notice.s2.p6') }}</span>
          <div class="sub-items">
            <div class="sub-item">{{ $t('notice.s2.p6a') }}</div>
            <div class="sub-item">{{ $t('notice.s2.p6b') }}</div>
            <div class="sub-item">{{ $t('notice.s2.p6c') }}</div>
          </div>
        </div>
        <div class="notice-item">
          <span class="item-num">7.</span>
          <span>{{ $t('notice.s2.p7') }}</span>
          <div class="sub-items">
            <div class="sub-item">{{ $t('notice.s2.p7a') }}</div>
            <div class="sub-item">{{ $t('notice.s2.p7b') }}</div>
          </div>
        </div>
        <div class="notice-item">
          <span class="item-num">8.</span>
          <span>{{ $t('notice.s2.p8') }}</span>
        </div>
      </div>

      <div class="notice-section">
        <h2 class="section-heading">{{ $t('notice.section3Title') }}</h2>
        <div class="notice-item">
          <span class="item-num">9.</span>
          <span>{{ $t('notice.s3.p9') }}</span>
          <div class="sub-items">
            <div class="sub-item">{{ $t('notice.s3.p9a') }}</div>
            <div class="sub-item">{{ $t('notice.s3.p9b') }}</div>
          </div>
        </div>
        <div class="notice-item">
          <span class="item-num">10.</span>
          <span>{{ $t('notice.s3.p10') }}</span>
          <div class="sub-items">
            <div class="sub-item">{{ $t('notice.s3.p10a') }}</div>
            <div class="sub-item">{{ $t('notice.s3.p10b') }}</div>
          </div>
        </div>
      </div>

      <div class="notice-section">
        <h2 class="section-heading">{{ $t('notice.section4Title') }}</h2>
        <div class="notice-item">
          <span class="item-num">11.</span>
          <span>{{ $t('notice.s4.p11') }}</span>
          <div class="sub-items">
            <div class="sub-item">{{ $t('notice.s4.p11a') }}</div>
            <div class="sub-item">{{ $t('notice.s4.p11b') }}</div>
          </div>
        </div>
        <div class="notice-item">
          <span class="item-num">12.</span>
          <span>{{ $t('notice.s4.p12') }}</span>
          <div class="sub-items">
            <div class="sub-item">{{ $t('notice.s4.p12a') }}</div>
            <div class="sub-item">{{ $t('notice.s4.p12b') }}</div>
          </div>
        </div>
        <div class="notice-item">
          <span class="item-num">13.</span>
          <span>{{ $t('notice.s4.p13') }}</span>
          <div class="sub-items">
            <div class="sub-item">{{ $t('notice.s4.p13a') }}</div>
            <div class="sub-item">{{ $t('notice.s4.p13b') }}</div>
          </div>
        </div>
        <div class="notice-item">
          <span class="item-num">14.</span>
          <span>{{ $t('notice.s4.p14') }}</span>
        </div>
        <div class="notice-item">
          <span class="item-num">15.</span>
          <span>{{ $t('notice.s4.p15') }}</span>
          <div class="sub-items">
            <div class="sub-item">{{ $t('notice.s4.p15a') }}</div>
            <div class="sub-item">{{ $t('notice.s4.p15b') }}</div>
          </div>
        </div>
      </div>

      <div class="notice-section">
        <h2 class="section-heading">{{ $t('notice.section5Title') }}</h2>
        <div class="notice-item">
          <span class="item-num">16.</span>
          <span>{{ $t('notice.s5.p16') }}</span>
        </div>
        <div class="notice-item">
          <span class="item-num">17.</span>
          <span>{{ $t('notice.s5.p17') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const goBack = () => {
  router.back();
};
</script>

<style scoped>
.notice-page {
  min-height: 100vh;
  background: #f8f8f8;
}

.notice-content {
  padding: 12px 16px 40px;
}

.notice-section {
  background: #fff;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.section-heading {
  font-size: 16px;
  font-weight: 700;
  color: var(--mobile-primary, #1976d2);
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8e8e8;
}

.notice-item {
  font-size: 14px;
  line-height: 1.7;
  color: #333;
  margin-bottom: 8px;
}

.item-num {
  font-weight: 600;
  margin-right: 4px;
}

.sub-items {
  margin-left: 24px;
  margin-top: 4px;
}

.sub-item {
  font-size: 13px;
  line-height: 1.7;
  color: #555;
  position: relative;
  padding-left: 12px;
}

.sub-item::before {
  content: '•';
  position: absolute;
  left: 0;
  color: #999;
}
</style>
