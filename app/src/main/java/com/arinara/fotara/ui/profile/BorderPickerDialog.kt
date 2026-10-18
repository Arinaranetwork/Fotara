// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.data.model.ProfileBorders
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeSubtitleGray

@Composable
fun BorderPickerDialog(
    currentAvatarPath: String?,
    selectedBorderId: String,
    onSelectBorder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(R.string.profile_border_picker_title),
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 20.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                items(ProfileBorders.ALL_BORDERS, key = { it.id }) { borderItem ->
                    val isSelected = borderItem.id == selectedBorderId

                    AppCard(
                        modifier = Modifier.aspectRatio(1f),
                        cornerRadius = 16.dp,
                        containerColor = if (isSelected) {
                            ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f)
                        } else {
                            ElectricSlateTokens.SurfacePanel
                        },
                        borderColor = if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                        onClick = {
                            onSelectBorder(borderItem.id)
                            onDismiss()
                        }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            ProfileAvatar(
                                avatarPath = currentAvatarPath,
                                borderId = borderItem.id,
                                avatarSize = ProfileAvatarDefaults.PickerAvatarSize,
                                showEditButton = false
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = borderItem.displayName,
                                color = if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextPrimary,
                                fontSize = 12.sp,
                                fontFamily = ElmsSans,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = stringResource(R.string.confirm_cancel),
                variant = AppButtonVariant.SECONDARY,
                onClick = onDismiss
            )
        }
    )
}
