# Entertainment Home V1 — Hisense SmartTV 4K FFM (Android 11)

Design direction confirmed by the user:
- Apple TV-inspired, clean and **dark**.
- Emphasis **80% entertainment discovery / 20% app launcher** (content priority, not a literal pixel allocation).
- Default app order: YouTube, Netflix, **Browser (3rd)**, Prime Video, Disney+, Settings.
- HDMI / Inputs: compact icon at top right, **not** a large app tile.
- Big hero artwork and landscape movie-series cards, episode navigation, visible D-pad focus and smooth lightweight motion.
- Future long-press-to-move behavior: hold OK **2 seconds**, Left/Right to move, OK to save, Back to cancel. Browser mock supports it already; native Android TV code will require real-remote verification.

## Deliverable and boundaries
This commit changes **preview/index.html**, **preview/smoke.cjs** and this spec, not native launcher Java code. No system packages, default HOME, streaming platform integrations or physical TV connections are touched.

All movie names, posters, metadata and episode counts are **fictional demonstrations**. Netflix, Prime Video and Disney+ do not automatically expose cross-app recommendations or Continue Watching feeds to this standalone launcher. Real artwork/feeds require permissible APIs, data providers or user-linked services, to be researched and approved separately.

The HTML prototype is a visual intent and interaction test. To make it production-ready for Hisense:
1. Review and approve preview on phone first.
2. Translate layout and focus/navigation to existing lightweight Java launcher.
3. Confirm available RAM/performance and protect remote keys, Inputs, Settings and original HOME.
4. Verify on virtual Android TV, then request permission for non-default sideload on physical Hisense before any device connection.

Source inherited from CleanYourTV with original MIT license. Physical device remains unchanged.
