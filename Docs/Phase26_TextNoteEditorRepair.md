<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Phase 26 - TextNoteEditorRepair

## Goal
Comprehensive repair of the Fotara Text Note Editor for Release 1.6.0 (Batch 1 of 3). Restores robust on-device editing invariants, real drawn interactive checklist checkboxes, list indentation (0..3 levels with 24dp visual indent), predictable Enter/Backspace keyboard handlers without text-length comparisons, block-level deletion of app-drawn elements (dividers, markers), and a fully reachable keyboard-docked formatting toolbar on 360dp mobile viewports.

## Scope
- Extract the `BasicTextField` change handler from `TextNoteEditorScreen.kt` into a standalone, pure, deterministic `EditorChangeHandler.kt` testable with `(oldValue: TextFieldValue, newValue: TextFieldValue)`.
- Real drawn checkbox widget on every line in edit mode (including caret line) and in preview mode, with instant tap toggling, strikethrough + dimming when checked, and atomic caret isolation from raw `- [ ] ` syntax.
- Reliable Enter key handling supporting item continuation, mid-text splitting, and designed empty-item list exit without character deletion.
- Indent and Outdent tool actions with 0..3 level limits (0, 2, 4, 6 spaces in Markdown), 24dp visual indentation per level, and dynamic toolbar button enablement (enabled only when caret/selection touches list items).
- Block deletion rules for app-drawn elements: Backspace after marker strips marker; tapping divider selects it for single-key deletion; Backspace from line below divider removes divider.
- Full toolbar reachability and button state validation for 360dp screens with generous end padding and correct active/enabled states.
- Dedicated unit test suite reproducing S1-S4 and validating target behavior with simulated IME commit sequences.

## Out Of Scope
- Database schema changes (`text_notes.body_markdown` remains canonical standard Markdown).
- Version number bump or APK release packaging (deferred to Batch 3 of 1.6.0).
- Settings, folder management, OCR, or canvas features.

---

## Phase 1 - Audit & Root Cause Analysis

### A. Component Implementation Audit Table

| Item | Status | File & Function Evidence | Notes |
| :--- | :--- | :--- | :--- |
| **Real checkbox in edit mode** | **Regressed** | `MarkdownVisualTransformation.kt` (`filter()`, lines 125-135) | Inserts Unicode text glyphs `"☐ "` / `"☑ "` into the transformed string instead of custom drawn vector boxes. If font `ElmsSans` lacks the glyph, it falls back to tofu, system font, or raw text. |
| **Checkbox toggle in edit mode** | **Regressed** | `TextNoteEditorScreen.kt` (`pointerInput`, lines 532-550), `TextEditorOps.kt` (`toggleChecklistAtOffset`) | Touch hit detection checked `clickOffset in lineStart..(lineStart + 2)` which fails on indented lines and wraps. Furthermore, `charOffset - 1` in `toggleChecklistAtOffset` matched previous line when cursor was at line start. |
| **Checkbox toggle in preview mode** | **Implemented** | `RichMarkdownText.kt` (`RichMarkdownColumn`, lines 410-469) | Toggles line index via `toggleChecklistAtLine`. However, preview used text glyphs `"☐"`/`"☑"` without strikethrough. |
| **Indent button & wiring** | **Regressed** | `EditorToolbar.kt` (lines 167-170), `TextEditorOps.kt` (`indent`) | Indent button existed in Group 4 and added 2 spaces to markdown, but was unconditionally enabled and visual 2-space proportional font indent was <2dp (imperceptible on screen). |
| **Outdent button & wiring** | **Regressed** | `EditorToolbar.kt` (lines 171-175), `TextEditorOps.kt` (`outdent`) | Existed, but was unconditionally enabled even at level 0 (doing nothing) and had no active state. |
| **Enter key handling** | **Regressed** | `TextNoteEditorScreen.kt` (`onValueChange`, lines 572-583), `TextEditorOps.kt` (`handleEnterKey`) | Detected Enter by string length check `newText.length == oldText.length + 1` which breaks under IME composing regions, autocorrect, and commits, causing buffer mismatch and corrective backward character deletion. |
| **Space marker rules** | **Implemented** | `TextEditorOps.kt` (`handleTypingShortcut`, lines 595-628) | Triggers `- `, `* `, `1. `, `[] `, `# `, `## `, `### `, `> ` shortcuts at line start. Space does not delete markers. |
| **Backspace marker rules** | **Regressed** | `TextNoteEditorScreen.kt` (lines 585-592), `TextEditorOps.kt` (`handleBackspaceKey`) | Backspace checked length decrease and passed `oldBody` rather than processing the exact delete event, occasionally deleting item characters rather than removing only the marker. |
| **Toolbar buttons reachability (360dp)** | **Regressed** | `EditorToolbar.kt` (lines 68-73) | 17+ buttons of 48dp each with 24dp end padding inside an IME-padded column caused rightmost buttons to be cut off or intercepted by Android gesture navigation. |
| **Info section and counts** | **Implemented** | `TextNoteEditorScreen.kt` (lines 250-272), `EditorState.kt` (lines 273-276) | Lives in 3-dot overflow menu; accurately counts words and characters using `editor_info_counts` string resource. |

