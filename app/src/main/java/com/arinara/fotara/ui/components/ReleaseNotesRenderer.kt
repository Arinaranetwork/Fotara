// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.markdown.MarkdownTable
import com.arinara.fotara.ui.markdown.MarkdownTableParser
import com.arinara.fotara.ui.markdown.ReleaseNotesBlock
import com.arinara.fotara.ui.markdown.TableColumnAlignment

/**
 * Shared Release Notes Renderer composable used across NewUpdateDialog, What's New, and UpdateScreen.
 * Supports:
 * - Real GitHub-syntax Markdown tables with horizontal scrolling container and column alignment
 * - TalkBack accessibility semantics reading "row x, column y" and heading marking
 * - Cached parsing via remember with stable keys
 * - Inline formatting in cells (bold, italic, strikethrough, inline code, links)
 * - Section headers, lists, code blocks, and blockquotes
 */
@Composable
fun ReleaseNotesRenderer(
    markdown: String,
    modifier: Modifier = Modifier,
    primaryTextColor: Color = Color.White.copy(alpha = 0.92f),
    accentColor: Color = Color(0xFFF77F00),
    surfaceColor: Color = Color(0xFF0F131D),
    cardBorder: Color = Color(0xFF26324A),
    allowImages: Boolean = true
) {
    val blocks = remember(markdown) {
        MarkdownTableParser.parseReleaseNotes(markdown)
    }
    var selectedFullscreenImageUrl by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is ReleaseNotesBlock.Header -> {
                    val fontSize = when (block.level) {
                        1 -> 18.sp
                        2 -> 15.sp
                        else -> 13.5.sp
                    }
                    Text(
                        text = block.text,
                        color = if (block.level <= 2) accentColor else primaryTextColor,
                        fontSize = fontSize,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                is ReleaseNotesBlock.Paragraph -> {
                    RichMarkdownText(
                        text = block.text,
                        color = primaryTextColor.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        accentColor = accentColor,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                is ReleaseNotesBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        RichMarkdownText(
                            text = block.text,
                            color = primaryTextColor,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            accentColor = accentColor
                        )
                    }
                }

                is ReleaseNotesBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = block.number,
                                color = accentColor,
                                fontSize = 11.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        RichMarkdownText(
                            text = block.text,
                            color = primaryTextColor,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            accentColor = accentColor
                        )
                    }
                }

                is ReleaseNotesBlock.ChecklistItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.indentLevel * 24).dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (block.isChecked) accentColor else Color.Transparent)
                                .border(BorderStroke(1.5.dp, accentColor), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (block.isChecked) {
                                Text("✓", color = Color(0xFF03071E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        RichMarkdownText(
                            text = block.text,
                            color = if (block.isChecked) primaryTextColor.copy(alpha = 0.5f) else primaryTextColor,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            accentColor = accentColor
                        )
                    }
                }

                is ReleaseNotesBlock.Blockquote -> {
                    RichMarkdownBlockquote(
                        quote = block.text,
                        accentColor = accentColor,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                is ReleaseNotesBlock.CodeBlock -> {
                    RichMarkdownCodeBlock(
                        code = block.code,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                is ReleaseNotesBlock.Divider -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(cardBorder)
                            .padding(vertical = 6.dp)
                    )
                }

                is ReleaseNotesBlock.Table -> {
                    ReleaseNotesTableComposable(
                        table = block.table,
                        primaryTextColor = primaryTextColor,
                        accentColor = accentColor,
                        cardBorder = cardBorder,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                is ReleaseNotesBlock.Image -> {
                    if (allowImages) {
                        ReleaseNotesImageComposable(
                            block = block,
                            onImageClick = { selectedFullscreenImageUrl = block.url },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    selectedFullscreenImageUrl?.let { imageUrl ->
        FullscreenReleaseImageViewer(
            imageUrl = imageUrl,
            onDismiss = { selectedFullscreenImageUrl = null }
        )
    }
}

@Composable
private fun ReleaseNotesImageComposable(
    block: ReleaseNotesBlock.Image,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    if (!block.isAllowed || !com.arinara.fotara.legal.NetworkGate.isConsentGranted(context)) {
        // Blocked untrusted or non-HTTPS image placeholder
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF141936))
                .border(1.dp, Color(0xFF242C56), RoundedCornerShape(16.dp))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFFF77F00),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Blocked external image (${block.alt.ifBlank { "untrusted source" }})",
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = ElmsSans,
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F131D))
            .border(1.dp, Color(0xFF26324A), RoundedCornerShape(16.dp))
            .clickable {
                if (block.linkUrl != null) {
                    try {
                        uriHandler.openUri(block.linkUrl)
                    } catch (_: Exception) {
                        onImageClick()
                    }
                } else {
                    onImageClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(block.url)
                .crossfade(true)
                .build(),
            contentDescription = block.alt.ifBlank { "Release image" },
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color(0xFF60A5FA),
                        strokeWidth = 2.5.dp
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = block.alt.ifBlank { "Image unavailable" },
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontFamily = ElmsSans
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun FullscreenReleaseImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Fullscreen release image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(state = transformState)
            )

            // Close button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Dedicated Table Composable adhering to:
 * - Rounded container with 1dp border in cardBorder
 * - Bold header row on slightly different surface token (Color(0xFF161F33))
 * - Horizontal row dividers in theme token (Color(0xFF1E283D))
 * - Cell padding: 12dp x 8dp
 * - Alignment from separator row
 * - Horizontal scroll inside its own container without intercepting vertical scroll
 * - Accessibility collection info (TalkBack row x, column y)
 * - Minimum ~64dp, maximum ~220dp column width
 */
@Composable
private fun ReleaseNotesTableComposable(
    table: MarkdownTable,
    primaryTextColor: Color,
    accentColor: Color,
    cardBorder: Color,
    modifier: Modifier = Modifier
) {
    val totalRows = table.rows.size + 1
    val colCount = table.headers.size

    val tableScrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(12.dp))
            .background(Color(0xFF0F131D))
            .semantics {
                collectionInfo = CollectionInfo(rowCount = totalRows, columnCount = colCount)
            }
    ) {
        // Horizontal scroll container that does not fight vertical scrolling
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(tableScrollState)
        ) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .fillMaxWidth()
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(Color(0xFF161F33))
                ) {
                    table.headers.forEachIndexed { colIndex, headerText ->
                        val alignment = table.alignments.getOrElse(colIndex) { TableColumnAlignment.START }
                        val textAlign = when (alignment) {
                            TableColumnAlignment.START -> TextAlign.Start
                            TableColumnAlignment.CENTER -> TextAlign.Center
                            TableColumnAlignment.END -> TextAlign.End
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .widthIn(min = 64.dp, max = 220.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .semantics {
                                    heading()
                                    collectionItemInfo = CollectionItemInfo(
                                        rowIndex = 0,
                                        rowSpan = 1,
                                        columnIndex = colIndex,
                                        columnSpan = 1
                                    )
                                },
                            contentAlignment = when (alignment) {
                                TableColumnAlignment.START -> Alignment.CenterStart
                                TableColumnAlignment.CENTER -> Alignment.Center
                                TableColumnAlignment.END -> Alignment.CenterEnd
                            }
                        ) {
                            RichMarkdownText(
                                text = headerText,
                                color = primaryTextColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                accentColor = accentColor
                            )
                        }
                    }
                }

                // Body Rows
                table.rows.forEachIndexed { rowIndex, rowCells ->
                    // Divider between rows
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF1E283D))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(if (rowIndex % 2 == 1) Color(0xFF121724) else Color(0xFF0F131D))
                    ) {
                        rowCells.forEachIndexed { colIndex, cellText ->
                            val alignment = table.alignments.getOrElse(colIndex) { TableColumnAlignment.START }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .widthIn(min = 64.dp, max = 220.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .semantics {
                                        collectionItemInfo = CollectionItemInfo(
                                            rowIndex = rowIndex + 1,
                                            rowSpan = 1,
                                            columnIndex = colIndex,
                                            columnSpan = 1
                                        )
                                    },
                                contentAlignment = when (alignment) {
                                    TableColumnAlignment.START -> Alignment.CenterStart
                                    TableColumnAlignment.CENTER -> Alignment.Center
                                    TableColumnAlignment.END -> Alignment.CenterEnd
                                }
                            ) {
                                RichMarkdownText(
                                    text = cellText,
                                    color = primaryTextColor.copy(alpha = 0.90f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    accentColor = accentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
