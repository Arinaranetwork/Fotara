# Fotara — Product Brief

## 1. Introduction

Fotara is a simple Android application designed to organize photos of notes, assignments, and study material into clearly labeled folders, so a student never has to scroll through a flat camera roll trying to remember which photo belongs to which subject or task. The core idea is a folder-based system that mirrors how a student mentally organizes coursework, combined with searchable metadata so content can be found by what's written in the photo, not just by filename or folder location.

## 2. Problem Statement

Students frequently photograph whiteboards, printed assignments, and handwritten notes throughout the day. Over time these photos accumulate in the default gallery with no structure, making it difficult to locate a specific note when it's needed — especially right before a deadline. Existing gallery apps organize by date or album, not by subject, task, or content, and none of them make handwritten or photographed text searchable.

## 3. Target User

Students managing multiple subjects and recurring assignments, who take frequent reference photos and need a fast way to file and retrieve them without manual sorting overhead.

## 4. Core Concept

- Home screen displays folders as rounded rectangle cards, each acting as a subject or topic container.
- A search bar sits at the top of the home screen.
- Folders can contain subfolders, but subfolders are not visually nested inside the folder card — they surface only once a folder is opened, through a separate navigation layer.
- Folders can be color-labeled, with the color indicator placed in the top-right corner of the card.
- Each folder supports direct photo upload/capture.

## 5. Feature List

### 5.1 Core Features

- **Folder system**: rounded-rectangle folder cards, name displayed on the card, color label indicator top-right.
- **Subfolder navigation**: presented via horizontal tabs (or breadcrumb + flat grid for deeper hierarchies) once a folder is opened, rather than nested visually inside the parent folder.
- **Search bar**: located at the top of the home screen; searches folder names and photo content (see OCR below), not just filenames.
- **Photo upload/capture per folder**: photos can be added directly into a folder or subfolder.
- **Color labeling**: applies to both folders and individual photos for quick visual categorization.

### 5.2 Photo Metadata

Each photo stores structured metadata to support search and organization:

```
Photo {
  id
  file_uri
  folder_id
  subfolder_id (nullable)
  created_at
  added_at
  tag_color (nullable)
  caption (manual, optional)
  ocr_text (auto-generated via on-device text recognition)
  source (camera / screenshot / import)
  linked_deadline (nullable)
}
```

- **OCR extraction**: on-device text recognition (e.g. ML Kit Text Recognition) runs on every photo at capture time, indexing any visible text (handwriting, printed text, whiteboard content) so it becomes searchable.
- **Linked deadline**: an optional due date tied to a photo, used for reminders.

### 5.3 UX Details

- Long-press on a photo opens a quick action menu (move folder, color label, share, delete) without entering a separate edit mode.
- Batch selection mode for moving multiple photos across folders/subfolders at once.
- In-folder sorting options: upload date, nearest deadline, or color label.
- A dedicated section on the home screen (outside the folder structure) surfacing "Added today" and "Due tomorrow" items.
- Folder pinning to keep actively used folders at the top of the home screen.
- Automatic perspective-crop applied when capturing a photo of a whiteboard or document, straightening the result.

### 5.4 Advanced Features

- **Auto-suggest folder on upload**: newly captured photos are OCR-scanned immediately, matched against keywords already present in existing folders, and the app suggests a destination folder instead of requiring manual filing.
- **Export folder to PDF**: compiles all photos within a folder or subfolder into a single sequential PDF, useful for submitting assignments or reviewing before an exam.
- **Cross-folder smart tags**: independent tags (e.g. "Important," "Unfinished") that surface a combined view of photos from multiple folders without physically moving the underlying files.
- **Multi-capture mode**: a camera mode that captures several consecutive pages into the same folder without returning to the gallery between shots, with automatic cropping applied to each.
- **Deadline reminders**: local Android notifications triggered from a photo's `linked_deadline` field, removing the need for a separate calendar app.
- **Home screen quick shortcut**: long-pressing the app icon offers a "capture to last used folder" shortcut, skipping navigation when time is limited.

## 6. Platform

- **Target OS**: Android.
- **Suggested stack**: Kotlin, on-device ML Kit Text Recognition for OCR (no external API calls required, works offline), local database (e.g. Room) for folder/photo/metadata storage.

## 7. Naming

Working name: **Fotara**. No conflicting app was found under this exact name on Play Store or App Store at the time of writing; availability of the package name and domain should still be manually confirmed before publishing.

## 8. Open Items for Further Definition

- Final decision on subfolder navigation pattern (tabs vs. breadcrumb + flat grid vs. sidebar) — likely tabs for the initial version given the expected folder depth.
- Visual design system (colors, typography, iconography) for folder cards and annotation elements.
- Whether cloud backup/sync is in scope for a future version, or the app remains fully local/offline.
