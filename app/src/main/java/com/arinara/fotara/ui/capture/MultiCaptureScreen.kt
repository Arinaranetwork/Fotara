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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoSource
import com.arinara.fotara.data.storage.PhotoStorageManager
import com.arinara.fotara.ocr.FolderSuggestEngine
import com.arinara.fotara.ocr.OcrEngine
import com.arinara.fotara.theme.DockSlatePill
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagEmerald
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
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
                        val ocr = ocrEngine.extractText(saved.filePath)
                        val nextIndex = capturedPhotos.size + 1
                        val newPhoto = Photo(
                            fileUri = saved.filePath,
                            thumbnailUri = saved.thumbnailPath,
                            folderId = folderId,
                            caption = "Imported Note #$nextIndex",
                            ocrText = ocr.fullText,
                            source = PhotoSource.IMPORT,
                            fileSizeBytes = saved.fileSizeBytes
                        )
                        capturedPhotos.add(newPhoto)
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
                .background(MidnightNavy)
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
                        .background(FolderBodyBlue.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = FolderTabCream,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Camera Permission Required",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Fotara needs camera access to capture handwritten notes, whiteboard diagrams, and lecture slides directly into your subject folders.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Allow Camera Access", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onDismiss) {
                    Text("Not Now", color = FolderTabCream, fontSize = 14.sp)
                }
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
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
                    } catch (exc: Exception) {
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
            val guideColor = if (isStraightenEnabled) TagEmerald else FolderTabCream

            val marginH = 24.dp.toPx()
            val marginV = 90.dp.toPx()

            // Top-Left Corner
            drawLine(guideColor, Offset(marginH, marginV), Offset(marginH + cornerLen, marginV), strokeW)
            drawLine(guideColor, Offset(marginH, marginV), Offset(marginH, marginV + cornerLen), strokeW)

            // Top-Right Corner
            drawLine(guideColor, Offset(w - marginH, marginV), Offset(w - marginH - cornerLen, marginV), strokeW)
            drawLine(guideColor, Offset(w - marginH, marginV), Offset(w - marginH, marginV + cornerLen), strokeW)

            // Bottom-Left Corner
            val bottomY = h - 140.dp.toPx()
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
            color = Color.Black.copy(alpha = 0.65f),
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
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Camera",
                        tint = FolderTabCream
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Auto-Perspective toggle
                Surface(
                    color = if (isStraightenEnabled) TagEmerald.copy(alpha = 0.25f) else DockSlatePill.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
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
                            tint = if (isStraightenEnabled) TagEmerald else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Auto-Crop",
                            color = if (isStraightenEnabled) TagEmerald else TextSecondary,
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
                        tint = if (flashEnabled) TagAmber else TextSecondary
                    )
                }
            }
        }

        // Bottom Capture Bar
        Surface(
            color = MidnightSurface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
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
                        tint = FolderTabCream
                    )
                }

                // Primary Shutter Button (Camera Capture)
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
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
                                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                        val bitmap = imageProxyToBitmap(imageProxy, rotationDegrees)
                                        imageProxy.close()

                                        coroutineScope.launch {
                                            try {
                                                val saved = photoStorageManager.saveBitmapAsPhoto(bitmap)
                                                val ocr = ocrEngine.extractText(saved.filePath)
                                                val nextIndex = capturedPhotos.size + 1
                                                val newPhoto = Photo(
                                                    fileUri = saved.filePath,
                                                    thumbnailUri = saved.thumbnailPath,
                                                    folderId = folderId,
                                                    caption = "Captured Note #$nextIndex",
                                                    ocrText = ocr.fullText,
                                                    source = PhotoSource.CAMERA,
                                                    fileSizeBytes = saved.fileSizeBytes
                                                )
                                                capturedPhotos.add(newPhoto)
                                            } catch (_: Exception) {
                                            } finally {
                                                isCapturing = false
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
                            color = FolderTabCream,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(FolderBodyBlue)
                                .border(2.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Capture Photo",
                                tint = Color.White,
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
                                containerColor = FolderBodyBlue,
                                contentColor = Color.White
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
                    Button(
                        onClick = {
                            if (capturedPhotos.isNotEmpty()) {
                                onFinishBatch(capturedPhotos.toList())
                            }
                        },
                        enabled = capturedPhotos.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FolderBodyBlue,
                            contentColor = Color.White,
                            disabledContainerColor = DockSlatePill.copy(alpha = 0.4f),
                            disabledContentColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Review",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy, rotationDegrees: Int): Bitmap {
    val planeProxy = image.planes[0]
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    return if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        if (rotated != original) original.recycle()
        rotated
    } else {
        original
    }
}
