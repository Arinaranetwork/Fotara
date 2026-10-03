// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.online

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.arinara.fotara.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class UpdateState {
    IDLE,
    CHECKING,
    NO_UPDATE,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    DOWNLOADED,
    ERROR
}

data class ReleaseInfo(
    val version: String,
    val title: String,
    val releaseNotes: String,
    val downloadUrl: String?,
    val bannerUrl: String? = null
)

class UpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow(UpdateState.IDLE)
    val updateState = _updateState.asStateFlow()

    private val _latestRelease = MutableStateFlow<ReleaseInfo?>(null)
    val latestRelease = _latestRelease.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0) // 0..100
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    var downloadedApkFile: File? = null
        private set

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val CHANNEL_ID = "fotara_updates"
    private val NOTIF_ID = 2001

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fotara App Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download progress for Fotara updates"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    var isDownloading: Boolean = false
        private set

    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice = _statusNotice.asStateFlow()

    fun clearStatusNotice() {
        _statusNotice.value = null
    }

    private var isCheckingInProgress = false

    suspend fun checkForUpdates(forceRefresh: Boolean = false): ReleaseInfo? = withContext(Dispatchers.IO) {
        if (isDownloading && downloadJob?.isActive == true) {
            return@withContext _latestRelease.value
        }
        if (isCheckingInProgress) {
            return@withContext _latestRelease.value
        }
        isCheckingInProgress = true
        _updateState.value = UpdateState.CHECKING
        _errorMessage.value = null
        if (forceRefresh) {
            _statusNotice.value = null
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://api.github.com/repos/Arinaranetwork/Fotara/releases")
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "Fotara-Android-App")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val releasesArray = JSONArray(jsonStr)
                if (releasesArray.length() == 0) {
                    _updateState.value = UpdateState.NO_UPDATE
                    _statusNotice.value = "No releases available in repository."
                    return@withContext null
                }

                // Scan all available releases to find the absolute newest release with an APK asset
                var newestRelease: ReleaseInfo? = null
                for (i in 0 until releasesArray.length()) {
                    val json = releasesArray.getJSONObject(i)
                    val tagName = json.optString("tag_name", "")
                    val title = json.optString("name", "Fotara Update")
                    val body = json.optString("body", "Bug fixes and performance improvements.")

                    var downloadUrl: String? = null
                    var bannerUrl: String? = null
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (j in 0 until assets.length()) {
                            val asset = assets.getJSONObject(j)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                downloadUrl = if (asset.has("browser_download_url")) asset.getString("browser_download_url") else null
                            } else if (name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".png", ignoreCase = true) || name.endsWith(".webp", ignoreCase = true)) {
                                bannerUrl = if (asset.has("browser_download_url")) asset.getString("browser_download_url") else null
                            }
                        }
                    }

                    if (downloadUrl != null) {
                        val candidate = ReleaseInfo(
                            version = tagName,
                            title = title,
                            releaseNotes = body,
                            downloadUrl = downloadUrl,
                            bannerUrl = bannerUrl
                        )
                        if (newestRelease == null || isNewerVersion(candidate.version, newestRelease.version)) {
                            newestRelease = candidate
                        }
                    }
                }

                if (newestRelease == null) {
                    _updateState.value = UpdateState.NO_UPDATE
                    _statusNotice.value = "No APK package found in available releases."
                    return@withContext null
                }

                // Check version comparison dynamically against installed app version
                val currentVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.5.4 Beta"
                } catch (_: Exception) {
                    "1.5.4 Beta"
                }

                val previousRelease = _latestRelease.value
                val isNewerThanApp = isNewerVersion(newestRelease.version, currentVersion)
                val isSkippedToNewer = previousRelease != null && isNewerVersion(newestRelease.version, previousRelease.version)

                if (isNewerThanApp) {
                    // If moving to a newer release than previously cached/displayed, clear old APK file
                    if (isSkippedToNewer) {
                        if (downloadedApkFile != null && downloadedApkFile!!.exists()) {
                            try { downloadedApkFile!!.delete() } catch (_: Exception) {}
                            downloadedApkFile = null
                        }
                        _statusNotice.value = "Newer release discovered (${newestRelease.version})! Switched to newest release."
                    } else if (previousRelease != null && previousRelease.version == newestRelease.version) {
                        _statusNotice.value = "You are already viewing the newest release available (${newestRelease.version})."
                    }

                    _latestRelease.value = newestRelease

                    // Check if current target APK is already downloaded locally or currently downloading
                    val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: context.externalCacheDir
                        ?: context.cacheDir
                    val expectedApk = File(targetDir, "Fotara_Update_${newestRelease.version.replace('/', '_')}.apk")
                    if (isDownloading && downloadJob?.isActive == true) {
                        // Keep DOWNLOADING state and live progress intact
                    } else if (expectedApk.exists() && expectedApk.length() > 0L) {
                        downloadedApkFile = expectedApk
                        _downloadProgress.value = 100
                        _updateState.value = UpdateState.DOWNLOADED
                    } else {
                        _updateState.value = UpdateState.UPDATE_AVAILABLE
                    }
                    newestRelease
                } else {
                    _updateState.value = UpdateState.NO_UPDATE
                    _statusNotice.value = "Your application is up to date (v$currentVersion)."
                    null
                }
            } else {
                _updateState.value = UpdateState.ERROR
                val err = "GitHub API responded with code ${connection.responseCode}"
                _errorMessage.value = err
                _statusNotice.value = err
                null
            }
        } catch (e: Exception) {
            _updateState.value = UpdateState.ERROR
            val err = e.message ?: "Unable to connect to update server"
            _errorMessage.value = err
            _statusNotice.value = err
            null
        } finally {
            isCheckingInProgress = false
            connection?.disconnect()
        }
    }

    fun isNewerVersion(remoteTag: String, currentTag: String): Boolean {
        val cleanRemote = remoteTag
            .replace("Fotara", "", ignoreCase = true)
            .trim('_', '-', ' ', 'v', 'V')
            .split("-", "_", " ")[0]
        val cleanCurrent = currentTag
            .replace("Fotara", "", ignoreCase = true)
            .trim('_', '-', ' ', 'v', 'V')
            .split("-", "_", " ")[0]

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun openConnectionWithRedirects(initialUrl: String, maxRedirects: Int = 5): HttpURLConnection {
        var currentUrl = initialUrl
        var redirects = 0
        while (redirects < maxRedirects) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Fotara-Android-App")
            }
            val status = conn.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER ||
                status == 307 || status == 308) {
                val location = conn.getHeaderField("Location") ?: break
                conn.disconnect()
                currentUrl = location
                redirects++
                continue
            }
            return conn
        }
        return (URL(currentUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 15000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Fotara-Android-App")
        }
    }

    fun startDownload(release: ReleaseInfo, onComplete: ((File) -> Unit)? = null) {
        if (isDownloading && downloadJob?.isActive == true) {
            return
        }
        val downloadUrl = release.downloadUrl ?: return
        isDownloading = true
        _updateState.value = UpdateState.DOWNLOADING
        _downloadProgress.value = 0
        _errorMessage.value = null

        val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.externalCacheDir
            ?: context.cacheDir
        val apkFile = File(targetDir, "Fotara_Update_${release.version.replace('/', '_')}.apk")
        val partFile = File(targetDir, "Fotara_Update_${release.version.replace('/', '_')}.apk.part")

        // If the complete final APK already exists and is non-empty, mark as DOWNLOADED immediately
        if (apkFile.exists() && apkFile.length() > 0L) {
            downloadedApkFile = apkFile
            _downloadProgress.value = 100
            _updateState.value = UpdateState.DOWNLOADED
            isDownloading = false
            onComplete?.invoke(apkFile)
            postCompletionNotification(release.title, apkFile)
            return
        }

        downloadJob = updateScope.launch {
            var connection: HttpURLConnection? = null
            try {
                connection = openConnectionWithRedirects(downloadUrl)
                val totalSize = connection.contentLength
                var downloaded = 0L

                connection.inputStream.use { input ->
                    FileOutputStream(partFile).use { output ->
                        val buffer = ByteArray(32768)
                        var read = input.read(buffer)
                        var lastNotifTime = 0L
                        var lastProgress = 0

                        while (read != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (totalSize > 0) {
                                val progress = ((downloaded * 100L) / totalSize).toInt().coerceIn(0, 100)
                                _downloadProgress.value = progress

                                val now = System.currentTimeMillis()
                                if (progress != lastProgress && (now - lastNotifTime >= 600L || progress == 100)) {
                                    lastNotifTime = now
                                    lastProgress = progress
                                    postProgressNotification(release.title, progress)
                                }
                            }
                            read = input.read(buffer)
                        }
                    }
                }

                if (partFile.exists() && partFile.length() > 0L) {
                    if (apkFile.exists()) {
                        apkFile.delete()
                    }
                    val renamed = partFile.renameTo(apkFile)
                    val targetApk = if (renamed) apkFile else partFile
                    downloadedApkFile = targetApk
                    _downloadProgress.value = 100
                    _updateState.value = UpdateState.DOWNLOADED
                    notificationManager.cancel(NOTIF_ID)
                    postCompletionNotification(release.title, targetApk)
                    onComplete?.invoke(targetApk)
                } else {
                    throw IllegalStateException("Downloaded file is empty")
                }
            } catch (ce: CancellationException) {
                // User cancelled explicitly
                notificationManager.cancel(NOTIF_ID)
                _updateState.value = UpdateState.UPDATE_AVAILABLE
            } catch (e: Exception) {
                notificationManager.cancel(NOTIF_ID)
                _updateState.value = UpdateState.ERROR
                _errorMessage.value = "Download failed: ${e.message}"
            } finally {
                isDownloading = false
                connection?.disconnect()
            }
        }
    }

    suspend fun downloadUpdate(release: ReleaseInfo, onComplete: (File) -> Unit) {
        startDownload(release, onComplete)
    }

    fun cancelDownload() {
        if (isDownloading) {
            downloadJob?.cancel()
            downloadJob = null
            isDownloading = false
            notificationManager.cancel(NOTIF_ID)
            _updateState.value = UpdateState.UPDATE_AVAILABLE
            _downloadProgress.value = 0
            val rel = _latestRelease.value
            if (rel != null) {
                val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.externalCacheDir
                    ?: context.cacheDir
                val partFile = File(targetDir, "Fotara_Update_${rel.version.replace('/', '_')}.apk.part")
                if (partFile.exists()) {
                    try { partFile.delete() } catch (_: Exception) {}
                }
            }
        }
    }

    private fun postProgressNotification(title: String, progress: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_UPDATE_SCREEN", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            104,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Downloading $title")
            .setContentText("$progress% completed")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIF_ID, notif)
    }

    private fun postCompletionNotification(title: String, apkFile: File) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_UPDATE_SCREEN", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            105,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Update Ready to Install")
            .setContentText("$title has finished downloading. Tap to install.")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIF_ID, notif)
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e("UpdateManager", "Failed to open install permission settings: ${e.message}")
            }
        }
    }

    fun triggerApkInstall(apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() <= 0L) {
            android.util.Log.e("UpdateManager", "APK file does not exist or is empty")
            return false
        }

        if (!canRequestPackageInstalls()) {
            openInstallPermissionSettings()
            return false
        }

        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }
            val resInfoList = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            android.util.Log.e("UpdateManager", "Failed to launch installer intent: ${e.message}", e)
            return false
        }
    }
}
