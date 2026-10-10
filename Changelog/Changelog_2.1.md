# Fotara 2.1 - Intelligent Version Management & Rollback
Released: 2026-10-10   Status: In Progress (Alpha)

## 2.1.0 Alpha - 2026-10-10
- **Version Rollback & Release History Engine**: Integrated comprehensive in-app version rollback functionality. Students can inspect previous official releases from GitHub, read changelogs for any prior release, and rollback/downgrade to an earlier APK version if experiencing an issue or regression.
- **Multi-Release Query & Semantic Categorization**: Expanded `UpdateManager` to parse all repository releases, filter past releases with valid APK binaries, and classify releases by semantic version and channel (Stable, Beta, Alpha).
- **Rollback Download & FileProvider Installer Pipeline**: Secure in-app downloading for previous release APKs with real-time download progress tracking, system download notifications, and automatic launch of Android Package Installer.
- **Pre-Rollback Safety Backup Guard**: Interactive modal warning students about Android package downgrade risks and offering a 1-tap notes backup export before beginning the rollback process.
- **Settings & Update Screen Navigation**: Direct "Version History & Rollback" entry point in Settings and expandable version cards in `UpdateScreen`.
