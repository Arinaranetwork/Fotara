<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 16 - Text Note Editor Rework (Release 1.5.5)

## Goal
Rework the native Text Note Editor in Fotara to achieve "Notion-lite" reliability: eliminate trapped carets and broken checkbox syntax, provide predictable keyboard behavior (Enter list continuation, blank-line list exit, single-stroke prefix removal, Space immunity), overhaul live markdown visual transformation and offset mapping to hide inline delimiters when untouched, ensure all toolbar buttons execute their intended transformations, and relocate word/character counters into a dedicated Info section in the three-dot overflow menu.

---

## Diagnostic Report & Root-Cause Analysis

### Symptom 1 (S1): Checkbox Tool Inserts Brackets, Typed Text Trapped Inside, Box Untappable
- **Exact Reproduction**:
  1. Open a text note and tap the checklist tool button in the editor toolbar.
  2. The toolbar inserts `- [ ] ` at the line start.
  3. Because `MarkdownVisualTransformation` replaces `- [ ] ` with `☐ ` on inactive lines, `MarkdownOffsetMapping` maps transformed length 2 to original length 6 via linear progress: `progress = (clamped - tStart) * oLen / tLen`.
  4. Tapping the checkbox glyph on-screen lands at transformed offset 1 (`1 * 6 / 2 = 3`), which corresponds to original offset 3—between `[` and `]` in `- [ | ] `.
  5. The line becomes active, switching `isActiveLine` to true (identity mapping), locking the caret between the brackets.
  6. Subsequent typing writes inside `[teyss ]`, breaking the `- [ ] ` regex. The line falls back to matching bullet list `- `, rendering `• [teyss ]`.
  7. Checkbox glyph was not directly interactive in edit mode.
- **Root Cause**:
  1. `MarkdownOffsetMapping.transformedToOriginal` divides prefix regions linearly rather than pinning caret positions strictly before or after the prefix.
  2. `MarkdownVisualTransformation` previously reverted active lines to raw markdown characters, exposing raw bracket syntax directly to the user.
  3. No tap detector in the editor intercepted clicks on the checkbox sprite region to toggle check state.

### Symptom 2 (S2): Enter Key Deletes Prefix Instead of Continuing, Space Fragility
- **Exact Reproduction**:
  1. Tap a list or checklist tool on an empty line. A prefix (e.g. `- `, `1. `, or `- [ ] `) is inserted with caret immediately following it.
  2. Press Enter on the keyboard.
  3. In `TextEditorOps.kt:444`, `if (parsed.content.isBlank())` immediately triggers "exit list", deleting the newly created prefix.
  4. In `TextNoteEditorScreen.kt:500`, Enter was detected via `newBody.text.length == oldBody.text.length + 1 && newBody.text.getOrNull(oldBody.selection.min) == '\n'`. On predictive keyboards (Gboard) and composition input, multicharacter commits fail this condition.
  5. In some autocorrect and space scenarios, typing space caused prefix alterations.
- **Root Cause**:
  1. Naive character-length delta comparison (`length + 1`) is fragile with Android IMEs, composition, autocorrect, and hardware keyboards.
  2. Single Enter on an empty line with only a prefix immediately deleted the prefix instead of allowing user typing.
  3. No distinction between Enter on an empty item vs. Enter on an item with text.

### Symptom 3 (S3): Inconsistent Rendering & Broken Delimiters
- **Exact Reproduction**:
  1. Create a numbered list (`1. item`). It renders as literal `1. item` with no formatting because `NUMBERED_LIST` was completely absent from `MarkdownVisualTransformation.kt:103-219`.
  2. Type bold `**word**`, italic `*word*`, strikethrough `~~word~~`, code `` `word` ``, or link `[test](url)`. On inactive lines, `MarkdownVisualTransformation` applied `SpanStyle` but never hid delimiters; raw asterisks, tildes, backticks, and URLs remained in the text buffer.
  3. Insert a horizontal divider (`---`). VisualTransformation replaced 3 characters with 34 characters (`──────────────────────────────────`), causing extreme offset distortion and leaving stray `"—"` artifacts on adjacent lines.
  4. Toolbar buttons for indent, outdent, code block, and link either lacked boundary guards or caused misaligned carets.
- **Root Cause**:
  1. Missing parser branches in `MarkdownVisualTransformation`.
  2. Failure to hide inline delimiter spans when the caret is outside their boundaries.
  3. Huge character length discrepancies in divider transformation.

