// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.friends.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * Navigation tabs for the Friends Hub.
 */
enum class FriendsTab(val label: String) {
    FRIENDS("Friends"),
    FOLLOWERS("Followers"),
    FOLLOWING("Following")
}

/**
 * Dedicated Friends System Screen for academic collaboration and peer networking.
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

    var selectedTab by remember { mutableStateOf(FriendsTab.FRIENDS) }
    var showHeaderMenu by remember { mutableStateOf(false) }

    var showQrDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showClaimUsernameDialog by remember { mutableStateOf(false) }
    var showPresenceSubjectDialog by remember { mutableStateOf(false) }
    var pendingStatusForSubject by remember { mutableStateOf<StudyPresenceStatus?>(null) }
    var newSubjectInput by remember { mutableStateOf("") }

    var friendToDelete by remember { mutableStateOf<FriendProfile?>(null) }
    var profileForDialog by remember { mutableStateOf<FriendProfile?>(null) }
    var isOwnProfileDialog by remember { mutableStateOf(false) }

    val ownProfile = remember(
        uiState.claimedUsername,
        uiState.myPresenceStatus,
        uiState.mySubject,
        uiState.followersCount,
        uiState.followingCount,
        uiState.totalFriendsCount
    ) {
        FriendProfile(
            id = "self_user",
            handle = uiState.claimedUsername,
            displayName = "My Profile",
            studyStatus = uiState.myPresenceStatus,
            currentSubject = uiState.mySubject,
            followersCount = uiState.followersCount,
            followingCount = uiState.followingCount
        )
    }

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
            // Standard Sub-Screen Header with back navigation and action tiles
            SettingsSubScreenHeader(
                title = "Friends",
                subtitle = "Academic peer network & presence",
                onBackClick = {
                    if (selectedTab != FriendsTab.FRIENDS) {
                        selectedTab = FriendsTab.FRIENDS
                    } else {
                        onBackClick()
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp),
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // QR Code Button
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

                        // Add Friend Button
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

                        // 3-Dots More Options Menu
                        Box {
                            IconButton(
                                onClick = { showHeaderMenu = true },
                                modifier = Modifier
                                    .size(SettingsSubScreenHeaderDefaults.ButtonSize)
                                    .clip(RoundedCornerShape(SettingsSubScreenHeaderDefaults.ButtonCornerRadius))
                                    .background(SettingsSubScreenHeaderDefaults.ButtonBackgroundColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More Options",
                                    tint = SettingsSubScreenHeaderDefaults.ButtonIconTint,
                                    modifier = Modifier.size(SettingsSubScreenHeaderDefaults.IconSize)
                                )
                            }

                            DropdownMenu(
                                expanded = showHeaderMenu,
                                onDismissRequest = { showHeaderMenu = false },
                                modifier = Modifier
                                    .background(HomeCardSurface)
                                    .border(BorderStroke(1.dp, HomeCardBorder), RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Followers",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Group,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showHeaderMenu = false
                                        selectedTab = FriendsTab.FOLLOWERS
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Following",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.PersonAdd,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showHeaderMenu = false
                                        selectedTab = FriendsTab.FOLLOWING
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "My Profile",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showHeaderMenu = false
                                        profileForDialog = ownProfile
                                        isOwnProfileDialog = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Claim Username",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Badge,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showHeaderMenu = false
                                        showClaimUsernameDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )

            // 1. Search Bar
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
                onProfileClick = {
                    profileForDialog = ownProfile
                    isOwnProfileDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // 3. Tab Bar Switcher (Friends / Followers / Following)
            FriendsTabBar(
                selectedTab = selectedTab,
                friendsCount = uiState.totalFriendsCount,
                followersCount = uiState.followersCount,
                followingCount = uiState.followingCount,
                onSelectTab = { selectedTab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 4. Content Area Based on Active Tab
            when (selectedTab) {
                FriendsTab.FRIENDS -> {
                    if (uiState.filteredFriends.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 72.dp),
                            contentAlignment = Alignment.Center
                        ) {
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
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Active Friends Group
                            if (uiState.activeFriends.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Active Friends",
                                        count = uiState.activeFriends.size,
                                        isAccent = true
                                    )
                                }
                                items(uiState.activeFriends, key = { it.id }) { friend ->
                                    FriendItemCard(
                                        friend = friend,
                                        onClick = {
                                            profileForDialog = friend
                                            isOwnProfileDialog = false
                                        },
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

                            // Offline Friends Group
                            if (uiState.offlineFriends.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Offline Friends",
                                        count = uiState.offlineFriends.size,
                                        isAccent = false
                                    )
                                }
                                items(uiState.offlineFriends, key = { it.id }) { friend ->
                                    FriendItemCard(
                                        friend = friend,
                                        onClick = {
                                            profileForDialog = friend
                                            isOwnProfileDialog = false
                                        },
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

                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }

                FriendsTab.FOLLOWERS -> {
                    if (uiState.followers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FollowersEmptyState()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                SectionHeader(
                                    title = "Followers",
                                    count = uiState.followers.size,
                                    isAccent = true
                                )
                            }
                            items(uiState.followers, key = { it.id }) { follower ->
                                val isFollowing = uiState.following.any { it.id == follower.id }
                                FollowerItemCard(
                                    follower = follower,
                                    isFollowing = isFollowing,
                                    onClick = {
                                        profileForDialog = follower
                                        isOwnProfileDialog = false
                                    },
                                    onFollowBack = { viewModel.followUser(follower) },
                                    onRemove = { viewModel.removeFollower(follower.id) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }

                FriendsTab.FOLLOWING -> {
                    if (uiState.following.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FollowingEmptyState(
                                onFindFriendsClick = { selectedTab = FriendsTab.FRIENDS }
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                SectionHeader(
                                    title = "Following",
                                    count = uiState.following.size,
                                    isAccent = true
                                )
                            }
                            items(uiState.following, key = { it.id }) { peer ->
                                FollowingItemCard(
                                    peer = peer,
                                    onClick = {
                                        profileForDialog = peer
                                        isOwnProfileDialog = false
                                    },
                                    onUnfollow = { viewModel.unfollowUser(peer.id) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
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

    // Dialog: Claim Username
    if (showClaimUsernameDialog) {
        ClaimUsernameDialog(
            currentUsername = uiState.claimedUsername,
            canChangeUsername = uiState.canChangeUsername,
            cooldownDaysRemaining = uiState.cooldownDaysRemaining,
            cooldownMessage = uiState.cooldownMessage,
            onDismiss = { showClaimUsernameDialog = false },
            onClaim = { newUsername, displayName ->
                viewModel.claimUsername(newUsername, displayName) { success, _ ->
                    if (success) {
                        showClaimUsernameDialog = false
                    }
                }
            }
        )
    }

    // Dialog: User Profile Detail Modal
    if (profileForDialog != null) {
        val targetProfile = profileForDialog!!
        val isSelf = isOwnProfileDialog
        val isCurrentlyFollowing = uiState.following.any { it.id == targetProfile.id }

        UserProfileDialog(
            profile = targetProfile,
            isSelf = isSelf,
            followersCount = if (isSelf) uiState.followersCount else targetProfile.followersCount,
            followingCount = if (isSelf) uiState.followingCount else targetProfile.followingCount,
            friendsCount = if (isSelf) uiState.totalFriendsCount else 0,
            canChangeUsername = uiState.canChangeUsername,
            cooldownDaysRemaining = uiState.cooldownDaysRemaining,
            isFollowing = isCurrentlyFollowing,
            onDismiss = { profileForDialog = null },
            onChangeUsernameClick = {
                profileForDialog = null
                showClaimUsernameDialog = true
            },
            onShareQrClick = {
                profileForDialog = null
                showQrDialog = true
            },
            onInviteToStudy = {
                profileForDialog = null
                if (onInviteToStudy != null) {
                    onInviteToStudy(targetProfile)
                } else {
                    Toast.makeText(context, "Study invitation sent to ${targetProfile.displayName}", Toast.LENGTH_SHORT).show()
                }
            },
            onShareNotes = {
                profileForDialog = null
                if (onShareNotes != null) {
                    onShareNotes(targetProfile)
                } else {
                    Toast.makeText(context, "Sharing notes with ${targetProfile.displayName}", Toast.LENGTH_SHORT).show()
                }
            },
            onToggleFollow = {
                if (isCurrentlyFollowing) {
                    viewModel.unfollowUser(targetProfile.id)
                } else {
                    viewModel.followUser(targetProfile)
                }
            },
            onRemoveFriend = {
                profileForDialog = null
                friendToDelete = targetProfile
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
                    text = "Remove Friend",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove ${friendToDelete?.displayName} (${friendToDelete?.formattedHandle}) from your friends list?",
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
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(48.dp)
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
                TextButton(
                    onClick = { friendToDelete = null },
                    modifier = Modifier.height(48.dp)
                ) {
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
 * Tab switcher row for Friends Hub.
 */
