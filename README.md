# Bloom

Watch one reel. Then stop. Bloom blocks Instagram Reels doomscrolling and keeps the reels your friends send you.

**Website:** https://jakadwangdu.github.io/Bloom/
**Android APK:** https://jakadwangdu.github.io/Bloom/Bloom.apk
**Desktop extension (Chrome, Edge, Firefox):** https://jakadwangdu.github.io/Bloom/Bloom-extension.zip

## One repo, three products
```
Bloom/
├─ app/, build.gradle.kts, ...   Android app (Kotlin + Jetpack Compose)
├─ bloom-extension/              Desktop browser extension (Manifest V3)
├─ site/                         Website: index.html, sitemap.xml, robots.txt, og.png
└─ .github/workflows/build.yml   One workflow that builds and ships everything
```

## What happens on every push to main
1. GitHub builds `Bloom.apk` from `app/`.
2. It zips `bloom-extension/` into a Chrome/Edge zip and a Firefox zip.
3. It creates a GitHub Release with the APK and both zips attached.
4. It deploys `site/` plus the APK and zips to GitHub Pages, so the website's download buttons are direct links on the same domain.

One-time setup: repo **Settings -> Pages -> Source: GitHub Actions**. Keep the repo public.

## Install
- **Android:** open the APK, allow installs from your browser, follow the in-app Setup guide.
- **Chrome / Edge:** unzip `Bloom-extension.zip`, open `chrome://extensions`, turn on Developer mode, Load unpacked.
- **Firefox:** see `bloom-extension/README.md`.

## Privacy
Neither the app nor the extension sends anything anywhere. The Android app has no internet permission. See `bloom-extension/PRIVACY.md`.
