![Fotara 1.4 Banner](../Assets/Banners/FotaraBanner_1.4_2026-09-29.jpg)

# Fotara 1.4 - Native Text Notes & High-Fidelity Documents
Released: 2026-09-29   Status: Beta

## What's New
- Native Rich-Text Notes: Create, format, and organize native text notes alongside photos, groups, and document files. Features a full Markdown-backed editor with live formatting (bold, italic, strikethrough, headings, lists, quotes, code, links), instant round-tripping, debounced autosave, and keyboard-docked formatting toolbar.
- In-App Reflowed DOCX Viewer: Directly view Microsoft Word (.docx) documents inside Fotara with rich styling (paragraphs, inline formatting, headings, bullet/numbered lists, tables, embedded graphics), progressive loading, and clear visual notices for unsupported elements.
- Dedicated Markdown & Plain-Text Sharing: Share text notes cleanly as raw Markdown (`.md`) or stripped Plain Text (`.txt`).
- Text Note Search Integration: Search indexing automatically includes text note titles and markdown body text for zero-latency retrieval.

## Changed
- High-Performance PDF Engine: Replaced low-resolution static page dumps with an on-demand, density-scaled PDF renderer with white canvas pre-fill, eliminating all black backgrounds and transparency artifacts.
- Virtualized PDF Page Viewer: Smooth, bounded memory rendering with LRU cache, single-writer thread-safe serialization, and instant page recycling.
- Pipelined PDF Import: First page viewable immediately while background OCR and remaining pages process asynchronously with a real progress bar.
- Unified Share As Discipline: Mixed selections containing text notes automatically restrict export to native files, cleanly skipping text notes during PDF/Word combine operations with helpful notification.

## Fixed
- Fixed black backgrounds and rendering glitches on transparent PDF pages.
- Resolved scroll lag and UI stutter when scrolling through multi-page PDFs.
- Added explicit error dialogs for password-protected and corrupted PDF documents.
- Fixed orphaned document and page file leaks when permanently purging folders.
- Fixed unhandled exceptions when importing damaged files.
