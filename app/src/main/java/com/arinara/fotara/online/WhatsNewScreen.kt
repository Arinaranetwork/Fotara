// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.online

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.ChannelPill
import com.arinara.fotara.ui.components.ReleaseNotesRenderer
import com.arinara.fotara.ui.components.RichMarkdownText

private val ScreenNavy = Color(0xFF03071E)
private val TabCream = Color(0xFFEAE3D2)
private val AccentGold = Color(0xFFF77F00)
private val CardBg = Color(0xFF141936)
private val CardBorder = Color(0xFF242C56)
private val ColorGreen = Color(0xFF2A9D8F)
private val ColorRed = Color(0xFFD62828)

private data class ReleaseChangelogEntry(
    val version: String,
    val channel: String, // "Stable" or "Beta"
    val releaseDate: String,
    val releaseUrl: String? = null,
    val breakingChanges: List<String> = emptyList(),
    val whatsNew: List<String> = emptyList(),
    val patchesAndFixes: List<String> = emptyList()
)

private val BundledReleases = listOf(
    ReleaseChangelogEntry(
        version = "1.7.1",
        channel = "Stable",
        releaseDate = "2026-10-05",
        releaseUrl = "https://github.com/Arinaranetwork/Fotara/releases/tag/v1.7.1",
        breakingChanges = emptyList(),
        whatsNew = listOf(
            "Zoom & Pan Smoothness Overhaul: Zero-recomposition GPU matrix transformations across Canvas and Photo Drawing, throttled viewport publishing (<=10Hz), and continuous centroid calculation eliminating stutter during pinch-to-zoom and multi-finger gestures.",
            "Screen Headers Descender & Layout Fix: Content-measured header heights and reserved tagline slot height across Home, Notes, and detail screens, eliminating text clipping on descenders and preventing vertical tab-switching jump across all font scales (0.85x to 2.0x).",
            "Workspace Tab Two-Stage Hold Gesture: Holding for ~400ms lifts tab and opens the action panel (kept open if finger is released); holding past ~1000ms enters Direct Move Mode with distinct haptic feedback, panel fade-out, 1.1x tab elevation, and instant horizontal drag reordering.",
            "Rich Release Notes & Update Banner Images: Native support for remote release banner artwork behind dark gradient scrims, GitHub markdown images, linked images, and HTML <img> tags with HTTPS allowlist validation and full-screen pinch-to-zoom inspection.",
            """
| Component | Improvement | User Impact |
|---|---|---|
| Canvas & Photo Drawing | GPU matrix transforms & <=10Hz publish | Smooth 60 FPS pinch-zoom |
| Screen Headers | Dynamic height & lineHeight padding | Zero descender clipping |
| Workspace Tabs | Two-stage hold for Direct Move Mode | Intuitive tab reordering |
| Update & Release Notes | Remote images & allowlist security | Rich release documentation |
            """.trimIndent()
        ),
        patchesAndFixes = listOf(
            "Prevented gesture cancellation spikes in PhotoDrawingEditor by stabilizing pointerInput modifier keys.",
            "Added vertical drag off-screen and shake gesture cancellation for workspace tab reordering.",
            "Enforced strict HTTPS domain allowlist rejecting unverified third-party image sources in release notes."
        )
    ),
    ReleaseChangelogEntry(
        version = "1.7.0",
        channel = "Stable",
        releaseDate = "2026-10-05",
        releaseUrl = "https://github.com/Arinaranetwork/Fotara/releases/tag/v1.7.0",
        breakingChanges = emptyList(),
        whatsNew = listOf(
            "Draw on Photo Notes: Annotate photo notes with freehand pen, natural highlighter with blend modes (Multiply, Darken, Screen), and an eraser tool with capsule hit testing. Includes 100-step undo/redo stack, 600ms debounced autosave, and non-destructive vector overlays.",
            "Export & Share Flattening: Photo notes with visible drawings automatically flatten into high-quality JPEG (quality 92) for direct sharing, PDF exports, and combined document generation.",
            "PDF Viewer Split Button Color Alignment: Aligned the 'Split to Images' action button tint with TabCream to maintain consistent top-bar styling alongside Share and viewer tools.",
            "Clean Channel Parsing: Integrated pure VersionInfo parser eliminating duplicate 'Beta' labels in update modals and settings cards while unifying channel pill presentation.",
            "Settings Pinned Title & Progressive Fade: Pinned Settings header with an ultra-smooth, zero-recomposition progressive vertical gradient fade driven by scroll state.",
            "Animated GIF Profile Banners: Support for animated GIF profile banners (Android 9+ / API 28+) with first-frame preview fallback, 8MB memory safety cap, and lifecycle-aware playback gating.",
            "Release Notes Markdown Tables: Full GitHub Flavored Markdown table syntax parsing with column alignments, alternating row highlights, horizontal scroll containers, and TalkBack accessibility semantics.",
            "Selection Mode Stability: Eliminated layout jumping and image blinking when toggling selection mode across Home and Folder Detail screens.",
            """
| Area | Changes | Impact |
|---|---|---|
| Photo Notes | Vector drawing tools & export flattening | Creative note annotation |
| PDF Viewer | Split button color alignment | Visual consistency |
| Versioning | Eliminated duplicate Beta pill | Clean release display |
| Settings | Pinned title & progressive fade | Smooth scroll UX |
| Profile | Animated GIF banner support | Dynamic customization |
| Release Notes | Markdown table parser & semantics | Rich release documentation |
| Selection Mode | Snapshot state & zero-shift overlays | Flicker-free browsing |
            """.trimIndent()
        ),
        patchesAndFixes = listOf(
            "Synchronized drawing coordinate transformations during 90° photo rotation and region cropping.",
            "Purged orphan drawing records automatically during startup maintenance and photo permanent deletion."
        )
    ),
    ReleaseChangelogEntry(
        version = "1.6.0",
        channel = "Stable",
        releaseDate = "2026-10-04",
        releaseUrl = "https://github.com/Arinaranetwork/Fotara/releases/tag/v1.6.0",
        breakingChanges = listOf(
            "Favorit and Arsip tabs were replaced by workspaces. Existing folders were moved to Home."
        ),
        whatsNew = listOf(
            "Workspaces Foundation: Organize study and project folders into dedicated workspaces. Switch seamlessly across tabs on the Home screen with custom names, custom order, and dedicated empty states.",
            "Add & Rename Workspaces: Create up to 10 workspaces with validation via the persistent (+) tab or overflow menu. Long-press any custom tab to quickly rename it.",
            "Drag & Drop Workspace Reordering: Press, hold, and drag workspace tabs horizontally with responsive haptic feedback to customize your workspace order, keeping Home firmly anchored at position 0.",
            "Move Folders Across Workspaces: Move single folders via the folder card menu or bulk move multiple folders in select mode with the new 'Move to workspace' action dialog.",
            "Delete Workspace with Content Controls: Delete custom workspaces with full control over their contents: move folders to Home safely, move all folders to Trash with 30-day recovery, or permanently erase everything.",
            "Workspace Destination Choice on Trash Restore: Restoring a folder from Trash or reviving an orphan note's parent folder prompts you to choose the destination workspace with a clear selector.",
            "Workspace-Grouped Folder Pickers: Folder selection dialogs across Folder Detail, Notes, Canvas Notes, Share Placement, and Trash organize folders with muted workspace headers.",
            "Workspace Search Filter Scope: Search features an always-visible workspace scope bar with 'All' and all workspaces, allowing you to instantly filter search queries across all note types.",
            "Real Interactive Checkboxes: Checklist items render custom-drawn rounded checkboxes with an Accent Gold outline for unchecked items and crisp checkmarks for checked items. Tapping toggles state with one undo step while keeping keyboard focus.",
            "Visual List Indentation (0 to 3 Levels): Nested bullet lists, numbered lists, and checklists display ~24dp indentation per level, supporting up to 3 indentation levels with reactive toolbar buttons.",
            "Atomic Block Deletion: App-drawn markers (bullets, numbers, quotes, checkboxes) and horizontal dividers delete as clean atomic units. Dividers can be tapped to select for single-key deletion or removed via Backspace from the line below.",
            "Canvas Selection & Free Eraser: Freeform lasso selection tool with interactive move, rotate, and stretch handles, along with a capsule sweep free eraser.",
            "Highlighter Blending: Natural highlighter stroke blending that preserves vector ink aesthetics without darkening overlapping lines.",
            "Profile Customization & Normalized Borders: Avatar decorative borders are normalized to a fixed-diameter circular profile picture with a 1.5% overlap seam, eliminating picture jumping and anchoring the edit button to 45 degrees.",
            "Searchable PDF Content: Automatic background OCR indexes text page-by-page off the main thread, making PDF documents immediately searchable without altering cards or viewer presentation.",
            "Unified Screen Header: Standardized screen headers across Home, Notes, and Settings with identical typography (38sp Medium Elms Sans), uniform padding, and reserved tagline height (20dp).",
            "On-Demand Notes Filter Chips: Notes screen conceals filter chips by default for a clean view. Tapping the header search button smoothly reveals or collapses chips with an active blue tint."
        ),
        patchesAndFixes = listOf(
            "Resolved Enter key handling composition issues with soft keyboards in the text note editor.",
            "Optimized editor toolbar reachability and added generous end-padding for 360dp compact displays.",
            "Hardened one-shot notification consumption to prevent repeating snackbar replays upon returning to Settings."
        )
    ),
    ReleaseChangelogEntry(
        version = "1.5.8",
        channel = "Beta",
        releaseDate = "2026-09-28",
        releaseUrl = "https://github.com/Arinaranetwork/Fotara/releases/tag/v1.5.8",
        whatsNew = listOf(
            "LinkIt Cluster Glow: Spatial corner glow converges toward shared center intersections for 3-4 card groups arranged in 2x2 or L-shaped clusters.",
            "Fullscreen Aspect-Locked Crop Editor: Precision crop editor with pinch zoom, pan clamping, and 4 corner handles for avatars and banners."
        ),
        patchesAndFixes = listOf(
            "Lossless 32-bit ARGB_8888 alpha transparency crop decoding for profile avatars and banners.",
            "Multi-tier image decoder prioritizing hardware-accelerated ImageDecoder with memory-safe fallbacks."
        )
    ),
    ReleaseChangelogEntry(
        version = "1.5.7",
        channel = "Beta",
        releaseDate = "2026-09-22",
        releaseUrl = "https://github.com/Arinaranetwork/Fotara/releases/tag/v1.5.7",
        whatsNew = listOf(
            "Settings Screen Redesign: Vertical list of sleek rounded cards, banner header with profile avatar, and bottom nav clearance.",
            "Search Interface Overhaul: Three top pill buttons, recent searches list with quick deletion chips, and idle state illustration.",
            "Interactive Profile Management: Gallery photo picking, live border preview picker, and atomic profile preferences storage."
        )
    ),
    ReleaseChangelogEntry(
        version = "1.5.6",
        channel = "Beta",
        releaseDate = "2026-09-15",
        releaseUrl = "https://github.com/Arinaranetwork/Fotara/releases/tag/v1.5.6",
        whatsNew = listOf(
            "Pull-to-Refresh: Animated curved-arrow vector indicator across Home, Notes, and FolderDetail screens.",
            "Add to Group Dialog: Grouped folder picker with atomic subfolder synchronization and preserved timestamps.",
            "Export Presets: Combined filename presets with dynamic tokens ({folder}, {date}, {time}, {count}) and custom names in Share dialog."
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewScreen(
    updateManager: UpdateManager,
    onBack: () -> Unit
) {
    var isExiting by remember { mutableStateOf(false) }
    val safeBack = {
        if (!isExiting) {
            isExiting = true
            onBack()
        }
    }

    // 1.6.0 is expanded by default
    var expandedVersions by remember { mutableStateOf(setOf("1.6.0")) }

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
                    fontFamily = ElmsSans,
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

            BundledReleases.forEach { releaseEntry ->
                val isExpanded = expandedVersions.contains(releaseEntry.version)

                ReleaseEntryCard(
                    entry = releaseEntry,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedVersions = if (isExpanded) {
                            expandedVersions - releaseEntry.version
                        } else {
                            expandedVersions + releaseEntry.version
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                    fontFamily = ElmsSans,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReleaseEntryCard(
    entry: ReleaseChangelogEntry,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Version, Channel Pill, Date/Link, Chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (entry.channel == "Stable") ColorGreen.copy(alpha = 0.18f) else AccentGold.copy(alpha = 0.18f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NewReleases,
                            contentDescription = null,
                            tint = if (entry.channel == "Stable") ColorGreen else AccentGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Fotara ${entry.version}",
                            color = TabCream,
                            fontSize = 17.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ChannelPill(channel = entry.channel)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Released ${entry.releaseDate}",
                        color = TabCream.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        fontFamily = ElmsSans
                    )
                    if (!entry.releaseUrl.isNullOrBlank()) {
                        val displayUrl = entry.releaseUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
                        Text(
                            text = displayUrl,
                            color = Color(0xFF60A5FA),
                            fontSize = 11.sp,
                            fontFamily = ElmsSans,
                            textDecoration = TextDecoration.Underline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                try {
                                    uriHandler.openUri(entry.releaseUrl)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Could not open release link", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TabCream.copy(alpha = 0.7f)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    // 1. Breaking Changes
                    if (entry.breakingChanges.isNotEmpty()) {
                        ChangelogSectionBlock(
                            title = "Breaking Changes",
                            icon = Icons.Default.Warning,
                            accentColor = ColorRed,
                            items = entry.breakingChanges
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 2. What's New
                    if (entry.whatsNew.isNotEmpty()) {
                        ChangelogSectionBlock(
                            title = "What's New",
                            icon = Icons.Default.NewReleases,
                            accentColor = if (entry.channel == "Stable") ColorGreen else AccentGold,
                            items = entry.whatsNew
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 3. Patches & Fixes
                    if (entry.patchesAndFixes.isNotEmpty()) {
                        ChangelogSectionBlock(
                            title = "Patches & Fixes",
                            icon = Icons.Default.CheckCircle,
                            accentColor = ColorGreen,
                            items = entry.patchesAndFixes
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangelogSectionBlock(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    items: List<String>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = TabCream,
                fontSize = 14.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${items.size}",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ScreenNavy.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                items.forEach { item ->
                    if (item.trim().startsWith("|") || item.contains("\n|")) {
                        ReleaseNotesRenderer(
                            markdown = item,
                            primaryTextColor = TabCream.copy(alpha = 0.90f),
                            accentColor = accentColor,
                            cardBorder = accentColor.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            ReleaseNotesRenderer(
                                markdown = item,
                                primaryTextColor = TabCream.copy(alpha = 0.90f),
                                accentColor = accentColor,
                                cardBorder = accentColor.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}
