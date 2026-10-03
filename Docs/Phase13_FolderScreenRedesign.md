# Phase 13 - FolderScreenRedesign

## Goal
Visually expand the new Fotara Home Screen design language into the Folder / Detail screen (and its note/document cards) so that the entire navigation journey feels unified, cohesive, and modern without altering any underlying folder or note management functionality.

## Scope
- Top App Bar redesign: 40dp circular back button (`#131925`), prominent folder name with folder color indicator dot, 40dp primary blue circular Add button (`#2563EB`), and 40dp circular overflow button (`#131925`).
- Subfolder / Category Tabs: Rounded pill geometry (`RoundedCornerShape(20.dp)`), active blue pill (`#2563EB`), inactive dark surface (`#111726` with 1dp border `#1D263B`), and consistent horizontal scroll.
- Note & Document Cards: Redesign `DetailPhotoCard`, `DetailGroupCard`, `DetailDocumentCard`, `DetailTextNoteCard`, and `DetailCanvasCard` to use the dark rounded card design (`#111726`, 20-22dp radius, 1dp `#1D263B` border), internal padding, clipped media thumbnails, dark translucent pill badges (top-left), circular 3-dots action menu buttons (top-right), and two-tier typography hierarchy (ElmsSans Medium title + ElmsSans Light metadata).
- Responsive spacing & WindowInsets: Edge-to-edge support with full-height viewport utilization and clean clearance above system navigation bars.

## Out Of Scope
- Changes to database schema, DAOs, or data models.
- Changes to note editors (Canvas note screen, Text note editor screen, PDF viewer, Photo viewer dialog).
- Changes to Home Screen or Settings screen (completed in Phase 11 & 12).

## Features
### Top App Bar Modernization
- **Normal Mode**:
  - Left: 40dp circular back button with `#131925` background and 20dp white arrow icon.
  - Center/Title: Folder name in ElmsSans Medium (~22sp, Color.White) accompanied by a 10dp circular color dot matching the folder's tag color.
  - Right:
    - Primary Add action: 40dp circular button with `HomeMainButtonBlue` (`#2563EB`) background and white plus icon.
    - Overflow action: 40dp circular button with `#131925` background and white vertical 3-dots icon.
- **Selection / Batch Mode**:
  - Contextual action bar displaying selected item count, Close button, Select All button, and action buttons (Delete, Move, Rename, Share, etc.) maintaining identical functionality.

### Category & Filter Tabs (Subfolder Row)
- Horizontal scrollable row with 8dp spacing and 16dp horizontal content padding.
- **Active Tab** ("All Notes" or selected subfolder): Full pill shape (`RoundedCornerShape(20.dp)`), filled with `HomeMainButtonBlue` (`#2563EB`), white text in ElmsSans Medium/Bold (~13-14sp).
- **Inactive Tabs**: Full pill shape (`RoundedCornerShape(20.dp)`), filled with `HomeCardSurface` (`#111726`), bordered with 1dp `HomeCardBorder` (`#1D263B`), text in `HomeSubtitleGray` (`#94A3B8`).
- **"+ Sub..." Button**: Matching pill style with plus icon and label for creating new subfolders.
- Long-press to rename/delete subfolders remains fully functional.

### Unified Note & Document Cards
- **Card Surface & Frame**:
  - Container color: `HomeCardSurface` (`#111726`).
  - Border: 1dp `HomeCardBorder` (`#1D263B`), expanding to 2dp active blue when selected.
  - Corner radius: 20-22dp with clean clipping.
  - Aspect ratio: balanced 2-column grid proportion.
- **Media Content**:
  - Photo / Group: Image thumbnail clipped cleanly inside the card with internal rounded corners (~14dp) and subtle background tint (`#0F1626`).
  - Document (DOCX / PDF): Clean centered document icon (`#2563EB` for DOCX, TagCrimson for PDF) with type label.
  - Text Note / Canvas: Snippet / canvas preview thumbnail cleanly clipped.
- **Top Badges & Overflow Menu**:
  - Top-Left: Translucent dark pill badge (`Color(0xCC0D131F)` with subtle 0.8dp border) showing type / count (e.g. `DOCX | 1 p`, `PDF | 3 p`, `❐ 3` for groups).
  - Top-Right: 32dp circular 3-dots menu button `(⋮)` with dark translucent surface (`Color(0x990D131F)`), allowing single-tap access to card quick actions alongside existing long-press.
