// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.theme.ElectricSlateTokens

@Composable
fun BannerSourceDialog(
    onDismiss: () -> Unit,
    onChooseImage: () -> Unit,
    onChooseGif: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.profile_action_change_banner),
                color = ElectricSlateTokens.TextPrimary,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChooseImage() }
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.profile_banner_choose_image),
                        color = ElectricSlateTokens.TextPrimary,
                        fontFamily = ElmsSans,
                        fontSize = 16.sp
                    )
                }

                HorizontalDivider(color = ElectricSlateTokens.BorderSubtle)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChooseGif() }
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.profile_banner_choose_gif),
                        color = ElectricSlateTokens.TextPrimary,
                        fontFamily = ElmsSans,
                        fontSize = 16.sp
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY
            )
        }
    )
}
