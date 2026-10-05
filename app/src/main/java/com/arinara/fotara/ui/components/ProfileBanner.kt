// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.ui.profile.BannerAnimationGate
import com.arinara.fotara.ui.profile.BannerCropTransform
import com.arinara.fotara.ui.profile.NormalizedCropRect
import com.arinara.fotara.util.ProfileImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Custom View for rendering AnimatedImageDrawable with precision crop transformation.
 * Invalidates automatically on native frame callbacks without Compose recomposition overhead.
 */
private class AnimatedBannerView(context: Context) : View(context) {
    var animatedDrawable: Drawable? = null
        set(value) {
            field?.callback = null
            field = value
            field?.callback = this
            invalidate()
        }

    var cropParams: BannerCropTransform.TransformParams? = null
        set(value) {
            field = value
            invalidate()
        }

    override fun verifyDrawable(who: Drawable): Boolean {
        return who == animatedDrawable || super.verifyDrawable(who)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = animatedDrawable ?: return
        canvas.save()
        val params = cropParams
        if (params != null) {
            canvas.clipRect(0, 0, width, height)
            canvas.translate(params.tx, params.ty)
            canvas.scale(params.scale, params.scale)
        }
        d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        d.draw(canvas)
        canvas.restore()
    }
}

/**
 * Shared Profile Banner composable used across Settings and Profile preview screens.
 * Supports:
 * - Hardware-accelerated GIF playback via AnimatedImageDrawable (API 28+)
 * - Gated playback: plays only when screen is RESUMED and banner is on-screen
 * - Pauses in background and stops when scrolled off-screen
 * - Shows static first frame when system animator duration scale is 0
 * - Releases memory and frame callbacks on disposal
 * - API 24-27 static first frame fallback with normalized crop transform
 * - Non-GIF banners render via Coil with memory caching
 * - Subtle theme gradient fallback when no custom banner is set
 */
