// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.persistence

import com.arinara.fotara.canvas.model.StrokeElement
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Compact binary serializer for photo overlay drawings.
 * Reuses high-density delta encoding from [StrokeCodec] for individual strokes.
 */
object PhotoDrawingCodec {

    private const val CODEC_VERSION: Byte = 0x01
    private const val MAX_SAFE_STROKES = 50_000

    /**
     * Serializes a list of [StrokeElement] into a compact binary byte array.
     */
    fun encode(strokes: List<StrokeElement>): ByteArray {
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { out ->
            out.writeByte(CODEC_VERSION.toInt())
            writeVarInt(out, strokes.size)
            for (stroke in strokes) {
                val strokeBytes = StrokeCodec.encode(stroke)
                writeVarInt(out, strokeBytes.size)
                out.write(strokeBytes)
            }
        }
        return baos.toByteArray()
    }

    /**
     * Deserializes binary data into a list of [StrokeElement].
     * Never throws on truncated or corrupt input; returns safe partial or empty list.
     */
    fun decode(bytes: ByteArray?, layerId: String = "photo_layer"): List<StrokeElement> {
        if (bytes == null || bytes.size < 2) {
            return emptyList()
        }

        return try {
            val bais = ByteArrayInputStream(bytes)
            DataInputStream(bais).use { input ->
                val version = input.readByte()
                if (version != CODEC_VERSION) {
                    return emptyList()
                }

                val count = readVarInt(input)
                if (count <= 0 || count > MAX_SAFE_STROKES) {
                    return emptyList()
                }

                val list = ArrayList<StrokeElement>(count.coerceAtMost(1024))
                for (i in 0 until count) {
                    val len = readVarInt(input)
                    if (len <= 0 || len > 10_000_000) break
                    val chunk = ByteArray(len)
                    input.readFully(chunk)
                    val stroke = StrokeCodec.decode(
                        bytes = chunk,
                        elementId = UUID.randomUUID().toString(),
                        layerId = layerId
                    )
                    if (stroke != null) {
                        list.add(stroke)
                    }
                }
                list
            }
        } catch (_: EOFException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeVarInt(out: OutputStream, value: Int) {
        var v = value
        while ((v and 0x7F.inv()) != 0) {
            out.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        out.write(v and 0x7F)
    }

    private fun readVarInt(input: InputStream): Int {
        var result = 0
        var shift = 0
        while (shift < 32) {
            val b = input.read()
            if (b == -1) throw EOFException("Unexpected end of varint")
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
        }
        throw IllegalArgumentException("Malformed varint")
    }
}
