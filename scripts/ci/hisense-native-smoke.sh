#!/usr/bin/env bash
# CI guard: strictly emulator-only. APK installs and UI checks must not affect HOME or OEM services.
set -Eeuo pipefail
mkdir -p artifacts/hisense-native
PKG=com.wittawat.cleanyourtv.preview
APK=launcher/entertainment-preview/build/outputs/apk/debug/entertainment-preview-debug.apk
test -s "$APK" || { echo "APK missing" >&2; exit 1; }
COUNT=$(adb devices | awk '$2=="device"{n++} END{print n+0}')
SERIAL=$(adb devices | awk '$2=="device"{print $1}')
if [[ "$COUNT" != 1 || "$SERIAL" != emulator-* ]];then
    echo "ABORT: exactly one emulator and no physical devices required" >&2;exit 1
fi
QEMU=$(adb -s "$SERIAL" shell getprop ro.kernel.qemu | tr -d '\r')
[[ "$QEMU" == 1 ]] || { echo "ABORT: not a virtual Android device" >&2; exit 1; }
SDK=$(adb -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')
echo "Android SDK=$SDK" | tee artifacts/hisense-native/device.txt
[[ "$SDK" == 30 ]] || { echo "ABORT: need Android 11 SDK 30" >&2; exit 1; }
adb -s "$SERIAL" shell 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME' > artifacts/hisense-native/home-before.txt
adb -s "$SERIAL" shell pm list packages -d | LC_ALL=C sort > artifacts/hisense-native/disabled-before.txt
adb -s "$SERIAL" install -r "$APK" | tee artifacts/hisense-native/install.txt
START=$(adb -s "$SERIAL" shell am start -W -n "$PKG/.NativePreviewActivity")
echo "$START" | tee artifacts/hisense-native/launch.txt
echo "$START" | grep -q 'Status: ok'
sleep 4
adb -s "$SERIAL" shell pidof "$PKG" > artifacts/hisense-native/pid.txt
test -s artifacts/hisense-native/pid.txt
adb -s "$SERIAL" exec-out screencap -p > artifacts/hisense-native/native-home.png
adb -s "$SERIAL" shell input keyevent 22
adb -s "$SERIAL" shell input keyevent 22
adb -s "$SERIAL" shell input keyevent 20
sleep 2
adb -s "$SERIAL" exec-out screencap -p > artifacts/hisense-native/native-dpad.png
adb -s "$SERIAL" shell 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME' > artifacts/hisense-native/home-after.txt
adb -s "$SERIAL" shell pm list packages -d | LC_ALL=C sort > artifacts/hisense-native/disabled-after.txt
diff -u artifacts/hisense-native/home-before.txt artifacts/hisense-native/home-after.txt
diff -u artifacts/hisense-native/disabled-before.txt artifacts/hisense-native/disabled-after.txt
python3 - <<'PY'
import struct,pathlib
folder=pathlib.Path('artifacts/hisense-native')
for name in ('native-home.png','native-dpad.png'):
    data=(folder/name).read_bytes()
    assert len(data)>1024 and data[:8]==b'\x89PNG\r\n\x1a\n',name
    w,h=struct.unpack('>II',data[16:24])
    assert w>h and h>=480,(name,w,h)
    print(name,w,h)
(folder/'safety-result.txt').write_text('PASS: API 30; HOME unchanged; disabled system packages unchanged; native Android APK launched\n')
PY
adb -s "$SERIAL" logcat -d -t 250 -v time > artifacts/hisense-native/logcat-tail.txt || true
echo 'PASS: Hisense native app and emulator system-safety smoke test'
