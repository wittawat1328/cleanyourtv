# Native Entertainment Preview V1 — Hisense SmartTV 4K FFM / Android 11

This module is a **native Android (Java) TV app**, NOT a default launcher replacement yet.

## What's inside
- Cinematic dark Home, custom-painted fictional Hero illustration, horizontal content, recommendations and episodes.
- D-pad navigation with small focus scale animations.
- Apps in this order: YouTube, Netflix, **Browser #3**, Prime Video, Disney+, Settings.
- Hold TV remote OK 2 seconds on an app: **release OK to remain in move mode**, Left/Right move, OK save, Back cancel. Order saved in private SharedPreferences. Long-press release must not immediately exit move mode.
- Compact HDMI/Inputs top icon and Settings icon. HDMI intentionally displays a safety dialog until device-specific input routing is verified.
- **All Apps** top icon discovers installed apps with exported launchable activities using Android 11 package queries, including newly installed apps. The user explicitly chooses Browser from installed apps once and the chosen package is stored locally.
- YouTube / Netflix / Prime / Disney+ launch only when their **explicit package** is installed. Browser deliberately prompts for the missing app identity rather than guessing.
- All show titles, poster gradients and episode details are fictional; no third-party streaming feeds, ads, remote API calls or video auto-play.

## Safety boundaries: REQUIRED
1. The APK ID is **com.wittawat.cleanyourtv.preview** and its AndroidManifest has only MAIN / LAUNCHER / LEANBACK_LAUNCHER; **no HOME** intent.
2. It has **zero uses-permission entries**; no AccessibilityService, WRITE_SECURE_SETTINGS, receivers, providers or privileged operations.
3. This is a completely separate Gradle module. The upstream \`:app\` and \`launcher/build-and-install.sh\` are unchanged.
4. Never execute \`launcher/build-and-install.sh\`, \`tools/disable.sh\`, \`tools/theme.sh\` or any recovery commands on real Hisense hardware as part of preview validation.
5. Never disable \`com.hisense.tv.smartsense\`, \`com.vt.tv.smartsense.ui\`, or \`com.vt.tv.aiot\`.
6. Keep \`com.google.android.tungsten.setupwraith\`, \`com.google.android.apps.tv.launcherx\`, Wi-Fi setup and OEM Settings enabled. Do not force any service disable override or stock launcher swap.
7. Keep \`com.mediatek.*\`, \`com.hisense.tv.hitvinput\`, \`com.mediatek.tv.service\`, \`com.hisense.tv.customerlauncher\`, and OTA/Google Cast/streaming services enabled.

## CI
Run \`./gradlew :entertainment-preview:assembleDebug :entertainment-preview:testDebugUnitTest :entertainment-preview:lintDebug\`.
APK: \`launcher/entertainment-preview/build/outputs/apk/debug/entertainment-preview-debug.apk\`.
A CI emulator can start \`com.wittawat.cleanyourtv.preview/.NativePreviewActivity\` explicitly and screenshot.
Never use emulator passing as proof of actual Hisense hardware compatibility.

## Go / no-go gates
- Audit merged manifest for the lack of HOME and privileged permissions.
- Pass automated unit/lint/build and Android TV virtual device checks (API 30 requested).
- Measure startup, focus transitions, Back, Home, installed app launches and standby behavior.
- Confirm the **actual Browser package** and Hisense HDMI routing separately. No guessing.
- Request explicit user approval before even a **non-default APK install on real TV**.
- Debloat (Phase B) requires a different explicit approval and rollback plan.

## Before the first physical-TV test
- Download only the `entertainment-preview-debug.apk` extracted from a **successful** `Hisense Native Android 11 Safety` Action run.
- Install as an ordinary application from the TV's file manager (or an authorized sideload), without running any disable scripts or setting a default launcher.
- Check that original Home, Settings, Wi-Fi, volume, HDMI and remote app hotkeys work before and after the preview.
- Launch `CleanYourTV Native Preview` from the original Home. Press **Back** to close it, press **Home** to return to the original Home.
- Test D-pad left/right/down/up, hold OK to reorder, All Apps list, and Browser selection. If Home behaves differently, stop and remove the preview through Settings > Apps, then do not proceed to launcher replacement.
- The preview is not an approved production Home replacement and has no debloat functionality.
