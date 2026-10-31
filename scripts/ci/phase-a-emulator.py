"""Phase A integration checks. Mutating HOME is permitted only on one verified API30 emulator."""
import json, os, pathlib, re, subprocess, time
out=pathlib.Path("artifacts/phase-a"); out.mkdir(parents=True,exist_ok=True)
pkg="com.wittawat.cleanyourtv.launcher"
component=pkg+"/com.wittawat.cleanyourtv.preview.NativePreviewActivity"
apk="launcher/phase-a-launcher/build/outputs/apk/debug/phase-a-launcher-debug.apk"
testapk="launcher/phase-a-launcher/build/outputs/apk/androidTest/debug/phase-a-launcher-debug-androidTest.apk"
def call(*args):
    return subprocess.check_output(args,stderr=subprocess.STDOUT).decode().strip()
devices=[line.split()[0] for line in call("adb","devices").splitlines()[1:] if line.endswith("\tdevice")]
assert len(devices)==1 and devices[0].startswith("emulator-"), devices
serial=devices[0]
def adb(*args): return call("adb","-s",serial,*args)
def sh(*args): return adb("shell",*args)
assert sh("getprop","ro.kernel.qemu")=="1"
assert sh("getprop","ro.build.version.sdk")=="30"
def home(): return sh("cmd","package","resolve-activity","--brief","-a","android.intent.action.MAIN","-c","android.intent.category.HOME").splitlines()[-1]
old=home()
assert "/" in old and pkg not in old and "ResolverActivity" not in old, old
disabled=sh("pm","list","packages","-d")
recovery="scripts/phase-a-recovery.py"
baseline=str(out/"recovery-private.json")
call("python3",recovery,"backup","--serial",serial,"--file",baseline)
aapt=os.path.join(os.environ["ANDROID_HOME"],"build-tools","34.0.0","aapt")
manifest=call(aapt,"dump","xmltree",apk,"AndroidManifest.xml")
(out/"manifest.txt").write_text(manifest)
assert "android.intent.category.HOME" in manifest
assert "uses-permission" not in manifest and "E: service " not in manifest and "E: receiver " not in manifest

(out/"baseline.json").write_text(json.dumps({"home":old,"disabled":disabled,"sdk":30},indent=2))
def screenshot(name):
    (out/name).write_bytes(subprocess.check_output(["adb","-s",serial,"exec-out","screencap","-p"]))
def resumed():
    return sh("dumpsys","activity","activities")
def launch(): return sh("am","start","-W","-n",component)
def assert_home():
    text=resumed()
    assert any(pkg in line for line in text.splitlines() if "mResumedActivity" in line), text[-4000:]
def dump():
    sh("uiautomator","dump","/sdcard/phase-a.xml")
    return sh("cat","/sdcard/phase-a.xml")