@Composable
fun ProfileBanner(
    bannerPath: String?,
    bannerUpdatedAt: Long,
    bannerCrop: String?,
    modifier: Modifier = Modifier,
    isOnScreen: Boolean = true
) {
    val context = LocalContext.current
    val bannerFile = remember(bannerPath, bannerUpdatedAt) {
        bannerPath?.let { File(it) }
    }

    if (bannerFile != null && bannerFile.exists()) {
        val isGif = remember(bannerFile.absolutePath) {
            bannerFile.name.endsWith(".gif", ignoreCase = true)
        }

        if (isGif) {
            val cropRect = remember(bannerCrop) {
                NormalizedCropRect.fromSerializedString(bannerCrop) ?: NormalizedCropRect.FULL
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val lifecycleOwner = LocalLifecycleOwner.current
                val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
                val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

                val animatorDurationScale = remember(context) {
                    try {
                        Settings.Global.getFloat(
                            context.contentResolver,
                            Settings.Global.ANIMATOR_DURATION_SCALE,
                            1.0f
                        )
                    } catch (_: Throwable) {
                        1.0f
                    }
                }

                val shouldAnimate = BannerAnimationGate.shouldAnimate(
                    isResumed = isResumed,
                    isOnScreen = isOnScreen,
                    animatorDurationScale = animatorDurationScale,
                    apiLevel = Build.VERSION.SDK_INT
                )

                var animatedDrawable by remember(bannerFile.absolutePath, bannerUpdatedAt) {
                    mutableStateOf<Drawable?>(null)
                }

                LaunchedEffect(bannerFile.absolutePath, bannerUpdatedAt) {
                    withContext(Dispatchers.IO) {
                        try {
                            val source = ImageDecoder.createSource(bannerFile)
                            val decoded = ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                                val targetWidth = 1080
                                if (info.size.width > targetWidth) {
                                    val scale = targetWidth.toFloat() / info.size.width.toFloat()
                                    val targetHeight = (info.size.height * scale).toInt().coerceAtLeast(1)
                                    decoder.setTargetSize(targetWidth, targetHeight)
                                }
                            }
                            if (decoded is AnimatedImageDrawable) {
                                decoded.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                            }
                            withContext(Dispatchers.Main) {
                                animatedDrawable = decoded
                            }
                        } catch (_: Throwable) {
                            animatedDrawable = null
                        }
                    }
                }

                // Control playback state
                LaunchedEffect(animatedDrawable, shouldAnimate) {
                    val d = animatedDrawable
                    if (d is AnimatedImageDrawable) {
                        if (shouldAnimate) {
                            d.start()
                        } else {
                            d.stop()
                        }
                    }
                }

                // Memory release on disposal
                DisposableEffect(animatedDrawable) {
                    onDispose {
                        val d = animatedDrawable
                        if (d is AnimatedImageDrawable) {
                            d.stop()
                            d.clearAnimationCallbacks()
                        }
                    }
                }

                val currentDrawable = animatedDrawable
                if (currentDrawable != null) {
                    AndroidView(
                        factory = { ctx ->
                            AnimatedBannerView(ctx).apply {
                                this.animatedDrawable = currentDrawable
                            }
                        },
                        update = { view ->
                            view.animatedDrawable = currentDrawable
                            if (view.width > 0 && view.height > 0 && currentDrawable.intrinsicWidth > 0 && currentDrawable.intrinsicHeight > 0) {
                                view.cropParams = BannerCropTransform.computeTransformParams(
                                    viewWidth = view.width.toFloat(),
                                    viewHeight = view.height.toFloat(),
                                    imageWidth = currentDrawable.intrinsicWidth.toFloat(),
                                    imageHeight = currentDrawable.intrinsicHeight.toFloat(),
                                    crop = cropRect
                                )
                            }
                        },
                        modifier = modifier.fillMaxSize()
                    )
                } else {
                    ProfileBannerFallbackGradient(modifier = modifier)
                }
            } else {
                // API 24-27: Show static first frame
                var firstFrameBitmap by remember(bannerFile.absolutePath, bannerUpdatedAt) {
                    mutableStateOf<Bitmap?>(null)
                }
                LaunchedEffect(bannerFile.absolutePath, bannerUpdatedAt) {
                    withContext(Dispatchers.IO) {
                        val bmp = ProfileImageUtils.decodeSampledBitmap(context, Uri.fromFile(bannerFile), 1080)
                        withContext(Dispatchers.Main) {
                            firstFrameBitmap = bmp
                        }
                    }
                }

                val bmp = firstFrameBitmap
                if (bmp != null) {
                    Canvas(modifier = modifier.fillMaxSize()) {
                        val params = BannerCropTransform.computeTransformParams(
                            viewWidth = size.width,
                            viewHeight = size.height,
                            imageWidth = bmp.width.toFloat(),
                            imageHeight = bmp.height.toFloat(),
                            crop = cropRect
                        )
                        drawIntoCanvas { canvas ->
                            val native = canvas.nativeCanvas
                            native.save()
                            native.clipRect(0f, 0f, size.width, size.height)
                            native.translate(params.tx, params.ty)
                            native.scale(params.scale, params.scale)
                            native.drawBitmap(bmp, 0f, 0f, null)
                            native.restore()
                        }
                    }
                } else {
                    ProfileBannerFallbackGradient(modifier = modifier)
                }
            }
        } else {
            // Non-GIF static image banner (PNG, WebP, JPEG)
            val bannerCacheKey = "${bannerFile.absolutePath}_${if (bannerUpdatedAt > 0L) bannerUpdatedAt else bannerFile.lastModified()}"
            val bannerReq = remember(bannerCacheKey) {
                ImageRequest.Builder(context)
                    .data(bannerFile)
                    .memoryCacheKey(bannerCacheKey)
                    .diskCacheKey(bannerCacheKey)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = bannerReq,
                contentDescription = "Profile Banner",
                contentScale = ContentScale.Crop,
                modifier = modifier.fillMaxSize()
            )
        }
    } else {
        ProfileBannerFallbackGradient(modifier = modifier)
    }
}

@Composable
private fun ProfileBannerFallbackGradient(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F172A),
                        HomeNearBlack
                    )
                )
            )
    )
}
