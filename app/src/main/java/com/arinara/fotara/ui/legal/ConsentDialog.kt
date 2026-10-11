// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.legal

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arinara.fotara.R
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.legal.LegalDocumentLoader
import com.arinara.fotara.legal.ParsedLegalDocument
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.ui.components.ReleaseNotesRenderer

/**
 * First-launch modal consent dialog for Terms of Service and Privacy Policy.
 * - Highest priority in AppDialogCoordinator; never preempted.
 * - Outside tap is blocked; back gesture or X closes the application (finishAffinity).
 * - Persists accepted versions and anonymous device count preference upon Accept.
 */
@Composable
fun ConsentDialog(
    onAccept: (deviceCountEnabled: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loader = remember(context) { LegalDocumentLoader(context) }

    val privacyDoc = remember(loader) {
        try { loader.loadPrivacyPolicy() } catch (_: Exception) {
            ParsedLegalDocument("Privacy Policy", 1, "2026-10-06", "Privacy Policy content loading...", "")
        }
    }
    val termsDoc = remember(loader) {
        try { loader.loadTermsOfService() } catch (_: Exception) {
            ParsedLegalDocument("Terms of Service", 1, "2026-10-06", "Terms of Service content loading...", "")
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var deviceCountChecked by rememberSaveable { mutableStateOf(SettingsRepository.DEVICE_COUNT_DEFAULT_ENABLED) }

    val privacyScrollState = rememberScrollState()
    val termsScrollState = rememberScrollState()

    // System back gesture closes the application
    BackHandler {
        (context as? Activity)?.finishAffinity()
    }

    Dialog(
        onDismissRequest = {
            // Outside tap does not dismiss, dialog is modal
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        val configuration = LocalConfiguration.current
        val screenHeightDp = configuration.screenHeightDp.dp
        val maxContentHeight = (screenHeightDp * 0.45f).coerceIn(160.dp, 360.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f))
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F131D)),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color(0xFF26324A)),
                modifier = modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume clicks to prevent backdrop trigger
                    )
            ) {
                val dialogScrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(dialogScrollState)
                        .padding(20.dp)
                ) {
                    // Header: Title and Close (X) button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.consent_dialog_title),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                (context as? Activity)?.finishAffinity()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close App",
                                tint = HomeSubtitleGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Segmented Control Tabs (Privacy Policy / Terms of Service)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF161C2C))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == 0) FolderBodyBlue else Color.Transparent)
                                .clickable { selectedTab = 0 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.privacy_policy_title),
                                color = if (selectedTab == 0) Color.White else HomeSubtitleGray,
                                fontSize = 13.sp,
                                fontFamily = ElmsSans,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == 1) FolderBodyBlue else Color.Transparent)
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.terms_of_service_title),
                                color = if (selectedTab == 1) Color.White else HomeSubtitleGray,
                                fontSize = 13.sp,
                                fontFamily = ElmsSans,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrollable document body (~half screen height)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxContentHeight)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0A0D14))
                            .border(1.dp, Color(0xFF1F273E), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        if (selectedTab == 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(privacyScrollState)
                            ) {
                                Text(
                                    text = "Version ${privacyDoc.version}, effective ${privacyDoc.effectiveDate}",
                                    color = HomeSubtitleGray,
                                    fontSize = 11.sp,
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                ReleaseNotesRenderer(
                                    markdown = privacyDoc.body,
                                    primaryTextColor = Color.White.copy(alpha = 0.88f),
                                    accentColor = Color(0xFF60A5FA),
                                    surfaceColor = Color(0xFF0A0D14),
                                    cardBorder = Color(0xFF1F273E),
                                    allowImages = false
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(termsScrollState)
                            ) {
                                Text(
                                    text = "Version ${termsDoc.version}, effective ${termsDoc.effectiveDate}",
                                    color = HomeSubtitleGray,
                                    fontSize = 11.sp,
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                ReleaseNotesRenderer(
                                    markdown = termsDoc.body,
                                    primaryTextColor = Color.White.copy(alpha = 0.88f),
                                    accentColor = Color(0xFF60A5FA),
                                    surfaceColor = Color(0xFF0A0D14),
                                    cardBorder = Color(0xFF1F273E),
                                    allowImages = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF26324A), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Anonymous Device Count Switch Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { deviceCountChecked = !deviceCountChecked }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.device_count_switch_title),
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.device_count_switch_desc),
                                color = HomeSubtitleGray,
                                fontSize = 11.5.sp,
                                fontFamily = ElmsSans,
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Switch(
                            checked = deviceCountChecked,
                            onCheckedChange = { deviceCountChecked = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = HomeMainButtonBlue,
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0xFF1E2638)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Muted notice
                    Text(
                        text = stringResource(R.string.consent_accept_statement),
                        color = HomeSubtitleGray,
                        fontSize = 11.5.sp,
                        fontFamily = ElmsSans,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Button: Accept and continue
                    Button(
                        onClick = {
                            onAccept(deviceCountChecked)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.consent_accept_button),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
