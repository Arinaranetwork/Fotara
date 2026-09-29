// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.arinara.fotara.data.model.TextNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object TextNoteExporter {

    suspend fun exportNoteAsMarkdown(context: Context, note: TextNote): File = withContext(Dispatchers.IO) {
        val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val cleanTitle = note.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "Note" }
        val file = File(exportsDir, "${cleanTitle}_${System.currentTimeMillis()}.md")
        FileOutputStream(file).use { out ->
            val content = "# ${note.title}\n\n${note.bodyMarkdown}\n"
            out.write(content.toByteArray(Charsets.UTF_8))
        }
        file
    }

    suspend fun exportNoteAsPlainText(context: Context, note: TextNote): File = withContext(Dispatchers.IO) {
        val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val cleanTitle = note.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "Note" }
        val file = File(exportsDir, "${cleanTitle}_${System.currentTimeMillis()}.txt")
        FileOutputStream(file).use { out ->
            val strippedBody = TextNote.stripMarkdownFormatting(note.bodyMarkdown)
            val content = "${note.title}\n${"=".repeat(note.title.length.coerceAtLeast(10))}\n\n$strippedBody\n"
            out.write(content.toByteArray(Charsets.UTF_8))
        }
        file
    }

    suspend fun shareTextNote(context: Context, note: TextNote, asMarkdown: Boolean) {
        val file = if (asMarkdown) {
            exportNoteAsMarkdown(context, note)
        } else {
            exportNoteAsPlainText(context, note)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mimeType = if (asMarkdown) "text/markdown" else "text/plain"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_TEXT, if (asMarkdown) note.bodyMarkdown else TextNote.stripMarkdownFormatting(note.bodyMarkdown))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Note As"))
    }

    fun shareNoteAsMarkdown(context: Context, note: TextNote, scope: kotlinx.coroutines.CoroutineScope? = null) {
        val s = scope ?: kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
        s.launch {
            shareTextNote(context, note, asMarkdown = true)
        }
    }

    fun shareNoteAsPlainText(context: Context, note: TextNote, scope: kotlinx.coroutines.CoroutineScope? = null) {
        val s = scope ?: kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
        s.launch {
            shareTextNote(context, note, asMarkdown = false)
        }
    }

    suspend fun shareMultipleTextNotes(context: Context, notes: List<TextNote>, asMarkdown: Boolean) {
        if (notes.isEmpty()) return
        if (notes.size == 1) {
            shareTextNote(context, notes[0], asMarkdown)
            return
        }
        val uris = ArrayList<android.net.Uri>()
        for (note in notes) {
            val file = if (asMarkdown) {
                exportNoteAsMarkdown(context, note)
            } else {
                exportNoteAsPlainText(context, note)
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            uris.add(uri)
        }
        val mimeType = if (asMarkdown) "text/markdown" else "text/plain"
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, "${notes.size} Text Notes from Fotara")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${notes.size} Notes As"))
    }
}
