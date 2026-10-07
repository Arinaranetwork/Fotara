# Anchor - Fotara 1.8.3
Status: Finished    Opened: 2026-10-07    Finished: 2026-10-07

| ID    | Request                                                                                                         | Phase | Acceptance                                                                 | Status   | Evidence                                                                      |
|-------|-----------------------------------------------------------------------------------------------------------------|-------|----------------------------------------------------------------------------|----------|-------------------------------------------------------------------------------|
| H-001 | If generated, display the device ID in an expandable panel below the allow anonymous share toggle               | 40    | Expandable panel below toggle shows generated UUID v4 with copy button     | Verified | DeviceRegistry.kt (deviceIdFlow), SettingsScreen.kt (DeviceIdExpandablePanel); DeviceRegistryTest passing |
| H-002 | Build as 1.8.3 Beta and upload to GitHub Releases with release notes                                            | 40    | Version 1.8.3 Beta (code 32), 100% tests pass, APK built and published     | Verified | build.gradle.kts (32 / 1.8.3 Beta), 718 tests passing, Fotara_1.8.3_Beta.apk, GitHub Release |
