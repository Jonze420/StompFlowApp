# StompFlow — Android App

A fully offline guitar FX pedalboard, drum machine, and tuner, wrapped as a
native Android app. There is no server dependencies at runtime —
the entire app is one bundled HTML/JS file
(`app/src/main/assets/StompFlow.html`) served locally through
`WebViewAssetLoader`.

## What's in this folder

```
StompFlowApp/
├── .github/workflows/build-apk.yml   # CI: builds + signs the release APK/AAB
├── app/
│   ├── build.gradle                  # app module config + release signing
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/stompflow/app/MainActivity.java
│       ├── res/                      # theme, layout, adaptive icon
│       └── assets/StompFlow.html     # <- the entire app lives here
├── build.gradle                      # root Gradle config
├── settings.gradle
├── gradle.properties
└── gradle/wrapper/gradle-wrapper.properties
```

## Building from your phone (no local Android Studio needed)

This repo is designed around GitHub Actions as the build environment, since
that's the whole point — you don't need a laptop or IDE.

1. Push this folder to `github.com/<you>/StompFlowApp` (or update your
   existing repo with these files).
2. In the repo, go to **Settings → Secrets and variables → Actions** and add
   four **repository secrets**:
   - `KEYSTORE_BASE64` — your signing keystore, base64-encoded
   - `KEY_ALIAS`
   - `KEY_PASSWORD`
   - `STORE_PASSWORD`
3. Push to `main` (or run the workflow manually from the **Actions** tab —
   "Build StompFlow APK" → **Run workflow**).
4. When the run finishes, open it and download the `StompFlow-release-apk`
   and `StompFlow-release-aab` artifacts. The AAB is what you'd upload to
   Google Play; the APK can be installed directly on a device.

### Don't have a keystore yet?

Generate one once (this can be done from any machine with a JDK, or a
temporary GitHub Codespace) and keep it somewhere safe — you'll need the
*same* keystore for every future release, or existing installs can't update:

```bash
keytool -genkeypair -v -keystore stompflow.keystore \
  -alias stompflow -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 stompflow.keystore   # paste this output into KEYSTORE_BASE64
```

## Updating the app itself

Everything the user sees and interacts with — the FX chain, drum machine,
tuner, presets, settings — lives in `app/src/main/assets/StompFlow.html`.
That file is a single self-contained HTML/CSS/JS document with no build
step: edit it directly (from GitHub's web editor or the GitHub mobile app
works fine) and push. The next CI run bundles your changes into a fresh
signed build automatically.

## Why WebViewAssetLoader instead of a plain file:// URL

Chromium's WebView engine treats `file://` pages as a non-secure context on
many Android versions, which silently blocks microphone access
(`getUserMedia`). `MainActivity` instead serves the bundled asset over a
synthetic `https://appassets.androidplatform.net/assets/...` origin via
`androidx.webkit.WebViewAssetLoader`, which behaves like a normal secure
origin and reliably allows mic capture for the tuner and live guitar input.

## Notes

- Minimum supported Android version is 8.0 (API 26).
- The app requests `RECORD_AUDIO` on launch for the live guitar input and
  tuner; drum machine and file playback work without it.
- Presets and drum patterns persist in the WebView's local storage on-device
  — there's no account system and nothing is uploaded anywhere.
- The Google Fonts `<link>` in `StompFlow.html` is best-effort: if the device
  is offline it just falls back to the system monospace/sans-serif fonts,
  nothing breaks.