@Composable
private fun FriendsTabBar(
    selectedTab: FriendsTab,
    friendsCount: Int,
    followersCount: Int,
    followingCount: Int,
    onSelectTab: (FriendsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F1420))
            .border(BorderStroke(1.dp, HomeCardBorder), RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FriendsTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            val count = when (tab) {
                FriendsTab.FRIENDS -> friendsCount
                FriendsTab.FOLLOWERS -> followersCount
                FriendsTab.FOLLOWING -> followingCount
            }
            val tabBg = if (isSelected) Color(0xFF1E2638) else Color.Transparent
            val tabText = if (isSelected) Color.White else TextSecondary
            val tabBorder = if (isSelected) BorderStroke(1.dp, Color(0xFF3B4863)) else null

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tabBg)
                    .then(if (tabBorder != null) Modifier.border(tabBorder, RoundedCornerShape(8.dp)) else Modifier)
                    .clickable { onSelectTab(tab) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tab.label,
                        fontFamily = ElmsSans,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = tabText
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) Color(0xFF28354D) else Color(0xFF151C2C))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            fontFamily = ElmsSans,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White else TextMuted
                        )
                    }
                }
            }
        }
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
                        text = "Search friends or @username...",
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
    onProfileClick: () -> Unit,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onProfileClick)
                    .padding(vertical = 2.dp),
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
 * Single Friend row card.
 */
