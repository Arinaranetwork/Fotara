// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.legal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class LegalDocumentLoaderTest {

    private fun findCanonicalLegalFile(filename: String): File {
        // Search current working directory or upward
        var current: File? = File(".").canonicalFile
        while (current != null) {
            val candidate = File(current, "Legal/$filename")
            if (candidate.exists()) {
                return candidate
            }
            current = current.parentFile
        }
        throw AssertionError("Could not locate canonical legal file 'Legal/$filename'")
    }

    @Test
    fun testPrivacyPolicyCanonicalFileValidAndParses() {
        val file = findCanonicalLegalFile("PRIVACY.md")
        assertTrue("Privacy file must exist", file.exists())
        val text = file.readText()

        val parsed = LegalDocumentParser.parse(text, "Privacy Policy")
        assertEquals("Privacy Policy", parsed.title)
        assertEquals(1, parsed.version)
        assertEquals("2026-10-06", parsed.effectiveDate)

        // Body must NOT contain the title or version metadata line
        assertFalse(parsed.body.contains("Version: 1 | Effective: 2026-10-06"))
        assertFalse(parsed.body.startsWith("Privacy Policy"))

        // Must cover mandatory topics audited in Task 0
        assertTrue(parsed.body.contains("What Stays on Your Device"))
        assertTrue(parsed.body.contains("Coursework and Notes"))
        assertTrue(parsed.body.contains("crash.log"))
        assertTrue(parsed.body.contains("What Leaves Your Device and When"))
        assertTrue(parsed.body.contains("api.github.com"))
        assertTrue(parsed.body.contains("nrvnhbizyvubcdqvzabv.supabase.co"))
        assertTrue(parsed.body.contains("User Feedback Submissions"))
        assertTrue(parsed.body.contains("Optional Anonymous Device Count"))
        assertTrue(parsed.body.contains("device_id"))
        assertTrue(parsed.body.contains("UUID v4"))
        assertTrue(parsed.body.contains("Permissions and Why They Are Needed"))
        assertTrue(parsed.body.contains("Children and Students"))
        assertTrue(parsed.body.contains("Changes to this Policy"))
    }

    @Test
    fun testTermsOfServiceCanonicalFileValidAndParses() {
        val file = findCanonicalLegalFile("TERMS.md")
        assertTrue("Terms file must exist", file.exists())
        val text = file.readText()

        val parsed = LegalDocumentParser.parse(text, "Terms of Service")
        assertEquals("Terms of Service", parsed.title)
        assertEquals(1, parsed.version)
        assertEquals("2026-10-06", parsed.effectiveDate)

        // Body must NOT contain the title or version metadata line
        assertFalse(parsed.body.contains("Version: 1 | Effective: 2026-10-06"))
        assertFalse(parsed.body.startsWith("Terms of Service"))

        // Must cover mandatory topics audited in Task 0
        assertTrue(parsed.body.contains("What Is Fotara"))
        assertTrue(parsed.body.contains("Software License and Permitted Use"))
        assertTrue(parsed.body.contains("Arinara Network Proprietary & Educational Software License"))
        assertTrue(parsed.body.contains("User Content and Ownership"))
        assertTrue(parsed.body.contains("Beta Software and Data Backups"))
        assertTrue(parsed.body.contains("Disclaimer of Warranty and Limitation of Liability"))
        assertTrue(parsed.body.contains("App Updates"))
        assertTrue(parsed.body.contains("Termination"))
    }

    @Test
    fun testMalformedOrMissingVersionLineThrows() {
        val missingVersion = """
            Privacy Policy
            ## Introduction
            Some content here.
        """.trimIndent()

        try {
            LegalDocumentParser.parse(missingVersion, "Privacy Policy")
            fail("Expected IllegalStateException for missing version line")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Malformed or missing version"))
        }

        val malformedDate = """
            Privacy Policy
            Version: 1 | Effective: October 6, 2026
            Body here
        """.trimIndent()

        try {
            LegalDocumentParser.parse(malformedDate, "Privacy Policy")
            fail("Expected IllegalStateException for malformed date")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Malformed or missing version"))
        }

        val malformedVersion = """
            Privacy Policy
            Version: v1.0 | Effective: 2026-10-06
            Body here
        """.trimIndent()

        try {
            LegalDocumentParser.parse(malformedVersion, "Privacy Policy")
            fail("Expected IllegalStateException for malformed version")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Malformed or missing version"))
        }
    }

    @Test
    fun testTitleMismatchThrows() {
        val doc = """
            Wrong Title
            Version: 1 | Effective: 2026-10-06
            Body content
        """.trimIndent()

        try {
            LegalDocumentParser.parse(doc, "Privacy Policy")
            fail("Expected IllegalStateException for title mismatch")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("title mismatch"))
        }
    }
}
