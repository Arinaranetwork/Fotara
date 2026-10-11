// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.persistence

import android.content.Context
import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.model.CanvasBackgroundStyle
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasElement
import com.arinara.fotara.canvas.model.CanvasLayer
import com.arinara.fotara.canvas.model.ImageElement
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.TextBackgroundStyle
import com.arinara.fotara.canvas.model.TextLayerElement
import com.arinara.fotara.data.db.FotaraDbHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.concurrent.ConcurrentHashMap

interface CanvasRepository {
    fun getDocumentFlow(canvasId: Long): Flow<CanvasDocument?>
    suspend fun loadDocument(canvasId: Long): CanvasDocument?
    suspend fun loadDocumentProgressive(
        canvasId: Long,
        chunkSize: Int = 500,
        onChunkLoaded: ((chunk: List<CanvasElement>, progress: Float) -> Unit)? = null
    ): CanvasDocument?
    fun markDirty(canvasId: Long, document: CanvasDocument)
    suspend fun saveDocumentImmediate(canvasId: Long, document: CanvasDocument)
    suspend fun purgeCanvas(canvasId: Long)
}

/**
 * Production Canvas repository managing dirty-tracking autosave, transactional writes,
 * binary stroke encoding, and progressive chunked loading to prevent OOM or main-thread jank.
 */
