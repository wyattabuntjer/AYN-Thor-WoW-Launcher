package app.gamenative.utils

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LsfgDllStoreTest {
    @get:Rule val directory = TemporaryFolder()

    private fun pe(fill: Byte = 0): ByteArray = ByteArray(128) { fill }.apply {
        this[0] = 0x4d
        this[1] = 0x5a
        for (i in 0x3c..0x3f) this[i] = 0
        this[0x3c] = 64
        this[64] = 0x50
        this[65] = 0x45
        this[66] = 0
        this[67] = 0
    }

    @Test fun importsOnlyAfterValidationAndKeepsSource() {
        val source = directory.newFile("source.dll").apply { writeBytes(pe()) }
        val destination = File(directory.root, "private/Lossless.dll")
        var validated = false
        source.inputStream().use { input ->
            LsfgDllStore.importTo(destination, input) { candidate ->
                assertFalse(destination.exists())
                assertArrayEquals(source.readBytes(), candidate.readBytes())
                validated = true
            }
        }
        assertTrue(validated)
        assertTrue(LsfgDllStore.sameContent(source, destination))
        assertEquals(listOf("Lossless.dll"), destination.parentFile!!.list()!!.toList())
    }

    @Test fun unsupportedShadersKeepPreviousImport() {
        val destination = directory.newFile("Lossless.dll").apply { writeText("previous working copy") }
        assertThrows(IllegalStateException::class.java) {
            LsfgDllStore.importTo(destination, ByteArrayInputStream(pe())) { error("unsupported shaders") }
        }
        assertEquals("previous working copy", destination.readText())
        assertEquals(listOf("Lossless.dll"), directory.root.list()!!.toList())
    }

    @Test fun interruptedReadKeepsPreviousImport() {
        val destination = directory.newFile("Lossless.dll").apply { writeText("previous working copy") }
        val broken = object : InputStream() {
            override fun read(): Int = throw IOException("disconnected source")
        }
        assertThrows(IOException::class.java) { LsfgDllStore.importTo(destination, broken) { } }
        assertEquals("previous working copy", destination.readText())
        assertEquals(listOf("Lossless.dll"), directory.root.list()!!.toList())
    }

    @Test fun malformedPeIsRejectedBeforeNativeParser() {
        val destination = directory.newFile("Lossless.dll").apply { writeText("previous working copy") }
        val bytes = pe().apply { this[0x3c] = 0x7f }
        assertThrows(IllegalArgumentException::class.java) {
            LsfgDllStore.importTo(destination, ByteArrayInputStream(bytes)) { fail("Invalid PE reached shader parser") }
        }
        assertEquals("previous working copy", destination.readText())
    }

    @Test fun equalSizeDllReplacementIsDetectedByContent() {
        val first = directory.newFile("first.dll").apply { writeBytes(pe(1)) }
        val second = directory.newFile("second.dll").apply { writeBytes(pe(2)) }
        assertEquals(first.length(), second.length())
        assertFalse(LsfgDllStore.sameContent(first, second))
    }
}
