// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.online

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.arinara.fotara.R
import com.arinara.fotara.ui.components.RichMarkdownBlockquote
import com.arinara.fotara.ui.components.RichMarkdownText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScreenNavy = Color(0xFF03071E)
private val TabCream = Color(0xFFEAE3D2)
private val AccentGold = Color(0xFFF77F00)
private val CardBg = Color(0xFF141936)
private val CardBorder = Color(0xFF242C56)
private val ColorGreen = Color(0xFF2A9D8F)
private val ColorRed = Color(0xFFD62828)
private val ColorBlue = Color(0xFF3A86FF)

private enum class NoteSectionType(
    val defaultTitle: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    WHATS_NEW("What's New", Icons.Default.NewReleases, AccentGold),
    PATCHES("Patches & Fixes", Icons.Default.CheckCircle, ColorGreen),
    BREAKING("Breaking Changes", Icons.Default.Warning, ColorRed),
    NOTES("Release Notes", Icons.Default.Info, ColorBlue),
    GENERAL("Highlights", Icons.Default.Info, TabCream)
}

private data class ParsedSection(
    val title: String,
    val type: NoteSectionType,
    val items: List<String>
)

private fun parseReleaseNotes(rawText: String): List<ParsedSection> {
    val sections = mutableListOf<ParsedSection>()
    var currentTitle = "What's New"
    var currentType = NoteSectionType.WHATS_NEW
    val currentItems = mutableListOf<String>()

    fun flushCurrent() {
        if (currentItems.isNotEmpty()) {
            sections.add(ParsedSection(currentTitle, currentType, currentItems.toList()))
            currentItems.clear()
        }
    }

    val lines = rawText.lines()
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("![") || trimmed.startsWith("# Fotara")) {
            continue
        }

        if (trimmed.startsWith("## ")) {
            flushCurrent()
            val headerText = trimmed.removePrefix("## ").trim()
            currentTitle = headerText
            currentType = when {
                headerText.contains("Breaking", ignoreCase = true) -> NoteSectionType.BREAKING
                headerText.contains("Patch", ignoreCase = true) || headerText.contains("Fix", ignoreCase = true) -> NoteSectionType.PATCHES
                headerText.contains("New", ignoreCase = true) || headerText.contains("Feature", ignoreCase = true) -> NoteSectionType.WHATS_NEW
                else -> NoteSectionType.NOTES
            }
        } else if (trimmed.startsWith("### ")) {
            val subHeader = trimmed.removePrefix("### ").trim()
            currentItems.add("[$subHeader]")
        } else if (trimmed.startsWith("> ") || trimmed.startsWith(">")) {
            currentItems.add(trimmed)
        } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            val itemText = trimmed.substring(2).trim()
            if (itemText.isNotEmpty()) {
                currentItems.add(itemText)
            }
        } else if (trimmed.length > 3 && !trimmed.startsWith("#")) {
            currentItems.add(trimmed)
        }
    }

    flushCurrent()

    if (sections.isEmpty()) {
        sections.add(
            ParsedSection(
                title = "What's New in Fotara",
                type = NoteSectionType.WHATS_NEW,
                items = listOf(
                    "High-contrast Amber LinkIt indicators across folders and detail views",
                    "Dedicated multi-select bottom contextual action dock",
                    "Offline-first persistent feedback queue with automatic sync",
                    "Integrated folder and subfolder suggestions in the camera triage modal",
                    "Real 90-degree image rotation in capture review",
                    "Dynamic package versioning and in-app updates with download tracking"
                )
            )
        )
    }

    return sections
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewScreen(
    updateManager: UpdateManager,
    onBack: () -> Unit
) {
    val release by updateManager.latestRelease.collectAsState()
    var isExiting by remember { mutableStateOf(false) }
    val safeBack = {
        if (!isExiting) {
            isExiting = true
            onBack()
        }
    }

    val versionText = release?.version ?: "Fotara v1.5.0 Beta"
    val defaultNotes = """
        ## What's New
        - Unlimited Drawing Canvas (Alpha): Introducing an infinite 2D vector drawing canvas note type with freeform pan, pinch-to-zoom, pressure-sensitive pen, highlighter, eraser, multi-layer management, image attachments, and high-resolution PNG export.
        - Universal Note Scheduling: Attach custom date and time reminder schedules to any note type (Photos, Groups, PDFs, Word DOCX, Text Notes, and Canvas) with user choice between standard notifications and ringing alarm alerts.
        - Dedicated "Today" Home Widget: A new rectangular Jetpack Glance widget displaying assignments due today, newly captured study notes, and upcoming scheduled alerts with instant deep-linking.
        - Share to Fotara: Seamlessly share images, PDFs, Word documents, Markdown, and plain text directly from other Android apps into Fotara with an intuitive multi-item destination placement screen.
        - Search Date Filtering: Filter notes instantly by date added with quick chips (Today, Yesterday, This week, This month, This year) and custom single-day or date-range pickers.
        - Rich Text Formatting Toolbar: Upgraded native text editor toolbar with full support for bold, italic, bold-italic, strikethrough, headings, inline code, link creation, dividers, and interactive checklists.
        - In-Viewer PDF Zoom: Smooth pinch-to-zoom and two-axis panning directly on PDF pages inside the native viewer with sharp on-demand viewport rasterization.

        ## Changed
        - Global English Standardization: Standardized all application text, dialogs, error messages, settings, notifications, widgets, and release notes exclusively in English.
        - Redesigned LinkIt Corner Glow: Replaced the loud amber pill badge with a subtle, elegant radial corner glow that remains crisp and uniform from Android 7.0 (API 24) to Android 16 (API 36).
        - Intelligent PDF Split to Images: PDF splitting now generates high-fidelity white-canvas images without dark artifacts, automatically grouping documents with 5 or more pages into a Photo Group while preserving page order.
        - Tightened Feedback Limits: Enforced a 5-submission rolling 24-hour quota and a 60-second cooldown timer on feedback and bug reports to ensure reliable service delivery.

        ## Fixed
        - Fixed intermittent UI freezing and frame stutter when scrolling through 30+ page PDF documents.
        - Fixed HTTP 400 Bad Request errors when submitting feedback and bug reports to the Supabase backend.
        - Fixed non-functional bold, italic, strikethrough, heading, and quote buttons in the native text note editor.
        - Fixed transparent page rasterization artifacts in PDF rendering and Split to Images export.
        - Removed legacy "Sharp PDF Note" label across all screen subtitles and headers.
    """.trimIndent()

    val rawNotes = release?.releaseNotes?.ifBlank { defaultNotes } ?: defaultNotes
    val sections = remember(rawNotes) { parseReleaseNotes(rawNotes) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "What's New",
                    color = TabCream,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = safeBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TabCream
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenNavy)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                val bannerModel = release?.bannerUrl?.takeIf { it.isNotBlank() }
                    ?: R.drawable.fotara_banner_1_5
                AsyncImage(
                    model = bannerModel,
                    contentDescription = "Fotara 1.5 Banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hero Release Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AccentGold.copy(alpha = 0.18f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = versionText,
                                color = TabCream,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentGold.copy(alpha = 0.20f),
                                border = BorderStroke(0.8.dp, AccentGold)
                            ) {
                                Text(
                                    text = "Latest",
                                    color = AccentGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Release Highlights & Improvements",
                            color = TabCream.copy(alpha = 0.70f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Render each parsed section cleanly
            sections.forEach { section ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // Section Header with Icon and Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = section.type.icon,
                            contentDescription = null,
                            tint = section.type.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = section.title,
                            color = TabCream,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = CircleShape,
                            color = section.type.accentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${section.items.size}",
                                color = section.type.accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            section.items.forEachIndexed { idx, item ->
                                if (item.startsWith("[") && item.endsWith("]")) {
                                    // Sub-header (e.g. [1.3.1 - 2026-09-25])
                                    Text(
                                        text = item.removeSurrounding("[", "]"),
                                        color = section.type.accentColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = if (idx > 0) 10.dp else 0.dp, bottom = 6.dp)
                                    )
                                } else if (item.startsWith("> ") || item.startsWith(">")) {
                                    // Blockquote callout
                                    RichMarkdownBlockquote(
                                        quote = item.removePrefix(">").trim(),
                                        accentColor = section.type.accentColor,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Bullet dot
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 7.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(section.type.accentColor)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        RichMarkdownText(
                                            text = item,
                                            color = TabCream.copy(alpha = 0.92f),
                                            fontSize = 13.5.sp,
                                            lineHeight = 20.sp,
                                            accentColor = section.type.accentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button
            Button(
                onClick = safeBack,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Got It",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
