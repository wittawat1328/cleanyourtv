# Safe Preview + Emulator (mobile-only development)

This is a fork-only test layer. Existing CleanYourTV source, cleanup scripts and TV settings remain unchanged.

## How to see results on a phone
1. Open your fork's GitHub Actions. If prompted, enable Actions for the fork.
2. Open Safe TV Preview and Emulator on the feature/safe-preview-emulator branch.
3. Download artifact tv-web-preview (interactive offline HTML + browser screenshots) and tv-emulator-results (actual Android TV emulator screenshots).
4. Open preview/index.html locally for interactive D-pad controls and themes.

## Safety
- The HTML mockup is offline-only; it sends no requests and never connects to any TV.
- Native test runs in ephemeral virtual Android TV. ADB hard-fails unless exactly one emulator-* device has ro.kernel.qemu=1.
- Only install APK in emulator and start MainActivity explicitly. No HOME setting, no package disable/uninstall, no real-device IP, no privileged settings or root.
- Do not run tools/disable.sh or launcher/build-and-install.sh on a physical TV until explicitly approved.
- Emulator screenshots do not prove physical TV firmware and remote compatibility.
- Upstream MIT license and attribution remain intact.
