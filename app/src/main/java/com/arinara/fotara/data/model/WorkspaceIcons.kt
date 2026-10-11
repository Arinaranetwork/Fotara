// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

import androidx.annotation.DrawableRes
import com.arinara.fotara.R

data class WorkspaceIconItem(
    val key: String,
    @get:DrawableRes val resId: Int,
    val label: String
)

object WorkspaceIcons {
    const val DEFAULT_KEY = "folder"

    val ALL_ICONS: List<WorkspaceIconItem> = listOf(
        WorkspaceIconItem("folder", R.drawable.ic_ws_folder, "Folder"),
        WorkspaceIconItem("book", R.drawable.ic_ws_book, "Book"),
        WorkspaceIconItem("school", R.drawable.ic_ws_school, "School"),
        WorkspaceIconItem("science", R.drawable.ic_ws_science, "Science"),
        WorkspaceIconItem("calculate", R.drawable.ic_ws_calculate, "Math"),
        WorkspaceIconItem("language", R.drawable.ic_ws_language, "Language"),
        WorkspaceIconItem("translate", R.drawable.ic_ws_translate, "Translate"),
        WorkspaceIconItem("history", R.drawable.ic_ws_history, "History"),
        WorkspaceIconItem("palette", R.drawable.ic_ws_palette, "Art"),
        WorkspaceIconItem("music", R.drawable.ic_ws_music, "Music"),
        WorkspaceIconItem("code", R.drawable.ic_ws_code, "Code"),
        WorkspaceIconItem("computer", R.drawable.ic_ws_computer, "Computer"),
        WorkspaceIconItem("work", R.drawable.ic_ws_work, "Work"),
        WorkspaceIconItem("home", R.drawable.ic_ws_home, "Home"),
        WorkspaceIconItem("star", R.drawable.ic_ws_star, "Star"),
        WorkspaceIconItem("favorite", R.drawable.ic_ws_favorite, "Favorite"),
        WorkspaceIconItem("lightbulb", R.drawable.ic_ws_lightbulb, "Idea"),
        WorkspaceIconItem("camera", R.drawable.ic_ws_camera, "Camera"),
        WorkspaceIconItem("sports", R.drawable.ic_ws_sports, "Sports"),
        WorkspaceIconItem("fitness", R.drawable.ic_ws_fitness, "Fitness"),
        WorkspaceIconItem("eco", R.drawable.ic_ws_eco, "Eco"),
        WorkspaceIconItem("restaurant", R.drawable.ic_ws_restaurant, "Food"),
        WorkspaceIconItem("flight", R.drawable.ic_ws_flight, "Travel"),
        WorkspaceIconItem("pets", R.drawable.ic_ws_pets, "Pets")
    )

    private val KEY_MAP: Map<String, Int> = ALL_ICONS.associate { it.key to it.resId }

    @DrawableRes
    fun getIconResId(key: String?): Int {
        if (key == null) return R.drawable.ic_ws_folder
        return KEY_MAP[key.lowercase().trim()] ?: R.drawable.ic_ws_folder
    }

    fun isValidKey(key: String?): Boolean {
        if (key == null) return false
        return KEY_MAP.containsKey(key.lowercase().trim())
    }
}
