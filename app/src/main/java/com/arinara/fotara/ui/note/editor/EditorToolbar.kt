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
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Link
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

private val ToolbarBg = Color(0xFF141936)
private val ToolbarBorder = Color(0xFF28325E)
private val TabCream = Color(0xFFEAE3D2)
private val AccentGold = Color(0xFFF77F00)
private val TextMuted = Color(0xFF8E9AAF)

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
                .padding(start = 8.dp, end = 24.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group 1: Undo & Redo
            ToolbarActionItem(
                icon = Icons.AutoMirrored.Filled.Undo,
                description = "Undo",
                enabled = state.canUndo,
                onClick = { state.undo() }
            )
            ToolbarActionItem(
                icon = Icons.AutoMirrored.Filled.Redo,
                description = "Redo",
                enabled = state.canRedo,
                onClick = { state.redo() }
            )

            GroupDivider()

            // Group 2: Inline Formatting [Bold, Italic, Strikethrough, Inline Code]
            ToolbarActionItem(
                icon = Icons.Default.FormatBold,
                description = "Bold",
                isActive = state.isBold,
                onClick = { state.executeAction { EditorActions.toggleBold(it) } }
            )
            ToolbarActionItem(
                icon = Icons.Default.FormatItalic,
                description = "Italic",
                isActive = state.isItalic,
                onClick = { state.executeAction { EditorActions.toggleItalic(it) } }
            )
            ToolbarActionItem(
                icon = Icons.Default.FormatStrikethrough,
                description = "Strikethrough",
                isActive = state.isStrikethrough,
                onClick = { state.executeAction { EditorActions.toggleStrikethrough(it) } }
            )
            ToolbarActionItem(
                icon = Icons.Default.Code,
                description = "Inline Code",
                isActive = state.isInlineCode,
                onClick = { state.executeAction { EditorActions.toggleInlineCode(it) } }
            )

            GroupDivider()

            // Group 3: Headings & Blockquote [H1, H2, H3, Quote]
            ToolbarTextActionItem(
                label = "H1",
                description = "Heading 1",
                isActive = state.isH1,
                onClick = { state.executeAction { EditorActions.toggleHeading(it, 1) } }
            )
            ToolbarTextActionItem(
                label = "H2",
                description = "Heading 2",
                isActive = state.isH2,
                onClick = { state.executeAction { EditorActions.toggleHeading(it, 2) } }
            )
            ToolbarTextActionItem(
                label = "H3",
                description = "Heading 3",
                isActive = state.isH3,
                onClick = { state.executeAction { EditorActions.toggleHeading(it, 3) } }
            )
            ToolbarActionItem(
                icon = Icons.Default.FormatQuote,
                description = "Blockquote",
                isActive = state.isQuote,
                onClick = { state.executeAction { EditorActions.toggleBlockquote(it) } }
            )

            GroupDivider()

            // Group 4: Lists & Indentation [Bullet, Numbered, Checklist, Indent, Outdent]
            ToolbarActionItem(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                description = "Bullet List",
                isActive = state.isBulletList,
                onClick = { state.executeAction { EditorActions.toggleBulletList(it) } }
            )
            ToolbarActionItem(
                icon = Icons.Default.FormatListNumbered,
                description = "Numbered List",
                isActive = state.isNumberedList,
                onClick = { state.executeAction { EditorActions.toggleNumberedList(it) } }
            )
            ToolbarActionItem(
                icon = Icons.Default.CheckBox,
                description = "Checklist",
                isActive = state.isChecklist,
                onClick = { state.executeAction { EditorActions.toggleChecklist(it) } }
            )
            ToolbarActionItem(
                icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                description = "Indent",
                onClick = { state.executeAction { EditorActions.indent(it) } }
            )
            ToolbarActionItem(
                icon = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                description = "Outdent",
                onClick = { state.executeAction { EditorActions.outdent(it) } }
            )

            GroupDivider()

            // Group 5: Insertions [Link, Code block, Divider]
            ToolbarActionItem(
                icon = Icons.Default.Link,
                description = "Insert Link",
                onClick = { state.openLinkDialog() }
            )
            ToolbarActionItem(
                icon = Icons.Default.DataObject,
                description = "Code Block",
                onClick = { state.executeAction { EditorActions.toggleCodeBlock(it) } }
            )
            ToolbarActionItem(
                icon = Icons.Default.HorizontalRule,
                description = "Horizontal Divider",
                onClick = { state.executeAction { EditorActions.insertDivider(it) } }
            )

            GroupDivider()

            // Group 6: Utilities [Find, Clear formatting, Schedule]
            ToolbarActionItem(
                icon = Icons.Default.FindReplace,
                description = "Find and Replace",
                isActive = state.showFindReplace,
                onClick = { state.showFindReplace = !state.showFindReplace }
            )
            ToolbarActionItem(
                icon = Icons.Default.FormatClear,
                description = "Clear Formatting",
                onClick = { state.executeAction { EditorActions.clearFormatting(it) } }
            )
            ToolbarActionItem(
                icon = Icons.Default.Alarm,
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
            .padding(horizontal = 6.dp)
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
    val bgColor = if (isActive) AccentGold.copy(alpha = 0.22f) else Color.Transparent
    val tint = when {
        !enabled -> TextMuted.copy(alpha = 0.35f)
        isActive -> AccentGold
        else -> TabCream
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .padding(2.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(22.dp)
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
    val bgColor = if (isActive) AccentGold.copy(alpha = 0.22f) else Color.Transparent
    val tint = when {
        !enabled -> TextMuted.copy(alpha = 0.35f)
        isActive -> AccentGold
        else -> TabCream
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .padding(2.dp)
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
