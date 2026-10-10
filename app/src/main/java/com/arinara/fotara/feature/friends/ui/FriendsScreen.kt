// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.friends.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arinara.fotara.feature.friends.model.FriendProfile
import com.arinara.fotara.feature.friends.model.StudyPresenceStatus
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.HomeSearchBarBorder
import com.arinara.fotara.theme.HomeSearchBarSurface
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.ui.components.SettingsSubScreenHeader
import com.arinara.fotara.ui.components.SettingsSubScreenHeaderDefaults

/**
 * Dedicated Friends System Screen for academic collaboration and study buddy networking.
 * Uses SettingsSubScreenHeader and windowInsetsPadding(WindowInsets.statusBars) to ensure
 * clean separation from phone hardware status bar and battery indicators.
 */
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onInviteToStudy: ((FriendProfile) -> Unit)? = null,
    onShareNotes: ((FriendProfile) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showQrDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showPresenceSubjectDialog by remember { mutableStateOf(false) }
    var pendingStatusForSubject by remember { mutableStateOf<StudyPresenceStatus?>(null) }
    var newSubjectInput by remember { mutableStateOf("") }

    var friendToDelete by remember { mutableStateOf<FriendProfile?>(null) }

    LaunchedEffect(uiState.message) {
        val msg = uiState.message
        if (!msg.isNullOrEmpty()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeNearBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Standard Settings Sub-Screen Header with back navigation and action tiles
            SettingsSubScreenHeader(
                title = "Study Buddies",
                subtitle = "Academic peer network & presence",
                onBackClick = onBackClick,
                modifier = Modifier.padding(horizontal = 16.dp),
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { showQrDialog = true },
                            modifier = Modifier
                                .size(SettingsSubScreenHeaderDefaults.ButtonSize)
                                .clip(RoundedCornerShape(SettingsSubScreenHeaderDefaults.ButtonCornerRadius))
                                .background(SettingsSubScreenHeaderDefaults.ButtonBackgroundColor)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCode,
                                contentDescription = "Show QR Code",
                                tint = SettingsSubScreenHeaderDefaults.ButtonIconTint,
                                modifier = Modifier.size(SettingsSubScreenHeaderDefaults.IconSize)
                            )
                        }

                        IconButton(
                            onClick = { showAddFriendDialog = true },
                            modifier = Modifier
                                .size(SettingsSubScreenHeaderDefaults.ButtonSize)
                                .clip(RoundedCornerShape(SettingsSubScreenHeaderDefaults.ButtonCornerRadius))
                                .background(SettingsSubScreenHeaderDefaults.ButtonBackgroundColor)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = "Add Friend",
                                tint = SettingsSubScreenHeaderDefaults.ButtonIconTint,
                                modifier = Modifier.size(SettingsSubScreenHeaderDefaults.IconSize)
                            )
                        }
                    }
                }
            )

            // 1. Search Bar (U-31: 16dp horizontal padding)
            FriendsSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // 2. My Study Presence Status Section
            MyPresenceStatusSection(
                currentStatus = uiState.myPresenceStatus,
                currentSubject = uiState.mySubject,
                myHandle = uiState.myHandle,
                onSelectStatus = { status ->
                    if (status == StudyPresenceStatus.STUDYING || status == StudyPresenceStatus.OPEN_TO_COLLAB) {
                        pendingStatusForSubject = status
                        newSubjectInput = uiState.mySubject ?: ""
                        showPresenceSubjectDialog = true
                    } else {
                        viewModel.updatePresence(status, null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Buddy List or Empty State
            if (uiState.filteredFriends.isEmpty()) {
                if (uiState.searchQuery.isNotEmpty()) {
                    FriendsSearchEmptyState(
                        query = uiState.searchQuery,
                        onClearSearch = { viewModel.onSearchQueryChanged("") }
                    )
                } else {
                    FriendsEmptyState(
                        onAddFriendClick = { showAddFriendDialog = true },
                        onShowQrClick = { showQrDialog = true }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Active Study Buddies Group
                    if (uiState.activeBuddies.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Active Collaborators",
                                count = uiState.activeBuddies.size,
                                isAccent = true
                            )
                        }
                        items(uiState.activeBuddies, key = { it.id }) { friend ->
                            FriendItemCard(
                                friend = friend,
                                onInvite = {
                                    if (onInviteToStudy != null) {
                                        onInviteToStudy(friend)
                                    } else {
                                        Toast.makeText(context, "Study invitation sent to ${friend.displayName}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onShare = {
                                    if (onShareNotes != null) {
                                        onShareNotes(friend)
                                    } else {
                                        Toast.makeText(context, "Sharing notes with ${friend.displayName}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(friend.id) },
                                onDelete = { friendToDelete = friend }
                            )
                        }
                    }

                    // Offline Contacts Group
                    if (uiState.offlineBuddies.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Offline Contacts",
                                count = uiState.offlineBuddies.size,
                                isAccent = false
                            )
                        }
                        items(uiState.offlineBuddies, key = { it.id }) { friend ->
                            FriendItemCard(
                                friend = friend,
                                onInvite = {
                                    Toast.makeText(context, "Peer is offline. Study invitation queued for ${friend.displayName}", Toast.LENGTH_SHORT).show()
                                },
                                onShare = {
                                    if (onShareNotes != null) {
                                        onShareNotes(friend)
                                    } else {
                                        Toast.makeText(context, "Sharing notes with ${friend.displayName}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(friend.id) },
                                onDelete = { friendToDelete = friend }
                            )
                        }
                    }

                    // Content Edge breathing room (U-29, U-30)
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .imePadding()
        )
    }

    // Dialog: QR Code & Share
    if (showQrDialog) {
        FriendQrDialog(
            myHandle = uiState.myHandle,
            shareCode = viewModel.getShareCode(),
            onDismiss = { showQrDialog = false },
            onAddFriendFromCode = { handle, name ->
                viewModel.addFriend(handle, name) { success, _ ->
                    if (success) {
                        showQrDialog = false
                    }
                }
            }
        )
    }

    // Dialog: Add Friend
    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onConfirm = { handle, name ->
                viewModel.addFriend(handle, name) { success, _ ->
                    if (success) {
                        showAddFriendDialog = false
                    }
                }
            }
        )
    }

    // Dialog: Specify Subject for Presence
    if (showPresenceSubjectDialog && pendingStatusForSubject != null) {
        PresenceSubjectDialog(
            status = pendingStatusForSubject!!,
            initialSubject = newSubjectInput,
            onDismiss = {
                showPresenceSubjectDialog = false
                pendingStatusForSubject = null
            },
            onConfirm = { subject ->
                viewModel.updatePresence(pendingStatusForSubject!!, subject)
                showPresenceSubjectDialog = false
                pendingStatusForSubject = null
            }
        )
    }

    // Dialog: Confirm Friend Deletion
    if (friendToDelete != null) {
        AlertDialog(
            onDismissRequest = { friendToDelete = null },
            title = {
                Text(
                    text = "Remove Study Buddy",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove ${friendToDelete?.displayName} (${friendToDelete?.handle}) from your study buddy list?",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            containerColor = HomeCardSurface,
            shape = RoundedCornerShape(20.dp),
            confirmButton = {
                Button(
                    onClick = {
                        val id = friendToDelete?.id
                        if (id != null) viewModel.removeFriend(id)
                        friendToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Remove",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { friendToDelete = null }) {
                    Text(
                        text = "Cancel",
                        fontFamily = ElmsSans,
                        color = TextSecondary
                    )
                }
            }
        )
    }
}

/**
 * Search Bar component.
 */
@Composable
private fun FriendsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = HomeSearchBarSurface,
        border = BorderStroke(1.dp, HomeSearchBarBorder),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Search study buddies or @handle...",
                        fontFamily = ElmsSans,
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = "Clear search",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * My study presence status selector row.
 */
@Composable
private fun MyPresenceStatusSection(
    currentStatus: StudyPresenceStatus,
    currentSubject: String?,
    myHandle: String,
    onSelectStatus: (StudyPresenceStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Study Presence",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color.White
                )
                Text(
                    text = myHandle,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = FolderTabCream
                )
            }

            if (!currentSubject.isNullOrBlank() && currentStatus != StudyPresenceStatus.OFFLINE) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Focus: $currentSubject",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Color(0xFF60A5FA)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Presence Chip Row (horizontal scroll for compactness)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudyPresenceStatus.entries.forEach { status ->
                    val isSelected = currentStatus == status
                    PresenceChip(
                        status = status,
                        isSelected = isSelected,
                        onClick = { onSelectStatus(status) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresenceChip(
    status: StudyPresenceStatus,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFF1E2638) else Color(0xFF0F1420)
    val borderColor = if (isSelected) Color(0xFF3B4863) else HomeCardBorder
    val textColor = if (isSelected) Color.White else TextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Status Dot Indicator
            val dotColor = when (status) {
                StudyPresenceStatus.OFFLINE -> Color(0xFF64748B)
                StudyPresenceStatus.STUDYING -> Color(0xFF60A5FA)
                StudyPresenceStatus.IN_LECTURE -> Color(0xFFA78BFA)
                StudyPresenceStatus.OPEN_TO_COLLAB -> Color(0xFF34D399)
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = status.displayLabel,
                fontFamily = ElmsSans,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                fontSize = 12.sp,
                color = textColor
            )
        }
    }
}

/**
 * Group section header with count badge.
 */
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    isAccent: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = Color.White
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1E2638))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

/**
 * Single study buddy row card.
 */
@Composable
private fun FriendItemCard(
    friend: FriendProfile,
    onInvite: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar / Initials container with Presence status dot
            Box(
                modifier = Modifier.size(44.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2638)),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = friend.displayName
                        .split(" ")
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .uppercase()

                    Text(
                        text = if (initials.isNotEmpty()) initials else "SB",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                // Presence Dot
                val dotColor = when (friend.studyStatus) {
                    StudyPresenceStatus.OFFLINE -> Color(0xFF64748B)
                    StudyPresenceStatus.STUDYING -> Color(0xFF60A5FA)
                    StudyPresenceStatus.IN_LECTURE -> Color(0xFFA78BFA)
                    StudyPresenceStatus.OPEN_TO_COLLAB -> Color(0xFF34D399)
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .align(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name, Handle, and Status details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = friend.displayName,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (friend.isFavorite) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = "Favorite",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = friend.formattedHandle,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = TextSecondary
                )

                if (!friend.currentSubject.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Studying: ${friend.currentSubject}",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF94A3B8)
                    )
                } else if (friend.isActive) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = friend.studyStatus.displayLabel,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF60A5FA)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Quick Action: Invite to Study (Pill button)
                OutlinedButton(
                    onClick = onInvite,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFF28354D)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Invite",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }

                // Quick Action: Share Notes
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share Notes",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Favorite Toggle
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (friend.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (friend.isFavorite) Color(0xFFF59E0B) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete buddy",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Empty state when student has no study buddies yet.
 */
@Composable
private fun FriendsEmptyState(
    onAddFriendClick: () -> Unit,
    onShowQrClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E2638)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Group,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Study Buddies Yet",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Connect with classmates by academic @handle or scan their QR code to study together, share lecture notes, and collaborate.",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onAddFriendClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                border = BorderStroke(1.dp, Color(0xFF28354D)),
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = "Add Study Buddy",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            OutlinedButton(
                onClick = onShowQrClick,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF28354D)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = "My QR Code",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Empty state when search query matches no contacts.
 */
@Composable
private fun FriendsSearchEmptyState(
    query: String,
    onClearSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Matches Found",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No study buddies match \"$query\". Check spelling or invite them by @handle.",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onClearSearch) {
            Text(
                text = "Clear Search",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF60A5FA)
            )
        }
    }
}

/**
 * Add Friend Dialog modal.
 */
@Composable
private fun AddFriendDialog(
    onDismiss: () -> Unit,
    onConfirm: (handle: String, displayName: String) -> Unit
) {
    var handleInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Study Buddy",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter your peer's academic tag and full display name to connect.",
                    fontFamily = ElmsSans,
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = handleInput,
                    onValueChange = {
                        handleInput = it
                        errorMessage = null
                    },
                    label = { Text("Academic Handle (@username)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = HomeCardBorder,
                        focusedContainerColor = HomeNearBlack,
                        unfocusedContainerColor = HomeNearBlack,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        errorMessage = null
                    },
                    label = { Text("Display Name (e.g. Alex Miller)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = HomeCardBorder,
                        focusedContainerColor = HomeNearBlack,
                        unfocusedContainerColor = HomeNearBlack,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        fontFamily = ElmsSans,
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        },
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        confirmButton = {
            Button(
                onClick = {
                    val handle = handleInput.trim()
                    val name = nameInput.trim()
                    if (handle.isEmpty()) {
                        errorMessage = "Please enter an academic handle"
                        return@Button
                    }
                    if (name.isEmpty()) {
                        errorMessage = "Please enter a display name"
                        return@Button
                    }
                    onConfirm(handle, name)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Add Contact",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    fontFamily = ElmsSans,
                    color = TextSecondary
                )
            }
        }
    )
}

/**
 * Modal to specify the current subject when switching presence to Studying or Open to Collab.
 */
@Composable
private fun PresenceSubjectDialog(
    status: StudyPresenceStatus,
    initialSubject: String,
    onDismiss: () -> Unit,
    onConfirm: (subject: String?) -> Unit
) {
    var subjectInput by remember { mutableStateOf(initialSubject) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Update Subject Focus",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "What course or subject are you focusing on?",
                    fontFamily = ElmsSans,
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = subjectInput,
                    onValueChange = { subjectInput = it },
                    placeholder = { Text("e.g. Organic Chemistry, Calculus II") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = HomeCardBorder,
                        focusedContainerColor = HomeNearBlack,
                        unfocusedContainerColor = HomeNearBlack,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        confirmButton = {
            Button(
                onClick = { onConfirm(subjectInput.trim().ifEmpty { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Set Presence",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    fontFamily = ElmsSans,
                    color = TextSecondary
                )
            }
        }
    )
}
