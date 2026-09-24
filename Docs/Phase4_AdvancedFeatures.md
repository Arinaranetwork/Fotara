# Phase 4 - AdvancedFeatures (Cross-Folder Smart Tags)

## Goal
Enable cross-folder academic synthesis by introducing Smart Tags. Coursework concepts frequently span multiple concurrent subjects (e.g. #formula across Physics and Calculus, #exam across all subjects, #lab across Chemistry and Biology). Cross-Folder Smart Tags allow students to label and aggregate notes across any folder, either by automatic hashtag extraction from captions, OCR, and notes, or through explicit tag assignments, backed by instant cross-folder search filtering.

## Scope
- **Smart Tag Extraction & Schema**: Add `tags: String?` to `Photo` model and SQLite schema; support automatic `#hashtag` extraction from captions, OCR, and custom notes alongside explicit tag chips.
- **Cross-Folder Tag Aggregation**: Repository method `getAllSmartTags()` returning all distinct tags across all folders, sorted by usage frequency.
- **Cross-Folder Tag Queries**: Repository method `getPhotosByTag(tag)` aggregating notes from any folder matching the chosen tag.
- **Search Bar Smart Tag Filter Row**: Horizontal scrollable tag chip row docked in `ActiveSearchBar` allowing one-tap cross-folder filtering.
- **Card & Viewer Tag Display**: Surface tag chips on photo cards and within the full-screen photo inspector.
- **Tag Management Actions**: Quick action to add and remove tags from individual notes.

## Out Of Scope
- Cloud tag synchronization (strictly offline-first per R-003).
- Hierarchical nested tags (flat hashtag structure optimized for speed and simplicity).

## Features
### Hashtag Tokenization & Smart Tag Aggregation
Automatically extracts hashtags (`#concept`, `#midterm`, `#formula`) embedded in photo captions, notes, and OCR text, combined with user-assigned tags. Aggregates unique tags across all active folders.

### Cross-Folder Tag Search & Filtering
In `ActiveSearchBar`, a horizontal scrolling chip row displays active smart tags. Selecting a tag filters notes across all subject folders instantly, even with an empty search query.

### Quick Tagging in Photo Inspector
Students can quickly add or remove tags from notes in the full-screen inspector, updating the search index immediately.

## UI Mockup
```
+------------------------------------------------------+
| [<-] Search notes, tags...                       [X] |
+------------------------------------------------------+
| ALL  TODAY  THIS WEEK  THIS MONTH   [Colors: * * *] |
| TAGS: [#exam] [#formula] [#lab] [#hw] [#midterm]     |
+------------------------------------------------------+
| CROSS-FOLDER NOTES FOR #formula (8)                  |
| [Calc] Derivative Chain Rule        [Phys] Maxwell Eq|
| [Chem] Ideal Gas Law                [Eng] Stress-Strain
+------------------------------------------------------+
```

## Logic Notes
- Tags are normalized to lowercase alphanumeric tokens without the leading `#`.
- Adding `#tag` in a photo's custom note or caption automatically promotes it to a searchable smart tag.
- Trashed notes are excluded from smart tag aggregation and queries.
- Deleting or untagging updates the tag frequency and removes unused tags from the filter row.

## Risks
- Tag explosion from OCR text false-positives -> Strict hashtag regex `#([a-zA-Z0-9_]{2,24})` applied only to user caption, note, and explicit tags; raw OCR text is queried via full-text search.

## Dependencies
- Phase 1 CoreFolderEngine, Phase 2 PhotoOcrEngine, Phase 3 CustomNotesAndSearch.

## Acceptance Criteria
- [x] `Photo` model and SQLite database schema support `tags: String?` with clean migration.
- [x] Automatic hashtag extraction from `caption` and `note` works alongside explicit tags.
- [x] `PhotoRepository.getAllSmartTags()` aggregates distinct tags across all active folders.
- [x] `PhotoRepository.getPhotosByTag(tag)` returns all active notes matching the tag across all folders.
- [x] `ActiveSearchBar` displays horizontal smart tags row and filters cross-folder notes on selection.
- [x] Full-screen inspector displays smart tags and provides quick tag editing.
- [x] Unit tests verify tag extraction, aggregation, cross-folder queries, and UI filtering.
