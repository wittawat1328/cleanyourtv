# Hisense All Apps + Favorites: Phase A
Native module discovers only Android MAIN+LEANBACK_LAUNCHER / MAIN+LAUNCHER exported enabled activities and excludes its own package. No QUERY_ALL_PACKAGES permission and no exposure of background services.

Favorites display installed launchable apps, may be reordered by holding OK for 2 seconds, and can be added/removed via All Apps without uninstalling. Initial priorities are YouTube, Netflix, a discovered browser in slot 3, Prime Video and Disney+ if present. Unavailable apps do not produce fake tiles. Changes refresh on activity resume and package add/remove broadcasts.

The preview remains an independent, non-HOME APK with no privileged permissions; no Google TV or Hisense OEM apps are disabled. Keep SmartSense, AIoT, setupwraith, Google TV launcher, settings, Wi-Fi and HDMI services unchanged. Physical TV installation requires separate user approval.