- **Card Footer**:
  - Title: Left-aligned, white, ElmsSans Medium (~13-14sp), single-line ellipsis. Group cards include a small layers icon prefix.
  - Metadata: Secondary line in `HomeSubtitleGray` (`#94A3B8`), ElmsSans Light (~11-12sp), showing item count and date (e.g. `3 notes · Sep 25`, `1 pages · Sep 30`, `Sep 25`).
  - Schedule & Deadline Badges: Preserved and aligned on the right of the metadata row.
  - LinkIt Corner Glow: Preserved at bottom-left corner with glowing accent.
  - Search Highlight Flash: 2-second white exposure overlay smoothly dissolving.

## UI Mockup
```
+-------------------------------------------------------+
|  (←)   Kimia ●                           (+)   (⋮)    |
|                                                       |
|  [All Notes]  [Catatan]  [Kisi Kisi Sem 1]  [+ Sub]   |
|                                                       |
|  +---------------------+   +---------------------+    |
|  | [DOCX | 1 p]    (⋮) |   | [❐ 3]           (⋮) |    |
|  |                     |   | +-----------------+ |    |
|  |         📄          |   | |   [Thumbnail]   | |    |
|  |        DOCX         |   | +-----------------+ |    |
|  |                     |   |                     |    |
|  | Kisi_Kisi_Soal_...  |   | ❐ Hukum Hess        |    |
|  | 1 pages · Sep 30    |   | 3 notes · Sep 25    |    |
|  +---------------------+   +---------------------+    |
|                                                       |
|  +---------------------+   +---------------------+    |
|  |                 (⋮) |   | [❐ 2]           (⋮) |    |
|  | +-----------------+ |   | +-----------------+ |    |
|  | |   [Thumbnail]   | |   | |   [Thumbnail]   | |    |
|  | +-----------------+ |   | +-----------------+ |    |
|  |                     |   |                     |    |
|  | Materi UH Sem 1     |   | Energi Ikatan Rata² |    |
|  | Sep 25              |   | 2 notes · Sep 25    |    |
|  +---------------------+   +---------------------+    |
|                                                       |
+-------------------------------------------------------+
```

## Logic Notes
- Single tap on the card continues to open the item (inspect photo, open group, open document, edit text note, open canvas).
- Tapping the 3-dots button `(⋮)` on a card triggers the same action sheet / dialog as long-pressing the card.
- Long-press continues to open the quick action sheet or trigger selection in batch mode.
- LinkIt corner glow is positioned at the bottom corner and uses the folder's corner radius (20dp).
- In batch mode, the checkmark selector circle appears on the top-left or top-right, replacing or coexisting with the badges.

## Risks
- Card height variance across different content types -> Enforce consistent aspect ratio and spacing so rows align cleanly in the 2-column grid.
- Overlapping tap targets between image click, card click, and 3-dots button -> Ensure 3-dots button has its own click handler with event consumption and a minimum 40dp touch target.

## Dependencies
- Phase 11 (HomeScreenRedesign) - Theme tokens, ElmsSans typography, LinkItGlow.
- Rules.md R-001, R-002, R-004.

## Acceptance Criteria
- [ ] Top app bar features 40dp circular back button, ElmsSans Medium folder title with color dot, 40dp blue Add button, and 40dp circular overflow button.
- [ ] Subfolder category tabs use rounded pill geometry (active blue pill, inactive dark card pill with border) with smooth horizontal scrolling.
- [ ] Note and document cards use the dark rounded card design (`#111726`, 20-22dp radius, 1dp `#1D263B` border).
- [ ] Photo and group cards display image thumbnails cleanly clipped inside internal rounded corners.
- [ ] DOCX and PDF document cards display crisp centered document icon and type/page badge.
- [ ] Each card includes a top-right 3-dots menu button that triggers item actions, alongside long-press support.
- [ ] Card typography follows the two-tier hierarchy (ElmsSans Medium title + ElmsSans Light metadata) with zero italics.
- [ ] All existing actions (opening notes, batch select, link/unlink, schedule reminders, rename, delete) remain 100% functional.
- [ ] Viewport extends full height, respecting Android system navigation insets without artificial dead space.
