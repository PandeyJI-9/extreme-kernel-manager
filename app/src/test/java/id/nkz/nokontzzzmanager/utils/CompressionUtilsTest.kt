package id.nkz.nokontzzzmanager.utils

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class CompressionUtilsTest {

    @Test
    fun `null input returns null`() {
        assertNull(CompressionUtils.compress(null))
        assertNull(CompressionUtils.decompress(null))
    }

    @Test
    fun `empty input returns empty`() {
        assertEquals("", CompressionUtils.compress(""))
        assertEquals("", CompressionUtils.decompress(""))
    }

    @Test
    fun `compress then decompress roundtrip`() {
        val original = "Hello, NKM! This is a test string with some content to compress."
        val compressed = CompressionUtils.compress(original)
        assertNotNull(compressed)
        assertNotEquals(original, compressed) // should actually be compressed/encoded
        val restored = CompressionUtils.decompress(compressed)
        assertEquals(original, restored)
    }

    @Test
    fun `roundtrip with long string`() {
        val original = "cpu0\ncpu4\ncpu7\n".repeat(200)
        val restored = CompressionUtils.decompress(CompressionUtils.compress(original))
        assertEquals(original, restored)
    }

    @Test
    fun `decompress plain string returns it unchanged`() {
        // Not base64-gzip, should fall back to original
        val plain = "schedutil"
        assertEquals(plain, CompressionUtils.decompress(plain))
    }

    @Test
    fun `decompress very short string returns it unchanged`() {
        // Length < 4, short-circuit path
        assertEquals("abc", CompressionUtils.decompress("abc"))
    }

    @Test
    fun `compress preserves unicode`() {
        val original = "Frekuensi CPU — 频率 — 주파수"
        val restored = CompressionUtils.decompress(CompressionUtils.compress(original))
        assertEquals(original, restored)
    }
}
