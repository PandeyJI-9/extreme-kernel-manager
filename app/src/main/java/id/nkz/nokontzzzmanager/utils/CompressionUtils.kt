package id.nkz.nokontzzzmanager.utils

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object CompressionUtils {

    /**
     * Compresses a string using GZIP and encodes it to Base64.
     */
    fun compress(data: String?): String? {
        if (data.isNullOrEmpty()) return data
        return try {
            val bos = ByteArrayOutputStream(data.length)
            val gzip = GZIPOutputStream(bos)
            gzip.write(data.toByteArray(Charsets.UTF_8))
            gzip.close()
            val compressed = bos.toByteArray()
            bos.close()
            Base64.encodeToString(compressed, Base64.NO_WRAP)
        } catch (e: Exception) {
            data // Fallback to raw data if compression fails
        }
    }

    /**
     * Decompresses a Base64 encoded GZIP string.
     * Falls back to returning the original string if it's not GZIP compressed.
     */
    fun decompress(compressedData: String?): String? {
        if (compressedData.isNullOrEmpty()) return compressedData
        
        // Quick check: if it doesn't look like Base64 or is too short, return as is
        if (compressedData.length < 4) return compressedData

        return try {
            val compressed = Base64.decode(compressedData, Base64.NO_WRAP)
            if (compressed.size < 2 || compressed[0] != 0x1f.toByte() || compressed[1] != 0x8b.toByte()) {
                // Not a GZIP stream (magic header missing), return original string
                return compressedData
            }

            val bis = ByteArrayInputStream(compressed)
            val gis = GZIPInputStream(bis)
            val bos = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var len: Int
            while (gis.read(buffer).also { len = it } > 0) {
                bos.write(buffer, 0, len)
            }
            gis.close()
            bis.close()
            val result = bos.toString(Charsets.UTF_8.name())
            bos.close()
            result
        } catch (e: Exception) {
            compressedData // Return as is if decompression fails (likely not compressed)
        }
    }
}