### Symptom 4 (S4): Top Bar Subtitle Wraps and Misaligns Header
- **Exact Reproduction**:
  1. Open a text note on any standard phone screen.
  2. TopAppBar title slot placed the title and `"28 words • 152 characters"` in a vertical `Column`.
  3. With navigation icon and 4 action buttons (Search, Preview, Share, Overflow), available title width is restricted, wrapping "characters" onto a second line.
- **Root Cause**:
  1. Subtitle occupied valuable top-bar horizontal real estate instead of following the established Phase 15 viewer pattern (single-line title + 3-dot menu Info section).

---

## Scope

### 1. Core Engine Rework (`TextEditorOps.kt` & `EditorActions.kt`)
- **Block Tool Line Operations**:
  - Support `H1`, `H2`, `H3`, `QUOTE`, `BULLET_LIST`, `NUMBERED_LIST`, `CHECKBOX`, `DIVIDER`.
  - Apply to all lines touched by selection. If all touched lines share the target type, toggle it off; otherwise convert/apply target type, preserving content and indent.
  - Caret placement rule: Caret always lands at the end of content or strictly after the prefix, never inside brackets or delimiters.
- **Keyboard Handling**:
  - **Enter on item with text**: Creates a new item below of the same type (checkbox unchecked, numbered list incremented by 1, identical indent), placing caret in the new item.
  - **Enter in middle of item text**: Splits the item at caret; remainder moves to new item below.
  - **Enter on empty item**: Exits the list, reverting the line to a plain paragraph at current indent.
  - **Backspace right after prefix**: Removes only the prefix, leaving content as a paragraph. If indented (>0), outdents one level first.
  - **Space**: Never deletes or alters a prefix. On an empty item, Space is normal text input.
  - **Typing shortcuts at line start**: `- ` or `* ` -> bullet; `1. ` -> numbered; `[] ` -> checkbox; `# ` `## ` `### ` -> headings; `> ` -> quote.
  - **Multi-line paste**: Preserves lines verbatim without triggering Enter continuation logic.
  - **Contiguous Numbered List Auto-Renumbering**: Renumbers sequences (`1.`, `2.`, `3.`) after insert, delete, or reorder, resetting on paragraph breaks.
- **Indentation & Outdentation**:
  - 2 spaces per level, bounded between min 0 and max 3 levels (0 to 6 spaces).
- **Inline Formatting**:
  - Bold (`**`), Italic (`*`), Strikethrough (`~~`), Inline Code (`` ` ``), Link (`[text](url)`).
  - Selection: wraps selection. If already wrapped, unwrap.
  - Collapsed caret: inserts markers and places caret between them.
  - Link: opens dialog (text + URL) and pre-populates existing link when caret is inside one.

### 2. Live Rendering & Offset Mapping (`MarkdownVisualTransformation.kt` & `MarkdownOffsetMapping.kt`)
- **Block Prefixes**: Always rendered as rich sprites (`• `, `1. `, `☐ `, `☑ `, `▎ `, `───`).
- **Caret Invariant**: Caret can NEVER land inside a prefix. Caret positions on prefixes clamp to the start of the line or start of the content.
- **Inline Mark Hiding**: Delimiters (`**`, `*`, `~~`, `` ` ``, `[`, `](url)`) are hidden when caret is outside the span; shown when caret is inside the span.
- **Checked Checkbox Styling**: Checked items display filled box (`☑ `), dimmed text (`#8E9AAF`), and strikethrough decoration.
- **Tappable Checkbox in Edit Mode**: Tapping the checkbox box toggles check state (`[ ]` <-> `[x]`) with undo history, keeping keyboard focus.

### 3. Toolbar Overhaul (`EditorToolbar.kt`)
- Horizontal scrolling row with proper end clearance padding so no button is cut off.
- Active states reflected accurately for cursor line/span.
- Remove redundant count box from toolbar to prevent horizontal bloat.
- Buttons: Undo, Redo, Bold, Italic, Strikethrough, Code, H1, H2, H3, Quote, Bullet, Numbered, Checkbox, Indent, Outdent, Link, Code Block, Divider, Clear Formatting, Schedule.

### 4. Top Bar & Info Overflow Menu (`TextNoteEditorScreen.kt`)
- TopAppBar title: single ellipsized line, vertically centered.
- Word and character counts relocated to three-dot overflow menu:
  - Header: non-clickable muted "INFO" (using `R.string.viewer_info_header`).
  - Text: non-clickable counts e.g. "28 words • 152 characters" (using `R.string.editor_info_counts`).
  - Secondary text: "100% Offline" (using `R.string.viewer_info_offline`).
  - Horizontal divider separating Info from action items (`Schedule...`).

