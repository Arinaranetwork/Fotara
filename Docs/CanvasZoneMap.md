<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Fotara Canvas Zone Map (Version 1.5.0 Alpha)

This document establishes the binding architectural layout and zone identifier taxonomy for the **Unlimited Canvas (Alpha)** introduced in Fotara 1.5.0 Beta. All UI components, composables, touch handlers, and documentation must adhere strictly to these defined zones.

## Overview Diagram

```
+------------------------------------------------------------------------------------+
|  [Z1: Top-Left Navigation]                              [Z2: Top-Right Operations] |
|  <-  Coursework Canvas  · Saved                          Undo  Redo  Layer  +  :   |
+------------------------------------------------------------------------------------+
|                                                                                    |
|                                                                                    |
|                                [Z6: Canvas Viewport]                               |
|                     - Infinite 2D Pan, Pinch Zoom, Fling Deceleration               |
|                     - Freeform Vector Strokes (Pen, Highlighter)                   |
|                     - Embedded Image Attachments & Transformation Bounds           |
|                                                                                    |
|                               +-----------------------------+                      |
|                               |  [Z8: Contextual Object Bar]|                      |
|                               |  [Dup] [Del] [Lock] [^] [v] |                      |
|                               +-----------------------------+                      |
|                                                                                    |
|                                                                                    |
|  [Z7: Zoom Indicator & Fit]                                                        |
|  [ 100% ]  [ Fit to Content ]                                                      |
|                                   +--------------------------------+               |
|                                   |  [Z4: Tool Options Popup]      |               |
|                                   |  Stroke: ---o---  Color: [][][]|               |
|                                   +--------------------------------+               |
|                                   |  [Z3: Bottom Tool Dock]        |               |
|                                   |  [Select] [Pen] [High] [Eraser]|               |
|                                   +--------------------------------+               |
+------------------------------------------------------------------------------------+
```

---

## Detailed Zone Definitions

### Z1 - Top-Left Navigation & Title Zone
- **Location**: Top-left header region, inset below status bars (`statusBarsPadding()`).
- **Components**:
  - **Back Button**: Safe navigation return to the originating folder, triggering debounced save.
  - **Note Title**: Inline editable title composable with single tap-to-rename dialog.
  - **Save State Indicator**: Real-time autosave status badge (`Saving...` with spinner, `Saved` with checkmark icon).
- **Constraints**: Minimum 48dp touch targets. Must not collide with Z2 on narrow screens.

### Z2 - Top-Right Operations Toolbar
- **Location**: Top-right header region, horizontally aligned with Z1.
- **Components**:
  - **Undo (`Icons.Default.Undo`)**: Reverts last stroke, image mutation, or layer adjustment.
  - **Redo (`Icons.Default.Redo`)**: Re-applies reverted action.
  - **Layers Trigger (`Icons.Default.Layers`)**: Opens Z5 (Layers Management Panel).
  - **Add Image (`Icons.Default.AddPhotoAlternate`)**: Opens modal picker to insert image from device gallery or existing Fotara notes.
  - **Schedule (`Icons.Default.AccessTime`)**: Opens note scheduling dialog (Milestone 2 integration).
  - **Export / Share (`Icons.Default.Share`)**: PNG export options (Full Canvas, Current Viewport, Selection) respecting the 100-page safety limit.
  - **Overflow Menu (⋮)**:
    - Background Style Picker (Blank, Grid, Dots, Ruled Lines).
    - Canvas Settings (Stylus-Only Mode, Palm Rejection Guard).
    - Note Info (Date Added, File Size, Total Vector Points).
    - Move Note to Folder / Subfolder.
    - Delete Note to Trash (30-day retention).
- **Constraints**: Overflow menu absorbs any secondary actions lacking an explicit zone.

### Z3 - Bottom Floating Tool Dock
- **Location**: Center-bottom dock elevated above navigation bars (`navigationBarsPadding()`).
- **Characteristics**: Collapsible floating pill with subtle elevation and ivory/gold brand accents.
- **Components**:
  - **Select & Transform Tool**: Pointer tool to select, translate, scale, and rotate strokes or images.
  - **Pen Tool**: Pressure-sensitive fine ink stroke rendering.
  - **Highlighter Tool**: Semi-transparent, multiply-blended chisel stroke rendering.
  - **Eraser Tool**: Dual mode (Stroke-level instant deletion vs. Pixel area erasing).
  - **Active Color Swatch**: Circular badge showing current drawing tint. Tapping toggles Z4.

### Z4 - Tool Options Flyout Popup
- **Location**: Anchored dynamically immediately above Z3 when the active tool is tapped a second time.
- **Components**:
  - **Stroke Width Slider**: Continuous thickness adjustment with live preview dot.
  - **Opacity Slider**: Alpha control (specifically for highlighter and custom inks).
  - **Nib Profile**: Chisel, Ballpoint, Brush options.
  - **Curated Brand Palette**: Quick color swatches + custom hex color picker.
- **Dismissal**: Closes on canvas tap or tool switch.

### Z5 - Layers Management Panel
- **Location**:
  - Mobile Phones: Animated bottom sheet.
  - Tablets & Landscape: Collapsible sliding drawer on the right edge.
- **Components**:
  - Layer list ordered bottom-to-top.
  - Layer items: Reorder drag handle, Name label, Visibility toggle (`Eye`), Lock toggle (`Padlock`), Opacity slider, Delete action.
  - `+ Add Layer` action bar.
  - Active layer highlight indicator.

### Z6 - Infinite Interactive Drawing Viewport
- **Location**: Full-screen canvas viewport underneath overlay zones.
- **Interaction Rules**:
  - Single Finger: Draws with active tool (unless Stylus-Only mode is enabled).
  - Two Fingers: Pan in any direction, pinch-to-zoom (10% to 500%), smooth inertia fling.
  - Stylus Input: Captures native hardware pressure and tilt values when supported by hardware.
  - Palm Rejection Guard: Discards touch events with large surface contact areas.

### Z7 - Viewport Controls (Bottom-Left)
- **Location**: Bottom-left corner, seated safely above system navigation bars and left of Z3.
- **Components**:
  - **Zoom Percentage Chip**: Displays current scale percentage (e.g., `100%`). Single tap resets scale immediately to `100.0f` centered on bounds.
  - **Fit-to-Content Action**: Calculates bounding box of all strokes and image objects, animating camera to fit all content comfortably with 32dp padding.

### Z8 - Contextual Object Action Bar
- **Location**: Positioned dynamically adjacent to active selection bounding box in Z6.
- **Components**:
  - **Duplicate**: Clones selected stroke group or image.
  - **Delete**: Removes selected object with undo history.
  - **Lock**: Freezes object position preventing accidental manipulation.
  - **Reorder Layer**: Bring Forward, Send Backward actions.

---

## State & Data Persistence
- **Storage Model**: Compact byte blob vector stream storing stroke coordinates `(x, y, pressure)` relative to virtual infinite canvas space.
- **Room Entity**: `CanvasNote` linked to `CanvasLayer`, `CanvasStroke`, and `CanvasImage` child records.
- **Autosave Pipeline**: 400ms debounced background coroutine writing delta mutations without blocking UI thread.
