# Phase 35 - LegalConsentAndDialogCoordinator
## Goal
Establish a compliant, student-focused legal framework and robust startup architecture for Fotara 1.8.0 Beta (Batch 1 of 3). This phase bundles canonical Privacy Policy and Terms of Service documents with build-time asset synchronization, introduces an app-wide dialog coordinator with a mandatory first-launch consent gate blocking all entry points and network traffic prior to acceptance, and implements an optional, privacy-preserving anonymous device counter backed by Supabase RPC.

## Scope
- Canonical legal documents: `Legal/PRIVACY.md` and `Legal/TERMS.md` with strict version headers (`Version: 1 | Effective: 2026-10-06`).
- Build-time asset synchronization Gradle task copying `Legal/*.md` into assets without source tree duplication.
- Legal document loader parsing metadata lines, validating headers, and exposing content for display.
- Settings > About & Legal screens: full-screen readers for Privacy Policy and Terms of Service with Markdown rendering and back navigation.
- Settings > About & Legal > Privacy section: anonymous device count toggle (`key_device_count_enabled`, default `DEVICE_COUNT_DEFAULT_ENABLED = true`).
- `AppDialogCoordinator`: centralized app-level dialog coordinator in `AppContainer` managing priority (`CONSENT` > `UPDATE` > others), deduplication, preemption without dismissal, and 250ms spacing between queued dialogs.
- `ConsentDialog`: first-launch modal dialog in Fotara theme (28dp corners, ElmsSans) featuring segmented tab switcher, scrollable Markdown viewport with per-tab scroll memory, anonymous device count switch, full-width "Accept and continue" button, and exit on dismiss (X button or system back finish app affinity).
- `NetworkGate`: universal gate enforcing that zero network requests (update checks, feedback queue flush, device count, remote images) occur before consent is granted.
- Entry-point gating: `MainActivity` and `ShareReceiverActivity` (retaining shared intent payload after consent) gated by consent; widget, notification, and deep links route through `MainActivity` gate.
- Exclude `fotara_legal_prefs.xml` and `fotara_device_prefs.xml` from Android Auto Backup and device transfer (`backup_rules.xml` and `data_extraction_rules.xml`).
- Supabase SQL schema and RPC functions: `Docs/Supabase/device_registry.sql` with table `app_devices`, RLS, `register_device`, and `unregister_device`.
- Unit tests covering loader, parser, coordinator state machine, NetworkGate, DeviceRegistry, and backup rules validation.

## Out Of Scope
- Batch 2 features: workspace icons, PDF page menu, pins, save to gallery, search jump and highlight.
- Batch 3 features: PDF page drawing editor and release build.
- Database schema changes (remains `DATABASE_VERSION = 17`).
- Bumping `versionName` or `versionCode` (remains `1.7.1` / `28` until Batch 3).
- Remote release build creation (debug APK only for Batch 1).

## Features
### Legal Document System
- Canonical Markdown documents in `Legal/` at root.
- Line 1: Document title ("Privacy Policy" / "Terms of Service").
- Line 2: Exactly `Version: N | Effective: YYYY-MM-DD`.
- Gradle task `copyLegalDocsToAssets` copies files into generated asset dir during build.
- `LegalDocumentLoader` reads bundled assets, validates header, parses version and date, and strips header from body text.

### About & Legal Screen Additions
- "Privacy Policy" row navigating to full-screen reader with title, "Version N, effective <date>", scrollable Markdown body, and back navigation.
- "Terms of Service" row navigating to full-screen reader with identical styling.
- "Privacy" section with "Share anonymous device count" toggle and explanation text.

### AppDialogCoordinator & Consent Gate
- Pure state machine owned by `AppContainer` exposing `StateFlow<AppDialogState>`.
- Priority ordering: `CONSENT` (highest, 100), `UPDATE` (medium, 50).
- At most one dialog shown at once. Higher priority preempts lower priority without dismiss; lower returns after 250ms gap when higher finishes.
- Consent gate checks `fotara_legal_prefs`: valid only if accepted privacy and terms versions are >= bundled asset versions.
- All network requests check `NetworkGate.isConsentGranted(context)`.

