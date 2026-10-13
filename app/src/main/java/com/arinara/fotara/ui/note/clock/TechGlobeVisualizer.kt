// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.clock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable
import kotlin.math.cos
import kotlin.math.sin

data class GlobeTimezoneLocation(
    val id: String,
    val countryCode: String,
    val name: String,
    val timeZoneId: String,
    val latitudeDeg: Float,
    val longitudeDeg: Float
)

val DEFAULT_GLOBE_LOCATIONS = listOf(
    GlobeTimezoneLocation("UTC", "INT", "UTC (Universal)", "UTC", 0f, 0f),
    GlobeTimezoneLocation("LON", "GBR", "London (GMT/BST)", "Europe/London", 51.5f, -0.12f),
    GlobeTimezoneLocation("NYC", "USA", "New York (EST/EDT)", "America/New_York", 40.7f, -74.0f),
    GlobeTimezoneLocation("SFO", "USA", "San Francisco (PST)", "America/Los_Angeles", 37.7f, -122.4f),
    GlobeTimezoneLocation("JKT", "IDN", "Jakarta (WIB)", "Asia/Jakarta", -6.2f, 106.8f),
    GlobeTimezoneLocation("TYO", "JPN", "Tokyo (JST)", "Asia/Tokyo", 35.6f, 139.7f),
    GlobeTimezoneLocation("DXB", "UAE", "Dubai (GST)", "Asia/Dubai", 25.2f, 55.3f),
    GlobeTimezoneLocation("SYD", "AUS", "Sydney (AEST)", "Australia/Sydney", -33.8f, 151.2f)
)

/**
 * Pure native Jetpack Compose 3D Low-Poly Tech Globe visualizer.
 * Supports touch rotation (yaw and pitch), rendering wireframe tech meridians,
 * auto-spin towards selected timezone, and 1:1 3D timezone pointers.
 * Strictly NO WebView.
 */
