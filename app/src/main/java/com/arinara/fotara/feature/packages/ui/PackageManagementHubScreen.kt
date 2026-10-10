// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.packages.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.feature.packages.loader.FotaraPackageManager
import com.arinara.fotara.feature.packages.model.FpkgManifest
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeAddButtonBlue
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.ui.components.SettingsSubScreenHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Settings hub screen managing dynamic Fotara modular add-ons (.fpkg).
 * Displays storage consumption, reclaimed disk quota, installed modules with runtime toggle,
 * and official repository packages available for on-demand installation.
 * Adheres strictly to Android.md tokens: ElmsSans typography, 16dp horizontal padding (U-31),
 * 8dp/12dp corner radii, and MidnightNavy palette.
 */
@Composable
fun PackageManagementHubScreen(
    packageManager: FotaraPackageManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val installedList by packageManager.installedPackages.collectAsState()
    val availableList by packageManager.availablePackages.collectAsState()
    val reclaimedBytes by packageManager.reclaimedStorageBytes.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var installingPackageId by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024.0 * 1024.0))
            bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes.toDouble() / 1024.0)
            else -> "$bytes B"
        }
    }

    val installedFootprint = packageManager.getInstalledStorageBytes()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeNearBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            SettingsSubScreenHeader(
                title = "Modular Add-Ons",
                subtitle = "Storage footprint optimization (<25MB Base APK)",
                onBackClick = onBackClick
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Storage Statistics Hero Card
                item {
                    StorageSummaryCard(
                        installedCount = installedList.size,
                        installedFootprint = formatBytes(installedFootprint),
                        reclaimedFootprint = formatBytes(reclaimedBytes)
                    )
                }

                // Status Message Feedback
                if (statusMessage != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1B2338))
                                .border(1.dp, HomeCardBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = statusMessage ?: "",
                                color = TextPrimary,
                                fontFamily = ElmsSans,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Section: Installed Packages
                item {
                    SectionHeader(
                        title = "INSTALLED PACKAGES",
                        badge = "${installedList.size}"
                    )
                }

                if (installedList.isEmpty()) {
                    item {
                        EmptyStateCard(
                            message = "No modular add-ons currently installed.",
                            subtext = "The base APK is operating at minimal storage footprint. Install official modules below on demand."
                        )
                    }
                } else {
                    items(installedList, key = { it.packageId }) { manifest ->
                        InstalledPackageCard(
                            manifest = manifest,
                            onToggle = { isEnabled ->
                                packageManager.togglePackage(manifest.packageId, isEnabled)
                            },
                            onUninstall = {
                                coroutineScope.launch {
                                    val result = packageManager.uninstallPackage(manifest.packageId)
                                    result.onSuccess { reclaimed ->
                                        statusMessage = "Uninstalled ${manifest.name}. Reclaimed ${formatBytes(reclaimed)}."
                                    }.onFailure { err ->
                                        statusMessage = "Uninstall failed: ${err.message}"
                                    }
                                }
                            }
                        )
                    }
                }

                // Section: Available Official Add-Ons
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        title = "AVAILABLE OFFICIAL ADD-ONS",
                        badge = "${availableList.size}"
                    )
                }

                if (availableList.isEmpty()) {
                    item {
                        EmptyStateCard(
                            message = "All official packages installed.",
                            subtext = "Every specialized capability is active in local storage."
                        )
                    }
                } else {
                    items(availableList, key = { it.packageId }) { manifest ->
                        val isDownloading = installingPackageId == manifest.packageId
                        AvailablePackageCard(
                            manifest = manifest,
                            isDownloading = isDownloading,
                            onInstall = {
                                installingPackageId = manifest.packageId
                                statusMessage = "Verifying signature and installing ${manifest.name}..."
                                coroutineScope.launch {
                                    delay(400) // Simulated smooth verification step
                                    val result = packageManager.installPackage(manifest.packageId)
                                    installingPackageId = null
                                    result.onSuccess {
                                        statusMessage = "Successfully installed ${manifest.name}."
                                    }.onFailure { err ->
                                        statusMessage = "Installation failed: ${err.message}"
                                    }
                                }
                            }
                        )
                    }
                }

                // Bottom Breathing Room
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StorageSummaryCard(
    installedCount: Int,
    installedFootprint: String,
    reclaimedFootprint: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HomeCardSurface)
            .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Storage Status",
                    tint = HomeAddButtonBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Storage & Modularity Quota",
                    color = TextPrimary,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn(label = "Installed", value = "$installedCount Packages")
                StatColumn(label = "In-Use Size", value = installedFootprint)
                StatColumn(label = "Reclaimed Space", value = reclaimedFootprint)
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = HomeSubtitleGray,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SectionHeader(title: String, badge: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            color = HomeSubtitleGray,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1B2438))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = badge,
                color = TextSecondary,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun EmptyStateCard(message: String, subtext: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HomeCardSurface)
            .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = message,
                color = TextPrimary,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtext,
                color = HomeSubtitleGray,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun InstalledPackageCard(
    manifest: FpkgManifest,
    onToggle: (Boolean) -> Unit,
    onUninstall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HomeCardSurface)
            .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Icon Tile (40dp, 8dp radius)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF172036)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolvePackageIcon(manifest.iconKey),
                        contentDescription = manifest.name,
                        tint = HomeAddButtonBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = manifest.name,
                        color = TextPrimary,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Version ${manifest.version} • ${manifest.formattedSize()}",
                        color = HomeSubtitleGray,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                // Uninstall Action
                IconButton(
                    onClick = onUninstall,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Uninstall Add-On",
                        tint = TagCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = manifest.description,
                color = TextSecondary,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Light,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Enable / Disable Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (manifest.isEnabled) "Status: Enabled" else "Status: Disabled",
                    color = if (manifest.isEnabled) TextPrimary else HomeSubtitleGray,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Switch(
                    checked = manifest.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = HomeAddButtonBlue,
                        uncheckedThumbColor = HomeSubtitleGray,
                        uncheckedTrackColor = Color(0xFF1A2234)
                    )
                )
            }
        }
    }
}

@Composable
private fun AvailablePackageCard(
    manifest: FpkgManifest,
    isDownloading: Boolean,
    onInstall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HomeCardSurface)
            .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Icon Tile (40dp, 8dp radius)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF172036)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolvePackageIcon(manifest.iconKey),
                        contentDescription = manifest.name,
                        tint = Color(0xFF93C5FD),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = manifest.name,
                        color = TextPrimary,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Package Size: ${manifest.formattedSize()}",
                        color = HomeSubtitleGray,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = manifest.description,
                color = TextSecondary,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Light,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            if (manifest.permissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (perm in manifest.permissions.take(3)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1B2338))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = perm,
                                color = HomeSubtitleGray,
                                fontFamily = ElmsSans,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button: Download & Install (56dp standard height, 8dp radius per Android.md)
            Button(
                onClick = onInstall,
                enabled = !isDownloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeAddButtonBlue,
                    disabledContainerColor = Color(0xFF1A2644)
                )
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verifying & Installing...",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Install Add-On",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Install Add-On (${manifest.formattedSize()})",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun resolvePackageIcon(iconKey: String): ImageVector {
    return when (iconKey) {
        "collab" -> Icons.Default.ViewInAr
        "whiteboard_ocr" -> Icons.Default.Memory
        "exam_analytics" -> Icons.Default.Timeline
        else -> Icons.Default.Extension
    }
}
