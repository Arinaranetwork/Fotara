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
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.arinara.fotara.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Universal Android Version Downgrade Engine.
 * Overcomes Android OS's native `INSTALL_FAILED_VERSION_DOWNGRADE` through dual pathways:
 * 1. Privileged in-place downgrade via Shizuku shell service (`pm install -d -r`).
 * 2. Zero-data-loss Guided Reinstall pipeline (FragileUserData, auto-vault backup, public Downloads staging,
 *    sticky post-uninstall notification, and uninstaller trigger).
 */
class DowngradeManager(private val context: Context) {

    companion object {
        const val ROLLBACK_NOTIF_CHANNEL_ID = "fotara_rollback_v1"
        const val ROLLBACK_NOTIF_ID = 90210
        const val SHIZUKU_REQUEST_CODE = 4410
    }

    init {
        createNotificationChannel()
    }

    /**
     * Checks if the Shizuku Binder service is currently active and reachable on the device.
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Checks if Shizuku shell permission has been granted to Fotara.
     */
    fun hasShizukuPermission(): Boolean {
        return try {
            if (!isShizukuAvailable()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Requests Shizuku permission.
     */
    fun requestShizukuPermission(requestCode: Int = SHIZUKU_REQUEST_CODE) {
        try {
            if (isShizukuAvailable() && !hasShizukuPermission()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Throwable) {
            android.util.Log.e("DowngradeManager", "Failed to request Shizuku permission", e)
        }
    }

    /**
     * Executes in-place downgrade using Shizuku shell service (`pm install -d -r <apkPath>`).
     * Returns Result.success with stdout on success, or Result.failure with error message.
     */
    suspend fun executeShizukuDowngrade(
        apkFile: File
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isShizukuAvailable()) {
            return@withContext Result.failure(IllegalStateException("Shizuku service is not active"))
        }
        if (!hasShizukuPermission()) {
            return@withContext Result.failure(SecurityException("Shizuku permission not granted"))
        }
        if (!apkFile.exists() || apkFile.length() <= 0L) {
            return@withContext Result.failure(IllegalArgumentException("Target APK does not exist or is empty"))
        }

        try {
            // Stage APK into public Downloads so shell UID (2000) can read it cleanly
            val stagedApk = stageApkToPublicDownloads(apkFile, "target_rollback")
            val command = arrayOf("pm", "install", "-d", "-r", stagedApk.absolutePath)

            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }

            val process = newProcessMethod.invoke(null, command, null, null) as java.lang.Process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (errorReader.readLine().also { line = it } != null) {
                output.append("ERROR: ").append(line).append("\n")
            }

            val exitCode = process.waitFor()
            val resultText = output.toString().trim()

            if (exitCode == 0 && (resultText.contains("Success", ignoreCase = true) || !resultText.contains("Failure", ignoreCase = true))) {
                Result.success(resultText.ifEmpty { "Downgrade successful!" })
            } else {
                Result.failure(RuntimeException("Shell install returned code $exitCode: $resultText"))
            }
        } catch (e: Throwable) {
            android.util.Log.e("DowngradeManager", "Error executing Shizuku downgrade", e)
            Result.failure(e)
        }
    }

    /**
     * Automatically exports a complete notes, folders, and preferences backup JSON into
     * the public external Downloads directory so it is preserved even if the application is uninstalled.
     */
    suspend fun createPreRollbackVaultBackup(
        settingsRepository: SettingsRepository?,
        targetVersion: String
    ): File? = withContext(Dispatchers.IO) {
        if (settingsRepository == null) return@withContext null
        try {
            val backupJson = settingsRepository.exportDataBackup()
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(downloadsDir, "Fotara_Rollback_Vault_${targetVersion}_$timestamp.json")

            FileOutputStream(backupFile).use { fos ->
                fos.write(backupJson.toByteArray(Charsets.UTF_8))
            }
            backupFile
        } catch (e: Throwable) {
            android.util.Log.e("DowngradeManager", "Failed to create pre-rollback vault backup", e)
            null
        }
    }

    /**
     * Copies the downloaded rollback APK into the public Downloads directory so it survives
     * app uninstallation and remains accessible to the system package installer.
     */
    suspend fun stageApkToPublicDownloads(
        apkFile: File,
        targetVersion: String
    ): File = withContext(Dispatchers.IO) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val sanitizedVersion = targetVersion.replace(" ", "_").replace("/", "_")
        val destFile = File(downloadsDir, "Fotara_${sanitizedVersion}.apk")

        FileInputStream(apkFile).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile
    }

    /**
     * Dispatches a sticky, high-priority notification to the notification drawer.
     * Tapping this notification launches the package installer for the staged rollback APK.
     * This notification stays in the notification drawer even after the current app is uninstalled.
     */
    fun postPostUninstallNotification(
        stagedApk: File,
        targetVersion: String
    ) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                stagedApk
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, ROLLBACK_NOTIF_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Fotara Rollback Ready: $targetVersion")
                .setContentText("Tap to install Fotara $targetVersion after uninstalling.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Target release $targetVersion is staged in Downloads. After uninstalling the current version, tap here to install.")
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(ROLLBACK_NOTIF_ID, notification)
        } catch (e: Throwable) {
            android.util.Log.e("DowngradeManager", "Failed to post post-uninstall notification", e)
        }
    }

    /**
     * Launches the system package uninstallation prompt.
     * In Android 10+, because `android:hasFragileUserData="true"` is enabled, the system dialog
     * includes a checkbox asking the user: "Keep app data?".
     */
    fun launchUninstallIntent() {
        try {
            val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(uninstallIntent)
        } catch (e: Throwable) {
            android.util.Log.e("DowngradeManager", "Failed to launch uninstall intent", e)
        }
    }

    /**
     * Generates the command-line instruction for ADB wireless or USB cable downgrade.
     */
    fun getAdbCommand(apkFileName: String): String {
        return "adb install -d -r \"$apkFileName\""
    }

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    ROLLBACK_NOTIF_CHANNEL_ID,
                    "Fotara Version Rollback",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications to assist with version rollback and downgrade"
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            }
        } catch (_: Throwable) {
            // Handled gracefully in testing environments
        }
    }
}