@Composable
fun TechGlobeVisualizer(
    selectedLocation: GlobeTimezoneLocation,
    onSelectLocation: (GlobeTimezoneLocation) -> Unit,
    modifier: Modifier = Modifier,
    locations: List<GlobeTimezoneLocation> = DEFAULT_GLOBE_LOCATIONS,
    showLocationChips: Boolean = true
) {
    var yaw by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(0.2f) }
    val textMeasurer = rememberTextMeasurer()

    // Smooth auto-rotation to selected location
    LaunchedEffect(selectedLocation) {
        val targetYaw = -Math.toRadians(selectedLocation.longitudeDeg.toDouble()).toFloat()
        val targetPitch = (Math.toRadians(selectedLocation.latitudeDeg.toDouble()).toFloat() * 0.4f).coerceIn(-1.2f, 1.2f)
        val startYaw = yaw
        val startPitch = pitch
        val anim = Animatable(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        ) {
            yaw = startYaw + (targetYaw - startYaw) * value
            pitch = startPitch + (targetPitch - startPitch) * value
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ElectricSlateTokens.SurfacePanel)
                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        yaw += dragAmount.x * 0.008f
                        pitch = (pitch - dragAmount.y * 0.008f).coerceIn(-1.3f, 1.3f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = minOf(cx, cy) * 0.78f

                val gridColor = ElectricSlateTokens.TrackInactive
                val highlightColor = ElectricSlateTokens.PrimaryAccent
                val pinAccentColor = ElectricSlateTokens.PrimaryAccent

                // Outer glowing tech ring
                drawCircle(
                    color = gridColor.copy(alpha = 0.35f),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // 3D coordinate projection helper
                fun project(latDeg: Float, lonDeg: Float, elevation: Float = 1.0f): Triple<Float, Float, Float> {
                    val latRad = Math.toRadians(latDeg.toDouble()).toFloat()
                    val lonRad = Math.toRadians(lonDeg.toDouble()).toFloat()

                    val r = radius * elevation
                    val x0 = r * cos(latRad) * sin(lonRad)
                    val y0 = -r * sin(latRad)
                    val z0 = r * cos(latRad) * cos(lonRad)

                    // Rotate around X (pitch)
                    val y1 = y0 * cos(pitch) - z0 * sin(pitch)
                    val z1 = y0 * sin(pitch) + z0 * cos(pitch)

                    // Rotate around Y (yaw)
                    val x2 = x0 * cos(yaw) + z1 * sin(yaw)
                    val z2 = -x0 * sin(yaw) + z1 * cos(yaw)

                    val screenX = cx + x2
                    val screenY = cy + y1
                    return Triple(screenX, screenY, z2)
                }

                // Draw latitude lines (parallels)
                val latitudes = listOf(-60f, -30f, 0f, 30f, 60f)
                for (lat in latitudes) {
                    val path = Path()
                    var first = true
                    for (lonStep in 0..36) {
                        val lon = lonStep * 10f
                        val (sx, sy, z) = project(lat, lon)
                        if (z > -radius * 0.2f) {
                            if (first) {
                                path.moveTo(sx, sy)
                                first = false
                            } else {
                                path.lineTo(sx, sy)
                            }
                        } else {
                            first = true
                        }
                    }
                    drawPath(
                        path = path,
                        color = if (lat == 0f) highlightColor.copy(alpha = 0.5f) else gridColor.copy(alpha = 0.35f),
                        style = Stroke(width = if (lat == 0f) 1.5.dp.toPx() else 1.dp.toPx())
                    )
                }

                // Draw longitude lines (meridians)
                val longitudes = (0 until 12).map { it * 30f }
                for (lon in longitudes) {
                    val path = Path()
                    var first = true
                    for (latStep in -9..9) {
                        val lat = latStep * 10f
                        val (sx, sy, z) = project(lat, lon)
                        if (z > -radius * 0.2f) {
                            if (first) {
                                path.moveTo(sx, sy)
                                first = false
                            } else {
                                path.lineTo(sx, sy)
                            }
                        } else {
                            first = true
                        }
                    }
                    drawPath(
                        path = path,
                        color = gridColor.copy(alpha = 0.35f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Draw 3D Timezone Pointers with 1:1 real-time coordinates
                for (loc in locations) {
                    val isSelected = loc.id == selectedLocation.id
                    val (baseX, baseY, baseZ) = project(loc.latitudeDeg, loc.longitudeDeg, 1.0f)
                    val (pinX, pinY, _) = project(loc.latitudeDeg, loc.longitudeDeg, 1.18f)

                    if (baseZ > 0) {
                        val pinColor = if (isSelected) pinAccentColor else highlightColor
                        val stemWidth = if (isSelected) 2.5.dp.toPx() else 1.5.dp.toPx()

                        // Pointer stem
                        drawLine(
                            color = pinColor,
                            start = Offset(baseX, baseY),
                            end = Offset(pinX, pinY),
                            strokeWidth = stemWidth,
                            cap = StrokeCap.Round
                        )

                        // Base anchor dot
                        drawCircle(
                            color = pinColor.copy(alpha = 0.8f),
                            radius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                            center = Offset(baseX, baseY)
                        )

                        // Pin head
                        drawCircle(
                            color = pinColor,
                            radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                            center = Offset(pinX, pinY)
                        )

                        // Label chip
                        val labelText = "${loc.countryCode} · ${loc.id}"
                        val textLayout = textMeasurer.measure(
                            text = labelText,
                            style = TextStyle(
                                color = if (isSelected) Color.White else ElectricSlateTokens.TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ElmsSans
                            )
                        )

                        val chipPadding = 4.dp.toPx()
                        val chipW = textLayout.size.width + chipPadding * 2
                        val chipH = textLayout.size.height + chipPadding * 1.5f
                        val chipLeft = pinX - chipW / 2f
                        val chipTop = pinY - chipH - 4.dp.toPx()

                        drawRoundRect(
                            color = if (isSelected) pinAccentColor else ElectricSlateTokens.SurfaceElevated,
                            topLeft = Offset(chipLeft, chipTop),
                            size = androidx.compose.ui.geometry.Size(chipW, chipH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )

                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(chipLeft + chipPadding, chipTop + chipPadding * 0.75f)
                        )
                    }
                }
            }

            // Drag hint at bottom
            Text(
                text = "Drag to rotate 3D Tech Globe · 1:1 World Timezones",
                color = ElectricSlateTokens.TextMuted,
                fontSize = 11.sp,
                fontFamily = ElmsSans,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }

        if (showLocationChips) {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(locations) { loc ->
                    val isSelected = loc.id == selectedLocation.id
                    Surface(
                        color = if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.SurfaceElevated,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .jigglePressClickable { onSelectLocation(loc) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[${loc.countryCode}] ${loc.id}",
                                color = if (isSelected) Color.White else ElectricSlateTokens.TextPrimary,
                                fontSize = 12.sp,
                                fontFamily = ElmsSans,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
