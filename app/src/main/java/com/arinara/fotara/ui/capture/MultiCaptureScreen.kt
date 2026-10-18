// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoSource
import com.arinara.fotara.data.storage.PhotoStorageManager
import com.arinara.fotara.ocr.FolderSuggestEngine
import com.arinara.fotara.ocr.OcrEngine
import com.arinara.fotara.ui.components.presets.AppBottomSheet
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

@Composable
fun MultiCaptureScreen(
    folderId: Long,
    ocrEngine: OcrEngine,
    folderSuggestEngine: FolderSuggestEngine,
    photoStorageManager: PhotoStorageManager,
    onFinishBatch: (List<Photo>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val bottomClearance = 100.dp

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val capturedPhotos = remember { mutableStateListOf<Photo>() }
    var isStraightenEnabled by remember { mutableStateOf(true) }
    var flashEnabled by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var targetRetakeIndex by remember { mutableStateOf<Int?>(null) }
    var selectedPreviewPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    val filmstripListState = rememberLazyListState()

    LaunchedEffect(capturedPhotos.size) {
        if (capturedPhotos.isNotEmpty()) {
            filmstripListState.animateScrollToItem(capturedPhotos.size - 1)
        }
    }

    // Intercept system back gestures to protect uncommitted photos
    BackHandler(enabled = true) {
        when {
            targetRetakeIndex != null -> targetRetakeIndex = null
            selectedPreviewPhotoIndex != null -> selectedPreviewPhotoIndex = null
            showDiscardConfirmDialog -> showDiscardConfirmDialog = false
            capturedPhotos.isNotEmpty() -> showDiscardConfirmDialog = true
            else -> onDismiss()
        }
    }

    val flashAlpha = remember { Animatable(0f) }

    // CameraX references
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    var camera by remember { mutableStateOf<Camera?>(null) }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                for (uri in uris) {
                    try {
                        val saved = photoStorageManager.saveUriAsPhoto(uri)
                        val nextIndex = capturedPhotos.size + 1
                        val newPhoto = Photo(
                            fileUri = saved.filePath,
                            thumbnailUri = saved.thumbnailPath,
                            folderId = folderId,
                            caption = "Imported Note #$nextIndex",
                            ocrText = "",
                            source = PhotoSource.IMPORT,
                            fileSizeBytes = saved.fileSizeBytes
                        )
                        capturedPhotos.add(newPhoto)

                        // Async OCR extraction without blocking camera thread
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val ocr = ocrEngine.extractText(saved.filePath)
                                val idx = capturedPhotos.indexOfFirst { it.fileUri == saved.filePath }
                                if (idx >= 0) {
                                    capturedPhotos[idx] = capturedPhotos[idx].copy(ocrText = ocr.fullText)
                                }
                            } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    if (!hasCameraPermission) {
        // Fallback UI when camera permission is not granted
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ElectricSlateTokens.BgCanvas)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(ElectricSlateTokens.SurfacePanel)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Camera Permission Required",
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Fotara needs camera access to capture handwritten notes, whiteboard diagrams, and lecture slides directly into your subject folders.",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                AppButton(
                    text = "Allow Camera Access",
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    variant = AppButtonVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppButton(
                    text = "Not Now",
                    onClick = onDismiss,
                    variant = AppButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ElectricSlateTokens.BgCanvas)
    ) {
        // Real CameraX Viewfinder Surface
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (_: Exception) {
                        // Safe fallback on binding failure
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Document Viewfinder Alignment Guides
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cornerLen = 36.dp.toPx()
            val strokeW = 3.dp.toPx()
            val guideColor = if (isStraightenEnabled) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle

            val marginH = 24.dp.toPx()
            val marginV = 90.dp.toPx()

            // Top-Left Corner
            drawLine(guideColor, Offset(marginH, marginV), Offset(marginH + cornerLen, marginV), strokeW)
            drawLine(guideColor, Offset(marginH, marginV), Offset(marginH, marginV + cornerLen), strokeW)

            // Top-Right Corner
            drawLine(guideColor, Offset(w - marginH, marginV), Offset(w - marginH - cornerLen, marginV), strokeW)
            drawLine(guideColor, Offset(w - marginH, marginV), Offset(w - marginH, marginV + cornerLen), strokeW)

            // Bottom-Left Corner
            val bottomY = h - 180.dp.toPx()
            drawLine(guideColor, Offset(marginH, bottomY), Offset(marginH + cornerLen, bottomY), strokeW)
            drawLine(guideColor, Offset(marginH, bottomY), Offset(marginH, bottomY - cornerLen), strokeW)

            // Bottom-Right Corner
            drawLine(guideColor, Offset(w - marginH, bottomY), Offset(w - marginH - cornerLen, bottomY), strokeW)
            drawLine(guideColor, Offset(w - marginH, bottomY), Offset(w - marginH, bottomY - cornerLen), strokeW)
        }

        // Shutter Flash Animation Overlay
        if (flashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha.value))
            )
        }

        // Top Control Bar
        Surface(
            color = ElectricSlateTokens.SurfacePanel.copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (capturedPhotos.isNotEmpty()) {
                        showDiscardConfirmDialog = true
                    } else {
                        onDismiss()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Camera",
                        tint = ElectricSlateTokens.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Auto-Perspective toggle
                Surface(
                    color = if (isStraightenEnabled) ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.2f) else ElectricSlateTokens.TrackInactive,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isStraightenEnabled) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle),
                    modifier = Modifier
                        .clickable { isStraightenEnabled = !isStraightenEnabled }
                        .padding(end = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isStraightenEnabled) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Auto-Crop",
                            color = if (isStraightenEnabled) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Flash toggle
                IconButton(onClick = {
                    flashEnabled = !flashEnabled
                    camera?.cameraControl?.enableTorch(flashEnabled)
                }) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Flash",
                        tint = if (flashEnabled) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted
                    )
                }
            }
        }

        // Bottom Capture Section: Live Filmstrip Tray and Shutter Controls
        Surface(
            color = ElectricSlateTokens.SurfacePanel.copy(alpha = 0.95f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                // Active Retake Status Indicator Banner
                if (targetRetakeIndex != null && (targetRetakeIndex ?: -1) in capturedPhotos.indices) {
                    val retakePage = (targetRetakeIndex ?: 0) + 1
                    Surface(
                        color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricSlateTokens.PrimaryAccent),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Retaking Page #$retakePage — Tap shutter to replace",
                                color = ElectricSlateTokens.PrimaryAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Retake",
                                tint = ElectricSlateTokens.TextMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { targetRetakeIndex = null }
                            )
                        }
                    }
                }

                // Live In-Situ Horizontal Filmstrip Tray
                if (capturedPhotos.isNotEmpty()) {
                    LazyRow(
                        state = filmstripListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(capturedPhotos) { index, photo ->
                            val isTargetRetake = targetRetakeIndex == index
                            val isSelected = selectedPreviewPhotoIndex == index
                            Box(
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(
                                        width = if (isTargetRetake || isSelected) 2.dp else 1.dp,
                                        color = if (isTargetRetake || isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedPreviewPhotoIndex = index
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(photo.thumbnailUri ?: photo.fileUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Page #${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Page Number Pill Badge
                                Surface(
                                    color = ElectricSlateTokens.SurfacePanel.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 12.dp),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        color = ElectricSlateTokens.TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Primary Capture Controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Import from Gallery Shortcut
                    IconButton(
                        onClick = {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = "Import from device library",
                            tint = ElectricSlateTokens.TextMuted
                        )
                    }

                    // Decoupled Rapid Shutter Button (<100ms re-arm)
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.TrackInactive.copy(alpha = 0.5f))
                            .clickable(enabled = !isCapturing) {
                                isCapturing = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                coroutineScope.launch {
                                    flashAlpha.animateTo(0.7f, tween(60))
                                    flashAlpha.animateTo(0f, tween(120))
                                }

                                val executor = ContextCompat.getMainExecutor(context)
                                imageCapture.takePicture(
                                    executor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                            coroutineScope.launch(Dispatchers.Default) {
                                                val bitmap = try {
                                                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                                    imageProxyToBitmap(imageProxy, rotationDegrees)
                                                } catch (_: Throwable) {
                                                    null
                                                } finally {
                                                    imageProxy.close()
                                                }

                                                if (bitmap == null) {
                                                    isCapturing = false
                                                    return@launch
                                                }

                                                try {
                                                    val saved = photoStorageManager.saveBitmapAsPhoto(bitmap)
                                                    val nextIndex = capturedPhotos.size + 1
                                                    val newPhoto = Photo(
                                                        fileUri = saved.filePath,
                                                        thumbnailUri = saved.thumbnailPath,
                                                        folderId = folderId,
                                                        caption = "Captured Note #$nextIndex",
                                                        ocrText = "",
                                                        source = PhotoSource.CAMERA,
                                                        fileSizeBytes = saved.fileSizeBytes
                                                    )

                                                    val retakeIdx = targetRetakeIndex
                                                    if (retakeIdx != null && retakeIdx in capturedPhotos.indices) {
                                                        val oldPhoto = capturedPhotos[retakeIdx]
                                                        capturedPhotos[retakeIdx] = newPhoto.copy(caption = oldPhoto.caption)
                                                        targetRetakeIndex = null
                                                        coroutineScope.launch(Dispatchers.IO) {
                                                            photoStorageManager.deletePhotoFiles(oldPhoto.fileUri, oldPhoto.thumbnailUri)
                                                        }
                                                    } else {
                                                        capturedPhotos.add(newPhoto)
                                                    }

                                                    // Re-arm shutter IMMEDIATELY (<100ms turnaround)
                                                    isCapturing = false

                                                    // Background OCR extraction without freezing capture pipeline
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        try {
                                                            val ocr = ocrEngine.extractText(saved.filePath)
                                                            val currentIdx = capturedPhotos.indexOfFirst { it.fileUri == saved.filePath }
                                                            if (currentIdx >= 0) {
                                                                capturedPhotos[currentIdx] = capturedPhotos[currentIdx].copy(ocrText = ocr.fullText)
                                                            }
                                                        } catch (_: Exception) {}
                                                    }
                                                } catch (_: Exception) {
                                                    isCapturing = false
                                                } finally {
                                                    bitmap.recycle()
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            isCapturing = false
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                color = ElectricSlateTokens.PrimaryAccent,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(ElectricSlateTokens.PrimaryAccent)
                                    .border(2.dp, ElectricSlateTokens.PrimaryAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Capture Photo",
                                    tint = ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    // Finish & Review Batch Button
                    BadgedBox(
                        badge = {
                            if (capturedPhotos.isNotEmpty()) {
                                Badge(
                                    containerColor = ElectricSlateTokens.PrimaryAccent,
                                    contentColor = ElectricSlateTokens.TextPrimary
                                ) {
                                    Text(
                                        text = capturedPhotos.size.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        AppButton(
                            text = "Review",
                            onClick = {
                                if (capturedPhotos.isNotEmpty()) {
                                    onFinishBatch(capturedPhotos.toList())
                                }
                            },
                            enabled = capturedPhotos.isNotEmpty(),
                            variant = AppButtonVariant.PRIMARY
                        )
                    }
                }
            }
        }

        // Filmstrip Page Inspection Bottom Sheet
        if (selectedPreviewPhotoIndex != null && (selectedPreviewPhotoIndex ?: -1) in capturedPhotos.indices) {
            val inspectIndex = selectedPreviewPhotoIndex ?: 0
            val photo = capturedPhotos[inspectIndex]

            AppBottomSheet(
                visible = true,
                onDismiss = { selectedPreviewPhotoIndex = null },
                title = "Page #${inspectIndex + 1} of ${capturedPhotos.size}"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Preview Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElectricSlateTokens.BgCanvas)
                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photo.thumbnailUri ?: photo.fileUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Page Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Reorder Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (inspectIndex > 0) {
                                    val item = capturedPhotos.removeAt(inspectIndex)
                                    capturedPhotos.add(inspectIndex - 1, item)
                                    selectedPreviewPhotoIndex = inspectIndex - 1
                                }
                            },
                            enabled = inspectIndex > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Move Left",
                                tint = if (inspectIndex > 0) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted.copy(alpha = 0.4f)
                            )
                        }

                        Text(
                            text = "Reorder Page Position",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        IconButton(
                            onClick = {
                                if (inspectIndex < capturedPhotos.size - 1) {
                                    val item = capturedPhotos.removeAt(inspectIndex)
                                    capturedPhotos.add(inspectIndex + 1, item)
                                    selectedPreviewPhotoIndex = inspectIndex + 1
                                }
                            },
                            enabled = inspectIndex < capturedPhotos.size - 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Move Right",
                                tint = if (inspectIndex < capturedPhotos.size - 1) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted.copy(alpha = 0.4f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons Row: Retake and Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppButton(
                            text = "Retake Page",
                            onClick = {
                                targetRetakeIndex = inspectIndex
                                selectedPreviewPhotoIndex = null
                            },
                            variant = AppButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f)
                        )

                        AppButton(
                            text = "Delete",
                            onClick = {
                                val removed = capturedPhotos.removeAt(inspectIndex)
                                coroutineScope.launch(Dispatchers.IO) {
                                    photoStorageManager.deletePhotoFiles(removed.fileUri, removed.thumbnailUri)
                                }
                                selectedPreviewPhotoIndex = null
                            },
                            variant = AppButtonVariant.DESTRUCTIVE,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Safe Discard All Confirmation Bottom Sheet
        if (showDiscardConfirmDialog) {
            AppBottomSheet(
                visible = true,
                onDismiss = { showDiscardConfirmDialog = false },
                title = "Discard Captured Photos?"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "You have ${capturedPhotos.size} captured photo(s) that have not been saved to a folder. Discarding will permanently delete all temporary photo files from device storage.",
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppButton(
                            text = "Keep Snapping",
                            onClick = { showDiscardConfirmDialog = false },
                            variant = AppButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f)
                        )

                        AppButton(
                            text = "Discard All",
                            onClick = {
                                coroutineScope.launch {
                                    capturedPhotos.forEach { photo ->
                                        photoStorageManager.deletePhotoFiles(photo.fileUri, photo.thumbnailUri)
                                    }
                                    capturedPhotos.clear()
                                    showDiscardConfirmDialog = false
                                    onDismiss()
                                }
                            },
                            variant = AppButtonVariant.DESTRUCTIVE,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy, rotationDegrees: Int): Bitmap? {
    return try {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
            if (rotated != original) original.recycle()
            rotated
        } else {
            original
        }
    } catch (_: Throwable) {
        null
    }
}
