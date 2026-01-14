# JP2 for Android

An open-source JPEG-2000 image encoder/decoder, forked from
[this Gemalto/Thales Repo](https://github.com/ThalesGroup/JP2ForAndroid),
which was forked and update once
[in this Repo](https://github.com/Tgo1014/JP2ForAndroid),
and updated to fit current play store rules in
[this Repo](https://github.com/prefectAtEarth/JP2ForAndroid),
for Android, based on [OpenJPEG](http://www.openjpeg.org/) v2.4.0.

## Set up

Add dependency to your `build.gradle.kts`:

```kotlin
implementation("com.youniqx:jp2k-sdk:<version>")
```

## Basic Usage

Decoding an image:

```kotlin
val bmp: Bitmap = new JP2Decoder(jp2data).decode()
imageView.setImageBitmap(bmp)
```

Encoding an image:

```kotlin
val bmp: Bitmap = ...
//lossless encode
val jp2data: ByteArray = JP2Encoder(bmp).encode()
//lossy encode (target PSNR = 50dB)
val jp2data: ByteArray = JP2Encoder(bmp)
    .setVisualQuality(50)
    .encode()
```

## Advanced Usage

### Multiple Resolutions

A single JPEG-2000 image can contain multiple resolutions.
The final resolution is always equal to `<image_width> x <image_height>`; each
additional resolution reduces the width and height by the factor of two. If you
don't need the full resolution, you can save memory and decoding time by
decoding at a lower resolution.

#### Encoding

Default number of resolutions is 6, but you can specify your own value:

```kotlin
val jp2data: ByteArray = JP2Encoder(bmp)
    .setNumResolutions(3)
    .encode()
```

The number of resolutions must be between 1 and 32 and both image width and
height must be greater or equal to `2^(numResolutions - 1)`.

#### Decoding

You can obtain the number of resolutions (as well as some other information
about the image) by calling the `readHeader()` method:

```kotlin
val header: Header = JP2Decoder(jp2data).readHeader()
val numResolutions: Int = header.numResolutions
```

If you don't need the full resolution image, you can skip one or more
resolutions during the decoding process.

```kotlin
val reducedBmp: Bitmap = JP2Decoder(jp2data)
    .setSkipResolutions(2)
    .decode()
```

### Multiple Quality Layer

Multiple layers can be encoded in a JPEG-2000 image, each having a different
visual quality. If you don't need maximum visual quality, you can save
decoding time by skipping the higher-quality layers.

#### Encoding

Quality layers can be specified in two ways: as a list of compression ratios
or visual qualities. The **compression ratios** are specified as factors of
compression, i.e. 20 means the size will be 20 times less than the raw
uncompressed size. Compression ratio 1 means lossless compression. Example:

```kotlin
val jp2data: ByteArray = JP2Encoder(bmp)
    //specify 3 quality layers with compression ratios 1:50, 1:20, and lossless
    .setCompressionRatio(50, 10, 1)
    .encode()
```

**Visual quality** is specified as
[PSNR](https://en.wikipedia.org/wiki/Peak_signal-to-noise_ratio)
values in dB. Usable values are roughly between 20 (very
aggressive compression) and 70 (almost lossless). Special value 0 indicates
lossless compression. Example:

```kotlin
val jp2data: ByteArray = JP2Encoder (bmp)
    //specify 3 quality layers with PSNR 30dB, 50dB, and lossless
    .setVisualQuality(30, 50, 0)
    .encode()
```

`setCompressionRatio()` and `setVisualQuality()` can not be used at the same time.

#### Decoding

You can obtain the number of available quality layers by calling
the `readHeader()` method:

```kotlin
val header: Header = JP2Decoder(jp2data).readHeader()
val numQualityLayers: Int = header.numQualityLayers
```

If you don't need a maximum quality image, you can trade some visual quality
for a shorter decoding time by not decoding all the quality layers.

```kotlin
val lowQualityBmp: Bitmap = JP2Decoder(jp2data)
    .setLayersToDecode(2)
    .decode()
```

### File Format

`JP2Encoder` supports two output formats:

* JP2 - standard JPEG-2000 file format (encapsulating a JPEG-2000 codestream)
* J2K - unencapsulated JPEG-2000 codestream

JP2 is the default output format, but it can be changed:

```kotlin
val j2kdata: ByteArray = JP2Encoder (bmp)
    .setOutputFormat(FORMAT_J2K)
    .encode()
```
