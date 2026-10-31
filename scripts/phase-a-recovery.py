"""Backup / restore Default HOME only. Never disables packages.
Usage: python scripts/phase-a-recovery.py backup --serial <tv-serial> --file backup/phase-a-home.json
       python scripts/phase-a-recovery.py restore --serial <tv-serial> --file backup/phase-a-home.json
Use backup BEFORE changing Default Home on the real TV. Keep the JSON private.
"""
import argparse, json, pathlib, subprocess
p=argparse.ArgumentParser(description=__doc__)
p.add_argument("operation", choices=["backup","restore","verify"])
p.add_argument("--serial",required=True)
p.add_argument("--file",required=True)
p.add_argument("--adb",default="adb")
a=p.parse_args()
def sh(*args):
    return subprocess.check_output([a.adb,"-s",a.serial,"shell",*args],stderr=subprocess.STDOUT).decode().strip()
def current_home(user):
    return sh("cmd","package","resolve-activity","--user",user,"--brief","-a","android.intent.action.MAIN","-c","android.intent.category.HOME").splitlines()[-1]
file=pathlib.Path(a.file)
user=sh("am","get-current-user")
if a.operation=="backup":
    if file.exists(): raise SystemExit("Backup exists: choose a new path. Never overwrite the original baseline.")
    home=current_home(user)
    if "/" not in home or "ResolverActivity" in home or home.startswith("com.wittawat.cleanyourtv.launcher/"):
        raise SystemExit("No unambiguous stock Home found. Select the original Home first.")
    data={"schema":1,"serial":a.serial,"user":user,"home":home,
          "fingerprint":sh("getprop","ro.build.fingerprint"),
          "properties":sh("getprop"),
          "disabled":sh("pm","list","packages","-d","--user",user).splitlines(),
          "packages":sh("pm","list","packages","--user",user).splitlines(),
          "settings_system":sh("settings","--user",user,"list","system"),
          "settings_secure":sh("settings","--user",user,"list","secure"),
          "settings_global":sh("settings","list","global")}
    file.parent.mkdir(parents=True,exist_ok=True)
    with file.open("x",encoding="utf-8") as f: json.dump(data,f,ensure_ascii=False,indent=2)
    print("Original Home and read-only system baseline saved. Keep this file private.")
else:
    data=json.loads(file.read_text(encoding="utf-8"))
    if data.get("schema")!=1 or data["serial"]!=a.serial or data["user"]!=user:
        raise SystemExit("Backup belongs to another device/user.")
    if data["fingerprint"]!=sh("getprop","ro.build.fingerprint"):
        raise SystemExit("Firmware changed: inspect the saved Home manually before restoring.")
    old=data["home"]
    homes=sh("cmd","package","query-activities","--brief","--user",user,"-a","android.intent.action.MAIN","-c","android.intent.category.HOME").splitlines()
    if old not in [line.strip() for line in homes]: raise SystemExit("Original Home is unavailable. Restore refused; no package changes made.")
    if a.operation=="restore":
        print(sh("cmd","package","set-home-activity","--user",user,old))
        sh("am","start","--user",user,"-a","android.intent.action.MAIN","-c","android.intent.category.HOME")
    if current_home(user)!=old: raise SystemExit("Default Home differs from saved baseline.")
    now=sh("pm","list","packages","-d","--user",user).splitlines()
    if sorted(now)!=sorted(data["disabled"]): raise SystemExit("Disabled packages differ from baseline; inspect manually.")
    print("PASS: Original Default Home and disabled package baseline match.")