---

## Out Of Scope
- Database schema changes (storage remains raw Markdown in `text_notes.body_markdown`).
- Migrating to external heavy rich-text libraries.
- Modifying unrelated screens (Photo viewer, PDF viewer, Canvas, Home, Settings).

---

## UI Mockup

```
Top Bar:
+-------------------------------------------------------------+
| (<-)  Edit Text Note                          (O) (Share) (:) |
+-------------------------------------------------------------+

Three-Dot Overflow Menu:
+------------------------------------+
| INFO                               |
| 28 words • 152 characters          |
| 100% Offline                       |
|------------------------------------|
| [Alarm] Schedule...                |
+------------------------------------+

Editor Body (Edit Mode):
+-------------------------------------------------------------+
| testing                                                     |
| [Oct 2, 6:00 PM]                                            |
|                                                             |
| heli (strikethrough formatted, tildes hidden)              |
| How can that be correct? (H2 sized)                         |
| ▎ does it benefit us? (Quote bar)                          |
| 1. j                                                        |
| • uwis                                                      |
| ────────────────────────────────────────── (Divider)        |
|                                                             |
| ☐ buy groceries (Tappable box icon)                         |
| ☑ done task (Strikethrough, dimmed)                         |
| [test](https://test.com) (Blue underlined link)             |
+-------------------------------------------------------------+

Docked Horizontal Toolbar:
+-------------------------------------------------------------+
| [Undo] [Redo] | [B] [I] [S] [`] | [H1] [H2] [H3] ["] | ... ->|
+-------------------------------------------------------------+
```

---

## Logic Notes

### Enter Key Disambiguation
```kotlin
fun onValueChange(newBody: TextFieldValue) {
    val oldBody = state.bodyValue
    val oldText = oldBody.text
    val newText = newBody.text
    val oldSel = oldBody.selection

    // Detect if Enter was pressed
    if (isEnterAction(oldText, oldSel, newText, newBody.selection)) {
        val handled = EditorActions.handleEnterKey(oldBody)
        if (handled != null) {
            state.onBodyChange(handled)
            return
        }
    }
    // Detect if Backspace was pressed
    if (isBackspaceAction(oldText, oldSel, newText, newBody.selection)) {
        val handled = EditorActions.handleBackspaceKey(oldBody)
        if (handled != null) {
            state.onBodyChange(handled)
            return
        }
    }
    // Detect typing shortcut at line start (e.g. "- ", "* ", "1. ", "[] ", "# ", "> ")
    val shortcutHandled = EditorActions.handleTypingShortcut(oldBody, newBody)
    if (shortcutHandled != null) {
        state.onBodyChange(shortcutHandled)
        return
    }

    state.onBodyChange(newBody)
}
```

### Monotonic Offset Mapping Invariants
- For any prefix sprite of original length `O` and transformed length `T`:
  - `originalToTransformed(offset)`: clamps `offset in 0..O` to `T`.
  - `transformedToOriginal(offset)`: clamps `offset in 0..T` to `O` (or `0` when at origin).
- Never interpolate linearly inside atomic prefixes like `[ ]`.

---

## Acceptance Criteria
- [x] Checkbox inserted via toolbar or shortcut places caret strictly after the prefix, never inside brackets.
- [x] Tapping checkbox toggles check state directly in edit mode without dismissing keyboard.
- [x] Enter on non-empty list/checkbox item generates next item with matching indent and auto-incremented numbering.
- [x] Enter in middle of item splits text cleanly.
- [x] Enter on empty list/checkbox item exits list to plain paragraph.
- [x] Backspace right after prefix deletes prefix, outdenting first if indented.
- [x] Space never deletes or corrupts prefixes.
- [x] Typing shortcuts (`- `, `* `, `1. `, `[] `, `# `, `## `, `### `, `> `) convert line start to corresponding block.
- [x] Numbered lists auto-renumber contiguous runs.
- [x] Delimiters (`**`, `*`, `~~`, `` ` ``, links) hide when caret is outside span and show when caret is inside.
- [x] Dividers render without stray characters or offset distortion.
- [x] Top bar title is single-line ellipsized; word/character counts live in 3-dot overflow menu under Info header.
- [x] All toolbar buttons are reachable via horizontal scroll and perform their documented transformations.
- [x] Pure-function unit tests and regression tests pass 100%.
