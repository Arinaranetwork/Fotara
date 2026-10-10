# Fotara 2.1 - Intelligent Version Management & Rollback
Released: 2026-10-10   Status: In Progress (Alpha)

## 2.1.0 Alpha - 2026-10-10
- **Version Rollback & Release History Engine**: Integrated comprehensive in-app version rollback functionality. Students can inspect previous official releases from GitHub, read changelogs for any prior release, and rollback/downgrade to an earlier APK version if experiencing an issue or regression.
- **Multi-Release Query & Semantic Categorization**: Expanded `UpdateManager` to parse all repository releases, filter past releases with valid APK binaries, and classify releases by semantic version and channel (Stable, Beta, Alpha).
- **Rollback Download & FileProvider Installer Pipeline**: Secure in-app downloading for previous release APKs with real-time download progress tracking, system download notifications, and automatic launch of Android Package Installer.
- **Pre-Rollback Safety Backup Guard**: Interactive modal warning students about Android package downgrade risks and offering a 1-tap notes backup export before beginning the rollback process.
## 2.1.1 Alpha - 2026-10-10
- **Settings Friends Card Harmonization**: Redesigned `FriendsSettingsCard` to seamlessly harmonize with the rest of the dark-navy Settings design system. Replaced saturated blue icons and green badges with standard neutral dark slate icon tiles (`#1E2638`), muted slate icon tint (`#94A3B8`), 16sp title, and 13sp light subtitle.
- **Hardware Status Bar Insets Clearance**: Corrected window insets padding across `FriendsScreen`. Top app bar elements, back navigation, and actions now strictly respect `WindowInsets.statusBars`, preventing any overlap with the physical phone battery, clock, notch, or notification area.
- **Settings Sub-Screen Header Standardization**: Unified `FriendsScreen` top app bar with `SettingsSubScreenHeader` conventions (40dp circular back button `#131925`, white back icon, 24sp Medium title, right-aligned action buttons).
- **Dark-Dominant UI Palette Polish**: Desaturated presence chips, search bars, contact cards, and QR modals across the Friends module, enforcing dark fills with lightness <= 35% in compliance with Arinara UI rules U-11 and U-13.

