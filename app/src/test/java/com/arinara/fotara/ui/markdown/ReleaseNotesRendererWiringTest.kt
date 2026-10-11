// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.markdown

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseNotesRendererWiringTest {

    @Test
    fun verifySharedRendererWiringAcrossScreens() {
        val baseDir = File("src/main/java/com/arinara/fotara")

        val newUpdateDialogFile = File(baseDir, "ui/components/NewUpdateDialog.kt")
        val updateScreenFile = File(baseDir, "online/UpdateScreen.kt")
        val whatsNewScreenFile = File(baseDir, "online/WhatsNewScreen.kt")
        val textNoteEditorFile = File(baseDir, "ui/note/TextNoteEditorScreen.kt")

        assertTrue("NewUpdateDialog.kt must exist", newUpdateDialogFile.exists())
        assertTrue("UpdateScreen.kt must exist", updateScreenFile.exists())
        assertTrue("WhatsNewScreen.kt must exist", whatsNewScreenFile.exists())
        assertTrue("TextNoteEditorScreen.kt must exist", textNoteEditorFile.exists())

        val dialogContent = newUpdateDialogFile.readText()
        val updateContent = updateScreenFile.readText()
        val whatsNewContent = whatsNewScreenFile.readText()
        val textNoteContent = textNoteEditorFile.readText()

        // 1. Verify NewUpdateDialog uses ReleaseNotesRenderer
        assertTrue(
            "NewUpdateDialog must use ReleaseNotesRenderer",
            dialogContent.contains("ReleaseNotesRenderer(")
        )

        // 2. Verify UpdateScreen uses ReleaseNotesRenderer
        assertTrue(
            "UpdateScreen must use ReleaseNotesRenderer",
            updateContent.contains("ReleaseNotesRenderer(")
        )

        // 3. Verify WhatsNewScreen uses ReleaseNotesRenderer
        assertTrue(
            "WhatsNewScreen must use ReleaseNotesRenderer",
            whatsNewContent.contains("ReleaseNotesRenderer(")
        )

        // 4. Verify TextNoteEditorScreen keeps using RichMarkdownColumn and remains unchanged
        assertTrue(
            "TextNoteEditorScreen must keep using RichMarkdownColumn",
            textNoteContent.contains("RichMarkdownColumn(")
        )
        assertFalse(
            "TextNoteEditorScreen must NOT be switched to ReleaseNotesRenderer",
            textNoteContent.contains("ReleaseNotesRenderer(")
        )
    }
}
