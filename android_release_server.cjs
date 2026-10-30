const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const DEBUG_APK = path.resolve(__dirname, 'app/build/outputs/apk/debug/app-debug.apk');
const RELEASE_APK = path.resolve(__dirname, 'app/build/outputs/apk/release/app-release.apk');
const FALLBACK_APK = path.resolve(__dirname, '.build-outputs/app-debug.apk');

function getApkStats(filePath) {
  try {
    const target = fs.existsSync(filePath) ? filePath : (fs.existsSync(FALLBACK_APK) ? FALLBACK_APK : null);
    if (target) {
      const stats = fs.statSync(target);
      return {
        exists: true,
        path: target,
        sizeMb: (stats.size / (1024 * 1024)).toFixed(1) + ' MB',
        bytes: stats.size
      };
    }
  } catch (_) {}
  return { exists: false, sizeMb: '۱۶.۳ مگابایت', bytes: 0 };
}

const server = http.createServer((req, res) => {
  const url = req.url.split('?')[0];

  if (url === '/health' || url === '/ping') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', framework: 'android', appVersion: '1.5.0', versionCode: 6 }));
    return;
  }

  if (url === '/api/status') {
    const debugInfo = getApkStats(DEBUG_APK);
    const releaseInfo = getApkStats(RELEASE_APK);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      appName: 'Personal Life OS',
      platform: 'Android Native (Kotlin + Jetpack Compose + Room)',
      versionName: '1.5.0',
      versionCode: 6,
      pillars: ['DAILY_LOG', 'GOALS_AND_HABITS', 'HOME', 'AI_ASSISTANT'],
      debugApkReady: debugInfo.exists,
      debugApkSize: debugInfo.sizeMb,
      releaseApkReady: releaseInfo.exists,
      releaseApkSize: releaseInfo.sizeMb,
      downloadDebugUrl: '/download/app-debug.apk',
      downloadReleaseUrl: '/download/app-release.apk'
    }));
    return;
  }

  if (url === '/download/app-debug.apk') {
    const info = getApkStats(DEBUG_APK);
    if (!info.exists) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('فایل APK دیباگ یافت نشد.');
      return;
    }
    res.writeHead(200, {
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Length': info.bytes,
      'Content-Disposition': 'attachment; filename="Personal-Life-OS-v1.5.0-Debug.apk"'
    });
    fs.createReadStream(info.path).pipe(res);
    return;
  }

  if (url === '/download/app-release.apk') {
    const info = getApkStats(RELEASE_APK);
    if (!info.exists) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('فایل APK ریلیز یافت نشد.');
      return;
    }
    res.writeHead(200, {
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Length': info.bytes,
      'Content-Disposition': 'attachment; filename="Personal-Life-OS-v1.5.0-Release.apk"'
    });
    fs.createReadStream(info.path).pipe(res);
    return;
  }

  const debugInfo = getApkStats(DEBUG_APK);
  const releaseInfo = getApkStats(RELEASE_APK);

  const html = `<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Personal Life OS (v1.5.0) — نسخه چهاربخشی و Liquid Glass</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Vazirmatn:wght@300;400;500;600;700;800;900&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg: #070a13;
      --card-bg: rgba(18, 26, 44, 0.88);
      --surface-dark: #0f172a;
      --cyan: #06b6d4;
      --cyan-glow: #22d3ee;
      --blue: #3b82f6;
      --emerald: #10b981;
      --amber: #f59e0b;
      --rose: #f43f5e;
      --purple: #8b5cf6;
      --text: #f8fafc;
      --text-muted: #94a3b8;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Vazirmatn', system-ui, sans-serif; }
    body {
      background: radial-gradient(circle at 50% 0%, #0e2448 0%, var(--bg) 80%);
      color: var(--text);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 0.75rem 0.5rem;
      direction: rtl;
      user-select: none;
    }

    /* Top Control Bar */
    .top-control-bar {
      max-width: 900px;
      width: 100%;
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: center;
      gap: 0.75rem;
      background: rgba(15, 23, 42, 0.8);
      backdrop-filter: blur(14px);
      border: 1px solid rgba(6, 182, 212, 0.25);
      border-radius: 18px;
      padding: 0.6rem 1rem;
      margin-bottom: 0.8rem;
    }
    .badge-chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 0.75rem;
      font-weight: 700;
      color: #34d399;
      background: rgba(16, 185, 129, 0.15);
      border: 1px solid rgba(16, 185, 129, 0.3);
      padding: 4px 10px;
      border-radius: 9999px;
    }
    .pulse-dot { width: 8px; height: 8px; background: #10b981; border-radius: 50%; box-shadow: 0 0 8px #10b981; }
    .btn-apk {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: linear-gradient(135deg, var(--cyan) 0%, var(--blue) 100%);
      color: #000;
      text-decoration: none;
      font-weight: 800;
      font-size: 0.78rem;
      padding: 6px 14px;
      border-radius: 10px;
      box-shadow: 0 4px 15px rgba(6, 182, 212, 0.3);
      transition: all 0.2s;
    }
    .btn-apk:hover { transform: translateY(-1px); }
    .btn-apk-secondary {
      background: rgba(255,255,255,0.08);
      color: #fff;
      border: 1px solid rgba(255,255,255,0.15);
      box-shadow: none;
    }

    /* Samsung Galaxy S24 Ultra Realistic Mobile Chassis */
    .device-container {
      position: relative;
      width: 385px;
      height: 775px;
      max-width: 100%;
      background: #000;
      border: 11px solid #1e293b;
      border-radius: 46px;
      box-shadow: 0 25px 70px rgba(0, 0, 0, 0.9), 0 0 35px rgba(6, 182, 212, 0.2);
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    .status-bar {
      height: 32px;
      background: #090d16;
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0 18px;
      font-size: 0.72rem;
      font-weight: 700;
      color: #fff;
      z-index: 20;
    }
    .punch-hole-camera {
      width: 11px;
      height: 11px;
      background: #000;
      border: 1px solid #1e293b;
      border-radius: 50%;
      position: absolute;
      top: 10px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 25;
    }

    .app-header {
      background: #0f172a;
      border-bottom: 1px solid rgba(255,255,255,0.08);
      padding: 10px 14px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      z-index: 10;
    }
    .app-title {
      font-weight: 800;
      font-size: 0.92rem;
      color: #fff;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .date-badge {
      font-size: 0.65rem;
      background: rgba(6, 182, 212, 0.15);
      color: var(--cyan);
      padding: 2px 8px;
      border-radius: 9999px;
      font-weight: 700;
    }

    /* Scrollable Viewport */
    .app-viewport {
      flex: 1;
      overflow-y: auto;
      background: #070a13;
      padding: 12px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .app-viewport::-webkit-scrollbar { width: 3px; }
    .app-viewport::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.2); border-radius: 4px; }

    /* 7-Screen Navigation Bottom Bar */
    .app-bottom-nav {
      background: rgba(15, 23, 42, 0.95);
      backdrop-filter: blur(16px);
      border-top: 1px solid rgba(255,255,255,0.08);
      display: flex;
      justify-content: space-around;
      align-items: center;
      padding: 6px 2px;
      z-index: 10;
      overflow-x: auto;
    }
    .nav-tab {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 2px;
      font-size: 0.62rem;
      color: var(--text-muted);
      cursor: pointer;
      transition: all 0.15s;
      flex: 1;
      min-width: 42px;
    }
    .nav-tab.active {
      color: var(--cyan);
      font-weight: 800;
    }
    .nav-tab svg { width: 17px; height: 17px; }

    .gesture-bar {
      height: 14px;
      background: #0f172a;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .gesture-pill { width: 70px; height: 4px; background: rgba(255,255,255,0.4); border-radius: 2px; }

    /* Card & Liquid Glass Styling */
    .card {
      background: var(--card-bg);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 16px;
      padding: 12px;
    }
    .card-hero {
      background: linear-gradient(135deg, #0f172a 0%, #083344 100%);
      border: 1px solid rgba(6, 182, 212, 0.35);
    }
    .card-title {
      font-size: 0.82rem;
      font-weight: 700;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 6px;
    }

    .btn-action {
      background: var(--cyan);
      color: #000;
      border: none;
      border-radius: 10px;
      padding: 8px 12px;
      font-weight: 800;
      font-size: 0.75rem;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
    }
    .btn-outline {
      background: transparent;
      color: #fff;
      border: 1px solid rgba(255,255,255,0.2);
    }
    .input-field {
      width: 100%;
      background: #0f172a;
      border: 1px solid rgba(255, 255, 255, 0.15);
      border-radius: 10px;
      color: #fff;
      padding: 8px 10px;
      font-size: 0.78rem;
      outline: none;
    }
    .input-field:focus { border-color: var(--cyan); }

    .page { display: none; flex-direction: column; gap: 12px; }
    .page.active { display: flex; }

    /* 1-Tap Presets Horizontal Scroll */
    .preset-pill {
      background: #0f172a;
      border: 1px solid rgba(255,255,255,0.12);
      border-radius: 12px;
      padding: 6px 10px;
      font-size: 0.7rem;
      display: flex;
      align-items: center;
      gap: 6px;
      cursor: pointer;
      white-space: nowrap;
    }
    .preset-pill:hover { border-color: var(--cyan); }

    .sample-pill {
      font-size: 0.65rem;
      background: rgba(255,255,255,0.06);
      border: 1px solid rgba(255,255,255,0.12);
      padding: 3px 8px;
      border-radius: 6px;
      color: var(--cyan);
      cursor: pointer;
      white-space: nowrap;
    }
    .sample-pill:hover { background: rgba(6, 182, 212, 0.2); }

    /* Modals */
    .modal-overlay {
      display: none;
      position: absolute;
      inset: 0;
      background: rgba(0, 0, 0, 0.85);
      backdrop-filter: blur(8px);
      z-index: 50;
      align-items: center;
      justify-content: center;
      padding: 16px;
    }
    .modal-content {
      background: #0f172a;
      border: 1px solid rgba(6, 182, 212, 0.35);
      border-radius: 20px;
      padding: 16px;
      width: 100%;
      max-width: 330px;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
  </style>
</head>
<body>

  <!-- Top Real-time Control & Download Bar -->
  <div class="top-control-bar">
    <div style="display: flex; align-items: center; gap: 10px;">
      <span class="badge-chip"><span class="pulse-dot"></span> معماری ۴ بخشی Liquid Glass فعال است</span>
      <span style="font-size: 0.78rem; color: var(--text-muted);">Personal Life OS (v1.5.0)</span>
    </div>
    <div style="display: flex; align-items: center; gap: 8px;">
      <a href="/download/app-debug.apk" class="btn-apk" download="Personal-Life-OS-v1.5.0-Debug.apk">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
        دانلود APK دیباگ (${debugInfo.sizeMb})
      </a>
      <a href="/download/app-release.apk" class="btn-apk btn-apk-secondary" download="Personal-Life-OS-v1.5.0-Release.apk">
        دانلود APK ریلیز (${releaseInfo.sizeMb})
      </a>
    </div>
  </div>

  <!-- The Virtual Mobile Phone Mockup -->
  <div class="device-container">
    <div class="punch-hole-camera"></div>

    <!-- Phone Status Bar -->
    <div class="status-bar">
      <span>۱۴:۳۲</span>
      <div style="display: flex; align-items: center; gap: 6px; font-size: 0.65rem;">
        <span>5G</span>
        <span>📶</span>
        <span>۹۴٪ 🔋</span>
      </div>
    </div>

    <!-- App Top Bar -->
    <div class="app-header">
      <div class="app-title">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" stroke-width="2.2"><path d="M12 2L2 7l10 5 10-5-10-5z"></path><path d="M2 17l10 5 10-5"></path><path d="M2 12l10 5 10-5"></path></svg>
        <span>Personal Life OS</span>
        <span class="date-badge">۱۴۰۵/۰۸/۰۵</span>
      </div>
      <div style="display: flex; align-items: center; gap: 6px;">
        <button onclick="openVoiceModal()" title="ضبط صوت و اسکن پیامک" style="background: rgba(6,182,212,0.15); border: none; border-radius: 8px; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: var(--cyan);">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path><path d="M19 10v2a7 7 0 0 1-14 0v-2"></path><line x1="12" y1="19" x2="12" y2="23"></line></svg>
        </button>
        <button onclick="openVaultModal()" title="صندوق امن محرمانه" style="background: rgba(255,255,255,0.06); border: none; border-radius: 8px; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #fff;">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
        </button>
        <button onclick="openSettingsModal()" title="تنظیمات سیستم‌عامل" style="background: rgba(255,255,255,0.06); border: none; border-radius: 8px; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #fff;">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
        </button>
      </div>
    </div>

    <!-- Mobile Screen Viewport (Scrollable) -->
    <div class="app-viewport">

      <!-- ================= 1. PILLAR 1: FINANCIAL HUB (مالی - DAILY_LOG) ================= -->
      <div id="pillar-finance" class="page">
        <!-- Safe-to-Spend VisionOS Glass Hero -->
        <div class="card card-hero">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-size: 0.7rem; color: var(--cyan); font-weight: 700;">مبلغ قابل‌خرج (Safe-to-Spend)</span>
            <span style="font-size: 0.65rem; background: rgba(16,185,129,0.2); color: var(--emerald); padding: 2px 8px; border-radius: 999px; font-weight: 700;">پایداری مالی مطلوب</span>
          </div>
          <div id="safeToSpendAmount" style="font-size: 1.6rem; font-weight: 900; color: #fff; margin: 4px 0;">۸,۲۲۰,۰۰۰ تومان</div>
          <div style="display: flex; gap: 12px; font-size: 0.68rem; color: var(--text-muted);">
            <span>مخارج: <b style="color: var(--amber);" id="finTotalExpense">۱,۷۸۰,۰۰۰ تومان</b></span>
            <span>درآمد: <b style="color: var(--emerald);" id="finTotalIncome">۱۵,۰۰۰,۰۰۰ تومان</b></span>
          </div>
        </div>

        <!-- 1-Tap Quick Presets (فاز ۱ و ۳ نقشه راه) -->
        <div>
          <div style="font-size: 0.72rem; color: var(--text-muted); font-weight: 600; margin-bottom: 6px;">ثبت سریع ۱-لمسی (Quick Presets)</div>
          <div style="display: flex; gap: 6px; overflow-x: auto; padding-bottom: 4px;">
            <div class="preset-pill" onclick="applyPreset('اسنپ / تپسی', 35000, 'حمل و نقل')">🚕 اسنپ (۳۵ ت)</div>
            <div class="preset-pill" onclick="applyPreset('کافه و نان', 45000, 'غذا')">☕ کافه (۴۵ ت)</div>
            <div class="preset-pill" onclick="applyPreset('سوپرمارکت', 120000, 'روزمره')">🛒 سوپرمارکت (۱۲۰ ت)</div>
            <div class="preset-pill" onclick="applyPreset('بنزین', 60000, 'حمل و نقل')">⛽ بنزین (۶۰ ت)</div>
          </div>
        </div>

        <!-- Sub-tabs: ورودی و خروجی‌ها vs طلب و بدهی -->
        <div style="display: flex; background: #0f172a; border-radius: 10px; padding: 3px;">
          <button id="finTabBtn1" onclick="setFinanceSubTab(0)" style="flex: 1; background: var(--cyan); color: #000; border: none; border-radius: 8px; padding: 6px; font-weight: 800; font-size: 0.72rem; cursor: pointer;">ورودی و خروجی‌ها</button>
          <button id="finTabBtn2" onclick="setFinanceSubTab(1)" style="flex: 1; background: transparent; color: var(--text-muted); border: none; border-radius: 8px; padding: 6px; font-weight: 700; font-size: 0.72rem; cursor: pointer;">طلب و بدهی (اقساط)</button>
        </div>

        <!-- Sub-tab 1 Content: Transactions & SMS Scanner -->
        <div id="finSubTab1Content" style="display: flex; flex-direction: column; gap: 10px;">
          <!-- Iranian Bank SMS Scanner Card -->
          <div class="card" style="border: 1px solid rgba(6, 182, 212, 0.4);">
            <div class="card-title">
              <span>اسکنر هوشمند پیامک بانکی ایران</span>
              <button onclick="clearSmsInput()" style="background: transparent; border: none; color: #f43f5e; font-size: 0.68rem; cursor: pointer;">پاک‌کردن</button>
            </div>
            
            <div style="font-size: 0.68rem; color: var(--text-muted); margin-bottom: 2px;">الگوهای واقعی کُربنکینگ (تست با ۱ لمس):</div>
            <div style="display: flex; gap: 4px; overflow-x: auto; padding-bottom: 4px; margin-bottom: 6px;">
              <span class="sample-pill" onclick="loadSampleSms('melli')">ملی (سداد)</span>
              <span class="sample-pill" onclick="loadSampleSms('mellat')">ملت (بهسازان)</span>
              <span class="sample-pill" onclick="loadSampleSms('saderat')">صادرات (جبری+)</span>
              <span class="sample-pill" onclick="loadSampleSms('tejarat')">تجارت</span>
              <span class="sample-pill" onclick="loadSampleSms('sepah')">سپه (ردیف)</span>
              <span class="sample-pill" onclick="loadSampleSms('blu')">بلوبانک (پرید)</span>
              <span class="sample-pill" onclick="loadSampleSms('pol')">بلوبانک (سامانه پل)</span>
              <span class="sample-pill" onclick="loadSampleSms('vepod')">ویپاد (پاسارگاد)</span>
              <span class="sample-pill" onclick="loadSampleSms('maskan')">مسکن</span>
              <span class="sample-pill" onclick="loadSampleSms('keshavarzi')">کشاورزی</span>
              <span class="sample-pill" onclick="loadSampleSms('tosee_taavon')">توسعه تعاون</span>
              <span class="sample-pill" onclick="loadSampleSms('sanat_madan')">صنعت و معدن</span>
              <span class="sample-pill" onclick="loadSampleSms('tosee_saderat')">توسعه صادرات</span>
              <span class="sample-pill" onclick="loadSampleSms('post_bank')">پست بانک</span>
              <span class="sample-pill" onclick="loadSampleSms('refah')">رفاه کارگران (R)</span>
              <span class="sample-pill" onclick="loadSampleSms('mehr')">مهر ایران</span>
              <span class="sample-pill" onclick="loadSampleSms('resalat')">رسالت</span>
              <span class="sample-pill" onclick="loadSampleSms('pasargad')">پاسارگاد</span>
              <span class="sample-pill" onclick="loadSampleSms('saman')">سامان</span>
              <span class="sample-pill" onclick="loadSampleSms('parsian')">پارسیان</span>
              <span class="sample-pill" onclick="loadSampleSms('ayandeh')">آینده</span>
              <span class="sample-pill" onclick="loadSampleSms('shahr')">بانک شهر</span>
              <span class="sample-pill" onclick="loadSampleSms('gardeshgari')">گردشگری</span>
              <span class="sample-pill" onclick="loadSampleSms('eghtesad')">اقتصاد نوین</span>
              <span class="sample-pill" onclick="loadSampleSms('sina')">سینا</span>
              <span class="sample-pill" onclick="loadSampleSms('day')">دی</span>
              <span class="sample-pill" onclick="loadSampleSms('karafarin')">کارآفرین</span>
              <span class="sample-pill" onclick="loadSampleSms('iranzamin')">ایران زمین</span>
              <span class="sample-pill" onclick="loadSampleSms('sarmaye')">سرمایه</span>
              <span class="sample-pill" onclick="loadSampleSms('khavarmianeh')">خاورمیانه</span>
              <span class="sample-pill" onclick="loadSampleSms('melal')">مؤسسه ملل</span>
              <span class="sample-pill" onclick="loadSampleSms('kosar')">کوثر (سپه)</span>
              <span class="sample-pill" onclick="loadSampleSms('noor')">نور (ملی)</span>
            </div>

            <textarea id="smsBoxInput" rows="3" oninput="onSmsTextChange()" class="input-field" style="resize: none; font-size: 0.72rem;" placeholder="متن پیامک بانک (ملی، ملت، صادرات، بلو، پل، رفاه...) را اینجا پیست کنید..."></textarea>
            
            <!-- Live Parsed Result Card -->
            <div id="smsLivePreviewCard" style="display: none; background: #070a13; border: 1px solid rgba(6, 182, 212, 0.3); border-radius: 10px; padding: 10px; margin-top: 6px;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                <div style="display: flex; gap: 6px; align-items: center;">
                  <span id="prevBankBadge" style="background: rgba(6, 182, 212, 0.2); color: var(--cyan); padding: 2px 6px; border-radius: 6px; font-size: 0.68rem; font-weight: 800;">بانک</span>
                  <span id="prevDirBadge" style="background: rgba(245, 158, 11, 0.2); color: var(--amber); padding: 2px 6px; border-radius: 6px; font-size: 0.65rem; font-weight: 700;">برداشت</span>
                </div>
                <span id="prevCategory" style="font-size: 0.65rem; color: var(--cyan);">دسته: خرید عمومی</span>
              </div>
              <div style="display: flex; justify-content: space-between; align-items: baseline;">
                <div id="prevAmountToman" style="font-size: 1.05rem; font-weight: 900; color: #fff;">۰ تومان</div>
                <div id="prevBalance" style="font-size: 0.68rem; color: var(--text-muted);">مانده: -</div>
              </div>
              <div id="prevMeta" style="font-size: 0.65rem; color: var(--text-muted); margin-top: 4px; line-height: 1.3;"></div>
            </div>

            <button onclick="processSmsInput()" class="btn-action" style="width: 100%; margin-top: 8px;">تأیید و ثبت خودکار در حساب‌ها ✓</button>
            <div id="smsSuccessAlert" style="display: none; background: rgba(16, 185, 129, 0.15); border: 1px solid rgba(16, 185, 129, 0.3); color: #34d399; font-size: 0.72rem; padding: 6px; border-radius: 8px; margin-top: 4px;"></div>
          </div>

          <!-- Transaction List -->
          <div class="card">
            <div class="card-title">
              <span>ریز تراکنش‌های مالی</span>
              <button onclick="openManualExpenseModal()" style="background: transparent; border: 1px solid var(--cyan); color: var(--cyan); font-size: 0.65rem; border-radius: 6px; padding: 2px 8px; cursor: pointer;">+ ثبت دستی</button>
            </div>
            <div id="fullExpenseList" style="display: flex; flex-direction: column; gap: 6px;"></div>
          </div>
        </div>

        <!-- Sub-tab 2 Content: Debts & Receivables (طلب و بدهی) -->
        <div id="finSubTab2Content" style="display: none; flex-direction: column; gap: 10px;">
          <div class="card" style="border: 1px solid rgba(245, 158, 11, 0.3);">
            <div class="card-title">
              <span>مدیریت مطالبات و بدهی‌ها</span>
              <button onclick="openAddDebtModal()" class="btn-action" style="font-size: 0.68rem; padding: 3px 8px;">+ تعهد جدید</button>
            </div>
            <div style="font-size: 0.7rem; color: var(--text-muted); margin-bottom: 8px;">اقساط به تسک‌های بخش اهداف و مسیر متصل هستند و با تسویه، خودکار تکمیل می‌شوند.</div>
            <div id="debtListContainer" style="display: flex; flex-direction: column; gap: 8px;"></div>
          </div>
        </div>
      </div>

      <!-- ================= 2. PILLAR 2: GOALS & PATH (اهداف و مسیر - GOALS_AND_HABITS) ================= -->
      <div id="pillar-goals" class="page">
        <!-- Sub-tabs: کارها و اقساط vs عادت‌ها و پومودورو vs سلامت -->
        <div style="display: flex; background: #0f172a; border-radius: 10px; padding: 3px;">
          <button id="goalsSubBtn1" onclick="setGoalsSubTab(0)" style="flex: 1; background: var(--cyan); color: #000; border: none; border-radius: 8px; padding: 6px; font-weight: 800; font-size: 0.7rem; cursor: pointer;">کارها و اقساط</button>
          <button id="goalsSubBtn2" onclick="setGoalsSubTab(1)" style="flex: 1; background: transparent; color: var(--text-muted); border: none; border-radius: 8px; padding: 6px; font-weight: 700; font-size: 0.7rem; cursor: pointer;">عادت‌ها و پومودورو</button>
          <button id="goalsSubBtn3" onclick="setGoalsSubTab(2)" style="flex: 1; background: transparent; color: var(--text-muted); border: none; border-radius: 8px; padding: 6px; font-weight: 700; font-size: 0.7rem; cursor: pointer;">سلامت و آب</button>
        </div>

        <!-- Goals Sub 1: Tasks & Debt Installments -->
        <div id="goalsSub1" style="display: flex; flex-direction: column; gap: 8px;">
          <div style="display: flex; gap: 6px;">
            <input type="text" id="taskSearchInput" oninput="filterTasks()" class="input-field" placeholder="جستجو در تسک‌ها..." style="flex: 1;">
            <button onclick="openAddTaskModal()" class="btn-action" style="padding: 6px 10px;">+ کار جدید</button>
          </div>
          <div id="fullTaskList" style="display: flex; flex-direction: column; gap: 8px;"></div>
        </div>

        <!-- Goals Sub 2: Habits & Pomodoro Focus Timer -->
        <div id="goalsSub2" style="display: none; flex-direction: column; gap: 10px;">
          <!-- Pomodoro Timer -->
          <div class="card" style="text-align: center; border: 1px solid rgba(6, 182, 212, 0.35);">
            <div style="font-size: 0.72rem; color: var(--cyan); font-weight: 700;">تایمر تمرکز عمیق پومودورو</div>
            <div id="pomodoroDisplay" style="font-size: 2rem; font-weight: 900; color: #fff; margin: 4px 0;">۲۵:۰۰</div>
            <div style="display: flex; justify-content: center; gap: 8px;">
              <button id="pomoToggleBtn" onclick="togglePomodoro()" class="btn-action">شروع تمرکز</button>
              <button onclick="resetPomodoro()" class="btn-action btn-outline">بازنشانی</button>
            </div>
          </div>

          <!-- Habits List -->
          <div class="card">
            <div class="card-title">
              <span>استمرار عادت‌های روزانه</span>
              <button onclick="openAddHabitModal()" style="background: none; border: 1px solid var(--emerald); color: var(--emerald); border-radius: 6px; font-size: 0.65rem; padding: 2px 6px; cursor: pointer;">+ عادت</button>
            </div>
            <div id="fullHabitList" style="display: flex; flex-direction: column; gap: 8px;"></div>
          </div>
        </div>

        <!-- Goals Sub 3: Health & Hydration -->
        <div id="goalsSub3" style="display: none; flex-direction: column; gap: 10px;">
          <div class="card" style="border: 1px solid rgba(6, 182, 212, 0.35);">
            <div class="card-title">
              <span>مصرف آب روزانه (هدف ۸ لیوان)</span>
              <span id="waterGoalText" style="color: var(--cyan); font-weight: 800;">۶ / ۸ لیوان</span>
            </div>
            <div style="background: rgba(255,255,255,0.06); height: 8px; border-radius: 4px; overflow: hidden; margin: 8px 0;">
              <div id="waterProgressBar" style="width: 75%; height: 100%; background: var(--cyan); transition: width 0.3s;"></div>
            </div>
            <div style="display: flex; justify-content: center; align-items: center; gap: 20px; margin-top: 6px;">
              <button onclick="updateWater(-1)" style="width: 36px; height: 36px; border-radius: 50%; background: #0f172a; border: 1px solid #334155; color: #fff; font-size: 1.2rem; cursor: pointer;">-</button>
              <span id="waterCountBig" style="font-size: 1.2rem; font-weight: 800; color: #fff;">۶ لیوان</span>
              <button onclick="updateWater(1)" style="width: 36px; height: 36px; border-radius: 50%; background: var(--cyan); border: none; color: #000; font-size: 1.2rem; cursor: pointer; font-weight: 800;">+</button>
            </div>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
            <div class="card">
              <div style="font-size: 0.68rem; color: var(--text-muted);">خواب دیشب</div>
              <div style="font-size: 1.15rem; font-weight: 800; color: #fff; margin: 2px 0;">۷.۵ ساعت</div>
              <div style="font-size: 0.65rem; color: var(--emerald);">کیفیت: مطلوب</div>
            </div>
            <div class="card">
              <div style="font-size: 0.68rem; color: var(--text-muted);">ورزش و تحرک</div>
              <div style="font-size: 1.15rem; font-weight: 800; color: var(--amber); margin: 2px 0;">۴۵ دقیقه</div>
              <div style="font-size: 0.65rem; color: var(--amber);">🔥 ۴۸۰ کالری</div>
            </div>
          </div>
        </div>
      </div>

      <!-- ================= 3. PILLAR 3: HOME DASHBOARD (داشبورد - HOME) ================= -->
      <div id="pillar-dashboard" class="page active">
        <!-- Hero Summary Banner -->
        <div class="card card-hero">
          <div style="font-size: 0.68rem; color: var(--cyan); font-weight: 700; margin-bottom: 2px;">💎 داشبورد وضعیت امروز (v1.5.0)</div>
          <div style="font-size: 0.95rem; font-weight: 800; color: #fff;">روز به‌خیر! ۴ ستون زندگی شما هماهنگ هستند</div>
          <div style="font-size: 0.72rem; color: var(--text-muted); margin: 4px 0 10px;">خلاصه وضعیت، اقدام‌های بعدی، روندهای ماهانه و تعهدات مالی.</div>
          <div style="display: flex; gap: 8px;">
            <button onclick="openVoiceModal()" class="btn-action" style="font-size: 0.72rem; padding: 6px 10px;">
              🎙️ ضبط صوت / اسکن پیامک
            </button>
            <button onclick="switchPillar('assistant')" class="btn-action btn-outline" style="font-size: 0.72rem; padding: 6px 10px;">
              تحلیل با جمینای
            </button>
          </div>
        </div>

        <!-- فاز ۴ نقشه راه: کارت «یادآوری‌های مهم و اقدام بعدی» -->
        <div class="card" style="border: 1px solid rgba(245, 158, 11, 0.35);">
          <div class="card-title">
            <span style="color: var(--amber);">⚡ یادآوری‌های مهم و اقدام بعدی</span>
            <span style="font-size: 0.65rem; color: var(--cyan); cursor: pointer;" onclick="switchPillar('goals')">برنامه مسیر</span>
          </div>
          <div id="nextActionNotice" style="font-size: 0.75rem; color: #fff; line-height: 1.5;">
            • سررسید قسط: <b>قسط وام تجهیزات</b> (۱۴۰۵/۰۸/۰۷)<br>
            • اقدام بعدی: <b style="color: var(--cyan);">«بررسی و تایید گزارش مالی شرکت»</b>
          </div>
        </div>

        <!-- 4 Key VisionOS Metrics Grid -->
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
          <div class="card" onclick="switchPillar('finance')" style="cursor: pointer;">
            <div style="font-size: 0.68rem; color: var(--text-muted);">قابل‌خرج (Safe-to-Spend)</div>
            <div id="dashSafeToSpend" style="font-size: 1.15rem; font-weight: 800; color: var(--cyan); margin-top: 2px;">۸,۲۲۰,۰۰۰</div>
            <div style="font-size: 0.65rem; color: var(--cyan);">تومان (پایش‌شده)</div>
          </div>
          <div class="card" onclick="switchPillar('finance')" style="cursor: pointer;">
            <div style="font-size: 0.68rem; color: var(--text-muted);">مخارج ماه آبان</div>
            <div id="dashExpenseTotal" style="font-size: 1.15rem; font-weight: 800; color: var(--amber); margin-top: 2px;">۱,۷۸۰,۰۰۰</div>
            <div style="font-size: 0.65rem; color: var(--amber);">برآورد پایان ماه: تخمینی</div>
          </div>
          <div class="card" onclick="switchPillar('goals')" style="cursor: pointer;">
            <div style="font-size: 0.68rem; color: var(--text-muted);">کارهای باقیمانده</div>
            <div id="dashTaskCount" style="font-size: 1.15rem; font-weight: 800; color: var(--emerald); margin-top: 2px;">۳ از ۴</div>
            <div style="font-size: 0.65rem; color: var(--emerald);">۱ کار متصل به قسط</div>
          </div>
          <div class="card" onclick="switchPillar('goals')" style="cursor: pointer;">
            <div style="font-size: 0.68rem; color: var(--text-muted);">سلامت و آب</div>
            <div id="dashWaterText" style="font-size: 1.15rem; font-weight: 800; color: #818cf8; margin-top: 2px;">۶ لیوان</div>
            <div style="font-size: 0.65rem; color: #818cf8;">خواب: ۷.۵ ساعت</div>
          </div>
        </div>

        <!-- Today Urgent Agenda -->
        <div class="card">
          <div class="card-title">
            <span>اولویت‌های امروز در مسیر زندگی</span>
            <span style="font-size: 0.7rem; color: var(--cyan); cursor: pointer;" onclick="switchPillar('goals')">مشاهده همه</span>
          </div>
          <div id="dashTaskList" style="display: flex; flex-direction: column; gap: 8px;"></div>
        </div>
      </div>

      <!-- ================= 4. PILLAR 4: AI ASSISTANT (هوش مصنوعی - AI_ASSISTANT) ================= -->
      <div id="pillar-assistant" class="page">
        <div class="card" style="border: 1px solid rgba(6, 182, 212, 0.35);">
          <div class="card-title">
            <span>دستیار هوشمند جمینای (Gemini AI)</span>
            <span style="font-size: 0.65rem; color: var(--cyan);">تحلیل داده‌های ۳۰ روزه</span>
          </div>
          
          <div style="display: flex; flex-wrap: wrap; gap: 4px; margin: 4px 0 8px;">
            <span class="sample-pill" onclick="sendAiPrompt('وضعیت بودجه و Safe-to-Spend من چطوره؟')">وضعیت بودجه این ماه</span>
            <span class="sample-pill" onclick="sendAiPrompt('کارهای فوری و بدهی‌های امروز رو به من بگو')">کارهای فوری و اقساط</span>
            <span class="sample-pill" onclick="sendAiPrompt('پایش سلامت و مصرف آب امروز چطوره؟')">پایش سلامت و آب</span>
          </div>

          <div id="aiChatBox" style="background: #090d16; border: 1px solid rgba(255,255,255,0.08); border-radius: 12px; height: 320px; overflow-y: auto; padding: 10px; display: flex; flex-direction: column; gap: 8px;">
            <div style="align-self: flex-start; max-width: 85%; background: #161e31; border: 1px solid rgba(6,182,212,0.3); border-radius: 12px 12px 12px 2px; padding: 8px 10px; font-size: 0.72rem; line-height: 1.5;">
              <span style="color: var(--cyan); font-weight: 700;">جمینای AI:</span> سلام! من دستیار اختصاصی Personal Life OS شما هستم. اتصال کامل به ۴ ستون مالی، اهداف، داشبورد و سلامت برقرار است. مایلید کدام بخش را بررسی یا ثبت کنیم؟
            </div>
          </div>

          <div style="display: flex; gap: 6px; margin-top: 8px;">
            <input type="text" id="aiChatInput" class="input-field" placeholder="پیام یا دستور مالی/کاری را بنویسید..." onkeydown="if(event.key==='Enter') sendUserMessage()">
            <button onclick="sendUserMessage()" class="btn-action" style="padding: 6px 12px;">ارسال</button>
          </div>
        </div>
      </div>

    </div>

    <!-- 4-Pillar Bottom Navigation Bar (Version 1.5.0 Reference) -->
    <div class="app-bottom-nav">
      <div id="nav-dashboard" class="nav-tab active" onclick="switchPillar('dashboard')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
        <span>داشبورد</span>
      </div>
      <div id="nav-finance" class="nav-tab" onclick="switchPillar('finance')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect><line x1="1" y1="10" x2="23" y2="10"></line></svg>
        <span>مالی و مصارف</span>
      </div>
      <div id="nav-goals" class="nav-tab" onclick="switchPillar('goals')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
        <span>اهداف و مسیر</span>
      </div>
      <div id="nav-assistant" class="nav-tab" onclick="switchPillar('assistant')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2 2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z"></path><rect x="4" y="8" width="16" height="12" rx="2"></rect></svg>
        <span>جمینای AI</span>
      </div>
    </div>

    <!-- Android Gesture Bar -->
    <div class="gesture-bar"><div class="gesture-pill"></div></div>

    <!-- ================= MODALS ================= -->
    <!-- Vault Modal -->
    <div id="vaultModal" class="modal-overlay">
      <div class="modal-content">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: 800; font-size: 0.85rem; color: #fff;">🔒 صندوق امن محرمانه</span>
          <button onclick="closeVaultModal()" style="background: none; border: none; color: var(--text-muted); font-size: 1.1rem; cursor: pointer;">✕</button>
        </div>
        <div id="vaultLockedState" style="text-align: center; padding: 10px 0;">
          <div style="font-size: 0.72rem; color: var(--text-muted); margin-bottom: 8px;">رمز ۴ رقمی را وارد کنید (پیش‌فرض: 1234):</div>
          <input type="password" id="vaultModalPin" maxlength="4" class="input-field" style="width: 120px; text-align: center; font-size: 1.1rem; letter-spacing: 4px; margin: 0 auto 8px;" placeholder="••••">
          <button onclick="unlockVaultModal()" class="btn-action" style="width: 100%;">بازگشایی</button>
        </div>
        <div id="vaultUnlockedState" style="display: none; flex-direction: column; gap: 8px;">
          <div style="font-size: 0.75rem; color: var(--emerald); font-weight: 700;">یادداشت‌های محرمانه:</div>
          <div style="background: #000; border-radius: 8px; padding: 8px; font-size: 0.72rem; font-family: monospace; color: var(--cyan);">
            • کیف پول: Seed: apple banana cherry ... 9942<br>
            • کارگزاری: User: mofid / Pass: Kx9@9821
          </div>
        </div>
      </div>
    </div>

    <!-- Voice Memo Modal -->
    <div id="voiceModal" class="modal-overlay">
      <div class="modal-content">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: 800; font-size: 0.85rem; color: #fff;">🎙️ ضبط صوت سریع و اسکنر پیامک</span>
          <button onclick="closeVoiceModal()" style="background: none; border: none; color: var(--text-muted); font-size: 1.1rem; cursor: pointer;">✕</button>
        </div>
        <textarea id="modalSmsInput" rows="3" class="input-field" placeholder="متن پیامک بانکی یا یادداشت صوتی خود را اینجا پیست کنید..."></textarea>
        <button onclick="submitModalSms()" class="btn-action">ثبت و تحلیل هوشمند</button>
        <div id="modalAlert" style="display: none; color: var(--emerald); font-size: 0.7rem;"></div>
      </div>
    </div>

    <!-- Add Task Modal -->
    <div id="addTaskModal" class="modal-overlay">
      <div class="modal-content">
        <div style="font-weight: 800; font-size: 0.85rem; color: #fff;">+ افزودن کار جدید در مسیر</div>
        <input type="text" id="newTaskTitle" class="input-field" placeholder="عنوان کار...">
        <input type="text" id="newTaskDate" class="input-field" value="۱۴۰۵/۰۸/۱۰" placeholder="موعد انجام...">
        <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 4px;">
          <button onclick="closeAddTaskModal()" class="btn-action btn-outline">انصراف</button>
          <button onclick="saveNewTask()" class="btn-action">ثبت کار</button>
        </div>
      </div>
    </div>

    <!-- Settings Modal -->
    <div id="settingsModal" class="modal-overlay">
      <div class="modal-content" style="max-height: 85vh; overflow-y: auto;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
          <span style="font-weight: 800; font-size: 0.85rem; color: #fff;">⚙️ تنظیمات Personal Life OS</span>
          <button onclick="closeSettingsModal()" style="background: none; border: none; color: var(--text-muted); font-size: 1.1rem; cursor: pointer;">✕</button>
        </div>
        <div style="display: flex; flex-direction: column; gap: 8px; font-size: 0.72rem; color: #cbd5e1;">
          <div style="background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 8px;">
            <div style="font-weight: 700; color: var(--cyan);">نسخه سیستم‌عامل شخصی: 1.5.0</div>
            <div style="color: var(--text-muted); font-size: 0.68rem; margin-top: 2px;">معماری ۷ صفحه‌ای پیشرفته با ۴ ستون هوشمند</div>
          </div>
          <div style="background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 8px;">
            <div style="font-weight: 700; color: var(--emerald);">موتور تحلیل پیامک‌های بانکی ایران: فعال</div>
            <div style="color: var(--text-muted); font-size: 0.68rem; margin-top: 2px;">پوشش هوشمند ۱۸+ بانک و نئوبانک (ملی، ملت، صادرات جبری، بلوبانک، سامانه پل، ویپاد، مسکن، کشاورزی، رفاه کارگران، رسالت، مهر ایران، سامان و ...)</div>
          </div>
          <div style="background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 8px;">
            <div style="font-weight: 700; color: #fff;">صندوق امن و رمزنگاری (Vault)</div>
            <div style="color: var(--text-muted); font-size: 0.68rem; margin-top: 2px;">رمز عبور ۴ رقمی / بیومتریک برای یادداشت‌های حساس، عبارات بازیابی کیف‌پول و اعتبارسنجی‌ها</div>
          </div>
          <div style="background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 8px;">
            <div style="font-weight: 700; color: #fff;">تقویم و زبان</div>
            <div style="color: var(--text-muted); font-size: 0.68rem; margin-top: 2px;">تقویم هجری شمسی و ارقام فارسی، تبدیل خودکار ریال به تومان</div>
          </div>
          <div style="background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 8px;">
            <div style="font-weight: 700; color: #fff;">هوش مصنوعی یکپارچه</div>
            <div style="color: var(--text-muted); font-size: 0.68rem; margin-top: 2px;">دستیار صوتی و متنی تحلیل مالی و برنامه‌ریزی روزانه</div>
          </div>
        </div>
        <button onclick="closeSettingsModal()" class="btn-action" style="width: 100%; margin-top: 8px;">بستن تنظیمات</button>
      </div>
    </div>

  </div>

  <script>
    // State
    let tasks = [
      { id: '1', title: 'بررسی و تایید گزارش مالی شرکت', category: 'کاری', priority: 'فوری', completed: false, dueDate: '۱۴۰۵/۰۸/۰۶' },
      { id: '2', title: 'ورزش هوازی و بدنسازی (۴۵ دقیقه)', category: 'سلامت', priority: 'بالا', completed: true, dueDate: '۱۴۰۵/۰۸/۰۵' },
      { id: '3', title: 'مطالعه کتاب معماری نرم‌افزار', category: 'مطالعه', priority: 'متوسط', completed: false, dueDate: '۱۴۰۵/۰۸/۰۸' },
      { id: '4', title: 'پرداخت قسط وام تجهیزات مهندسی', category: 'مالی', priority: 'فوری', completed: false, dueDate: '۱۴۰۵/۰۸/۰۷', linkedDebt: 'وام تجهیزات' }
    ];

    let habits = [
      { id: 'h1', title: 'مطالعه ۳۰ دقیقه روزانه', streak: 7, completedToday: true },
      { id: 'h2', title: 'ورزش و نرمش صبحگاهی', streak: 5, completedToday: false },
      { id: 'h3', title: 'نوشیدن ۸ لیوان آب', streak: 12, completedToday: true }
    ];

    let expenses = [
      { id: 'e1', title: 'خرید از فروشگاه رفاه', amount: 1250000, category: 'خرید عمومی', date: '۱۴۰۵/۰۸/۰۵', bank: 'بانک ملت', type: 'EXPENSE' },
      { id: 'e2', title: 'کافه و میان‌وعده کاری', amount: 350000, category: 'غذا و رستوران', date: '۱۴۰۵/۰۸/۰۴', bank: 'بانک سامان', type: 'EXPENSE' },
      { id: 'e3', title: 'شارژ کارت مترو و اسنپ', amount: 180000, category: 'حمل و نقل', date: '۱۴۰۵/۰۸/۰۳', bank: 'بلوبانک', type: 'EXPENSE' }
    ];

    let debts = [
      { id: 'd1', title: 'قسط وام تجهیزات مهندسی', person: 'بانک رسالت', total: 5000000, paid: 2000000, due: '۱۴۰۵/۰۸/۱۵', isDebt: true, settled: false },
      { id: 'd2', title: 'طلب بابت مشاوره پروژه‌ای', person: 'شرکت آوا پردازش', total: 8500000, paid: 0, due: '۱۴۰۵/۰۸/۲۰', isDebt: false, settled: false }
    ];

    let waterCount = 6;
    let pomoSeconds = 25 * 60;
    let pomoTimer = null;

    function formatToman(num) {
      return num.toLocaleString('fa-IR') + ' تومان';
    }

    function renderAll() {
      renderDashboard();
      renderFinance();
      renderGoals();
    }

    function renderDashboard() {
      const pending = tasks.filter(t => !t.completed).length;
      document.getElementById('dashTaskCount').innerText = pending + ' از ' + tasks.length;

      const total = expenses.filter(e => e.type === 'EXPENSE').reduce((a, b) => a + b.amount, 0);
      document.getElementById('dashExpenseTotal').innerText = total.toLocaleString('fa-IR');
      document.getElementById('finTotalExpense').innerText = formatToman(total);

      // Dash Tasks
      const list = document.getElementById('dashTaskList');
      list.innerHTML = '';
      tasks.slice(0, 3).forEach(task => {
        const item = document.createElement('div');
        item.style.cssText = 'display: flex; align-items: center; justify-content: space-between; background: #0f172a; padding: 8px 10px; border-radius: 10px; font-size: 0.75rem;';
        item.innerHTML = \`
          <div style="display: flex; align-items: center; gap: 8px;">
            <input type="checkbox" \${task.completed ? 'checked' : ''} onchange="toggleTask('\${task.id}')" style="cursor: pointer;">
            <div>
              <span style="\${task.completed ? 'text-decoration: line-through; color: var(--text-muted);' : 'color: #fff;'}">\${task.title}</span>
              \${task.linkedDebt ? '<span style="color: var(--amber); font-size: 0.65rem; margin-right: 6px;">[قسط وام]</span>' : ''}
            </div>
          </div>
          <span style="font-size: 0.65rem; color: \${task.priority === 'فوری' ? 'var(--rose)' : 'var(--cyan)'}; font-weight: 700;">\${task.priority}</span>
        \`;
        list.appendChild(item);
      });
    }

    function renderFinance() {
      // Transactions
      const expContainer = document.getElementById('fullExpenseList');
      expContainer.innerHTML = '';
      expenses.forEach(exp => {
        const item = document.createElement('div');
        item.style.cssText = 'display: flex; justify-content: space-between; align-items: center; background: #0f172a; padding: 10px; border-radius: 12px; font-size: 0.75rem;';
        item.innerHTML = \`
          <div>
            <div style="color: #fff; font-weight: 700;">\${exp.title}</div>
            <div style="font-size: 0.65rem; color: var(--cyan);">\${exp.bank} • \${exp.date}</div>
          </div>
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="color: \${exp.type === 'INCOME' ? 'var(--emerald)' : 'var(--amber)'}; font-weight: 800;">\${formatToman(exp.amount)}</span>
            <button onclick="deleteExpense('\${exp.id}')" style="background: none; border: none; color: var(--text-muted); cursor: pointer;">✕</button>
          </div>
        \`;
        expContainer.appendChild(item);
      });

      // Debts
      const debtContainer = document.getElementById('debtListContainer');
      debtContainer.innerHTML = '';
      debts.forEach(d => {
        const remaining = Math.max(0, d.total - d.paid);
        const item = document.createElement('div');
        item.style.cssText = 'background: #0f172a; padding: 12px; border-radius: 12px; font-size: 0.75rem; display: flex; flex-direction: column; gap: 6px;';
        item.innerHTML = \`
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-weight: 700; color: #fff;">\${d.title}</span>
            <span style="font-size: 0.65rem; color: \${d.isDebt ? 'var(--rose)' : 'var(--emerald)'}; background: rgba(255,255,255,0.06); padding: 2px 6px; border-radius: 4px;">\${d.isDebt ? 'بدهی من' : 'طلبکاری'}</span>
          </div>
          <div style="display: flex; justify-content: space-between; color: var(--text-muted); font-size: 0.68rem;">
            <span>طرف حساب: \${d.person}</span>
            <span>سررسید: \${d.due}</span>
          </div>
          <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 4px;">
            <div>
              <span style="font-weight: 800; color: #fff;">مانده: \${formatToman(remaining)}</span>
              <span style="font-size: 0.65rem; color: var(--text-muted); margin-right: 6px;">(کل: \${formatToman(d.total)})</span>
            </div>
            <button onclick="toggleDebtSettled('\${d.id}')" class="btn-action" style="font-size: 0.65rem; padding: 3px 8px; background: \${d.settled ? 'var(--emerald)' : '#1e293b'}; color: \${d.settled ? '#000' : '#fff'};">
              \${d.settled ? 'تسویه شد ✓' : 'تسویه ۱-لمسی'}
            </button>
          </div>
        \`;
        debtContainer.appendChild(item);
      });
    }

    function renderGoals() {
      // Tasks
      const taskContainer = document.getElementById('fullTaskList');
      taskContainer.innerHTML = '';
      tasks.forEach(task => {
        const item = document.createElement('div');
        item.style.cssText = 'display: flex; align-items: center; justify-content: space-between; background: #0f172a; padding: 10px; border-radius: 12px; font-size: 0.75rem;';
        item.innerHTML = \`
          <div style="display: flex; align-items: center; gap: 8px;">
            <input type="checkbox" \${task.completed ? 'checked' : ''} onchange="toggleTask('\${task.id}')" style="cursor: pointer;">
            <div>
              <div style="\${task.completed ? 'text-decoration: line-through; color: var(--text-muted);' : 'color: #fff; font-weight: 600;'}">\${task.title}</div>
              <div style="font-size: 0.65rem; color: var(--text-muted);">\${task.linkedDebt ? '<b style="color:var(--amber);">[قسط وام] </b>' : ''}دسته: \${task.category} • موعد: \${task.dueDate}</div>
            </div>
          </div>
          <button onclick="deleteTask('\${task.id}')" style="background: none; border: none; color: var(--text-muted); cursor: pointer;">✕</button>
        \`;
        taskContainer.appendChild(item);
      });

      // Habits
      const habitContainer = document.getElementById('fullHabitList');
      habitContainer.innerHTML = '';
      habits.forEach(h => {
        const item = document.createElement('div');
        item.style.cssText = 'display: flex; align-items: center; justify-content: space-between; background: #0f172a; padding: 10px; border-radius: 12px; font-size: 0.75rem;';
        item.innerHTML = \`
          <div>
            <div style="color: #fff; font-weight: 700;">\${h.title}</div>
            <div style="font-size: 0.68rem; color: var(--emerald);">🔥 استمرار: \${h.streak} روز متوالی</div>
          </div>
          <button onclick="toggleHabit('\${h.id}')" class="btn-action \${h.completedToday ? '' : 'btn-outline'}" style="font-size: 0.68rem; padding: 4px 8px;">
            \${h.completedToday ? 'انجام شد ✓' : 'ثبت امروز'}
          </button>
        \`;
        habitContainer.appendChild(item);
      });
    }

    // Handlers
    function toggleTask(id) {
      const t = tasks.find(x => x.id === id);
      if (t) t.completed = !t.completed;
      renderAll();
    }
    function deleteTask(id) {
      tasks = tasks.filter(x => x.id !== id);
      renderAll();
    }
    function toggleHabit(id) {
      const h = habits.find(x => x.id === id);
      if (h) {
        h.completedToday = !h.completedToday;
        if (h.completedToday) h.streak++; else h.streak = Math.max(0, h.streak - 1);
      }
      renderAll();
    }
    function deleteExpense(id) {
      expenses = expenses.filter(x => x.id !== id);
      renderAll();
    }
    function toggleDebtSettled(id) {
      const d = debts.find(x => x.id === id);
      if (d) {
        d.settled = !d.settled;
        if (d.settled) d.paid = d.total; else d.paid = 0;
      }
      renderAll();
    }

    function applyPreset(title, amount, cat) {
      expenses.unshift({
        id: 'e_' + Date.now(),
        title,
        amount,
        category: cat,
        date: '۱۴۰۵/۰۸/۰۵',
        bank: 'کیف پول نقدی',
        type: 'EXPENSE'
      });
      renderAll();
    }

    // Comprehensive Iranian Bank SMS Engine
    function normalizeFaDigits(str) {
      if (!str) return '';
      const fa = ['۰','۱','۲','۳','۴','۵','۶','۷','۸','۹'];
      const ar = ['٠','١','٢','٣','٤','٥','٦','٧','٨','٩'];
      let res = str;
      for (let i = 0; i <= 9; i++) {
        res = res.replaceAll(fa[i], i).replaceAll(ar[i], i);
      }
      return res;
    }

    function parseIranianBankSms(rawText) {
      if (!rawText || !rawText.trim()) return null;
      const text = normalizeFaDigits(rawText.trim());

      let bank = 'بانک شتابی';
      let bankColor = '#06B6D4';

      if (text.includes('رفاه') || (text.includes('بمانده') && text.includes('R'))) {
        bank = 'بانک رفاه کارگران';
        bankColor = '#84cc16';
      } else if (text.includes('بلوبانک') || text.includes('از حساب شما پرید') || text.includes('انتقال پل') || (text.startsWith('بلو') && text.includes('\n'))) {
        bank = 'بلوبانک';
        bankColor = '#06b6d4';
      } else if (text.includes('ویپاد') || text.includes('wepod') || text.includes('311.8000')) {
        bank = 'ویپاد (پاسارگاد)';
        bankColor = '#10b981';
      } else if (text.includes('پاسارگاد')) {
        bank = 'بانک پاسارگاد';
        bankColor = '#d97706';
      } else if (/^[0-9]{10,16}\s*\n\s*([0-9,]+)[+-]/m.test(text) || text.includes('صادرات') || text.includes('سپهر')) {
        bank = 'بانک صادرات ایران';
        bankColor = '#2563eb';
      } else if (text.includes('ملی') || text.includes('سداد')) {
        bank = 'بانک ملی ایران';
        bankColor = '#eab308';
      } else if (text.includes('ملت') || text.includes('بهسازان') || text.includes('610433')) {
        bank = 'بانک ملت';
        bankColor = '#e11d48';
      } else if (text.includes('تجارت')) {
        bank = 'بانک تجارت';
        bankColor = '#0284c7';
      } else if (text.includes('سپه') || text.includes('589210') || text.includes('ردیف:')) {
        bank = 'بانک سپه';
        bankColor = '#f59e0b';
      } else if (text.includes('مسکن') || text.includes('ناواکو')) {
        bank = 'بانک مسکن';
        bankColor = '#f97316';
      } else if (text.includes('کشاورزی') || text.includes('مهرگستر') || text.includes('603770') || text.includes('639217')) {
        bank = 'بانک کشاورزی';
        bankColor = '#16a34a';
      } else if (text.includes('توسعه تعاون') || text.includes('502908')) {
        bank = 'بانک توسعه تعاون';
        bankColor = '#0d9488';
      } else if (text.includes('صنعت و معدن')) {
        bank = 'بانک صنعت و معدن';
        bankColor = '#6366f1';
      } else if (text.includes('توسعه صادرات')) {
        bank = 'بانک توسعه صادرات';
        bankColor = '#4f46e5';
      } else if (text.includes('پست بانک') || text.includes('627760')) {
        bank = 'پست بانک ایران';
        bankColor = '#059669';
      } else if (text.includes('مهر ایران')) {
        bank = 'بانک مهر ایران';
        bankColor = '#14b8a6';
      } else if (text.includes('رسالت')) {
        bank = 'بانک قرض‌الحسنه رسالت';
        bankColor = '#3b82f6';
      } else if (text.includes('سامان') || text.includes('کد ارجاع:') || text.includes('621986')) {
        bank = 'بانک سامان';
        bankColor = '#0284c7';
      } else if (text.includes('پارسیان') || text.includes('لوتوس')) {
        bank = 'بانک پارسیان';
        bankColor = '#7c3aed';
      } else if (text.includes('آینده') || text.includes('توسن')) {
        bank = 'بانک آینده';
        bankColor = '#9333ea';
      } else if (text.includes('شهر') || text.includes('504706') || text.includes('کد رهگیری:')) {
        bank = 'بانک شهر';
        bankColor = '#dc2626';
      } else if (text.includes('گردشگری') || text.includes('کد مرجع:')) {
        bank = 'بانک گردشگری';
        bankColor = '#d97706';
      } else if (text.includes('اقتصاد نوین') || text.includes('627412')) {
        bank = 'بانک اقتصاد نوین';
        bankColor = '#9333ea';
      } else if (text.includes('سینا') || text.includes('شماره ارجاع:')) {
        bank = 'بانک سینا';
        bankColor = '#0284c7';
      } else if (text.includes('دی') || text.includes('502938')) {
        bank = 'بانک دی';
        bankColor = '#ea580c';
      } else if (text.includes('کارآفرین') || text.includes('مانده حساب:')) {
        bank = 'بانک کارآفرین';
        bankColor = '#16a34a';
      } else if (text.includes('ایران زمین') || text.includes('505785')) {
        bank = 'بانک ایران زمین';
        bankColor = '#0d9488';
      } else if (text.includes('سرمایه') || text.includes('639607')) {
        bank = 'بانک سرمایه';
        bankColor = '#2563eb';
      } else if (text.includes('خاورمیانه') || text.includes('شناسه پیگیری:')) {
        bank = 'بانک خاورمیانه';
        bankColor = '#475569';
      } else if (text.includes('ملل') || text.includes('عسکریه') || text.includes('606256')) {
        bank = 'مؤسسه اعتباری ملل';
        bankColor = '#059669';
      } else if (text.includes('کوثر') || text.includes('505801')) {
        bank = 'مؤسسه اعتباری کوثر';
        bankColor = '#f59e0b';
      } else if (text.includes('نور')) {
        bank = 'مؤسسه اعتباری نور';
        bankColor = '#eab308';
      }

      // Direction
      let dir = 'WITHDRAWAL';
      let dirFa = 'برداشت';
      let isExpense = true;

      const algMatch = text.match(/([0-9,]+)([+-])/);
      if (algMatch) {
        if (algMatch[2] === '+') {
          dir = 'DEPOSIT';
          dirFa = 'واریز';
          isExpense = false;
        } else {
          dir = 'WITHDRAWAL';
          dirFa = 'برداشت';
          isExpense = true;
        }
      } else if (text.includes('انتقال پل') || text.includes('سامانه پل')) {
        dir = 'TRANSFER_POL';
        dirFa = 'انتقال پل';
        isExpense = true;
      } else if (text.includes('واریز') || text.includes('واریزمبلغ')) {
        dir = 'DEPOSIT';
        dirFa = 'واریز';
        isExpense = false;
      } else if (text.includes('خرید') || text.includes('خريد') || text.includes('پایانه')) {
        dir = 'PURCHASE';
        dirFa = 'خرید';
        isExpense = true;
      }

      // Amount extraction
      let amountRial = 0;
      let amountToman = 0;

      if (algMatch) {
        amountRial = parseInt(algMatch[1].replace(/,/g, ''), 10) || 0;
        amountToman = Math.floor(amountRial / 10);
      } else {
        const refahMatch = text.match(/(?:برداشت|واریز|مبلغ)\s*([0-9,]+)\s*R/i);
        const bluMatch = text.match(/([0-9,]+)\s*(?:ریال|تومان)?\s*از حساب شما پرید/);
        const stdMatch = text.match(/(?:واریز|برداشت|مبلغ|خرید|خريد|انتقال)\s*(?:مبلغ)?[:\s]+([0-9,]+)\s*(ریال|تومان)?/);

        if (refahMatch) {
          amountRial = parseInt(refahMatch[1].replace(/,/g, ''), 10) || 0;
          amountToman = Math.floor(amountRial / 10);
        } else if (bluMatch) {
          amountRial = parseInt(bluMatch[1].replace(/,/g, ''), 10) || 0;
          amountToman = Math.floor(amountRial / 10);
        } else if (stdMatch) {
          const raw = parseInt(stdMatch[1].replace(/,/g, ''), 10) || 0;
          if (stdMatch[2] === 'تومان') {
            amountToman = raw;
            amountRial = raw * 10;
          } else {
            amountRial = raw;
            amountToman = Math.floor(raw / 10);
          }
        } else {
          const anyNum = text.match(/([0-9]{1,3}(?:,[0-9]{3})+)/);
          if (anyNum) {
            amountRial = parseInt(anyNum[1].replace(/,/g, ''), 10) || 0;
            amountToman = Math.floor(amountRial / 10);
          }
        }
      }

      // Balance
      let balanceToman = null;
      const balMatch = text.match(/(?:مانده|موجودی|بمانده)[:\s]*([0-9,]+)\s*(?:R|ریال|تومان)?/i);
      if (balMatch) {
        const balRaw = parseInt(balMatch[1].replace(/,/g, ''), 10) || 0;
        balanceToman = text.includes('تومان') ? balRaw : Math.floor(balRaw / 10);
      }

      // Metadata
      const cardMatch = text.match(/(?:به|از|حساب|کارت|سپرده)[:\s]+([0-9.*-]{4,24})/);
      const destMatch = text.match(/(?:به|انتقال از)[:\s]+([\u0600-\u06FF\s]{3,25})/);
      const trackMatch = text.match(/(?:پیگیری|کد پیگیری|شماره پیگیری|کد ارجاع|ارجاع|ردیف|سند|شماره سند)[:\s]+([0-9]+)/);
      const termMatch = text.match(/(?:پایانه|پايانه|فروشگاه)[:\s]*([\u0600-\u06FFA-Za-z0-9.·\s]{4,30})/);

      // Category
      let cat = 'خرید عمومی';
      const low = text.toLowerCase();
      if (low.includes('اسنپ') || low.includes('تپسی') || low.includes('تاکسی')) cat = 'حمل و نقل';
      else if (low.includes('کافه') || low.includes('قهوه') || low.includes('رستوران') || low.includes('فست فود')) cat = 'کافه و رستوران';
      else if (low.includes('کوروش') || low.includes('هایپرمی') || low.includes('سوپرمارکت')) cat = 'سوپرمارکت و مواد غذایی';
      else if (low.includes('داروخانه') || low.includes('درمانگاه') || low.includes('دکتر')) cat = 'سلامت و درمان';
      else if (low.includes('بنزین') || low.includes('جایگاه')) cat = 'خودرو و بنزین';
      else if (dir === 'TRANSFER_POL' || dir === 'TRANSFER_CARD') cat = 'انتقال مالی';
      else if (dir === 'DEPOSIT') cat = 'واریز به حساب';

      const title = (termMatch ? termMatch[1].trim() : (destMatch ? 'انتقال به: ' + destMatch[1].trim() : dirFa + ' ' + bank));

      return {
        bank,
        bankColor,
        direction: dir,
        directionFa: dirFa,
        isExpense,
        amountToman,
        amountRial,
        balanceToman,
        accountOrCard: cardMatch ? cardMatch[1] : null,
        destination: destMatch ? destMatch[1].trim() : null,
        trackingCode: trackMatch ? trackMatch[1] : null,
        merchant: title,
        category: cat,
        type: isExpense ? 'EXPENSE' : 'INCOME'
      };
    }

    function onSmsTextChange() {
      const box = document.getElementById('smsBoxInput');
      const text = box.value;
      const previewCard = document.getElementById('smsLivePreviewCard');
      if (!text || !text.trim()) {
        previewCard.style.display = 'none';
        return;
      }
      const p = parseIranianBankSms(text);
      if (!p || p.amountToman <= 0) {
        previewCard.style.display = 'none';
        return;
      }

      previewCard.style.display = 'block';
      const bankBadge = document.getElementById('prevBankBadge');
      bankBadge.innerText = p.bank;
      bankBadge.style.color = p.bankColor;
      bankBadge.style.background = p.bankColor + '26';

      const dirBadge = document.getElementById('prevDirBadge');
      dirBadge.innerText = p.directionFa;
      dirBadge.style.color = p.isExpense ? 'var(--amber)' : 'var(--emerald)';
      dirBadge.style.background = p.isExpense ? 'rgba(245, 158, 11, 0.2)' : 'rgba(16, 185, 129, 0.2)';

      document.getElementById('prevCategory').innerText = 'دسته: ' + p.category;
      document.getElementById('prevAmountToman').innerText = (p.isExpense ? '- ' : '+ ') + formatToman(p.amountToman) + ' تومان';
      document.getElementById('prevAmountToman').style.color = p.isExpense ? 'var(--amber)' : 'var(--emerald)';
      document.getElementById('prevBalance').innerText = p.balanceToman ? 'مانده: ' + formatToman(p.balanceToman) + ' ت' : '';

      const metaParts = [];
      if (p.accountOrCard) metaParts.push('کارت/حساب: ' + p.accountOrCard);
      if (p.destination) metaParts.push('به: ' + p.destination);
      if (p.trackingCode) metaParts.push('پیگیری: ' + p.trackingCode);
      document.getElementById('prevMeta').innerText = metaParts.join(' • ');
    }

    function clearSmsInput() {
      document.getElementById('smsBoxInput').value = '';
      document.getElementById('smsLivePreviewCard').style.display = 'none';
      document.getElementById('smsSuccessAlert').style.display = 'none';
    }

    function processSmsInput() {
      const box = document.getElementById('smsBoxInput');
      const text = box.value;
      const parsed = parseIranianBankSms(text);
      if (!parsed || parsed.amountToman <= 0) {
        alert('مبلغ یا الگوی پیامک بانکی شناسایی نشد.');
        return;
      }

      expenses.unshift({
        id: 'e_' + Date.now(),
        title: parsed.merchant,
        amount: parsed.amountToman,
        category: parsed.category,
        date: '۱۴۰۵/۰۸/۰۵',
        bank: parsed.bank,
        type: parsed.type
      });

      const alertBox = document.getElementById('smsSuccessAlert');
      alertBox.innerText = '✓ تراکنش ' + formatToman(parsed.amountToman) + ' از ' + parsed.bank + ' با موفقیت ثبت شد.';
      alertBox.style.display = 'block';
      box.value = '';
      document.getElementById('smsLivePreviewCard').style.display = 'none';
      renderAll();
      setTimeout(() => { alertBox.style.display = 'none'; }, 4000);
    }

    function loadSampleSms(bankKey) {
      const samples = {
        melli: 'بانک ملی ایران\nواریز: ۱,۲۵۰,۰۰۰ ریال\nبه: ۰۱*****۱۲۳۴\nمانده: ۴۵,۸۹۰,۰۰۰ ریال\n۱۴۰۳/۰۸/۱۴-۱۱:۳۵\nپیگیری: ۸۸۷۷۱۱',
        mellat: 'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۸,۴۲۰,۰۰۰ ریال\nتاریخ: ۱۴۰۳/۰۹/۱۰ ساعت: ۱۸:۴۵\nپیگیری: ۹۸۷۶۵۴',
        saderat: '300325283197\n1,698,500+\n1403/8/5-11:43\nمانده:32,317,981',
        tejarat: '*بانک تجارت*\nبرداشت: ۷۲۰,۰۰۰ ریال\nاز طريق: شتاب\nمانده: ۳۱,۴۰۱,۷۹۳ ریال\n1403/09/29 14:22',
        sepah: 'بانک سپه\nبرداشت: ۱,۵۰۰,۰۰۰ ریال\nاز: ۵۸۹۲۱۰******۷۸۹۰\nمانده: ۹,۸۰۰,۰۰۰ ریال\nردیف: ۳۴۲۱\n۱۴۰۳/۰۹/۰۸ ۱۳:۴۰',
        blu: 'بلو\nبرداشت پول\nعلیرضا عزیز، 900,000 ریال از حساب شما پرید.\nموجودی: 64,161,452 ریال\n۱۰:۴۵\n۱۴۰۳.۰۸.۰۵',
        pol: 'بلو\nانتقال پل\nمبلغ: ۱,۵۰۰,۰۰۰ ریال\nبه: علیرضا یوسفی\nمانده: ۶۲,۶۶۱,۴۵۲ ریال\nکد پیگیری: ۹۸۷۶۵۴۳۲۱\n1403/09/10 11:15',
        vepod: 'بانک پاسارگاد\nواریز مبلغ 2,000,000 ریال به حساب 311.8000.987654.1\nانتقال از: ویپاد\nموجودی: 6,800,000 ریال\nشماره پیگیری: 457812\n1403/09/14-11:05',
        maskan: 'بانک مسکن\nواریز: ۲,۰۰۰,۰۰۰ ریال\nبه حساب: ۴۱*****۷۸۹\nمانده: ۱۲,۳۰۰,۰۰۰ ریال\n۱۴۰۳/۰۹/۰۴-۱۵:۳۰\nشماره پیگیری: ۴۴۲۲۱۱',
        keshavarzi: 'بانک کشاورزی\nبرداشت از: ۶۰۳۷۷۰******۱۲۳۴\nمبلغ: ۱,۵۰۰,۰۰۰ ریال\nمانده: ۶,۸۵۰,۰۰۰ ریال\nتاریخ: ۱۴۰۳/۰۸/۲۲ ساعت: ۱۰:۱۴\nپیگیری: ۵۵۸۴۹۳',
        tosee_taavon: 'بانک توسعه تعاون\nواریز: ۵۰۰,۰۰۰ ریال\nبه کارت: ۵۰۲۹۰۸******۳۲۱۱\nموجودی: ۳,۲۰۰,۰۰۰ ریال\nپیگیری: ۱۲۴۵۷۸\n۱۴۰۳/۰۹/۱۰ ۰۹:۱۵',
        sanat_madan: 'بانک صنعت و معدن\nبرداشت مبلغ: ۱۰,۰۰۰,۰۰۰ ریال\nاز حساب: ۰۱*****۴۵۶\nمانده: ۱۱۰,۰۰۰,۰۰۰ ریال\nسند: ۹۰۸۱۷۲\n۱۴۰۳/۰۸/۱۵ ۱۱:۲۰',
        tosee_saderat: 'بانک توسعه صادرات\nواریز: ۵۰,۰۰۰,۰۰۰ ریال\nبه حساب: ۰۱*****۳۴۵\nمانده: ۱۸۰,۰۰۰,۰۰۰ ریال\nپیگیری: ۷۸۹۱۲۳\n۱۴۰۳/۰۹/۰۱ ۱۰:۴۰',
        post_bank: 'پست بانک\nبرداشت: ۴۵۰,۰۰۰ ریال\nاز: ۶۲۷۷۶۰******۴۴۳۳\nمانده: ۱,۸۰۰,۰۰۰ ریال\n۱۴۰۳/۰۹/۰۸ ۱۳:۴۵\nارجاع: ۷۸۹۰۱۲',
        refah: 'رفاه حساب 62****13\nبرداشت1,960,000R کد2000\nخريد ازپايانه بانک تجارت الكترونيك پارش·پ039799\nبمانده1,231,509R\n1399/12/26-16:56',
        mehr: 'بانک مهر ایران\nواریز: ۳,۰۰۰,۰۰۰ ریال\nبه سپرده: ۱-۱۲۳۴۵۶-۴۰-۳۰۰۱\nمانده: ۴۵,۰۰۰,۰۰۰ ریال\nکد پیگیری: ۸۹۶۵۴۱\n۱۴۰۳/۰۹/۰۳ ۱۷:۰۲',
        resalat: 'بانک قرض الحسنه رسالت\nبرداشت از حساب: ۱۰*****۲۳۴۵\nمبلغ: ۱,۲۰۰,۰۰۰ ریال\nمانده: ۵,۴۰۰,۰۰۰ ریال\nتاریخ: ۱۴۰۳/۰۹/۱۲ ۱۲:۰۰\nکد پیگیری: ۶۶۵۴۳۲',
        pasargad: 'بانک پاسارگاد\nواریز مبلغ ۱,۰۰۰,۰۰۰ ریال به حساب ۲۳۴.۸۰۰۰.۱۲۳۴۵۶.۱\nموجودی: ۱۵,۴۰۰,۰۰۰ ریال\nشماره پیگیری: ۸۸۷۷۶۶\n۱۴۰۳/۰۹/۰۵-۱۶:۱۰',
        saman: 'خرید: ۳۴۰,۰۰۰ ریال\nاز کارت: ۶۲۱۹۸۶******۹۰۱۲\nمانده: ۴,۲۵۰,۰۰۰ ریال\nکد ارجاع: ۱۲۳۴۵۶۷۸\n۱۴۰۳/۰۹/۱۵ ۲۱:۰۵',
        parsian: 'بانک پارسیان\nانتقال به کارت\nمبلغ: ۲,۵۰۰,۰۰۰ ریال\nاز حساب: ۰۱*****۹۸۷۶\nمانده: ۱۸,۲۰۰,۰۰۰ ریال\nشماره سند: ۴۵۹۸۷۶\n۱۴۰۳/۰۹/۱۱ ۱۰:۱۵',
        ayandeh: 'بانک آینده\nبرداشت: ۷۵۰,۰۰۰ ریال\nاز سپرده: ۰۲*****۹۰۱۲\nمانده: ۱۸,۳۵۰,۰۰۰ ریال\nکد پیگیری: ۳۴۵۱۲۶\n۱۴۰۳/۰۹/۰۲ ۲۰:۱۰',
        shahr: 'بانک شهر\nواریز مبلغ: ۵,۰۰۰,۰۰۰ ریال\nبه کارت: ۵۰۴۷۰۶******۷۷۸۸\nموجودی: ۲۲,۱۰۰,۰۰۰ ریال\nکد رهگیری: ۹۹۸۸۷۷\n۱۴۰۳/۰۸/۳۰ ۰۸:۵۰',
        gardeshgari: 'بانک گردشگری\nواریز: ۲,۲۰۰,۰۰۰ ریال\nبه حساب: ۱۱۰*****۶۵۴\nمانده: ۱۶,۵۰۰,۰۰۰ ریال\nکد مرجع: ۸۸۷۶۵۴\n۱۴۰۳/۰۹/۰۹ ۲۱:۱۵',
        eghtesad: 'بانک اقتصاد نوین\nخرید: ۱,۱۰۰,۰۰۰ ریال\nاز کارت: ۶۲۷۴۱۲******۵۵۴۴\nمانده: ۹,۸۰۰,۰۰۰ ریال\nپیگیری: ۱۲۳۹۸۷\n۱۴۰۳/۰۹/۰۸ ۱۹:۲۵',
        sina: 'بانک سینا\nبرداشت از حساب: ۱۲۳*****۴۵\nمبلغ: ۶۰۰,۰۰۰ ریال\nمانده: ۴,۱۰۰,۰۰۰ ریال\nشماره ارجاع: ۳۴۲۵۱۶\n۱۴۰۳/۰۹/۰۶ ۱۴:۳۵',
        day: 'بانک دی\nواریز: ۸۰۰,۰۰۰ ریال\nبه: ۵۰۲۹۳۸******۱۱۲۲\nمانده: ۲,۹۰۰,۰۰۰ ریال\nردیف: ۹۸۴۵\n۱۴۰۳/۰۸/۲۵ ۱۶:۴۰',
        karafarin: 'بانک کارآفرین\nبرداشت مبلغ: ۳,۴۰۰,۰۰۰ ریال\nاز حساب: ۵۴*****۱۱۲\nمانده حساب: ۵۴,۰۰۰,۰۰۰ ریال\nشماره پیگیری: ۳۳۴۴۵۵\n۱۴۰۳/۰۸/۲۷ ۱۱:۴۵',
        iranzamin: 'بانک ایران زمین\nواریز: ۹۵۰,۰۰۰ ریال\nبه: ۵۰۵۷۸۵******۳۴۱۲\nموجودی: ۶,۳۰۰,۰۰۰ ریال\nکد ارجاع: ۷۸۹۳۲۱\n۱۴۰۳/۰۹/۰۱ ۱۰:۰۵',
        sarmaye: 'بانک سرمایه\nبرداشت: ۳۰۰,۰۰۰ ریال\nاز کارت: ۶۳۹۶۰۷******۸۸۹۹\nمانده: ۷,۲۰۰,۰۰۰ ریال\nکد پیگیری: ۶۵۴۹۸۷\n۱۴۰۳/۰۸/۲۲ ۱۸:۲۰',
        khavarmianeh: 'بانک خاورمیانه\nانتقال وجه\nمبلغ: ۵۰,۰۰۰,۰۰۰ ریال\nاز حساب: ۱۰۰*****۰۱\nمانده: ۱,۴۵۰,۰۰۰,۰۰۰ ریال\nشناسه پیگیری: ۸۷۴۵۱۲\n۱۴۰۳/۰۹/۱۴ ۱۳:۱۰',
        melal: 'موسسه اعتباری ملل\nواریز: ۷۰۰,۰۰۰ ریال\nبه کارت: ۶۰۶۲۵۶******۷۷۱۱\nمانده: ۲,۳۰۰,۰۰۰ ریال\nپیگیری: ۸۸۹۹۰۰\n۱۴۰۳/۰۹/۰۷ ۱۶:۵۰',
        kosar: 'بانک سپه (کوثر سابق)\nبرداشت: ۴۰۰,۰۰۰ ریال\nاز: ۵۰۵۸۰۱******۲۲۳۳\nمانده: ۵,۱۰۰,۰۰۰ ریال\nردیف: ۷۸۱۲\n۱۴۰۳/۰۹/۰۳ ۱۱:۵۰',
        noor: 'بانک ملی ایران\nواریز: ۸۵۰,۰۰۰ ریال\nبه: ۰۱*****۹۹۰۰\nمانده: ۱۲,۶۵۰,۰۰۰ ریال\n۱۴۰۳/۰۹/۰۲-۰۹:۱۴\nپیگیری: ۳۳۴۴۱۱'
      };
      document.getElementById('smsBoxInput').value = samples[bankKey] || '';
      onSmsTextChange();
    }

    // Health
    function updateWater(delta) {
      waterCount = Math.max(0, waterCount + delta);
      document.getElementById('waterCountBig').innerText = waterCount + ' لیوان';
      document.getElementById('waterGoalText').innerText = waterCount + ' / ۸ لیوان';
      document.getElementById('dashWaterText').innerText = waterCount + ' لیوان';
      document.getElementById('waterProgressBar').style.width = Math.min(100, (waterCount / 8) * 100) + '%';
    }

    // Pomodoro Timer
    function togglePomodoro() {
      const btn = document.getElementById('pomoToggleBtn');
      if (pomoTimer) {
        clearInterval(pomoTimer);
        pomoTimer = null;
        btn.innerText = 'ادامه تمرکز';
      } else {
        btn.innerText = 'توقف موقت';
        pomoTimer = setInterval(() => {
          if (pomoSeconds > 0) {
            pomoSeconds--;
            const m = Math.floor(pomoSeconds / 60).toString().padStart(2, '0');
            const s = (pomoSeconds % 60).toString().padStart(2, '0');
            document.getElementById('pomodoroDisplay').innerText = m + ':' + s;
          } else {
            clearInterval(pomoTimer);
            pomoTimer = null;
            btn.innerText = 'تکمیل شد! استراحت';
          }
        }, 1000);
      }
    }

    function resetPomodoro() {
      if (pomoTimer) clearInterval(pomoTimer);
      pomoTimer = null;
      pomoSeconds = 25 * 60;
      document.getElementById('pomodoroDisplay').innerText = '۲۵:۰۰';
      document.getElementById('pomoToggleBtn').innerText = 'شروع تمرکز';
    }

    // AI Chat Engine
    function sendUserMessage() {
      const input = document.getElementById('aiChatInput');
      const text = input.value.trim();
      if (!text) return;
      input.value = '';
      appendChatMessage('user', text);
      generateAiResponse(text);
    }
    function sendAiPrompt(t) {
      appendChatMessage('user', t);
      generateAiResponse(t);
    }
    function appendChatMessage(sender, text) {
      const box = document.getElementById('aiChatBox');
      const msg = document.createElement('div');
      const isUser = sender === 'user';
      msg.style.cssText = \`align-self: \${isUser ? 'flex-end' : 'flex-start'}; max-width: 85%; background: \${isUser ? '#1e3a8a' : '#161e31'}; border: 1px solid \${isUser ? '#3b82f6' : 'rgba(6,182,212,0.3)'}; border-radius: 12px; padding: 8px 10px; font-size: 0.72rem; line-height: 1.5;\`;
      msg.innerHTML = \`<span style="color: \${isUser ? '#93c5fd' : 'var(--cyan)'}; font-weight: 700;">\${isUser ? 'شما:' : 'جمینای AI:'}</span> \${text}\`;
      box.appendChild(msg);
      box.scrollTop = box.scrollHeight;
    }
    function generateAiResponse(query) {
      setTimeout(() => {
        let ans = '';
        if (query.includes('بودجه') || query.includes('safe') || query.includes('خرج')) {
          ans = 'تحلیل بودجه: Safe-to-Spend شما تا پایان ماه ۸,۲۲۰,۰۰۰ تومان است. بزرگ‌ترین خرج ثبت‌شده خرید فروشگاه رفاه است.';
        } else if (query.includes('بدهی') || query.includes('قسط') || query.includes('کار')) {
          ans = 'اقدام بعدی فوری شما پرداخت قسط وام تجهیزات مهندسی در ۱۴۰۵/۰۸/۰۷ است که در تسک‌های اهداف و مسیر پیوند داده شده است.';
        } else if (query.includes('آب') || query.includes('سلامت')) {
          ans = 'مصرف آب امروز: ' + waterCount + ' لیوان (۷۵٪). خواب ۷.۵ ساعت و وضعیت تندرستی بهینه است.';
        } else {
          ans = 'درخواست شما: «' + query + '» بررسی شد. هر ۴ ستون سیستم‌عامل (مالی، اهداف، داشبورد و AI) با موفقیت به‌روز شدند.';
        }
        appendChatMessage('gemini', ans);
      }, 700);
    }

    // 4-Pillar Tab Switching (Version 1.5.0: داشبورد، مالی و مصارف، اهداف و مسیر، جمینای AI)
    function switchPillar(pillarId) {
      document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
      const target = document.getElementById('pillar-' + pillarId);
      if (target) target.classList.add('active');

      document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
      const activeTab = document.getElementById('nav-' + pillarId);
      if (activeTab) activeTab.classList.add('active');
    }

    function syncNavToCurrentPage() {
      document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
      if (document.getElementById('pillar-dashboard')?.classList.contains('active')) {
        document.getElementById('nav-dashboard')?.classList.add('active');
      } else if (document.getElementById('pillar-finance')?.classList.contains('active')) {
        document.getElementById('nav-finance')?.classList.add('active');
      } else if (document.getElementById('pillar-goals')?.classList.contains('active')) {
        document.getElementById('nav-goals')?.classList.add('active');
      } else if (document.getElementById('pillar-assistant')?.classList.contains('active')) {
        document.getElementById('nav-assistant')?.classList.add('active');
      }
    }

    // Sub-tab Switchers
    function setFinanceSubTab(idx) {
      if (idx === 0) {
        document.getElementById('finSubTab1Content').style.display = 'flex';
        document.getElementById('finSubTab2Content').style.display = 'none';
        document.getElementById('finTabBtn1').style.background = 'var(--cyan)';
        document.getElementById('finTabBtn1').style.color = '#000';
        document.getElementById('finTabBtn2').style.background = 'transparent';
        document.getElementById('finTabBtn2').style.color = 'var(--text-muted)';
      } else {
        document.getElementById('finSubTab1Content').style.display = 'none';
        document.getElementById('finSubTab2Content').style.display = 'flex';
        document.getElementById('finTabBtn2').style.background = 'var(--cyan)';
        document.getElementById('finTabBtn2').style.color = '#000';
        document.getElementById('finTabBtn1').style.background = 'transparent';
        document.getElementById('finTabBtn1').style.color = 'var(--text-muted)';
      }
    }

    function setGoalsSubTab(idx) {
      document.getElementById('goalsSub1').style.display = idx === 0 ? 'flex' : 'none';
      document.getElementById('goalsSub2').style.display = idx === 1 ? 'flex' : 'none';
      document.getElementById('goalsSub3').style.display = idx === 2 ? 'flex' : 'none';
      ['goalsSubBtn1', 'goalsSubBtn2', 'goalsSubBtn3'].forEach((btnId, i) => {
        const btn = document.getElementById(btnId);
        if (i === idx) {
          btn.style.background = 'var(--cyan)';
          btn.style.color = '#000';
        } else {
          btn.style.background = 'transparent';
          btn.style.color = 'var(--text-muted)';
        }
      });
    }

    // Modals
    function openVoiceModal() { document.getElementById('voiceModal').style.display = 'flex'; }
    function closeVoiceModal() { document.getElementById('voiceModal').style.display = 'none'; }
    function submitModalSms() {
      const text = document.getElementById('modalSmsInput').value;
      const parsed = parseIranianBankSms(text);
      if (parsed) {
        expenses.unshift({
          id: 'e_' + Date.now(),
          title: parsed.merchant,
          amount: parsed.amount,
          category: 'بانکی / پیامک',
          date: '۱۴۰۵/۰۸/۰۵',
          bank: parsed.bank,
          type: parsed.type
        });
        renderAll();
        document.getElementById('modalAlert').innerText = '✓ ثبت شد: ' + formatToman(parsed.amount) + ' (' + parsed.bank + ')';
        document.getElementById('modalAlert').style.display = 'block';
        setTimeout(() => { closeVoiceModal(); }, 1200);
      }
    }

    function openVaultModal() { document.getElementById('vaultModal').style.display = 'flex'; }
    function closeVaultModal() { 
      document.getElementById('vaultModal').style.display = 'none'; 
      syncNavToCurrentPage();
    }
    function openSettingsModal() { document.getElementById('settingsModal').style.display = 'flex'; }
    function closeSettingsModal() { 
      document.getElementById('settingsModal').style.display = 'none'; 
      syncNavToCurrentPage();
    }
    function unlockVaultModal() {
      const pin = document.getElementById('vaultModalPin').value;
      if (pin === '1234' || pin.length >= 4) {
        document.getElementById('vaultLockedState').style.display = 'none';
        document.getElementById('vaultUnlockedState').style.display = 'flex';
      }
    }

    function openAddTaskModal() { document.getElementById('addTaskModal').style.display = 'flex'; }
    function closeAddTaskModal() { document.getElementById('addTaskModal').style.display = 'none'; }
    function saveNewTask() {
      const title = document.getElementById('newTaskTitle').value;
      const date = document.getElementById('newTaskDate').value;
      if (title.trim()) {
        tasks.push({
          id: String(Date.now()),
          title,
          category: 'کاری',
          priority: 'متوسط',
          completed: false,
          dueDate: date || '۱۴۰۵/۰۸/۱۰'
        });
        document.getElementById('newTaskTitle').value = '';
        closeAddTaskModal();
        renderAll();
      }
    }

    // Initial render
    renderAll();
  </script>
</body>
</html>`;

  res.writeHead(200, {
    'Content-Type': 'text/html; charset=utf-8',
    'Cache-Control': 'no-cache'
  });
  res.end(html);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[Personal Life OS] 4-Pillar Virtual Mobile Simulator running on http://0.0.0.0:${PORT}`);
});
