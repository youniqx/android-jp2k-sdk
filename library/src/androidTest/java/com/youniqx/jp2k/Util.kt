package com.youniqx.jp2k

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import org.junit.Assert
import org.junit.Assert.fail
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.pow
import kotlin.math.sqrt

class Util(
    private val ctx: Context,
) {
    fun assertBitmapsEqual(
        expected: Bitmap,
        actual: Bitmap,
    ) {
        assertBitmapsEqual(null, expected, actual)
    }

    fun assertBitmapsEqual(
        message: String?,
        expected: Bitmap,
        actual: Bitmap,
    ) {
        Assert.assertEquals(message, expected.getWidth().toLong(), actual.getWidth().toLong())
        Assert.assertEquals(message, expected.getHeight().toLong(), actual.getHeight().toLong())
        val pixels1 = IntArray(expected.getWidth() * expected.getHeight())
        val pixels2 = IntArray(actual.getWidth() * actual.getHeight())
        expected.getPixels(pixels1, 0, expected.getWidth(), 0, 0, expected.getWidth(), expected.getHeight())
        actual.getPixels(pixels2, 0, actual.getWidth(), 0, 0, actual.getWidth(), actual.getHeight())
        for (i in pixels1.indices) {
            if (pixels1[i] != pixels2[i]) {
                fail(
                    message?.let { "$it; " } +
                        String.format(
                            "pixel $i different - expected %08X, got %08X",
                            pixels1[i],
                            pixels2[i],
                        ),
                )
            }
        }
    }

    fun assertBitmapsEqualWithTolerance(
        message: String?,
        expected: Bitmap,
        actual: Bitmap,
        toleranceThreshold: Double,
    ) {
        Assert.assertEquals(message, expected.width.toLong(), actual.width.toLong())
        Assert.assertEquals(message, expected.height.toLong(), actual.height.toLong())
        val pixels1 = IntArray(expected.width * expected.height)
        val pixels2 = IntArray(actual.width * actual.height)
        expected.getPixels(pixels1, 0, expected.width, 0, 0, expected.width, expected.height)
        actual.getPixels(pixels2, 0, actual.width, 0, 0, actual.width, actual.height)
        for (i in pixels1.indices) {
            if (pixels1[i] != pixels2[i]) {
                val distance = calculateDeltaE(pixels1[i], pixels2[i])
                if (distance < toleranceThreshold) {
                    Log.d("Utils", "Color distance within tolerance of $toleranceThreshold --> $distance")
                    continue
                }
                fail(
                    "${message?.let { "$it; " }}pixel $i different - " +
                        "expected: ${pixels1[i].toColorHex()} " +
                        "| actual: ${pixels2[i].toColorHex()} " +
                        "| distance: $distance",
                )
            }
        }
    }

    // https://en.wikipedia.org/wiki/Color_difference
    private fun calculateDeltaE(
        expectedPixel: Int,
        actualPixel: Int,
    ): Double =
        sqrt(
            (
                (actualPixel.red - expectedPixel.red).toDouble().pow(2.0) +
                    (actualPixel.green - expectedPixel.green).toDouble().pow(2.0) +
                    (actualPixel.blue - expectedPixel.blue).toDouble().pow(2.0)
            ),
        )

    fun assertBitmapsEqual(
        message: String?,
        expected: IntArray,
        actual: IntArray,
    ) {
        Assert.assertEquals(
            "${message.let { "$it; "}}different number of pixels",
            expected.size.toLong(),
            actual.size.toLong(),
        )
        for (i in expected.indices) {
            if (expected[i] != actual[i]) {
                fail(
                    "${message?.let { "$it; " }}pixel $i different - " +
                        "expected: ${expected[i].toColorHex()} | actual: ${actual[i].toColorHex()}",
                )
            }
        }
    }

    @Throws(Exception::class)
    fun loadAssetFile(name: String): ByteArray? {
        ctx.getResources().getAssets().open(name).use { `is` ->
            val out = ByteArrayOutputStream(`is`.available())
            val buffer = ByteArray(8192)
            var count: Int
            while ((`is`.read(buffer).also { count = it }) >= 0) {
                out.write(buffer, 0, count)
            }
            return out.toByteArray()
        }
    }

    @Throws(IOException::class)
    fun openAssetStream(name: String): InputStream = ctx.getAssets().open(name)

    @Throws(Exception::class)
    fun loadFile(name: String?): ByteArray? {
        FileInputStream(name).use { `is` ->
            val out = ByteArrayOutputStream(`is`.available())
            val buffer = ByteArray(8192)
            var count: Int
            while ((`is`.read(buffer).also { count = it }) >= 0) {
                out.write(buffer, 0, count)
            }
            return out.toByteArray()
        }
    }

    @Throws(Exception::class)
    fun loadAssetBitmap(name: String): Bitmap {
        ctx.getResources().getAssets().open(name).use { `is` ->
            val opts = BitmapFactory.Options()
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888
            opts.inPremultiplied = false
            var bmp = BitmapFactory.decodeStream(`is`, null, opts)
            if (bmp!!.getConfig() != Bitmap.Config.ARGB_8888) {
                // convert to ARGB_8888 for pixel comparison purposes
                val pixels = IntArray(bmp.getWidth() * bmp.getHeight())
                bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight())
                bmp = Bitmap.createBitmap(pixels, bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888)
            }
            return bmp
        }
    }

    @Throws(Exception::class)
    fun loadAssetRawPixels(name: String): IntArray {
        // raw bitmaps are stored by component in RGBA order (i.e.) first all R, then all G, then all B, then all A
        var data: ByteArray? = null
        ctx.getResources().getAssets().open(name).use { `is` ->
            val out = ByteArrayOutputStream(`is`.available())
            val buffer = ByteArray(8192)
            var count: Int
            while ((`is`.read(buffer).also { count = it }) >= 0) {
                out.write(buffer, 0, count)
            }
            data = out.toByteArray()
        }
        Assert.assertEquals("raw data length not divisible by 4", 0, (data!!.size % 4).toLong())
        val length = data.size / 4
        val pixels = IntArray(length)
        for (i in 0..<length) {
            pixels[i] = (
                ((data[i].toInt() and 0xFF) shl 16) // R
                    or ((data[i + length].toInt() and 0xFF) shl 8) // G
                    or (data[i + length * 2].toInt() and 0xFF) // B
                    or ((data[i + length * 3].toInt() and 0xFF) shl 24)
            ) // A
        }
        return pixels
    }

    @Throws(Exception::class)
    fun loadAssetRawBitmap(
        name: String,
        width: Int,
        height: Int,
    ): Bitmap {
        val pixels = loadAssetRawPixels(name)
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    @Throws(IOException::class)
    fun createFile(
        name: String,
        encoded: ByteArray?,
    ): File {
        val outFile = File(ctx.getFilesDir(), name)
        val out = FileOutputStream(outFile)
        out.write(encoded)
        out.close()
        return outFile
    }

    @Throws(IOException::class)
    fun createFile(encoded: ByteArray?): File {
        val outFile = File.createTempFile("testjp2", "tmp", ctx.getFilesDir())
        val out = FileOutputStream(outFile)
        out.write(encoded)
        out.close()
        return outFile
    }

    private fun Int.toColorHex() = toString().format("%08x")
}
