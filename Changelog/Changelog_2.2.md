# Fotara 2.2 - Universal Android Version Downgrade Engine
Released: 2026-10-10   Status: In Progress (Alpha)

## 2.2.0 Alpha - 2026-10-10
- **Universal Android Version Downgrade Engine**: Solved Android OS's native `INSTALL_FAILED_VERSION_DOWNGRADE` restriction by implementing a dual-path rollback architecture that enables reliable downgrades on all Android devices.
- **Privileged In-Place Downgrade via Shizuku**: Integrated Shizuku shell service to execute `pm install -d -r` directly, allowing students with Shizuku or wireless debugging to downgrade in place without uninstalling or losing data.
- **Zero-Data-Loss Guided Reinstall Pipeline**: For devices without Shizuku, implemented an automated safe reinstall workflow:
  - Enabled `android:hasFragileUserData="true"` in `AndroidManifest.xml` so Android 10+ natively offers a "Keep app data" option upon uninstallation.
  - Automatic pre-export of coursework notes, folders, and settings to public storage (`Downloads/Fotara_Rollback_Vault_<version>.json`).
  - Staging the target rollback APK into public `Downloads/` so it survives uninstallation.
  - Sticky post-uninstall system notification allowing 1-tap installation of the target APK after uninstalling.
- **Interactive Downgrade Wizard in UpdateScreen**: Multi-path dialog presenting live Shizuku detection, 1-tap guided safe reinstall, and copyable `adb install -d -r` command for USB/Wireless debugging.