### B. Pure Function Call Sites & Handler Architecture

| Pure Function (`EditorActions.kt`) | Composable / State Call Site | Status |
| :--- | :--- | :--- |
| `toggleBold` | `EditorToolbar.kt` (line 96) via `state.executeAction` | Active |
| `toggleItalic` | `EditorToolbar.kt` (line 103) via `state.executeAction` | Active |
| `toggleStrikethrough` | `EditorToolbar.kt` (line 108) via `state.executeAction` | Active |
| `toggleInlineCode` | `EditorToolbar.kt` (line 114) via `state.executeAction` | Active |
| `toggleHeading` | `EditorToolbar.kt` (lines 124, 130, 136) via `state.executeAction` | Active |
| `toggleBlockquote` | `EditorToolbar.kt` (line 142) via `state.executeAction` | Active |
| `toggleBulletList` | `EditorToolbar.kt` (line 152) via `state.executeAction` | Active |
| `toggleNumberedList` | `EditorToolbar.kt` (line 158) via `state.executeAction` | Active |
| `toggleChecklist` | `EditorToolbar.kt` (line 164) via `state.executeAction` | Active |
| `handleEnterKey` | `TextNoteEditorScreen.kt` (line 578) inside inline lambda | Regressed (inline) |
| `handleBackspaceKey` | `TextNoteEditorScreen.kt` (line 587) inside inline lambda | Regressed (inline) |
| `handleTypingShortcut` | `TextNoteEditorScreen.kt` (line 595) inside inline lambda | Regressed (inline) |
| `indent` | `EditorToolbar.kt` (line 170) via `state.executeAction` | Active |
| `outdent` | `EditorToolbar.kt` (line 174) via `state.executeAction` | Active |
| `toggleCodeBlock` | `EditorToolbar.kt` (line 188) via `state.executeAction` | Active |
| `insertDivider` | `EditorToolbar.kt` (line 194) via `state.executeAction` | Active |
| `insertOrEditLink` | `EditorState.kt` (line 364) via `confirmLinkDialog` | Active |
| `removeLink` | None | Missing (no call sites) |
| `clearFormatting` | `EditorToolbar.kt` (line 208) via `state.executeAction` | Active |
| `toggleChecklistAtOffset` | `EditorState.kt` (line 223) via `TextNoteEditorScreen` pointer input | Active |
| `toggleChecklistAtLine` | `EditorState.kt` (line 229) via `RichMarkdownColumn` preview toggle | Active |

**Change Handler Architecture**:
The `BasicTextField` change handler in `TextNoteEditorScreen.kt` lines 557-603 was previously an inline anonymous lambda containing mutable state captures and complex conditional branches. To guarantee deterministic unit testing, it is extracted into:
```kotlin
object EditorChangeHandler {
    fun processChange(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue
}
```
`TextNoteEditorScreen.kt` will invoke this single entry point:
```kotlin
onValueChange = { newBody ->
    val processed = EditorChangeHandler.processChange(state.bodyValue, newBody)
    state.onBodyChange(processed)
}
```

