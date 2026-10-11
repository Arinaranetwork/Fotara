// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.math

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeAddButtonBlue
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.TextPrimary

/**
 * Promotional modal bottom sheet presented when attempting to use LaTeX typesetting
 * while the modular package (addon.latex.math) is not installed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatexAddonPromoSheet(
    onDismiss: () -> Unit,
    onOpenAddonHub: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HomeCardSurface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(HomeAddButtonBlue.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Functions,
                        contentDescription = "LaTeX Add-On",
                        tint = HomeAddButtonBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "LaTeX Math Support Add-On",
                        color = TextPrimary,
                        fontFamily = ElmsSans,
                        fontSize = 18.sp,
                        lineHeight = 24.sp
                    )
                    Text(
                        text = "Modular Academic Extension (1.4 MB)",
                        color = HomeSubtitleGray,
                        fontFamily = ElmsSans,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Render advanced mathematical formulas, matrices, calculus notation, and scientific equations directly inside your notes.",
                color = TextPrimary.copy(alpha = 0.9f),
                fontFamily = ElmsSans,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CTA Button: Open Add-On Hub
            Button(
                onClick = {
                    onDismiss()
                    onOpenAddonHub()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeAddButtonBlue
                )
            ) {
                Text(
                    text = "Open Add-On Hub",
                    fontFamily = ElmsSans,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Button: Dismiss
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.dp, HomeCardBorder)
            ) {
                Text(
                    text = "Dismiss",
                    fontFamily = ElmsSans,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