class DefaultCanvasRepository(
    private val canvasDao: CanvasDao,
    private val assetManager: CanvasAssetManager,
    private val dbHelper: FotaraDbHelper,
    repositoryScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : CanvasRepository {

    private val scope = repositoryScope
    private val documentFlows = ConcurrentHashMap<Long, MutableStateFlow<CanvasDocument?>>()

    // Conflated autosave pipeline with single-writer coroutine
    private data class SaveRequest(val canvasId: Long, val document: CanvasDocument)
    private val saveChannel = Channel<SaveRequest>(Channel.CONFLATED)

    init {
        scope.launch {
            startAutosaveWorker()
        }
    }

    private suspend fun startAutosaveWorker() {
        while (scope.isActive) {
            try {
                val request = saveChannel.receive()
                // Debounce 600ms to coalesce rapid drawing gestures
                delay(600)

                // Drain any newer pending request for the same canvas
                var latest = request
                while (true) {
                    val poll = saveChannel.tryReceive().getOrNull() ?: break
                    latest = poll
                }

                writeDocumentToDisk(latest.canvasId, latest.document)
            } catch (_: Exception) {}
        }
    }

    override fun getDocumentFlow(canvasId: Long): Flow<CanvasDocument?> {
        return documentFlows.getOrPut(canvasId) {
            val flow = MutableStateFlow<CanvasDocument?>(null)
            scope.launch {
                val loaded = loadDocument(canvasId)
                flow.value = loaded
            }
            flow
        }.asStateFlow()
    }

    override suspend fun loadDocument(canvasId: Long): CanvasDocument? = withContext(Dispatchers.IO) {
        loadDocumentProgressive(canvasId, chunkSize = 1000, onChunkLoaded = null)
    }

    /**
     * Loads a canvas document progressively in chunks.
     * Prevents UI stutter and bounded memory spikes even for documents with 20,000+ strokes.
     */
    override suspend fun loadDocumentProgressive(
        canvasId: Long,
        chunkSize: Int,
        onChunkLoaded: ((chunk: List<CanvasElement>, progress: Float) -> Unit)?
    ): CanvasDocument? = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeReadableDatabase()

        // 1. Fetch canvas note title and metadata
        var title = "Untitled Canvas"
        var createdAt = System.currentTimeMillis()
        var updatedAt = System.currentTimeMillis()

        db.rawQuery(
            "SELECT title, created_at, updated_at FROM canvas_notes WHERE id = ?",
            arrayOf(canvasId.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                title = cursor.getString(0)
                createdAt = cursor.getLong(1)
                updatedAt = cursor.getLong(2)
            } else {
                return@withContext null
            }
        }

        // 2. Load Layers
        val layerEntities = canvasDao.loadLayers(canvasId)
        val layers = if (layerEntities.isEmpty()) {
            listOf(CanvasLayer(id = "layer_default", name = "Layer 1", order = 0))
        } else {
            layerEntities.map { entity ->
                CanvasLayer(
                    id = entity.id,
                    name = entity.name,
                    isVisible = entity.isVisible,
                    isLocked = entity.isLocked,
                    opacity = entity.opacity,
                    order = entity.sortOrder
                )
            }
        }

        // 3. Count total elements for progressive chunking
        val totalElements = canvasDao.countElements(canvasId)
        val loadedElements = ArrayList<CanvasElement>(totalElements)

        var offset = 0
        while (offset < totalElements) {
            val rawChunk = canvasDao.loadElementsChunk(canvasId, offset, chunkSize)
            if (rawChunk.isEmpty()) break

            val decodedChunk = mutableListOf<CanvasElement>()
            for (entity in rawChunk) {
                val element = decodeElementEntity(entity)
                if (element != null) {
                    decodedChunk.add(element)
                }
            }

            loadedElements.addAll(decodedChunk)
            offset += rawChunk.size

            val progress = if (totalElements > 0) (offset.toFloat() / totalElements).coerceIn(0f, 1f) else 1f
            onChunkLoaded?.invoke(decodedChunk, progress)
        }

        val doc = CanvasDocument(
            id = canvasId,
            title = title,
            backgroundStyle = CanvasBackgroundStyle.GRID,
            createdAt = createdAt,
            updatedAt = updatedAt,
            layers = layers,
            elements = loadedElements
        )

        // Update cached flow
        documentFlows[canvasId]?.value = doc
        doc
    }

    private fun decodeElementEntity(entity: CanvasElementEntity): CanvasElement? {
        return when (entity.elementType) {
            "STROKE" -> {
                StrokeCodec.decode(entity.dataChunk, entity.id, entity.layerId)
            }
            "IMAGE" -> {
                decodeImageElement(entity)
            }
            "TEXT" -> {
                decodeTextLayerElement(entity)
            }
            else -> null
        }
    }

    private fun decodeImageElement(entity: CanvasElementEntity): ImageElement? {
        return try {
            val bais = ByteArrayInputStream(entity.dataChunk)
            DataInputStream(bais).use { input ->
                val assetId = input.readUTF()
                val x = input.readFloat()
                val y = input.readFloat()
                val w = input.readFloat()
                val h = input.readFloat()
                val scale = input.readFloat()
                val rot = input.readFloat()

                val bounds = CanvasRect(
                    left = entity.boundsLeft,
                    top = entity.boundsTop,
                    right = entity.boundsRight,
                    bottom = entity.boundsBottom
                )

                ImageElement(
                    id = entity.id,
                    layerId = entity.layerId,
                    assetId = assetId,
                    x = x,
                    y = y,
                    width = w,
                    height = h,
                    scale = scale,
                    rotationDegrees = rot,
                    bounds = bounds,
                    zIndex = entity.zIndex
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun encodeImageElement(element: ImageElement): ByteArray {
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { out ->
            out.writeUTF(element.assetId)
            out.writeFloat(element.x)
            out.writeFloat(element.y)
            out.writeFloat(element.width)
            out.writeFloat(element.height)
            out.writeFloat(element.scale)
            out.writeFloat(element.rotationDegrees)
        }
        return baos.toByteArray()
    }

    private fun decodeTextLayerElement(entity: CanvasElementEntity): TextLayerElement? {
        return try {
            val bais = ByteArrayInputStream(entity.dataChunk)
            DataInputStream(bais).use { input ->
                val text = input.readUTF()
                val x = input.readFloat()
                val y = input.readFloat()
                val w = input.readFloat()
                val h = input.readFloat()
                val fontSizeSp = input.readFloat()
                val color = input.readLong()
                val fontWeight = input.readInt()
                val styleName = input.readUTF()
                val rot = input.readFloat()
                val bgStyle = try {
                    TextBackgroundStyle.valueOf(styleName)
                } catch (_: Exception) {
                    TextBackgroundStyle.TRANSPARENT
                }

                val bounds = CanvasRect(
                    left = entity.boundsLeft,
                    top = entity.boundsTop,
                    right = entity.boundsRight,
                    bottom = entity.boundsBottom
                )

                TextLayerElement(
                    id = entity.id,
                    layerId = entity.layerId,
                    text = text,
                    x = x,
                    y = y,
                    width = w,
                    height = h,
                    fontSizeSp = fontSizeSp,
                    color = color,
                    fontWeight = fontWeight,
                    backgroundStyle = bgStyle,
                    rotationDegrees = rot,
                    bounds = bounds,
                    zIndex = entity.zIndex
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun encodeTextLayerElement(element: TextLayerElement): ByteArray {
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { out ->
            out.writeUTF(element.text)
            out.writeFloat(element.x)
            out.writeFloat(element.y)
            out.writeFloat(element.width)
            out.writeFloat(element.height)
            out.writeFloat(element.fontSizeSp)
            out.writeLong(element.color)
            out.writeInt(element.fontWeight)
            out.writeUTF(element.backgroundStyle.name)
            out.writeFloat(element.rotationDegrees)
        }
        return baos.toByteArray()
    }

    override fun markDirty(canvasId: Long, document: CanvasDocument) {
        documentFlows[canvasId]?.value = document
        saveChannel.trySend(SaveRequest(canvasId, document))
    }

    override suspend fun saveDocumentImmediate(
        canvasId: Long,
        document: CanvasDocument
    ) = withContext(Dispatchers.IO) {
        writeDocumentToDisk(canvasId, document)
    }

    private suspend fun writeDocumentToDisk(canvasId: Long, document: CanvasDocument) {
        // Convert Layers
        val layerEntities = document.layers.map { layer ->
            CanvasLayerEntity(
                id = layer.id,
                canvasId = canvasId,
                name = layer.name,
                isVisible = layer.isVisible,
                isLocked = layer.isLocked,
                opacity = layer.opacity,
                sortOrder = layer.order
            )
        }

        // Convert Elements
        val elementEntities = mutableListOf<CanvasElementEntity>()
        val activeAssetIds = mutableSetOf<String>()

        for (el in document.elements) {
            when (el) {
                is StrokeElement -> {
                    val chunk = StrokeCodec.encode(el)
                    elementEntities.add(
                        CanvasElementEntity(
                            id = el.id,
                            canvasId = canvasId,
                            layerId = el.layerId,
                            elementType = "STROKE",
                            boundsLeft = el.bounds.left,
                            boundsTop = el.bounds.top,
                            boundsRight = el.bounds.right,
                            boundsBottom = el.bounds.bottom,
                            zIndex = el.zIndex,
                            dataChunk = chunk
                        )
                    )
                }
                is ImageElement -> {
                    activeAssetIds.add(el.assetId)
                    val chunk = encodeImageElement(el)
                    elementEntities.add(
                        CanvasElementEntity(
                            id = el.id,
                            canvasId = canvasId,
                            layerId = el.layerId,
                            elementType = "IMAGE",
                            boundsLeft = el.bounds.left,
                            boundsTop = el.bounds.top,
                            boundsRight = el.bounds.right,
                            boundsBottom = el.bounds.bottom,
                            zIndex = el.zIndex,
                            dataChunk = chunk
                        )
                    )
                }
                is TextLayerElement -> {
                    val chunk = encodeTextLayerElement(el)
                    elementEntities.add(
                        CanvasElementEntity(
                            id = el.id,
                            canvasId = canvasId,
                            layerId = el.layerId,
                            elementType = "TEXT",
                            boundsLeft = el.bounds.left,
                            boundsTop = el.bounds.top,
                            boundsRight = el.bounds.right,
                            boundsBottom = el.bounds.bottom,
                            zIndex = el.zIndex,
                            dataChunk = chunk
                        )
                    )
                }
            }
        }

        // Transactional commit
        canvasDao.saveDocumentTransaction(canvasId, layerEntities, elementEntities)

        // Clean up unreferenced assets
        assetManager.purgeOrphanedAssets(canvasId, activeAssetIds)
    }

    override suspend fun purgeCanvas(canvasId: Long): Unit = withContext(Dispatchers.IO) {
        canvasDao.purgeCanvasData(canvasId)
        assetManager.purgeOrphanedAssets(canvasId, emptySet())
        documentFlows.remove(canvasId)
        Unit
    }
}
