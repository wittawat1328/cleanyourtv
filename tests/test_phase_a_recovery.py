"""Recovery guards without a physical device."""
import contextlib, io, json, pathlib, runpy, sys, tempfile, unittest
from unittest.mock import patch
SCRIPT=pathlib.Path(__file__).resolve().parents[1]/"scripts"/"phase-a-recovery.py"
class PhaseARecoveryTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.file=pathlib.Path(self.tmp.name)/"home.json"
        self.calls=[]
        self.data={"schema":1,"serial":"test-emulator","user":"0","fingerprint":"test-build",
                   "home":"org.example.home/.Home","disabled":["package:org.example.disabled"]}
    def adb(self, argv, **kwargs):
        cmd=argv[4:]; self.calls.append(cmd)
        if cmd==["am","get-current-user"]: return b"0\n"
        if cmd==["getprop","ro.build.fingerprint"]: return b"test-build\n"
        if cmd[:3]==["cmd","package","resolve-activity"]: return b"org.example.home/.Home\n"
        if cmd[:3]==["cmd","package","query-activities"]: return b"1 activities found:\n  org.example.home/.Home\n"
        if cmd[:4]==["pm","list","packages","-d"]: return b"package:org.example.disabled\n"
        return b"\n"
    def run_tool(self,operation):
        argv=[str(SCRIPT),operation,"--serial","test-emulator","--file",str(self.file)]
        with patch.object(sys,"argv",argv), patch("subprocess.check_output",side_effect=self.adb), contextlib.redirect_stdout(io.StringIO()):
            runpy.run_path(str(SCRIPT),run_name="__main__")
    def store(self): self.file.write_text(json.dumps(self.data))
    def assert_no_home_mutation(self):
        self.assertFalse(any("set-home-activity" in cmd for cmd in self.calls))
    def test_backup_is_read_only_and_refuses_overwrite(self):
        self.run_tool("backup"); self.assertTrue(self.file.exists()); self.assert_no_home_mutation()
        original=self.file.read_bytes()
        with self.assertRaises(SystemExit): self.run_tool("backup")
        self.assertEqual(original,self.file.read_bytes())
    def test_wrong_device_is_rejected(self):
        self.data["serial"]="different-device"; self.store()
        with self.assertRaises(SystemExit): self.run_tool("restore")
        self.assert_no_home_mutation()
    def test_changed_firmware_is_rejected(self):
        self.data["fingerprint"]="other-build"; self.store()
        with self.assertRaises(SystemExit): self.run_tool("restore")
        self.assert_no_home_mutation()
    def test_missing_home_is_rejected(self):
        self.data["home"]="org.missing/.Home"; self.store()
        with self.assertRaises(SystemExit): self.run_tool("restore")
        self.assert_no_home_mutation()
    def test_restore_changes_only_exact_saved_home_for_user(self):
        self.store(); self.run_tool("restore")
        mutation=[cmd for cmd in self.calls if "set-home-activity" in cmd]
        self.assertEqual([["cmd","package","set-home-activity","--user","0","org.example.home/.Home"]],mutation)
        self.assertFalse(any("disable-user" in cmd or "uninstall" in cmd or "enable" in cmd for cmd in self.calls))
if __name__=="__main__": unittest.main()
