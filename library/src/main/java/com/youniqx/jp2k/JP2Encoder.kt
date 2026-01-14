package com.youniqx.jp2k

import android.graphics.Bitmap
import android.util.Log
import java.io.OutputStream
import java.util.Collections
import kotlin.math.log2
import kotlin.math.sign

/**
 * JPEG-2000 bitmap encoder. Output properties:
 * <ul]
 *     <li>file format: JP2 (standard JPEG-2000 file format) or J2K (JPEG-2000 codestream)</li>
 *     <li>colorspace: RGB or RGBA (depending on the
 *          {@link Bitmap#hasAlpha() hasAlpha()} value of the input bitmap)</li>
 *     <li>precision: 8 bits per channel</li>
 *     <li>image quality: can be set by visual quality or compression ratio; or lossless</li>
 * </ul>
 */

private const val TAG = "JP2Encoder"

@Suppress("TooManyFunctions")
class JP2Encoder(
    val bmp: Bitmap,
) {
    // maximum resolutions possible to create with the given image dimensions [ = floor(log2(min_image_dimension)) + 1]
    private val maxResolutions: Int

    var outputFormat = OutputFormat.FORMAT_JP2

    enum class OutputFormat {
        /** JPEG 2000 codestream format */
        FORMAT_J2K,

        /** The standard JPEG-2000 file format */
        FORMAT_JP2,
    }

    var numResolution = DEFAULT_NUM_RESOLUTIONS
        set(value) {
            require(value in MIN_RESOLUTIONS .. maxResolutions) {
                "Maximum number of resolutions for this image is between $MIN_RESOLUTIONS and $maxResolutions: $value"
            }
            field = value
        }

    var compressionRatios: FloatArray = floatArrayOf()
        set(value) {
            if (value.isEmpty()) {
                field = value
                return
            }
            require(value.all { it >= 1f }) { "compression ratio must be at least 1" }

            // check for conflicting settings
            require(qualityValues.isEmpty()) {
                "compressionRatios and qualityValues must not be used together!"
            }
            // sort the values and filter out duplicates
            field = sort(value, false, 1f)
        }

    var qualityValues: FloatArray = floatArrayOf()
        set(value) {
            if (value.isEmpty()) {
                field = value
                return
            }
            // check for invalid values
            require(value.all { it >= 0f }) { "quality values must not be negative" }
            // check for conflicting settings
            require(compressionRatios.isEmpty()) {
                "compressionRatios and qualityValues must not be used together!"
            }

            // sort the values and filter out duplicates
            field = sort(value, true, 0f)
        }

    /**
     * Creates a new instance of the JPEG-2000 encoder.
     * loads "openjpeg" C library
     * @param bmp the bitmap to encode
     */
    init {
        System.loadLibrary("openjpeg")
        maxResolutions =
            (log2(bmp.width.coerceAtMost(bmp.height).toDouble()).toInt() + 1)
                .coerceAtMost(MAX_RESOLUTIONS_GLOBAL)
        if (numResolution > maxResolutions) numResolution = maxResolutions
        Log.d(TAG, "openjpeg encode: image size = ${bmp.width} x ${bmp.height}, maxResolutions = $maxResolutions")
    }

    /**
     * Set the number of resolutions. It corresponds to the number of DWT decompositions +1.
     * Minimum number of resolutions is 1. Maximum is floor(log2(min_image_dimension)) + 1.<br><br>
     *
     * Some software might be able to take advantage of this and decode only smaller resolution
     * when appropriate. (This library is one such software. See {@link JP2Decoder#setSkipResolutions(int)}).<br><br>
     *
     * Default value: 6 if the image dimensions are at least 32x32. Otherwise, the maximum supported
     * number of resolutions.
     * @param numResolution number of resolutions
     * @return this {@code JP2Encoder} instance
     */
    fun setNumResolutions(numResolution: Int): JP2Encoder {
        this.numResolution = numResolution
        return this
    }

    /**
     * Set compression ratio. The value is a factor of compression, thus 20 means 20 times compressed
     * (measured against the raw, uncompressed image size). 1 indicates lossless compression
     *
     * This option produces a predictable image size, but the visual image quality will depend on how
     * "compressible" the original image is. If you want to get predictable visual quality (but
     * unpredictable size), use {@link #setVisualQuality(float...)}.
     *
     * You can set multiple compression ratios - this will produce an image with multiple quality layers.
     * Some software might be able to take advantage of this and decode only lesser quality layer
     * when appropriate. (This library is one such software. See {@link JP2Decoder#setLayersToDecode(int)}.)
     *
     * Default value: a single lossless quality layer.
     *
     * Note: {@link #setCompressionRatio(float...)} and {@link #setVisualQuality(float...)}
     * cannot be used together.
     * @param compressionRatios compression ratios
     * @return this {@code JP2Encoder} instance
     */
    @Deprecated("Use property setter to set a floatArrayOf(*) instead")
    fun setCompressionRatio(vararg compressionRatios: Float): JP2Encoder {
        this.compressionRatios = compressionRatios
        return this
    }

    /**
     * Set image quality. The value is a <a href="https://en.wikipedia.org/wiki/Peak_signal-to-noise_ratio">PSNR</a>,
     * measured in dB. Higher PSNR means higher quality. A special value 0 indicates lossless quality.<br><br>
     *
     * As for reasonable values: 20 is extremely aggressive compression, 60-70 is close to lossless.
     * For "normal" compression you might want to aim at 30-50, depending on your needs.<br><br>
     *
     * This option produces predictable visual image quality, but the file size will depend on how
     * "compressible" the original image is. If you want to get predictable size (but
     * unpredictable visual quality), use {@link #setCompressionRatio(float...)}.<br><br>
     *
     * You can set multiple quality values - this will produce an image with multiple quality layers.
     * Some software might be able to take advantage of this and decode only lesser quality layer
     * when appropriate. (This library is one such software. See {@link JP2Decoder#setLayersToDecode(int)}.)<br><br>
     *
     * Default value: a single lossless quality layer.<br><br>
     *
     * Note: {@link #setVisualQuality(float...)} and {@link #setCompressionRatio(float...)} cannot be used together.
     * @param qualityValues quality layer PSNR values
     * @return this {@code JP2Encoder} instance
     */
    @Deprecated("Use property setter to set a floatArrayOf(*) instead")
    fun setVisualQuality(vararg qualityValues: Float): JP2Encoder {
        this.qualityValues = qualityValues
        return this
    }

    /**
     * Sets the output file format. The default value is {@link #FORMAT_JP2}.
     * @param outputFormat {@link #FORMAT_J2K} or {@link #FORMAT_JP2}
     * @return this {@code JP2Encoder} instance
     */
    @Deprecated("Use property setter with OutputFormat enum instead")
    @Throws(IllegalArgumentException::class)
    fun setOutputFormat(outputFormatByOrdinal: Int): JP2Encoder {
        val outputFormat = OutputFormat.entries[outputFormatByOrdinal]
        this.outputFormat = outputFormat
        return this
    }

    /**
     * Encode to JPEG-2000, return the result as a byte array.
     * @return the JPEG-2000 encoded data
     */
    fun encode(): ByteArray = encodeInternal(bmp)

    /**
     * Encode to JPEG-2000, store the result into a file.
     * @param fileName the name of the output file
     * @return {@code true} if the image was successfully converted and stored; {@code false} otherwise
     */
    fun encode(fileName: String): Boolean = encodeInternal(bmp, fileName)

    /**
     * Encode to JPEG-2000, write the result into an {@link OutputStream}.
     * @param out the stream into which the result will be written
     * @return the number of bytes written; 0 in case of a conversion error
     * @throws java.io.IOException if there's an error writing the result into the output stream
     */
    fun encode(out: OutputStream): Int {
        val data = encodeInternal(bmp)
        out.write(data)
        return data.size
    }

    private fun encodeInternal(bmp: Bitmap): ByteArray {
        val pixels = createPixels(bmp)
        val start = System.currentTimeMillis()
        val result =
            encodeJP2ByteArray(
                pixels,
                bmp.hasAlpha(),
                bmp.width,
                bmp.height,
                outputFormat.ordinal,
                numResolution,
                compressionRatios,
                qualityValues,
            )
        Log.d(TAG, "converting to JP2: ${System.currentTimeMillis() - start}ms")
        return result
    }

    private fun encodeInternal(
        bmp: Bitmap,
        fileName: String,
    ): Boolean {
        val pixels = createPixels(bmp)
        val start = System.currentTimeMillis()
        val result =
            encodeJP2File(
                fileName,
                pixels,
                bmp.hasAlpha(),
                bmp.width,
                bmp.height,
                outputFormat.ordinal,
                numResolution,
                compressionRatios,
                qualityValues,
            )
        Log.d(TAG, "converting to JP2: ${System.currentTimeMillis() - start}ms")
        return result == EXIT_SUCCESS
    }

    private fun createPixels(bmp: Bitmap): IntArray {
        val pixels = IntArray(bmp.width * bmp.height)
        bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        return pixels
    }

    private fun sort(
        array: FloatArray,
        ascending: Boolean,
        losslessValue: Float,
    ): FloatArray {
        if (array.isEmpty()) return floatArrayOf()
        val list = array.toSet().toMutableList()
        Collections.sort(
            list,
            Comparator<Float> { o1, o2 ->
                // lossless value must always come last
                when {
                    o1 == losslessValue && o2 != losslessValue -> 1
                    o2 == losslessValue && o1 != losslessValue -> -1
                    else -> sign(if (ascending) o1 - o2 else o2 - o1).toInt()
                }
            },
        )
        // copy from list back to array
        val ret = FloatArray(list.size)
        for (i in ret.indices) ret[i] = list[i]
        return ret
    }

    @Suppress("LongParameterList")
    private external fun encodeJP2File(
        filename: String,
        pixels: IntArray,
        hasAlpha: Boolean,
        width: Int,
        height: Int,
        fileFormat: Int,
        numResolutions: Int,
        compressionRatios: FloatArray,
        qualityValues: FloatArray,
    ): Int

    @Suppress("LongParameterList")
    private external fun encodeJP2ByteArray(
        pixels: IntArray,
        hasAlpha: Boolean,
        width: Int,
        height: Int,
        fileFormat: Int,
        numResolutions: Int,
        compressionRatios: FloatArray,
        qualityValues: FloatArray,
    ): ByteArray

    companion object {
        // TODO in case of update to a newer version of OpenJPEG, check if it still
        // throws error in case of too high resolution number
        // minimum resolutions supported by OpenJPEG 2.3.0
        private const val MIN_RESOLUTIONS = 1
        // maximum resolutions supported by OpenJPEG 2.3.0
        private const val MAX_RESOLUTIONS_GLOBAL = 32

        private const val EXIT_SUCCESS = 0
        private const val EXIT_FAILURE = 1

        private const val DEFAULT_NUM_RESOLUTIONS = 6

        @Deprecated("Use OutputFormat Enum values instead")
        const val FORMAT_J2K = 0

        @Deprecated("Use OutputFormat Enum values instead")
        const val FORMAT_JP2 = 1
    }
}