@Composable
private fun FriendItemCard(
    friend: FriendProfile,
    onClick: () -> Unit,
    onInvite: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                        text = if (initials.isNotEmpty()) initials else "FR",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

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
                OutlinedButton(
                    onClick = onInvite,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFF28354D)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = "Invite",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share Notes",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (friend.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (friend.isFavorite) Color(0xFFF59E0B) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Remove friend",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Follower list row card.
 */
@Composable
private fun FollowerItemCard(
    follower: FriendProfile,
    isFollowing: Boolean,
    onClick: () -> Unit,
    onFollowBack: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E2638)),
                contentAlignment = Alignment.Center
            ) {
                val initials = follower.displayName
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .uppercase()

                Text(
                    text = if (initials.isNotEmpty()) initials else "FL",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = follower.displayName,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = follower.formattedHandle,
                    fontFamily = ElmsSans,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isFollowing) {
                    Button(
                        onClick = onFollowBack,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = "Follow Back",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonRemove,
                        contentDescription = "Remove follower",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Following list row card.
 */
@Composable
private fun FollowingItemCard(
    peer: FriendProfile,
    onClick: () -> Unit,
    onUnfollow: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E2638)),
                contentAlignment = Alignment.Center
            ) {
                val initials = peer.displayName
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .uppercase()

                Text(
                    text = if (initials.isNotEmpty()) initials else "PG",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.displayName,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = peer.formattedHandle,
                    fontFamily = ElmsSans,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onUnfollow,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF28354D)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Unfollow",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Empty state when student has no friends yet.
 * Centers properly within the viewport.
 */
@Composable
private fun FriendsEmptyState(
    onAddFriendClick: () -> Unit,
    onShowQrClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E2638)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Group,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "No Friends Yet",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Connect with classmates by academic @username or scan their QR code to study together, share lecture notes, and collaborate.",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onAddFriendClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                border = BorderStroke(1.dp, Color(0xFF28354D)),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Add Friend",
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
                modifier = Modifier.height(48.dp)
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
 * Empty state for Followers list.
 */
@Composable
private fun FollowersEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E2638)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Group,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "No Followers Yet",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Peers who follow your academic profile will appear here.",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Empty state for Following list.
 */
@Composable
private fun FollowingEmptyState(
    onFindFriendsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E2638)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PersonAdd,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Not Following Anyone Yet",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Follow classmates to stay updated on their study sessions and academic progress.",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onFindFriendsClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
            border = BorderStroke(1.dp, Color(0xFF28354D)),
            modifier = Modifier.height(48.dp)
        ) {
            Text(
                text = "Find Friends",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color.White
            )
        }
    }
}

