// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.outlined.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatClear
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.HomeAddButtonBlue
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.TextMuted

private val ToolbarBg = HomeCardSurface
private val ToolbarBorder = HomeCardBorder
private val TabCream = FolderTabCream
private val ActiveBlue = HomeAddButtonBlue
private val TextMutedColor = TextMuted

@Composable
fun EditorToolbar(
    state: EditorState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ToolbarBg,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 8.dp, end = 64.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group 1: Undo & Redo
            ToolbarActionItem(
                icon = Icons.AutoMirrored.Outlined.Undo,
                description = "Undo",
                enabled = state.canUndo,
                onClick = { state.undo() }
            )
            ToolbarActionItem(
                icon = Icons.AutoMirrored.Outlined.Redo,
                description = "Redo",
                enabled = state.canRedo,
                onClick = { state.redo() }
            )

            GroupDivider()

            // Group 2: Inline Formatting [Bold, Italic, Strikethrough, Inline Code, LaTeX Math]
            ToolbarActionItem(
                icon = Icons.Outlined.FormatBold,
                description = "Bold",
                isActive = state.isBold,
                onClick = { state.executeAction { EditorActions.toggleBold(it) } }
            )
            ToolbarActionItem(
                icon = Icons.Outlined.FormatItalic,
                description = "Italic",
                isActive = state.isItalic,
                onClick = { state.executeAction { EditorActions.toggleItalic(it) } }
            )
            ToolbarActionItem(
                icon = Icons.Outlined.FormatStrikethrough,
                description = "Strikethrough",
                isActive = state.isStrikethrough,
                onClick = { state.executeAction { EditorActions.toggleStrikethrough(it) } }
            )
            ToolbarActionItem(
                icon = Icons.Outlined.Code,
                description = "Inline Code",
                isActive = state.isInlineCode,
                onClick = { state.executeAction { EditorActions.toggleInlineCode(it) } }
            )
            ToolbarTextActionItem(
                label = "fx",
                description = "LaTeX Math ($...$)",
                isActive = state.isInlineMath || state.isBlockMath,
                onClick = { state.executeAction { EditorActions.toggleInlineMath(it) } }
            )

            GroupDivider()

            // Group 3: Headings & Blockquote [H1, H2, H3, Quote] (Disabled in tables)
            val blockToolsEnabled = !state.isInsideTable
            ToolbarTextActionItem(
                label = "H1",
                description = "Heading 1",
                isActive = state.isH1,
                enabled = blockToolsEnabled,
                onClick = { state.executeAction { EditorActions.toggleHeading(it, 1) } }
            )
            ToolbarTextActionItem(
                label = "H2",
                description = "Heading 2",
                isActive = state.isH2,
                enabled = blockToolsEnabled,
                onClick = { state.executeAction { EditorActions.toggleHeading(it, 2) } }
            )
            ToolbarTextActionItem(
                label = "H3",
                description = "Heading 3",
                isActive = state.isH3,
                enabled = blockToolsEnabled,
                onClick = { state.executeAction { EditorActions.toggleHeading(it, 3) } }
            )
            ToolbarActionItem(
                icon = Icons.Outlined.FormatQuote,
                description = "Blockquote",
                isActive = state.isQuote,
                enabled = blockToolsEnabled,
                onClick = { state.executeAction { EditorActions.toggleBlockquote(it) } }
            )

            GroupDivider()

            // Group 4: Lists & Indentation [Bullet, Numbered, Checklist, Indent, Outdent] (Disabled in tables)
            ToolbarActionItem(
                icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                description = "Bullet List",
                isActive = state.isBulletList,
                enabled = blockToolsEnabled,
                onClick = { state.executeAction { EditorActions.toggleBulletList(it) } }
            )
            ToolbarActionItem(
                icon = Icons.Outlined.FormatListNumbered,
                description = "Numbered List",
                isActive = state.isNumberedList,
                enabled = blockToolsEnabled,
                onClick = { state.executeAction { EditorActions.toggleNumberedList(it) } }
            )
            ToolbarActionItem(
                icon = Icons.Outlined.CheckBox,
                description = "Checklist",
                isActive = state.isChecklist,
                enabled = blockToolsEnabled,
                onClick = { state.executeAction { EditorActions.toggleChecklist(it) } }
            )
            ToolbarActionItem(
                icon = Icons.AutoMirrored.Outlined.FormatIndentIncrease,
                description = "Indent",
                enabled = state.canIndent && blockToolsEnabled,
                onClick = { state.executeAction { EditorActions.indent(it) } }
            )
            ToolbarActionItem(
                icon = Icons.AutoMirrored.Outlined.FormatIndentDecrease,
                description = "Outdent",
                enabled = state.canOutdent && blockToolsEnabled,
                onClick = { state.executeAction { EditorActions.outdent(it) } }
            )

            GroupDivider()

            // Group 5: Insertions [Link, Table, Code block, Divider]
            ToolbarActionItem(
                icon = Icons.Outlined.Link,
                description = "Insert Link",
                onClick = { state.openLinkDialog() }
            )
            ToolbarTextActionItem(
                label = "▦",
                description = "Insert Table",
                isActive = state.isInsideTable,
                onClick = { state.executeAction { EditorActions.insertTable(it) } }
            )
            ToolbarActionItem(
                icon = Icons.Outlined.DataObject,
                description = "Code Block",
                enabled = blockToolsEnabled,
                onClick = { state.executeAction { EditorActions.toggleCodeBlock(it) } }
            )
            ToolbarActionItem(
                icon = Icons.Outlined.HorizontalRule,
                description = "Horizontal Divider",
                enabled = blockToolsEnabled,
                onClick = { state.executeAction { EditorActions.insertDivider(it) } }
            )

            // Contextual Table Tools (Shown when cursor is inside table)
            if (state.isInsideTable) {
                GroupDivider()
                ToolbarTextActionItem(
                    label = "+R",
                    description = "Insert Row Below",
                    onClick = { state.executeAction { EditorActions.addTableRowBelow(it) } }
                )
                ToolbarTextActionItem(
                    label = "-R",
                    description = "Delete Row",
                    onClick = { state.executeAction { EditorActions.deleteCurrentTableRow(it) } }
                )
                ToolbarTextActionItem(
                    label = "+C",
                    description = "Insert Column Right",
                    onClick = { state.executeAction { EditorActions.addTableColumnRight(it) } }
                )
                ToolbarTextActionItem(
                    label = "-C",
                    description = "Delete Column",
                    onClick = { state.executeAction { EditorActions.deleteCurrentTableColumn(it) } }
                )
            }

            GroupDivider()

            // Group 6: Utilities [Find, Clear formatting, Schedule]
            ToolbarActionItem(
                icon = Icons.Outlined.FindReplace,
                description = "Find and Replace",
                isActive = state.showFindReplace,
                onClick = { state.showFindReplace = !state.showFindReplace }
            )
            ToolbarActionItem(
                icon = Icons.Outlined.FormatClear,
                description = "Clear Formatting",
                enabled = state.bodyValue.text.isNotEmpty(),
                onClick = { state.executeAction { EditorActions.clearFormatting(it) } }
            )
            ToolbarActionItem(
                icon = Icons.Outlined.Alarm,
                description = "Schedule Note",
                onClick = { state.openScheduleDialog() }
            )
        }
    }
}

@Composable
private fun GroupDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .width(1.dp)
            .height(24.dp)
            .background(ToolbarBorder)
    )
}

@Composable
private fun ToolbarActionItem(
    icon: ImageVector,
    description: String,
    isActive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) ActiveBlue.copy(alpha = 0.22f) else Color.Transparent
    val tint = when {
        !enabled -> TextMutedColor.copy(alpha = 0.38f)
        isActive -> ActiveBlue
        else -> TabCream
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .padding(4.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ToolbarTextActionItem(
    label: String,
    description: String,
    isActive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) ActiveBlue.copy(alpha = 0.22f) else Color.Transparent
    val tint = when {
        !enabled -> TextMutedColor.copy(alpha = 0.38f)
        isActive -> ActiveBlue
        else -> TabCream
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .padding(4.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
    ) {
        Text(
            text = label,
            color = tint,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
