// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.packages.loader

import android.content.Context
import com.arinara.fotara.feature.packages.model.FpkgManifest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream

/**
 * Core runtime lifecycle manager for Fotara Modular Packages (.fpkg).
 * Governs package installation, integrity verification, uninstallation,
 * enablement toggling, and storage footprint accounting.
 * Maintains the lean <25MB Base APK invariant by loading specialized academic add-ons on demand.
 */
class FotaraPackageManager(
    val packagesDir: File
) {

    /**
     * Secondary constructor resolving default application storage path: `context.filesDir/packages/`.
     */
    constructor(context: Context) : this(File(context.filesDir, "packages"))

    private val mutex = Mutex()
    private val reclaimedStorageFile = File(packagesDir, ".storage_reclaimed")

    private val _installedPackages = MutableStateFlow<List<FpkgManifest>>(emptyList())
    val installedPackages: StateFlow<List<FpkgManifest>> = _installedPackages.asStateFlow()

    private val _availablePackages = MutableStateFlow<List<FpkgManifest>>(emptyList())
    val availablePackages: StateFlow<List<FpkgManifest>> = _availablePackages.asStateFlow()

    private val _reclaimedStorageBytes = MutableStateFlow(0L)
    val reclaimedStorageBytes: StateFlow<Long> = _reclaimedStorageBytes.asStateFlow()

    init {
        if (!packagesDir.exists()) {
            packagesDir.mkdirs()
        }
        loadReclaimedStorage()
        reloadPackagesInternal()
    }

    private fun loadReclaimedStorage() {
        if (reclaimedStorageFile.exists()) {
            val bytes = reclaimedStorageFile.readText().trim().toLongOrNull() ?: 0L
            _reclaimedStorageBytes.value = bytes
        }
    }

    private fun saveReclaimedStorage(bytes: Long) {
        _reclaimedStorageBytes.value = bytes
        try {
            reclaimedStorageFile.writeText(bytes.toString())
        } catch (_: Exception) {
            // Non-critical persistence failure handled gracefully
        }
    }

    private fun reloadPackagesInternal() {
        val installedList = mutableListOf<FpkgManifest>()
        val packageFolders = packagesDir.listFiles { file -> file.isDirectory } ?: emptyArray()

        for (folder in packageFolders) {
            val manifestFile = File(folder, MANIFEST_FILENAME)
            if (manifestFile.exists()) {
                val manifestText = manifestFile.readText()
                val manifest = FpkgManifest.fromJson(manifestText)
                if (manifest != null && manifest.isInstalled) {
                    installedList.add(manifest)
                }
            }
        }

        val installedMap = installedList.associateBy { it.packageId }
        val catalog = testCatalog ?: OFFICIAL_PACKAGES
        val availableList = catalog.filter { official ->
            !installedMap.containsKey(official.packageId)
        }

        _installedPackages.value = installedList
        _availablePackages.value = availableList
    }

    /**
     * Returns an immutable list of currently installed modular packages.
     */
    fun getInstalledPackages(): List<FpkgManifest> {
        return _installedPackages.value
    }

    /**
     * Returns an immutable list of available official add-ons ready for on-demand installation.
     */
    fun getAvailablePackages(): List<FpkgManifest> {
        return _availablePackages.value
    }

    /**
     * Determines whether the specified [packageId] is currently installed on disk.
     */
    fun isPackageInstalled(packageId: String): Boolean {
        return _installedPackages.value.any { it.packageId == packageId && it.isInstalled }
    }

    /**
     * Installs a package with SHA-256 integrity check and Arinara Network signature validation.
     * If [payload] is null, resolves the official authenticated payload corresponding to [packageId].
     */
    suspend fun installPackage(
        packageId: String,
        payload: ByteArray? = null
    ): Result<FpkgManifest> = mutex.withLock {
        installPackageInternal(packageId, payload)
    }

    /**
     * Synchronous variant of package installation.
     */
    fun installPackageSync(
        packageId: String,
        payload: ByteArray? = null
    ): Result<FpkgManifest> {
        val packageDir = File(packagesDir, packageId)
        val manifest = (testCatalog ?: OFFICIAL_PACKAGES).find { it.packageId == packageId }
            ?: return Result.failure(IllegalArgumentException("Unknown package: $packageId"))

        val rawPayload = payload ?: getOfficialMockPayload(packageId)

        // 1. Cryptographic Verification
        val verifyResult = FpkgSignatureVerifier.verify(manifest, rawPayload)
        if (verifyResult !is VerificationResult.Success) {
            return Result.failure(
                SecurityException("Package signature verification failed for $packageId: $verifyResult")
            )
        }

        // 2. Prepare Directory
        if (!packageDir.exists()) {
            packageDir.mkdirs()
        }

        val payloadFile = File(packageDir, PAYLOAD_FILENAME)
        val manifestFile = File(packageDir, MANIFEST_FILENAME)

        // 3. Write Atomic Artifacts
        try {
            FileOutputStream(payloadFile).use { it.write(rawPayload) }
            val installedManifest = manifest.copy(isInstalled = true, isEnabled = true)
            manifestFile.writeText(installedManifest.toJson())

            reloadPackagesInternal()
            return Result.success(installedManifest)
        } catch (e: Exception) {
            // Clean up partial artifacts
            if (packageDir.exists()) {
                packageDir.deleteRecursively()
            }
            return Result.failure(e)
        }
    }

    private fun installPackageInternal(
        packageId: String,
        payload: ByteArray? = null
    ): Result<FpkgManifest> {
        return installPackageSync(packageId, payload)
    }

    /**
     * Uninstalls the specified package, purges local filesystem assets, updates available catalog,
     * and accounts for reclaimed storage.
     */
    suspend fun uninstallPackage(packageId: String): Result<Long> = mutex.withLock {
        uninstallPackageInternal(packageId)
    }

    /**
     * Synchronous variant of package uninstallation.
     */
    fun uninstallPackageSync(packageId: String): Result<Long> {
        val installed = _installedPackages.value.find { it.packageId == packageId }
            ?: return Result.failure(IllegalStateException("Package is not installed: $packageId"))

        val packageDir = File(packagesDir, packageId)
        val reclaimedBytes = installed.sizeBytes

        if (packageDir.exists()) {
            packageDir.deleteRecursively()
        }

        val updatedReclaimed = _reclaimedStorageBytes.value + reclaimedBytes
        saveReclaimedStorage(updatedReclaimed)

        reloadPackagesInternal()
        return Result.success(reclaimedBytes)
    }

    private fun uninstallPackageInternal(packageId: String): Result<Long> {
        return uninstallPackageSync(packageId)
    }

    /**
     * Toggles the active/enabled state of an installed package.
     */
    fun togglePackage(packageId: String, isEnabled: Boolean): Boolean {
        val target = _installedPackages.value.find { it.packageId == packageId } ?: return false
        val packageDir = File(packagesDir, packageId)
        val manifestFile = File(packageDir, MANIFEST_FILENAME)

        val updated = target.copy(isEnabled = isEnabled)
        if (manifestFile.exists()) {
            manifestFile.writeText(updated.toJson())
        }

        reloadPackagesInternal()
        return true
    }

    /**
     * Returns total disk bytes currently occupied by installed packages.
     */
    fun getInstalledStorageBytes(): Long {
        return _installedPackages.value.sumOf { it.sizeBytes }
    }

    /**
     * Returns total disk bytes reclaimed across uninstallation operations.
     */
    fun getReclaimedStorageBytes(): Long {
        return _reclaimedStorageBytes.value
    }

    companion object {
        const val MANIFEST_FILENAME = "manifest.json"
        const val PAYLOAD_FILENAME = "payload.fpkg"

        // Official Package Identifiers
        const val PACKAGE_COLLAB = "com.arinara.fotara.pkg.collab"
        const val PACKAGE_WHITEBOARD_OCR = "com.arinara.fotara.pkg.whiteboard_ocr"
        const val PACKAGE_EXAM_ANALYTICS = "com.arinara.fotara.pkg.exam_analytics"

        // Mock Payload Data Blocks
        private val PAYLOAD_COLLAB_RAW = "ARINARA_FPKG_OFFICIAL_COLLAB_PAYLOAD_V1_2026".toByteArray(Charsets.UTF_8)
        private val PAYLOAD_OCR_RAW = "ARINARA_FPKG_OFFICIAL_WHITEBOARD_OCR_PAYLOAD_V1_2026".toByteArray(Charsets.UTF_8)
        private val PAYLOAD_ANALYTICS_RAW = "ARINARA_FPKG_OFFICIAL_EXAM_ANALYTICS_PAYLOAD_V1_2026".toByteArray(Charsets.UTF_8)

        /**
         * Resolves the official authenticated mock payload for a given package ID.
         */
        fun getOfficialMockPayload(packageId: String): ByteArray {
            return when (packageId) {
                PACKAGE_COLLAB -> PAYLOAD_COLLAB_RAW
                PACKAGE_WHITEBOARD_OCR -> PAYLOAD_OCR_RAW
                PACKAGE_EXAM_ANALYTICS -> PAYLOAD_ANALYTICS_RAW
                else -> "ARINARA_FPKG_GENERIC_PAYLOAD_FOR_$packageId".toByteArray(Charsets.UTF_8)
            }
        }

        @Volatile
        var testCatalog: List<FpkgManifest>? = null

        /**
         * Standard official Fotara modular add-on definitions.
         * Default is empty in production: modules are delivered on demand.
         */
        val OFFICIAL_PACKAGES: List<FpkgManifest> = emptyList()

        /**
         * Sample catalog definitions available for testing or dynamic loading.
         */
        val SAMPLE_PACKAGES: List<FpkgManifest> = listOf(
            FpkgManifest(
                packageId = PACKAGE_COLLAB,
                name = "Real-Time Collab Canvas",
                version = "1.0.0",
                minFotaraVersion = "2.0.0",
                description = "Synchronized peer-to-peer visual workspace for concurrent lecture diagramming, vector annotations, and study rooms.",
                iconKey = "collab",
                sizeBytes = 3_355_443L, // 3.2 MB
                permissions = listOf("ACCESS_NETWORK_STATE", "INTERNET", "HIGH_REFRESH_RATE_CANVAS"),
                signatureSha256 = FpkgSignatureVerifier.computeSha256(PAYLOAD_COLLAB_RAW),
                isEnabled = true,
                isInstalled = false
            ),
            FpkgManifest(
                packageId = PACKAGE_WHITEBOARD_OCR,
                name = "Advanced Mathematical OCR",
                version = "1.0.0",
                minFotaraVersion = "2.0.0",
                description = "Deep neural document parser extracting complex LaTeX equations, matrix layouts, and scientific derivations from whiteboard photos.",
                iconKey = "whiteboard_ocr",
                sizeBytes = 5_033_164L, // 4.8 MB
                permissions = listOf("NEURAL_PROCESSING_UNIT", "CAMERA_HIGH_RES"),
                signatureSha256 = FpkgSignatureVerifier.computeSha256(PAYLOAD_OCR_RAW),
                isEnabled = true,
                isInstalled = false
            ),
            FpkgManifest(
                packageId = PACKAGE_EXAM_ANALYTICS,
                name = "Predictive Exam Analytics",
                version = "1.0.0",
                minFotaraVersion = "2.0.0",
                description = "Predictive academic performance model calculating optimal coursework deadlines, target letter grades, and retention decay.",
                iconKey = "exam_analytics",
                sizeBytes = 2_202_009L, // 2.1 MB
                permissions = listOf("HISTORICAL_RECALL_INDEXING", "LOCAL_DATA_AGGREGATION"),
                signatureSha256 = FpkgSignatureVerifier.computeSha256(PAYLOAD_ANALYTICS_RAW),
                isEnabled = true,
                isInstalled = false
            )
        )

        @Volatile
        private var instance: FotaraPackageManager? = null

        /**
         * Singleton accessor for global package manager instance.
         */
        fun getInstance(context: Context): FotaraPackageManager {
            return instance ?: synchronized(this) {
                instance ?: FotaraPackageManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
