// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.expression.CustomExpression
import com.arinara.fotara.expression.ExpressionCategory
import com.arinara.fotara.expression.ExpressionDefinition
import com.arinara.fotara.expression.ExpressionEngine
import com.arinara.fotara.expression.ExpressionRepository
import com.arinara.fotara.expression.ExpressionType
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.presets.AppBottomSheet
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.components.presets.AppListItem
import com.arinara.fotara.ui.components.presets.AppListItemAction
import com.arinara.fotara.ui.components.presets.AppNoticeBanner
import com.arinara.fotara.ui.components.presets.AppNoticeVariant
import com.arinara.fotara.ui.components.presets.AppPillChip
import com.arinara.fotara.ui.components.presets.AppScreen
import com.arinara.fotara.ui.components.presets.AppTopBar
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpressionSettingsScreen(
    onBack: () -> Unit,
    expressionRepository: ExpressionRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val customExpressions by expressionRepository.customExpressionsFlow.collectAsState()
    val disabledBuiltIns by expressionRepository.disabledBuiltInsFlow.collectAsState()
    val defaultDateFormat by expressionRepository.defaultDateFormatFlow.collectAsState()
    val defaultTimeFormat by expressionRepository.defaultTimeFormatFlow.collectAsState()

    var selectedTab by remember { mutableStateOf<ExpressionCategory?>(null) }
    var editingExpression by remember { mutableStateOf<CustomExpression?>(null) }
    var isCreateSheetOpen by remember { mutableStateOf(false) }
    var showFormatPickerSheet by remember { mutableStateOf(false) }
    var deletingExpression by remember { mutableStateOf<CustomExpression?>(null) }

    val sampleContext = remember(defaultDateFormat, defaultTimeFormat) {
        com.arinara.fotara.expression.ExpressionContext(
            dateFormat = defaultDateFormat,
            timeFormat = defaultTimeFormat,
            title = "Sample Note",
            folder = "Computer Science",
            userName = "Student",
            page = 1,
            pages = 12
        )
    }

    val filteredBuiltIns = remember(selectedTab, disabledBuiltIns) {
        if (selectedTab == null) {
            ExpressionEngine.BUILT_IN_DEFINITIONS
        } else {
            ExpressionEngine.BUILT_IN_DEFINITIONS.filter { it.category == selectedTab }
        }
    }

    AppScreen(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Expression Title",
                onBackClick = onBack
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 100.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Overview & Default Formats
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ElectricSlateTokens.SurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    tint = ElectricSlateTokens.PrimaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Expression Titles",
                                    color = ElectricSlateTokens.TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ElmsSans
                                )
                                Text(
                                    text = "Dynamic & static shorthand tokens starting with '!'",
                                    color = ElectricSlateTokens.TextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = ElmsSans
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Default Formats Summary
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricSlateTokens.SurfaceElevated)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Default Date & Time Formats",
                                    color = ElectricSlateTokens.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = ElmsSans
                                )
                                Text(
                                    text = "Date: $defaultDateFormat • Time: $defaultTimeFormat",
                                    color = ElectricSlateTokens.TextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = ElmsSans
                                )
                            }
                            AppButton(
                                text = "Change",
                                variant = AppButtonVariant.Secondary,
                                height = 36.dp,
                                onClick = { showFormatPickerSheet = true }
                            )
                        }
                    }
                }
            }

            // Section 2: Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        AppPillChip(
                            label = "All",
                            isSelected = selectedTab == null,
                            onClick = { selectedTab = null }
                        )
                    }
                    items(ExpressionCategory.values().toList()) { category ->
                        AppPillChip(
                            label = category.name.lowercase().replaceFirstChar { it.titlecase() },
                            isSelected = selectedTab == category,
                            onClick = { selectedTab = category }
                        )
                    }
                }
            }

            // Section 3: Custom Expressions Manager
            if (selectedTab == null || selectedTab == ExpressionCategory.CUSTOM) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Custom Expressions (${customExpressions.size})",
                                color = ElectricSlateTokens.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ElmsSans
                            )
                            Text(
                                text = "User-defined words & multi-token combinations",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 12.sp,
                                fontFamily = ElmsSans
                            )
                        }
                        AppButton(
                            text = "Create",
                            variant = AppButtonVariant.Primary,
                            icon = Icons.Default.Add,
                            height = 36.dp,
                            onClick = {
                                editingExpression = null
                                isCreateSheetOpen = true
                            }
                        )
                    }
                }

                if (customExpressions.isEmpty()) {
                    item {
                        AppCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No Custom Expressions",
                                    color = ElectricSlateTokens.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = ElmsSans
                                )
                                Text(
                                    text = "Create shortcuts like !sig for \"Created by Me\" or combine !day + !date for logs.",
                                    color = ElectricSlateTokens.TextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = ElmsSans
                                )
                            }
                        }
                    }
                } else {
                    items(customExpressions, key = { it.id }) { expr ->
                        CustomExpressionCard(
                            expression = expr,
                            onEdit = {
                                editingExpression = expr
                                isCreateSheetOpen = true
                            },
                            onDuplicate = {
                                coroutineScope.launch {
                                    val dup = expressionRepository.duplicateCustomExpression(expr.id)
                                    if (dup != null) {
                                        Toast.makeText(context, "Duplicated !${expr.name}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Cannot duplicate !${expr.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDelete = {
                                deletingExpression = expr
                            },
                            sampleContext = sampleContext,
                            allCustom = customExpressions,
                            disabledBuiltIns = disabledBuiltIns
                        )
                    }
                }
            }

            // Section 4: Built-in Expressions List
            if (selectedTab != ExpressionCategory.CUSTOM) {
                item {
                    Text(
                        text = "Built-in Expressions",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ElmsSans
                    )
                }

                items(filteredBuiltIns, key = { it.token }) { def ->
                    val isEnabled = !disabledBuiltIns.contains(def.token.lowercase())
                    val sampleResolved = remember(def.token, sampleContext, isEnabled) {
                        if (isEnabled) {
                            ExpressionEngine.resolve(def.displayToken, sampleContext)
                        } else {
                            def.displayToken
                        }
                    }

                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        AppListItem(
                            title = def.displayToken,
                            subtitle = "${def.description}\nSample: \"$sampleResolved\"",
                            leadingIcon = when (def.category) {
                                ExpressionCategory.TIME -> Icons.Default.Schedule
                                ExpressionCategory.CONTEXT -> Icons.Default.Title
                                ExpressionCategory.SEQUENCE -> Icons.Default.AutoFixHigh
                                ExpressionCategory.TRANSFORM -> Icons.Default.TextFields
                                else -> Icons.Default.AutoFixHigh
                            },
                            action = AppListItemAction.Toggle(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    coroutineScope.launch {
                                        expressionRepository.toggleBuiltIn(def.token, checked)
                                    }
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet 1: Create or Edit Custom Expression
    if (isCreateSheetOpen) {
        val initialExpr = editingExpression ?: CustomExpression(
            name = "",
            type = ExpressionType.WORD,
            value = ""
        )
        CustomExpressionEditorSheet(
            expression = initialExpr,
            onDismiss = { isCreateSheetOpen = false },
            onSave = { saved ->
                coroutineScope.launch {
                    val validation = expressionRepository.saveCustomExpression(saved)
                    if (validation.isValid) {
                        Toast.makeText(context, "Saved !${saved.name}", Toast.LENGTH_SHORT).show()
                        isCreateSheetOpen = false
                    } else {
                        Toast.makeText(context, validation.errorMessage ?: "Validation failed", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            existingList = customExpressions,
            disabledBuiltIns = disabledBuiltIns,
            sampleContext = sampleContext
        )
    }

    // Modal Sheet 2: Delete Confirmation Sheet with Dependency Notice
    if (deletingExpression != null) {
        val customToDelete = deletingExpression!!
        val referencingExpressions = remember(customToDelete, customExpressions) {
            customExpressions.filter { other ->
                other.id != customToDelete.id && other.value.contains("!${customToDelete.name}")
            }
        }

        AppBottomSheet(
            visible = true,
            onDismiss = { deletingExpression = null },
            title = "Delete Expression !${customToDelete.name}"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (referencingExpressions.isNotEmpty()) {
                    AppNoticeBanner(
                        visible = true,
                        message = "Warning: !${customToDelete.name} is referenced inside: ${referencingExpressions.joinToString { "!${it.name}" }}. Deleting it will cause these combinations to resolve as plain text.",
                        variant = AppNoticeVariant.WARNING
                    )
                } else {
                    Text(
                        text = "Are you sure you want to delete the custom expression '!${customToDelete.name}'?",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppButton(
                        text = "Cancel",
                        variant = AppButtonVariant.Secondary,
                        modifier = Modifier.weight(1f),
                        onClick = { deletingExpression = null }
                    )
                    AppButton(
                        text = "Delete",
                        variant = AppButtonVariant.Destructive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            coroutineScope.launch {
                                expressionRepository.deleteCustomExpression(customToDelete.id)
                                Toast.makeText(context, "Deleted !${customToDelete.name}", Toast.LENGTH_SHORT).show()
                                deletingExpression = null
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal Sheet 3: Default Date & Time Formats Picker Sheet
    if (showFormatPickerSheet) {
        AppBottomSheet(
            visible = true,
            onDismiss = { showFormatPickerSheet = false },
            title = "Default Date & Time Formats"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Default Date Format",
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ElmsSans
                )

                val dateFormats = listOf("dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy", "dd MMM yyyy", "dd.MM.yyyy")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dateFormats.forEach { fmt ->
                        AppPillChip(
                            label = fmt,
                            isSelected = defaultDateFormat == fmt,
                            onClick = {
                                coroutineScope.launch {
                                    expressionRepository.setDefaultDateFormat(fmt)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Default Time Format",
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ElmsSans
                )

                val timeFormats = listOf("HH:mm", "hh:mm a", "HH:mm:ss")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timeFormats.forEach { fmt ->
                        AppPillChip(
                            label = fmt,
                            isSelected = defaultTimeFormat == fmt,
                            onClick = {
                                coroutineScope.launch {
                                    expressionRepository.setDefaultTimeFormat(fmt)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                AppButton(
                    text = "Done",
                    variant = AppButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showFormatPickerSheet = false }
                )
            }
        }
    }
}

@Composable
private fun CustomExpressionCard(
    expression: CustomExpression,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    sampleContext: com.arinara.fotara.expression.ExpressionContext,
    allCustom: List<CustomExpression>,
    disabledBuiltIns: Set<String>
) {
    val resolvedPreview = remember(expression.value, sampleContext, allCustom) {
        ExpressionEngine.resolve(
            input = expression.value,
            context = sampleContext,
            customExpressions = allCustom,
            disabledBuiltIns = disabledBuiltIns
        )
    }

    AppCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "!${expression.name}",
                        color = ElectricSlateTokens.PrimaryAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ElmsSans
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (expression.type == ExpressionType.WORD) "Custom Word" else "Combination",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 11.sp,
                            fontFamily = ElmsSans
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .jigglePressClickable(onClick = onEdit),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .jigglePressClickable(onClick = onDuplicate),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate",
                            tint = ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .jigglePressClickable(onClick = onDelete),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ElectricSlateTokens.Destructive,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = "Template: ${expression.value}",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 14.sp,
                fontFamily = ElmsSans
            )

            // Resolved live preview chip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricSlateTokens.SurfaceElevated)
                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Resolves to: \"$resolvedPreview\"",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomExpressionEditorSheet(
    expression: CustomExpression,
    onDismiss: () -> Unit,
    onSave: (CustomExpression) -> Unit,
    existingList: List<CustomExpression>,
    disabledBuiltIns: Set<String>,
    sampleContext: com.arinara.fotara.expression.ExpressionContext
) {
    var name by remember { mutableStateOf(expression.name.removePrefix("!")) }
    var type by remember { mutableStateOf(expression.type) }
    var value by remember { mutableStateOf(expression.value) }

    val exprCount = remember(value) {
        ExpressionEngine.countExpressionsInCombination(value)
    }

    val liveResolved = remember(value, sampleContext, existingList) {
        ExpressionEngine.resolve(
            input = value,
            context = sampleContext,
            customExpressions = existingList,
            disabledBuiltIns = disabledBuiltIns
        )
    }

    val candidate = expression.copy(
        name = name.trim(),
        type = type,
        value = value
    )

    val validation = remember(name, type, value, existingList) {
        ExpressionEngine.validateCustomExpression(
            expression = candidate,
            existing = existingList,
            disabledBuiltIns = disabledBuiltIns
        )
    }

    AppBottomSheet(
        visible = true,
        onDismiss = onDismiss,
        title = if (expression.id == 0L) "New Custom Expression" else "Edit !${expression.name}"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Type Selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppPillChip(
                    label = "Custom Word (Type A)",
                    isSelected = type == ExpressionType.WORD,
                    onClick = { type = ExpressionType.WORD }
                )
                AppPillChip(
                    label = "Combination (Type B)",
                    isSelected = type == ExpressionType.COMBINATION,
                    onClick = { type = ExpressionType.COMBINATION }
                )
            }

            // Name field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Token Name",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.SurfaceElevated)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "!",
                        color = ElectricSlateTokens.PrimaryAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ElmsSans
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    BasicTextField(
                        value = name,
                        onValueChange = { input ->
                            val clean = input.filter { it.isLetterOrDigit() || it == '_' }
                            if (clean.length <= ExpressionEngine.MAX_CUSTOM_NAME_LENGTH) {
                                name = clean
                            }
                        },
                        textStyle = TextStyle(
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 14.sp,
                            fontFamily = ElmsSans
                        ),
                        cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Value field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (type == ExpressionType.WORD) "Text Value" else "Expression Combination Template",
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans
                    )
                    if (type == ExpressionType.COMBINATION) {
                        Text(
                            text = "$exprCount / 5 expressions",
                            color = if (exprCount > 5) ElectricSlateTokens.Destructive else ElectricSlateTokens.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ElmsSans
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.SurfaceElevated)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { value = it },
                        textStyle = TextStyle(
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 14.sp,
                            fontFamily = ElmsSans
                        ),
                        cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Quick Insertion Tokens for Combinations
            if (type == ExpressionType.COMBINATION) {
                Text(
                    text = "Quick Insert Tokens:",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 11.sp,
                    fontFamily = ElmsSans
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickTokens = listOf("!date", "!time", "!day", "!month", "!year", "!folder", "!title", "!user", "!n")
                    quickTokens.forEach { token ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(ElectricSlateTokens.SurfacePanel)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(999.dp))
                                .clickable {
                                    val delimiter = if (value.isNotEmpty() && !value.endsWith(" ")) " " else ""
                                    value = "$value$delimiter$token"
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+ $token",
                                color = ElectricSlateTokens.TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = ElmsSans
                            )
                        }
                    }
                }
            }

            // Live Preview Card
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Live Preview",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 11.sp,
                    fontFamily = ElmsSans
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.SurfacePanel)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (value.isBlank()) "Type a value above to preview" else liveResolved,
                        color = if (value.isBlank()) ElectricSlateTokens.TextMuted else ElectricSlateTokens.TextPrimary,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans
                    )
                }
            }

            // Error notice banner if validation fails
            if (!validation.isValid && validation.errorMessage != null) {
                AppNoticeBanner(
                    visible = true,
                    message = validation.errorMessage,
                    variant = AppNoticeVariant.ERROR
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppButton(
                    text = "Cancel",
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                )
                AppButton(
                    text = "Save",
                    variant = AppButtonVariant.Primary,
                    enabled = validation.isValid,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (validation.isValid) {
                            onSave(candidate)
                        }
                    }
                )
            }
        }
    }
}