### C. Root Causes for S1 - S5

1. **S1 (Checkbox text vs drawn box, toggle failure)**:
   - *Cause 1*: `MarkdownVisualTransformation` emitted raw Unicode characters `"☐ "` (U+2610) and `"☑ "` (U+2611). `ElmsSans` lacks these glyphs, triggering erratic system font fallbacks or tofu symbols.
   - *Cause 2*: The tap detector in `TextNoteEditorScreen.kt` relied on `clickOffset in lineStart..(lineStart + 2)` which completely failed when lines were indented with spaces or wrapped.
   - *Cause 3*: `TextEditorOps.toggleChecklistAtOffset` used `charOffset - 1` to find line boundaries, causing offsets at line start to target the preceding line.
2. **S2 (Indent & Outdent buttons do nothing / no feedback)**:
   - *Cause 1*: Indent added 2 spaces (`"  "`), which in proportional font `ElmsSans` renders at ~1.5dp width, visually appearing identical to unindented text.
   - *Cause 2*: Buttons lacked conditional enabled state (`canIndent`, `canOutdent`), remaining active even when no list item was touched or when boundary levels (0 or 3) were reached.
3. **S3 (Enter removes character backward instead of new line/item)**:
   - *Cause 1*: The inline change handler evaluated Enter using `newText.length == oldText.length + 1`. When soft keyboard IMEs (Gboard/Samsung Keyboard) committed composing regions while injecting `\n`, this length condition failed or produced out-of-sync text values.
   - *Cause 2*: When `handleEnterKey(oldBody)` returned a transformed state, the IME detected a buffer discrepancy and dispatched a corrective backspace, which the app interpreted as user backspace, deleting a character backward.
4. **S4 (App-drawn blocks cannot be deleted as a block)**:
   - *Cause 1*: Backspace logic did not handle caret positioned at the content boundary of drawn blocks; instead of stripping the entire marker atomically, standard delete operations engaged or failed.
   - *Cause 2*: Dividers (`---`) had no tap-selection state and could not be removed cleanly by pressing Backspace from the line below.
5. **S5 (Toolbar right end cut off on 360dp screen)**:
   - *Cause 1*: 17 buttons of 48dp each across 6 groups exceeded 850dp total width. The scroll container had only 24dp trailing padding, leaving the final utilities inside Android's system back-gesture swipe zone.
   - *Cause 2*: Missing auto-scroll or reachability hints caused users to miss unreachable controls.

---

## Phase 2 - Target Behavior Specification

### 2.1 Checkbox & Block Markers
- In edit mode, checklist lines display a real custom drawn box (size 18dp x 18dp, 4dp rounded corners) on every line (including caret line).
  - Unchecked: 1.8dp outline in `AccentGold`.
  - Checked: filled with `AccentGold`, crisp checkmark icon inside, with dimmed content text and strikethrough.
- Raw `- [ ] ` syntax is never visible, and caret cannot enter inside brackets or prefix markers.
- Tapping the checkbox box toggles state immediately in both edit mode and preview mode, preserving keyboard focus and caret position as 1 undo step.
- Bullet (`•`), numbered list (`1.`), quote bar (`▎`), and divider (`────────────────────────`) render consistently without raw markdown clutter. Inline formatting markers hide unless caret is within that span.

### 2.2 Keyboard Handling
- **Enter on an item WITH text**: Inserts a new item of the same type below (checkbox unchecked, numbered +1, preserving indent), placing caret in the new item; no character deleted.
- **Enter in the middle of item text**: Splits the line; remainder moves to new item below.
- **Enter on an EMPTY item**: Exits the list, transforming the line into a plain paragraph.
- **Backspace right after a marker**: Removes only the marker; if indent > 0, outdents one level first.
- **Space**: Never deletes or converts existing markers.
- **Line-start shortcuts**: Typing `- `, `* `, `1. `, `[] `, `# `, `## `, `### `, `> ` at line start transforms line into corresponding block.
- **Multi-line paste**: Kept intact without triggering Enter logic.
- Numbered list renumbers contiguous runs after insertions or deletions.

