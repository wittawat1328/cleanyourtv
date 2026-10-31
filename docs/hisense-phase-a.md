# Hisense Phase A — Launcher

Target: Hisense SmartTV 4K FFM / Android 11.
Build: `cd launcher && ./gradlew -PincludePhaseA=true :phase-a-launcher:assembleDebug`
App ID: `com.wittawat.cleanyourtv.launcher` / version 0.2.1-phase-a-wallpaper.
The earlier Native Preview remains a separate application.

## Personal Home wallpaper
Place a local JPEG at `launcher/phase-a-launcher/src/main/assets/home_wallpaper.jpg` before building.
The asset is excluded from Git to keep personal photos out of the public repository.
The APK bundles the image for offline use. The background scales to cover the screen without
stretching; cached translucent gradients keep labels readable. No image is downloaded at runtime.
A generic build without the personal asset falls back to the dark background.

## Home and apps
- Offline native Home with remote focus.
- All Apps discovers installed launchable apps. Select an app to open it or pin/unpin it.
- Hold OK for 2 seconds on a favourite. Left/right moves it. Release the held OK first.
  A second OK saves. Back cancels. Leaving Home cancels unsaved changes.
- App order survives process restart. Removed app pins are omitted.
- HDMI lists system-provided HDMI inputs and asks Android to open the selected input.
  When the firmware exposes no compatible input it offers TV Settings and the remote Input button.
- Home/Recovery opens Android's Home selection settings or another enabled Home temporarily.
  Opening the stock Home temporarily does not change the default.
- No boot receiver or accessibility service. Android manages the selected Home.
  Power, volume and vendor remote shortcuts are passed to Android.

## Windows recovery without Python
This computer already has Node.js. Use the dependency-free Node tool with the same baseline schema:
```sh
node scripts/phase-a-recovery.cjs backup --serial <tv-serial> --file backup/phase-a-home.json --adb "<absolute-adb-path>"
node scripts/phase-a-recovery.cjs restore --serial <tv-serial> --file backup/phase-a-home.json --adb "<absolute-adb-path>"
node scripts/phase-a-recovery.cjs verify --serial <tv-serial> --file backup/phase-a-home.json --adb "<absolute-adb-path>"
```
The emulator integration exercises Node restore and Python verification against the original baseline.

## Backup before the real TV trial
Real TV installation and Default Home changes require the user's approval after emulator results.
Before installing or selecting the new Home:
```sh
python scripts/phase-a-recovery.py backup --serial <tv-serial> --file backup/phase-a-home.json
```
The backup includes the exact current Home component, active user, firmware, package lists
and settings snapshots. It contains private device data: keep it locally and do not commit/share it.
This is a Home/settings baseline, not a full firmware or app-data image.

## Restore
Using the same TV connection and the original backup:
```sh
python scripts/phase-a-recovery.py restore --serial <tv-serial> --file backup/phase-a-home.json
python scripts/phase-a-recovery.py verify --serial <tv-serial> --file backup/phase-a-home.json
```
The tool checks device/user/firmware and availability of the saved Home.
It changes only Default Home, opens Home and verifies the original disabled-package list.
It never disables/uninstalls/enables any package. It refuses to overwrite an existing baseline.

On the TV: Settings > Apps > Default apps > Home app > original Google TV Home.
If that menu is unavailable use the saved ADB restore procedure.
Keep the original Google TV Home, Settings, input services, SmartSense, AIoT and Setup Wizard enabled.
This phase performs no debloating.

## Emulator limitation found during Phase A
The API30 Google Android TV image gives the stock TV launcher HOME intent priority 2.
Third party apps have priority 0. Setting the HOME role reports success but the stock
launcher still receives Home. This is a blocked end-to-end check, never a full PASS.
The integration runner continues independent UI/sleep/recovery tests, records BLOCKED,
restores the exact original Home, and exits nonzero.
No system packages are disabled to obtain a passing result.
A real Hisense must be inspected for the same behaviour before authorizing Default Home.
The Home menu includes a status check to show which Home Android actually resolves.

## Real TV acceptance checklist (after approval)
1. Back up while the original Home is still selected; verify Wi-Fi/ADB access.
2. Install the separate Phase A APK; open it as an ordinary app first.
3. Test D-pad edges, OK hold/save/cancel, All Apps pin/unpin and application launches.
4. Press the real Home/Volume/Input/Settings remote buttons.
5. With explicit approval select Phase A as Home through the system menu.
6. Test Home from streaming apps, every used HDMI input, standby/wake and a reboot.
7. Restore original Home using the saved baseline and verify Home/Wi-Fi/inputs.
8. Choose whether to keep Phase A only after both directions work.
9. Close ADB/debugging after the accepted setup.

Emulator success covers Android behaviour only. HDMI switching, CEC, the Hisense remote
and OEM standby must be tested on the real TV and are not certified by the emulator.