### Optional Anonymous Device Count
- Random UUID v4 generated locally on first use after consent, stored in `fotara_device_prefs`.
- Sends only `device_id`, `app_version`, and `channel` (`stable` or `beta`).
- Throttled to at most once per 24 hours, or immediately upon app version upgrade.
- When toggled off, immediately clears local device ID and queues best-effort unregister RPC call.

## UI Mockup
### First-Launch Consent Dialog
```
+-------------------------------------------------------------+
| Terms of Service and Privacy Policy                     [X] |
+-------------------------------------------------------------+
|  [ Privacy Policy ]   |   [ Terms of Service ]              |
+-------------------------------------------------------------+
| # Privacy Policy                                            |
| Version 1, effective 2026-10-06                             |
|                                                             |
| Fotara is an offline-first educational application...       |
| [Scrollable markdown content...]                            |
+-------------------------------------------------------------+
| ----------------------------------------------------------- |
| [x] Share anonymous device count                            |
|     Sends a random ID and the app version so the developer  |
|     can count active devices. No personal data. You can     |
|     change this anytime in Settings.                        |
|                                                             |
| By tapping Accept you agree to the Privacy Policy and the   |
| Terms of Service.                                           |
|                                                             |
| +---------------------------------------------------------+ |
| |                  Accept and continue                    | |
| +---------------------------------------------------------+ |
+-------------------------------------------------------------+
```

## Logic Notes
- `NetworkGate.isConsentGranted(context)` reads `fotara_legal_prefs` and compares accepted versions against `LegalDocumentLoader.BUNDLED_PRIVACY_VERSION` and `BUNDLED_TERMS_VERSION`.
- `UpdateManager.checkForUpdates()` and `FeedbackManager.flushLocalQueue()` bail early if `!NetworkGate.isConsentGranted(context)`.
- `AppDialogCoordinator` manages active dialog request. If activity is paused or not resumed, dialog display is deferred.
- If consent dialog close button [X] or system back gesture is pressed, `activity.finishAffinity()` is invoked and nothing is persisted.

## Risks & Mitigations
- *Risk*: First-launch dialog blocks critical onboarding setup.
  - *Mitigation*: The consent gate is placed as the prerequisite gate above all screens and onboarding; once accepted, coordinator immediately presents the next queued dialog or proceeds directly into Onboarding/Home.
- *Risk*: Network requests fire during early application or ViewModel initialization before UI renders.
  - *Mitigation*: Hard checks added at the entry of every network call via `NetworkGate.isConsentGranted(context)`.
- *Risk*: Restored device from Android Cloud Backup bypasses consent or duplicates anonymous device ID.
  - *Mitigation*: Explicit XML exclusion rules in `backup_rules.xml` and `data_extraction_rules.xml` covering `fotara_legal_prefs.xml` and `fotara_device_prefs.xml`.

## Dependencies
- Phase 32 (QualityAndSmoothness - 1.7.1)
- Phase 34 (WorkspacePolishAndUiRefinements - 1.7.2)

## Acceptance Criteria
- [x] Legal canonical files created at `Legal/PRIVACY.md` and `Legal/TERMS.md` with exact header syntax.
- [x] Gradle task bundles legal documents into generated assets during build.
- [x] `LegalDocumentLoader` and parser validate version header and strip it from rendered body.
- [x] Settings > About & Legal displays "Privacy Policy" and "Terms of Service" full screens and "Privacy" device count toggle.
- [x] `AppDialogCoordinator` coordinates `CONSENT` and `UPDATE` dialogs with priority, preemption, deduplication, and 250ms delay.
- [x] First-launch consent dialog finishes app on X/back, accepts and persists consent, and unlocks app navigation.
- [x] `NetworkGate` prevents all network calls prior to consent.
- [x] `DeviceRegistry` creates UUID v4, throttles sends (24h / version bump), unregisters on toggle off.
- [x] Android backup rules exclude `fotara_legal_prefs.xml` and `fotara_device_prefs.xml`.
- [x] Unit test suite passes 100%. Debug APK builds successfully.
