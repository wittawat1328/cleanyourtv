# Hisense Phase A — Launcher

Target: Hisense SmartTV 4K FFM / Android 11.
Build: `cd launcher && ./gradlew -PincludePhaseA=true :phase-a-launcher:assembleDebug`
App ID: `com.wittawat.cleanyourtv.launcher` / version 0.2-phase-a.
The earlier Native Preview remains a separate application.

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
