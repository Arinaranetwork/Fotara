# Phase 33 - PDF Search Deep Link and Content Highlighting

## Goal
Enable deep-linking from search query results directly into specific pages of PDF documents, automatically scrolling the viewport to the target page and rendering a visual highlight over the matched text snippet.

## Scope
- Search result selection deep-linking: pass target PDF page number and character/bounding coordinates to `PdfViewerScreen`.
- Viewport auto-scroll: trigger animated scroll to the target page index when opened from a search match.
- Content highlighting: render a semi-transparent highlight overlay on the matched word or line in the PDF page viewer.

## Out Of Scope
- General PDF editing or annotation.
- Non-PDF document search deep-linking.

## Dependencies
- Phase 30 & Phase 32 PDF viewing foundation.
- ML Kit / PDF OCR text indexing data.

## Acceptance Criteria
- Tapping a PDF search match opens the PDF directly on the matching page.
- Target page automatically scrolls into view.
- Matched text is visually highlighted.
