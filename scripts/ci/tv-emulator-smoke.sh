#!/usr/bin/env bash
# CI-only virtual TV test: abort if any physical ADB device is seen.
set -Eeuo pipefail
mkdir -p artifacts/tv-emulator
APK=launcher/app/build/outputs/apk/debug/app-debug.apk
test -s "$APK"
COUNT=$(adb devices | awk '$2=="device"{n++} END{print n+0}')
SERIAL=$(adb devices | awk '$2=="device"{print $1}')
if [[ "$COUNT" != 1 || "$SERIAL" != emulator-* ]];then echo "FAIL SAFE: emulator only" >&2;exit 1;fi
QEMU=$(adb -s "$SERIAL" shell getprop ro.kernel.qemu | tr -d '\r')
[[ "$QEMU" == 1 ]] || { echo "FAIL SAFE: physical device" >&2;exit 1; }
on_exit(){ code=$?; adb -s "$SERIAL" logcat -d -t 250 > artifacts/tv-emulator/logcat-tail.txt 2>/dev/null || true; echo "exit_code=$code" > artifacts/tv-emulator/result.txt; }
trap on_exit EXIT
adb -s "$SERIAL" install -r "$APK" | tee artifacts/tv-emulator/install.txt
adb -s "$SERIAL" shell am start -W -n com.gathin.tvhome/.MainActivity | tee artifacts/tv-emulator/launch.txt
sleep 8
adb -s "$SERIAL" shell pidof com.gathin.tvhome > artifacts/tv-emulator/pid.txt
test -s artifacts/tv-emulator/pid.txt
adb -s "$SERIAL" exec-out screencap -p > artifacts/tv-emulator/home.png
for k in 22 20 21 19;do adb -s "$SERIAL" shell input keyevent "$k";sleep 1;done
adb -s "$SERIAL" exec-out screencap -p > artifacts/tv-emulator/after-dpad.png
python3 - <<'PY'
import pathlib,struct
for name in ('home.png','after-dpad.png'):
 d=(pathlib.Path('artifacts/tv-emulator')/name).read_bytes()
 assert len(d)>1024 and d[:8]==b'\x89PNG\r\n\x1a\n'
 w,h=struct.unpack('>II',d[16:24])
 assert w>h and h>=480,(w,h)
 print(name,w,h)
PY
echo "PASS: virtual Android TV"
