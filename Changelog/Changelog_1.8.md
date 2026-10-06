# Fotara 1.8 - Workspace Icons, Legal Compliance & Canvas Drawing
Released: In Development   Status: In Development

## 1.8.0 Beta - In Development
By downloading, installing or using Fotara you acknowledge that you have read, understood and agree to the Privacy Policy and the Terms of Service. The same documents are shown inside the app on first launch and must be accepted to use it.

### Batch 1: Legal Documents, Consent Gate, Dialog Coordinator & Device Count
- **Legal Compliance Documents**: Bundled canonical Privacy Policy (`Legal/PRIVACY.md`) and Terms of Service (`Legal/TERMS.md`) synchronized at build time via Gradle asset integration with strict header validation (`Version: 1 | Effective: 2026-10-06`).
- **Settings Legal Screens**: Added dedicated Privacy Policy and Terms of Service readers in Settings > About & Legal rendered with Markdown support, back navigation, and responsive typography.
- **First-Launch Consent Gate**: App-level modal consent dialog gating cold starts, share intents, widget clicks, and deep links. Back gesture or close button finishes the app without storing unaccepted state.
- **AppDialogCoordinator**: Centralized priority-based dialog coordinator in `AppContainer` managing `CONSENT`, `UPDATE`, and automatic prompts with preemption without dismissal and 250ms inter-dialog pacing.
- **NetworkGate Enforcement**: Universally halts all network requests (update checks, feedback queue syncing, anonymous counts, remote images) until user consent is explicitly granted.
- **Optional Anonymous Device Count**: Local-only random UUID v4 counter with Supabase RPC backend (`register_device` / `unregister_device`), throttled to 24-hour intervals and version changes, with instant ID deletion and unregister queuing when disabled.
- **Backup & Device Transfer Exclusions**: Excluded `fotara_legal_prefs.xml` and `fotara_device_prefs.xml` from Android Auto Backup and device transfers in `backup_rules.xml` and `data_extraction_rules.xml`.
