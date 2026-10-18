// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePlacementSidePanelSheet(
    visible: Boolean,
    stagedItems: List<StagedShareItem>,
    isGroupDestination: Boolean,
    placeableCount: Int,
    onDismiss: () -> Unit,
    onSetAllSelected: (Boolean) -> Unit,
    onToggleItemSelection: (String) -> Unit,
    onRemoveStagedItem: (String) -> Unit
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Shared Items (${stagedItems.size})",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                val allSelected = stagedItems.all { it.isSelected || it.isError }
                AppButton(
                    text = if (allSelected) "Deselect All" else "Select All",
                    onClick = { onSetAllSelected(!allSelected) },
                    variant = AppButtonVariant.OUTLINED,
                    height = 32.dp,
                    cornerRadius = 12.dp
                )
            }

            HorizontalDivider(color = MidnightCardOutline, modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stagedItems) { item ->
                    StagedItemRow(
                        item = item,
                        isGroupDestination = isGroupDestination,
                        showCheckbox = true,
                        onToggle = { onToggleItemSelection(item.id) },
                        onRemove = { onRemoveStagedItem(item.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AppButton(
                text = "Done Selecting ($placeableCount)",
                onClick = onDismiss,
                variant = AppButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth(),
                height = 48.dp,
                cornerRadius = 12.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
