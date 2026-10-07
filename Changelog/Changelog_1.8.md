# Fotara 1.8 - Workspace Icons, PDF Tools & Page Drawing
Released: 2026-10-07   Status: Released (Beta)

## 1.8.0 Beta - 2026-10-07
By downloading, installing or using Fotara you acknowledge that you have read, understood and agree to the Privacy Policy and the Terms of Service. The same documents are shown inside the app on first launch and must be accepted to use it.

### Batch 1: Legal Documents, Consent Gate, Dialog Coordinator & Device Count
- **Legal Compliance Documents**: Bundled canonical Privacy Policy (`Legal/PRIVACY.md`) and Terms of Service (`Legal/TERMS.md`) synchronized at build time via Gradle asset integration with strict header validation (`Version: 1 | Effective: 2026-10-06`).
- **Settings Legal Screens**: Added dedicated Privacy Policy and Terms of Service readers in Settings > About & Legal rendered with Markdown support, back navigation, and responsive typography.
- **First-Launch Consent Gate**: App-level modal consent dialog gating cold starts, share intents, widget clicks, and deep links. Back gesture or close button finishes the app without storing unaccepted state.
- **AppDialogCoordinator**: Centralized priority-based dialog coordinator in `AppContainer` managing `CONSENT`, `UPDATE`, and automatic prompts with preemption without dismissal and 250ms inter-dialog pacing.
- **NetworkGate Enforcement**: Universally halts all network requests (update checks, feedback queue syncing, anonymous counts, remote images) until user consent is explicitly granted.
- **Optional Anonymous Device Count**: Local-only random UUID v4 counter with Supabase RPC backend (`register_device` / `unregister_device`), throttled to 24-hour intervals and version changes, with instant ID deletion and unregister queuing when disabled.
- **Backup & Device Transfer Exclusions**: Excluded `fotara_legal_prefs.xml` and `fotara_device_prefs.xml` from Android Auto Backup and device transfers in `backup_rules.xml` and `data_extraction_rules.xml`.

### Final Batch: Workspace Icons, PDF Page Menu & Pins, Saved Image Location, Search Match Deep-Link, and PDF Page Drawing Editor
- **Workspace Icons**: Personalize custom workspaces with 24 bundled vector icons (Book, Science, Code, Sports, Art, and more) selected from a 6-column picker grid, displayed consistently across Home/Notes tabs, search scope chips, and folder destination rows.
- **PDF Page Menu & Pins**: Contextual 3-dot options menu on every PDF page card. Pin up to 3 pages per document with jump chips below the top bar without modifying original PDF files.
- **Save PDF Page to Gallery**: Render and export individual PDF pages as high-resolution 300 DPI JPEGs with visible drawing annotations flattened into the page canvas.
- **Saved Image Location Setting**: Configure preferred export directory in Settings under General (Pictures/Fotara, DCIM/Fotara, or custom subfolder under Pictures) with Android 10+ MediaStore and Android 7-9 SAF compatibility.
- **Search Jump & Transient Highlights**: PDF search results open directly at the matching page with OCR word bounding boxes highlighted. Text note matches open with smooth auto-scroll and 4-second fading highlight without modifying note content.
- **PDF Page Drawing Editor**: Dedicated drawing screen featuring Pen, Highlighter (Multiply blend), Eraser, 8 curated colors, 1x–6x GPU pinch-zoom & pan, 100-step undo/redo, and 600ms autosave in PDF page point coordinates.
- **Zero-Stub Production Hygiene**: Complete empty, loading, error, and populated states with full test verification across the offline SQLite schema v18.

### Addendum: Quality & Consistency Fixes
- **Profile Header & Banner Harmonization**: Unified header layout, back button geometry, status-bar offset, title typography, and 16:9 banner preview across Settings, Profile, and image/banner crop editors using shared `SettingsSubScreenHeader` and `ProfileBannerHeader` components.
- **Floating Action Button Stability**: Hoisted the floating (+) button to the root tab container level in `HomeScreen` with persistent bottom clearance retention (`TabContainerBottomState`). Switching tabs between Home, Notes, and Settings eliminates all vertical measurement jumps and unmounts, fading out smoothly with alpha on Settings.
- **Drawing Stroke Touch Alignment**: Fixed touch coordinate double-inversion on zoom levels between 1.0x and 6.0x across Photo and PDF page drawing editors with pure coordinate transformation `DrawingViewTransform`.

