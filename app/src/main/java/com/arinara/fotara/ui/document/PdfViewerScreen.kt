// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.document

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentPage
import com.arinara.fotara.util.PdfCorruptException
import com.arinara.fotara.util.PdfPageRenderer
import com.arinara.fotara.util.PdfPasswordException
import kotlinx.coroutines.delay
import java.io.File

private val ScreenNavy = Color(0xFF03071E)
private val TabCream = Color(0xFFEAE3D2)
private val AccentGold = Color(0xFFF77F00)
private val CardBg = Color(0xFF141936)
private val DangerRed = Color(0xFFD62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    documentNote: DocumentNote,
    pages: List<DocumentPage>,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onSplitToImages: () -> Unit,
    onDelete: () -> Unit
) {
    var showSplitConfirmDialog by remember { mutableStateOf(false) }
    var pdfRenderer by remember { mutableStateOf<PdfPageRenderer?>(null) }
    var openErrorMessage by remember { mutableStateOf<String?>(null) }

    val file = remember(documentNote.originFileUri) { File(documentNote.originFileUri) }

    DisposableEffect(file) {
        try {
            val renderer = PdfPageRenderer(file)
            pdfRenderer = renderer
            openErrorMessage = null
        } catch (e: PdfPasswordException) {
            openErrorMessage = "This document is password-protected and cannot be opened."
        } catch (e: PdfCorruptException) {
            openErrorMessage = "This document is corrupt or invalid: ${e.message}"
        } catch (e: Exception) {
            openErrorMessage = "Failed to open document: ${e.message}"
        }

        onDispose {
            pdfRenderer?.close()
            pdfRenderer = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = documentNote.name,
                        color = TabCream,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${pdfRenderer?.pageCount ?: pages.size} pages • PDF Document",
                        color = TabCream.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TabCream
                    )
                }
            },
            actions = {
                IconButton(onClick = { showSplitConfirmDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = "Split to Images",
                        tint = AccentGold
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share As",
                        tint = TabCream
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Document",
                        tint = DangerRed
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenNavy)
        )

        val err = openErrorMessage
        val renderer = pdfRenderer

        when {
            err != null -> {
                // Error state: Clear message instead of crash
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(60.dp))
                    Icon(
                        imageVector = if (err.contains("password", ignoreCase = true)) Icons.Default.Lock else Icons.Default.Warning,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cannot Open PDF",
                        color = TabCream,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        color = TabCream.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = CardBg)
                    ) {
                        Text("Go Back", color = TabCream)
                    }
                }
            }
            renderer == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGold)
                }
            }
            else -> {
                // Continuous Virtualized Vertical List of PDF pages
                val totalCount = renderer.pageCount
                val configuration = LocalConfiguration.current
                val density = LocalDensity.current
                val screenWidthPx = with(density) { (configuration.screenWidthDp.dp - 32.dp).roundToPx() }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(totalCount, key = { index -> "${documentNote.id}_page_$index" }) { pageIndex ->
                        val placeholderPage = pages.getOrNull(pageIndex)
                        VirtualizedPdfPageView(
                            pageIndex = pageIndex,
                            totalPages = totalCount,
                            renderer = renderer,
                            targetWidthPx = screenWidthPx,
                            placeholderUri = placeholderPage?.imageUri
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (showSplitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSplitConfirmDialog = false },
            title = {
                Text(
                    text = "Split PDF into Images?",
                    fontWeight = FontWeight.Bold,
                    color = TabCream
                )
            },
            text = {
                val totalCount = pdfRenderer?.pageCount ?: pages.size
                val targetDesc = if (totalCount >= 5) {
                    "a new Photo Group '${documentNote.name}'"
                } else {
                    "standalone photo notes"
                }
                Text(
                    text = "This action permanently deletes the original PDF file and converts all $totalCount pages into $targetDesc in this folder. This action cannot be undone.",
                    color = TabCream.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSplitConfirmDialog = false
                        onSplitToImages()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Split & Delete PDF", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSplitConfirmDialog = false }) {
                    Text("Cancel", color = TabCream)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun VirtualizedPdfPageView(
    pageIndex: Int,
    totalPages: Int,
    renderer: PdfPageRenderer,
    targetWidthPx: Int,
    placeholderUri: String?
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var highResBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var aspectRatio by remember { mutableFloatStateOf(0.707f) }
    var isRenderingSharp by remember { mutableStateOf(false) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = newScale
        if (newScale > 1.02f) {
            val maxOffsetX = (targetWidthPx * (newScale - 1f)) / 2f
            val maxOffsetY = ((targetWidthPx / aspectRatio) * (newScale - 1f)) / 2f
            val newX = (offset.x + offsetChange.x).coerceIn(-maxOffsetX, maxOffsetX)
            val newY = (offset.y + offsetChange.y).coerceIn(-maxOffsetY, maxOffsetY)
            offset = Offset(newX, newY)
        } else {
            scale = 1f
            offset = Offset.Zero
        }
    }

    // Determine real aspect ratio
    LaunchedEffect(pageIndex) {
        aspectRatio = renderer.getPageAspectRatio(pageIndex)
    }

    // Re-render sharply on scale or page display with debounce
    LaunchedEffect(pageIndex, targetWidthPx, (scale * 10).toInt()) {
        isRenderingSharp = true
        val targetHeightPx = (targetWidthPx / aspectRatio).toInt().coerceAtLeast(100)
        // Debounce zoom re-render
        if (scale > 1.05f) {
            delay(120)
        }
        val rendered = renderer.renderPage(
            pageIndex = pageIndex,
            destWidth = targetWidthPx,
            destHeight = targetHeightPx,
            renderScale = scale.coerceIn(1f, 2.5f)
        )
        if (rendered != null) {
            highResBitmap = rendered
        }
        isRenderingSharp = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Page ${pageIndex + 1} of $totalPages",
                    color = TabCream.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isRenderingSharp && highResBitmap == null) {
                    Text(
                        text = "Rendering…",
                        color = TabCream.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (scale > 1.05f) {
                    Text(
                        text = "${(scale * 100).toInt()}%",
                        color = AccentGold,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .background(Color.White)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    }
                    .transformable(state = transformState),
                contentAlignment = Alignment.Center
            ) {
                val sharp = highResBitmap
                if (sharp != null && !sharp.isRecycled) {
                    // Crisp sharp density-scaled render with white pre-fill
                    Image(
                        bitmap = sharp.asImageBitmap(),
                        contentDescription = "Page ${pageIndex + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = if (scale > 2.5f) scale / 2.5f else 1f,
                                scaleY = if (scale > 2.5f) scale / 2.5f else 1f,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                } else if (placeholderUri != null && File(placeholderUri).exists()) {
                    // Instant low-res stored placeholder while sharp render is processing
                    AsyncImage(
                        model = File(placeholderUri),
                        contentDescription = "Page ${pageIndex + 1} Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                } else {
                    CircularProgressIndicator(
                        color = AccentGold,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}
