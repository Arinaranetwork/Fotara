# Fotara 2.2 - Universal Android Version Downgrade Engine & Academic Social Ecosystem
Released: 2026-10-10   Status: Released (Alpha)

## 2.2.2 Alpha - 2026-10-10
- **Unique Academic Username System (`@username`)**: Integrated Supabase `user_profiles` schema with global uniqueness verification, regex validation (`^[a-z0-9_]{3,20}$`), local offline fallback, and custom QR code generator for academic handle sharing.
- **7-Day Rate-Limit Cooldown**: Enforced strict 7-day cooldown on username changes across local SharedPreferences and remote PostgreSQL triggers, with live countdown indicator in the profile UI.
- **Universal Peer Terminology Migration**: Completely replaced legacy "Study Buddy" terminology with "Friend" and "Friends" across all UI screens, cards, headers, viewmodels, and string resources.
- **Academic Profile Modal (`UserProfileDialog`)**:
  - Horizontal gradient banner (`Brush.horizontalGradient`) with `"ACADEMIC PROFILE"`.
  - Overlapping avatar initials with real-time presence status indicator dot (Studying, In Lecture, Open to Collab, Offline).
  - Academic handle row with one-tap clipboard copy button.
  - Three stat counter tiles: Followers, Following, and Friends.
  - Contextual action buttons: Change Username and Share QR Code for self; Invite to Study, Share Notes, Follow/Unfollow, and Remove Friend for peers.
- **Friends Hub Polish & 3-Dots Menu**: Added top-bar overflow menu navigating directly to Followers, Following, My Profile, and Claim Username; centered empty state layout within viewport matching design specifications.
- **Consolidated Daily Schedule Glance Widget**: Replaced fragmented widgets with a unified responsive Schedule widget (`TimetableGlanceWidget.kt`) featuring real-time day rollover and high-contrast "None" / "No classes scheduled" empty state.
- **Class Schedule Reminder Notifications**: Added reminder settings in the schedule management pill dialog to schedule automated daily notifications alerting upcoming classes with the text `<day>`.
- **Custom Day Rollover Bounds**: Enforced duration <= 23h limit in `ScheduleManagementSheet.kt`, strictly rejecting 24-hour periods while supporting custom start and end cutoff times.
- **Anti-Procrastination Alarm Temporary State**: Gracefully disabled with 50% opacity, muted typography, and explicit `(Temporarily Disabled)` pill while maintaining layout position.
- **Modular Add-Ons Bottom Clearance**: Added 120dp bottom content padding to `PackageManagementHubScreen.kt` preventing hardware bottom navigation bar collisions, and cleared pre-packaged mock add-ons.
- **Settings Back Navigation Polish**: Eliminated jumping residue text during back navigation by applying smooth fade transitions in `SettingsScreen.kt`.
- **Release Banner Artwork Recognition**: In-app updater dynamically detects GitHub release banner artwork and binds target version correctly during rollback downloads.
- **Darkened Action Sheets & Note Cards**: Deepened quick action bottom sheets and note cards to near-black tokens (`#0F1422` / `HomeCardSurface` / `#0D1220`).


- **Universal Android Version Downgrade Engine**: Solved Android OS's native `INSTALL_FAILED_VERSION_DOWNGRADE` restriction by implementing a dual-path rollback architecture that enables reliable downgrades on all Android devices.
- **Privileged In-Place Downgrade via Shizuku**: Integrated Shizuku shell service to execute `pm install -d -r` directly, allowing students with Shizuku or wireless debugging to downgrade in place without uninstalling or losing data.
- **Zero-Data-Loss Guided Reinstall Pipeline**: For devices without Shizuku, implemented an automated safe reinstall workflow:
  - Enabled `android:hasFragileUserData="true"` in `AndroidManifest.xml` so Android 10+ natively offers a "Keep app data" option upon uninstallation.
  - Automatic pre-export of coursework notes, folders, and settings to public storage (`Downloads/Fotara_Rollback_Vault_<version>.json`).
  - Staging the target rollback APK into public `Downloads/` so it survives uninstallation.
  - Sticky post-uninstall system notification allowing 1-tap installation of the target APK after uninstalling.
- **Interactive Downgrade Wizard in UpdateScreen**: Multi-path dialog presenting live Shizuku detection, 1-tap guided safe reinstall, and copyable `adb install -d -r` command for USB/Wireless debugging.