checks=[]
blocked=[]
default_supported=False
try:
    adb("install","-r",apk); adb("install","-r",testapk)
    assert home()==old, "Installing a HOME candidate changed the default"
    checks.append("Installation preserves original Default Home")
    launch(); time.sleep(2); assert_home(); screenshot("01-home.png")
    tests=sh("am","instrument","-w",pkg+".test/com.wittawat.cleanyourtv.preview.LauncherUiTest")
    (out/"instrumentation.txt").write_text(tests)
    assert "OK (1 test)" in tests and "FAILURES" not in tests, tests
    checks.append("Reorder: held OK release, save, cancel and pause cancellation")
    launch(); sh("input","keyevent","3"); time.sleep(1)
    assert any(old.split("/")[0] in line for line in resumed().splitlines() if "mResumedActivity" in line)
    checks.append("Before opt-in: Home returns to stock launcher")
    (out/"select-home.txt").write_text(sh("cmd","package","set-home-activity","--user","0",component))
    (out/"home-candidates.txt").write_text(sh("cmd","package","query-activities","--brief","-a","android.intent.action.MAIN","-c","android.intent.category.HOME"))
    (out/"home-role.txt").write_text(sh("dumpsys","role"))
    sh("input","keyevent","3"); time.sleep(2)
    (out/"home-after-selection.txt").write_text(home()+"\n"+resumed())
    default_supported = home()==component and any(pkg in line for line in resumed().splitlines() if "mResumedActivity" in line)
    if default_supported:
        checks.append("Opt-in: Home launches Phase A")
        sh("am","start","-a","android.settings.SETTINGS"); sh("input","keyevent","3"); time.sleep(1); assert_home()
        checks.append("Home returns from Settings")
        sh("am","force-stop",pkg); sh("input","keyevent","3"); time.sleep(1); assert_home()
        checks.append("Home recovers after app process stops")
    else:
        blocked.append("Default Home: role selection succeeds but stock TV launcher priority 2 overrides Phase A priority 0; no packages disabled")
        call("python3",recovery,"restore","--serial",serial,"--file",baseline)
        launch(); time.sleep(1); assert_home()
    # Move focus up from app row and use the labelled Inputs button through UIAutomator bounds.
    xml=dump()
    import xml.etree.ElementTree as ET
    def click_label(label):
        root=ET.fromstring(dump())
        node=next(n for n in root.iter("node") if n.get("content-desc")==label)
        nums=list(map(int,re.findall(r"\d+",node.get("bounds"))))
        sh("input","tap",str((nums[0]+nums[2])//2),str((nums[1]+nums[3])//2))
        time.sleep(1)
    click_label("HDMI / Inputs")
    assert "HDMI / Inputs" in dump()
    screenshot("02-inputs-fallback.png"); sh("input","keyevent","4")
    checks.append("HDMI discovery: no physical ports => safe visible fallback")
    click_label("All Apps"); assert "All Apps" in dump(); screenshot("03-all-apps.png"); sh("input","keyevent","4")
    checks.append("All Apps dialog opens")
    # Pin an installed app through the user-facing menu and verify after process death.
    click_label("All Apps")
    root=ET.fromstring(dump())
    entries=[n for n in root.iter("node") if n.get("resource-id")=="android:id/text1"
             and n.get("text") not in ("YouTube","Netflix","Browser","Prime Video","Disney+","Settings")]
    if not entries:
        raise AssertionError("No independent installed app available for pinning test")
    chosen=entries[0]; chosen_label=chosen.get("text")
    def tap_node(node):
        nums=list(map(int,re.findall(r"\\d+",node.get("bounds"))))
        sh("input","tap",str((nums[0]+nums[2])//2),str((nums[1]+nums[3])//2)); time.sleep(1)
    tap_node(chosen)
    action=next(n for n in ET.fromstring(dump()).iter("node") if n.get("text")=="ปักหมุดในหน้า Home")
    tap_node(action)
    sh("am","force-stop",pkg); launch(); time.sleep(1)
    for _ in range(8): sh("input","keyevent","22")
    assert chosen_label in dump(), "Pinned app missing after restart"
    checks.append("Installed app pin survives process restart")
    # Returning Home repositions focus and the screen before the next toolbar action.
    sh("input","keyevent","3"); time.sleep(1)

    sh("input","keyevent","223"); time.sleep(2)
    assert "mWakefulness=Asleep" in sh("dumpsys","power"), sh("dumpsys","power")[-2000:]
    sh("input","keyevent","224"); time.sleep(2)
    assert "mWakefulness=Awake" in sh("dumpsys","power")
    assert_home()
    checks.append("Android sleep/wake preserves running Launcher activity (OEM standby still requires real TV)")
    adb("reboot")
    for _ in range(90):
        time.sleep(2)
        try:
            if sh("getprop","sys.boot_completed")=="1": break
        except subprocess.CalledProcessError: pass
    else: raise AssertionError("Emulator reboot timed out")
    sh("input","keyevent","224"); time.sleep(3)
    if default_supported:
        assert home()==component; assert_home()
        checks.append("Reboot retains chosen Home")
    else:
        assert home()==old
        blocked.append("Reboot into Phase A cannot pass while stock Home overrides the selected role")
        launch(); time.sleep(1); assert_home()
    screenshot("04-home-after-reboot.png")
finally:
    # Always restore the exact original component, even if an assertion fails.
    (out/"restore.txt").write_text(call("node","scripts/phase-a-recovery.cjs","restore","--serial",serial,"--file",baseline))
    call("python3",recovery,"verify","--serial",serial,"--file",baseline)
    sh("am","start","-a","android.intent.action.MAIN","-c","android.intent.category.HOME")
    time.sleep(1)
    assert home()==old
    assert sh("pm","list","packages","-d")==disabled
    (out/"logcat.txt").write_text(sh("logcat","-d","-v","time"))
checks.append("Exact original Home restored; disabled package list unchanged")
(out/"result.json").write_text(json.dumps({"status":"PASS" if not blocked else "BLOCKED","checks":checks,"blocked":blocked,"limitations":["Physical HDMI switching, real remote and Hisense standby require real TV test"]},indent=2))
print(json.dumps({"checks":checks,"blocked":blocked},indent=2))
if blocked: raise SystemExit(2)