### 2.3 Indent and Outdent
- Both buttons active and enabled ONLY when caret or selection touches list items (bullet, numbered, checkbox).
- Levels 0 to 3: Indent adds 2 spaces in Markdown; outdent removes 2 spaces.
- Visual display provides ~24dp visual indentation per level.
- Enter preserves current indentation level; Backspace at the start of an indented item outdents first.
- Indent disabled at level 3; outdent disabled at level 0. Caret preserves relative text position.

### 2.4 Deleting App-Drawn Blocks
- Backspace or Delete with caret at start of block content removes the marker atomically.
- Dividers: Tapping selects the divider (visible highlight); Backspace or Delete removes the entire line; Backspace from the start of the line below removes the divider line cleanly.
- Long-press on bullet, number, or quote marker selects the full block line for deletion.

### 2.5 Toolbar Button Inventory

| Group | Button | Icon / Label | Enabled Condition | Active Condition | Action |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **G1** | Undo | `Undo` | `state.canUndo` | N/A | Reverts last editing operation |
| **G1** | Redo | `Redo` | `state.canRedo` | N/A | Re-applies reverted operation |
| **G2** | Bold | `FormatBold` | Always | Caret in bold span | Wraps/unwraps `**text**` |
| **G2** | Italic | `FormatItalic` | Always | Caret in italic span | Wraps/unwraps `*text*` |
| **G2** | Strikethrough | `FormatStrikethrough` | Always | Caret in strike span | Wraps/unwraps `~~text~~` |
| **G2** | Inline Code | `Code` | Always | Caret in code span | Wraps/unwraps `` `text` `` |
| **G3** | Heading 1 | `H1` | Always | Line is H1 | Toggles `# ` prefix |
| **G3** | Heading 2 | `H2` | Always | Line is H2 | Toggles `## ` prefix |
| **G3** | Heading 3 | `H3` | Always | Line is H3 | Toggles `### ` prefix |
| **G3** | Blockquote | `FormatQuote` | Always | Line is Quote | Toggles `> ` prefix |
| **G4** | Bullet List | `FormatListBulleted` | Always | Line is Bullet | Toggles `- ` prefix |
| **G4** | Numbered List | `FormatListNumbered` | Always | Line is Numbered | Toggles `1. ` prefix |
| **G4** | Checklist | `CheckBox` | Always | Line is Checkbox | Toggles `- [ ] ` prefix |
| **G4** | Indent | `FormatIndentIncrease` | Touches list item & level < 3 | N/A | Indents 2 spaces |
| **G4** | Outdent | `FormatIndentDecrease` | Touches list item & level > 0 | N/A | Outdents 2 spaces |
| **G5** | Insert Link | `Link` | Always | N/A | Opens Link Dialog |
| **G5** | Code Block | `DataObject` | Always | N/A | Toggles ```` ``` ```` code block |
| **G5** | Divider | `HorizontalRule` | Always | N/A | Inserts `---` divider line |
| **G6** | Find & Replace | `FindReplace` | Always | Find bar visible | Toggles Find & Replace bar |
| **G6** | Clear Formatting | `FormatClear` | Has content | N/A | Strips markdown formatting |
| **G6** | Schedule Note | `Alarm` | Always | N/A | Opens Schedule Dialog |

---

## Acceptance Criteria
- [x] Full audit tables (Implemented/Missing/Regressed, pure function call sites, toolbar inventory) completed in documentation.
- [x] `EditorChangeHandler.processChange` extracted and wired to `BasicTextField`.
- [x] Unit tests reproducing S1-S4 written and verified failing before fix.
- [x] Real drawn checkboxes in edit mode and preview with instant tap toggle and strikethrough.
- [x] Enter and Backspace logic operates without length-comparison bugs across IME and physical inputs.
- [x] Indent and Outdent properly restricted to 0..3 levels with 24dp visual indentation and enabled states.
- [x] App-drawn blocks (dividers, markers) atomically deleted without syntax debris.
- [x] Toolbar fully reachable on 360dp display with 64dp end clearance.
- [x] All unit tests pass with zero regressions.