/**
 * Empty state when search query matches no friends.
 */
@Composable
private fun FriendsSearchEmptyState(
    query: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
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
            text = "No friends match \"$query\". Check spelling or invite them by @username.",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = onClearSearch,
            modifier = Modifier.height(48.dp)
        ) {
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
 * User Profile Detail modal dialog.
 */
@Composable
private fun UserProfileDialog(
    profile: FriendProfile,
    isSelf: Boolean,
    followersCount: Int,
    followingCount: Int,
    friendsCount: Int,
    canChangeUsername: Boolean,
    cooldownDaysRemaining: Int,
    isFollowing: Boolean,
    onDismiss: () -> Unit,
    onChangeUsernameClick: () -> Unit,
    onShareQrClick: () -> Unit,
    onInviteToStudy: () -> Unit,
    onShareNotes: () -> Unit,
    onToggleFollow: () -> Unit,
    onRemoveFriend: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Academic Profile Banner with Avatar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                ) {
                    // Profile Banner Background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF162032),
                                        Color(0xFF0F172A),
                                        Color(0xFF1E3A5F)
                                    )
                                )
                            )
                            .border(BorderStroke(1.dp, Color(0xFF283548)), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "ACADEMIC PROFILE",
                            fontFamily = ElmsSans,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        )
                    }

                    // Avatar Tile positioned over the banner
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(64.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF1E2638))
                                .border(BorderStroke(2.dp, HomeCardSurface), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val initials = profile.displayName
                                .split(" ")
                                .mapNotNull { it.firstOrNull()?.toString() }
                                .take(2)
                                .joinToString("")
                                .uppercase()

                            Text(
                                text = if (initials.isNotEmpty()) initials else if (isSelf) "ME" else "FR",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        }

                        val dotColor = when (profile.studyStatus) {
                            StudyPresenceStatus.OFFLINE -> Color(0xFF64748B)
                            StudyPresenceStatus.STUDYING -> Color(0xFF60A5FA)
                            StudyPresenceStatus.IN_LECTURE -> Color(0xFFA78BFA)
                            StudyPresenceStatus.OPEN_TO_COLLAB -> Color(0xFF34D399)
                        }
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .border(BorderStroke(1.5.dp, HomeCardSurface), CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Display Name
                Text(
                    text = profile.displayName,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Handle row with copy handle button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = profile.formattedHandle,
                        fontFamily = ElmsSans,
                        fontSize = 14.sp,
                        color = FolderTabCream
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Academic Handle", profile.formattedHandle))
                            Toast.makeText(context, "Handle copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy handle",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Presence Status & Subject Focus
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF131B2E))
                        .border(BorderStroke(1.dp, Color(0xFF22314E)), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    val statusText = if (!profile.currentSubject.isNullOrBlank() && profile.studyStatus != StudyPresenceStatus.OFFLINE) {
                        "${profile.studyStatus.displayLabel} • Focus: ${profile.currentSubject}"
                    } else {
                        profile.studyStatus.displayLabel
                    }
                    Text(
                        text = statusText,
                        fontFamily = ElmsSans,
                        fontSize = 12.sp,
                        color = Color(0xFF60A5FA)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stat Counters Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCounterCard(
                        count = followersCount,
                        label = "Followers",
                        modifier = Modifier.weight(1f)
                    )
                    StatCounterCard(
                        count = followingCount,
                        label = "Following",
                        modifier = Modifier.weight(1f)
                    )
                    StatCounterCard(
                        count = friendsCount,
                        label = "Friends",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                if (isSelf) {
                    // Own profile actions
                    Button(
                        onClick = onChangeUsernameClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Change Username",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            val cooldownStatusText = if (canChangeUsername) {
                                "Available to change"
                            } else {
                                "Cooldown active: $cooldownDaysRemaining days remaining"
                            }
                            Text(
                                text = cooldownStatusText,
                                fontFamily = ElmsSans,
                                fontSize = 10.sp,
                                color = if (canChangeUsername) Color(0xFF34D399) else Color(0xFFF59E0B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onShareQrClick,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF28354D)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCode,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share QR Code",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    // Peer profile actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onInviteToStudy,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Invite to Study",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = onShareNotes,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF28354D)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Share Notes",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onToggleFollow,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF28354D)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isFollowing) TextSecondary else Color(0xFF60A5FA)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = if (isFollowing) "Unfollow" else "Follow",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = onRemoveFriend,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1818)),
                            border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Remove Friend",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = Color(0xFFF87171)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Close",
                    fontFamily = ElmsSans,
                    color = TextSecondary
                )
            }
        }
    )
}

@Composable
private fun StatCounterCard(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF161D2C))
            .border(BorderStroke(1.dp, HomeCardBorder), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontFamily = ElmsSans,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

/**
 * Claim or Change Username dialog modal.
 */
@Composable
private fun ClaimUsernameDialog(
    currentUsername: String,
    canChangeUsername: Boolean,
    cooldownDaysRemaining: Int,
    cooldownMessage: String?,
    onDismiss: () -> Unit,
    onClaim: (newUsername: String, displayName: String) -> Unit
) {
    var rawInput by remember { mutableStateOf("") }
    var displayNameInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Claim Username",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Current username: $currentUsername",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = FolderTabCream
                )

                // Cooldown notice
                if (!canChangeUsername) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A1C16))
                            .border(BorderStroke(1.dp, Color(0xFF78350F)), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = cooldownMessage ?: "Username can only be changed once every 7 days. Cooldown active for $cooldownDaysRemaining days.",
                            fontFamily = ElmsSans,
                            fontSize = 12.sp,
                            color = Color(0xFFFBBF24)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF102A1E))
                            .border(BorderStroke(1.dp, Color(0xFF065F46)), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Available to change. A 7-day cooldown applies after claiming.",
                            fontFamily = ElmsSans,
                            fontSize = 12.sp,
                            color = Color(0xFF34D399)
                        )
                    }
                }

                OutlinedTextField(
                    value = rawInput,
                    onValueChange = {
                        rawInput = it.removePrefix("@")
                        errorMessage = null
                    },
                    label = { Text("New Username") },
                    prefix = { Text("@") },
                    placeholder = { Text("username") },
                    singleLine = true,
                    enabled = canChangeUsername,
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
                    value = displayNameInput,
                    onValueChange = {
                        displayNameInput = it
                        errorMessage = null
                    },
                    label = { Text("Display Name") },
                    placeholder = { Text("Your full name") },
                    singleLine = true,
                    enabled = canChangeUsername,
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
                    val clean = rawInput.trim().removePrefix("@")
                    if (clean.isEmpty()) {
                        errorMessage = "Please enter a username"
                        return@Button
                    }
                    if (clean.length < 3) {
                        errorMessage = "Username must be at least 3 characters"
                        return@Button
                    }
                    val fullUsername = "@$clean"
                    val finalDisplayName = displayNameInput.trim().ifEmpty { clean }
                    onClaim(fullUsername, finalDisplayName)
                },
                enabled = canChangeUsername,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Claim Username",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
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
                text = "Add Friend",
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
                    text = "Enter your classmate's academic username and full display name to connect.",
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
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(48.dp)
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
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
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
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(48.dp)
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
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Cancel",
                    fontFamily = ElmsSans,
                    color = TextSecondary
                )
            }
        }
    )
}
