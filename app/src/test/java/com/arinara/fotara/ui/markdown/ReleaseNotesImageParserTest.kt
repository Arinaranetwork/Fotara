// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesImageParserTest {

    @Test
    fun testScenario1_noImages_rendersTextAndListsOnly() {
        val markdown = """
            # Version 1.7.1
            - Performance improvements
            - Screen header layout fixes
        """.trimIndent()

        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is ReleaseNotesBlock.Header)
        assertTrue(blocks[1] is ReleaseNotesBlock.BulletItem)
        assertTrue(blocks[2] is ReleaseNotesBlock.BulletItem)
        assertTrue(blocks.none { it is ReleaseNotesBlock.Image })
    }

    @Test
    fun testScenario2_bannerOnly() {
        val markdown = "![Fotara Banner](https://raw.githubusercontent.com/Arinaranetwork/Fotara/main/Assets/Banners/FotaraBanner_1.7.0.png)"
        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)

        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is ReleaseNotesBlock.Image)
        val img = blocks[0] as ReleaseNotesBlock.Image
        assertEquals("Fotara Banner", img.alt)
        assertEquals("https://raw.githubusercontent.com/Arinaranetwork/Fotara/main/Assets/Banners/FotaraBanner_1.7.0.png", img.url)
        assertNull(img.linkUrl)
        assertTrue(img.isAllowed)
    }

    @Test
    fun testScenario3_inlineMarkdownImages() {
        val markdown = """
            ## Screenshots
            ![Canvas Zoom](https://github.com/Arinaranetwork/Fotara/releases/download/v1.7.0/screenshot_zoom.png)
            ![Header Fix](https://user-images.githubusercontent.com/12345/header_fix.png)
        """.trimIndent()

        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is ReleaseNotesBlock.Header)

        assertTrue(blocks[1] is ReleaseNotesBlock.Image)
        val img1 = blocks[1] as ReleaseNotesBlock.Image
        assertEquals("Canvas Zoom", img1.alt)
        assertTrue(img1.isAllowed)

        assertTrue(blocks[2] is ReleaseNotesBlock.Image)
        val img2 = blocks[2] as ReleaseNotesBlock.Image
        assertEquals("Header Fix", img2.alt)
        assertTrue(img2.isAllowed)
    }

    @Test
    fun testScenario4_htmlImgAndLinkedMarkdownImages() {
        val markdown = """
            [![GitHub Release](https://github.com/Arinaranetwork/Fotara/releases/download/v1.7.1/banner.png)](https://github.com/Arinaranetwork/Fotara/releases)
            <img src="https://raw.githubusercontent.com/Arinaranetwork/Fotara/main/Assets/Banners/banner.png" alt="HTML Banner" />
        """.trimIndent()

        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)
        assertEquals(2, blocks.size)

        // Linked markdown image
        assertTrue(blocks[0] is ReleaseNotesBlock.Image)
        val linked = blocks[0] as ReleaseNotesBlock.Image
        assertEquals("GitHub Release", linked.alt)
        assertEquals("https://github.com/Arinaranetwork/Fotara/releases/download/v1.7.1/banner.png", linked.url)
        assertEquals("https://github.com/Arinaranetwork/Fotara/releases", linked.linkUrl)
        assertTrue(linked.isAllowed)

        // HTML img
        assertTrue(blocks[1] is ReleaseNotesBlock.Image)
        val htmlImg = blocks[1] as ReleaseNotesBlock.Image
        assertEquals("HTML Banner", htmlImg.alt)
        assertEquals("https://raw.githubusercontent.com/Arinaranetwork/Fotara/main/Assets/Banners/banner.png", htmlImg.url)
        assertTrue(htmlImg.isAllowed)
    }

    @Test
    fun testScenario5_mixedTextTablesAndImages() {
        val markdown = """
            # Quality Release 1.7.1
            
            ![Architecture Overview](https://raw.githubusercontent.com/Arinaranetwork/Fotara/main/docs/arch.png)
            
            | Module | Status | Notes |
            | :--- | :---: | ---: |
            | Canvas | 60 FPS | Smooth pan & zoom |
            | Headers | Fixed | No descender clipping |
            
            - Done with all verification
        """.trimIndent()

        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)
        assertEquals(4, blocks.size)
        assertTrue(blocks[0] is ReleaseNotesBlock.Header)
        assertTrue(blocks[1] is ReleaseNotesBlock.Image)
        assertTrue(blocks[2] is ReleaseNotesBlock.Table)
        assertTrue(blocks[3] is ReleaseNotesBlock.BulletItem)

        val table = (blocks[2] as ReleaseNotesBlock.Table).table
        assertEquals(2, table.rows.size)
        assertEquals(listOf("Canvas", "60 FPS", "Smooth pan & zoom"), table.rows[0])
    }

    @Test
    fun testScenario6_invalidAndUntrustedUrls_rejectedByAllowlist() {
        // Non-HTTPS
        assertFalse(ReleaseNotesImageValidator.isAllowedImageUrl("http://github.com/image.png"))
        assertFalse(ReleaseNotesImageValidator.isAllowedImageUrl("ftp://github.com/image.png"))
        assertFalse(ReleaseNotesImageValidator.isAllowedImageUrl("file:///sdcard/image.png"))

        // Disallowed / untrusted third-party hosts
        assertFalse(ReleaseNotesImageValidator.isAllowedImageUrl("https://malicious-site.com/exploit.png"))
        assertFalse(ReleaseNotesImageValidator.isAllowedImageUrl("https://random-cdn.org/pic.jpg"))
        assertFalse(ReleaseNotesImageValidator.isAllowedImageUrl("https://github.evil.com/fake.png"))

        // Allowed GitHub & Arinara hosts
        assertTrue(ReleaseNotesImageValidator.isAllowedImageUrl("https://github.com/Arinaranetwork/Fotara/releases/download/v1.7.1/banner.png"))
        assertTrue(ReleaseNotesImageValidator.isAllowedImageUrl("https://raw.githubusercontent.com/Arinaranetwork/Fotara/main/banner.png"))
        assertTrue(ReleaseNotesImageValidator.isAllowedImageUrl("https://user-images.githubusercontent.com/123/img.png"))
        assertTrue(ReleaseNotesImageValidator.isAllowedImageUrl("https://arinara.network/assets/banner.png"))
        assertTrue(ReleaseNotesImageValidator.isAllowedImageUrl("https://arinaranetwork.github.io/banner.png"))

        // Parsed markdown block correctly flags isAllowed = false
        val markdown = "![Untrusted](https://sketchy-domain.com/tracker.png)"
        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)
        assertEquals(1, blocks.size)
        val img = blocks[0] as ReleaseNotesBlock.Image
        assertEquals("Untrusted", img.alt)
        assertFalse("Disallowed domain must have isAllowed = false", img.isAllowed)
    }
}
