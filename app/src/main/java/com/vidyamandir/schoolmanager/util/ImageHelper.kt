package com.vidyamandir.schoolmanager.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.ImageView
import java.io.ByteArrayOutputStream
import java.io.File

object ImageHelper {

    /**
     * Downsamples high-resolution camera photos to avoid OutOfMemoryError.
     * Compresses the output to a compact Base64 JPEG string (~10-15 KB).
     */
    fun convertUriToBase64(context: Context, uri: Uri, maxSize: Int = 180): String? {
        return try {
            // First pass: query dimensions without allocating memory
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxSize * 2 ||
                   options.outHeight / sampleSize > maxSize * 2) {
                sampleSize *= 2
            }

            // Second pass: decode with sample size and 16-bit RGB_565 configuration
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val ratio = sampledBitmap.width.toFloat() / sampledBitmap.height.toFloat()
            val finalWidth = if (ratio > 1f) maxSize else maxOf(1, (maxSize * ratio).toInt())
            val finalHeight = if (ratio > 1f) maxOf(1, (maxSize / ratio).toInt()) else maxSize

            val finalBitmap = Bitmap.createScaledBitmap(sampledBitmap, finalWidth, finalHeight, true)
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val byteArray = outputStream.toByteArray()

            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    fun loadProfilePhoto(photoData: String?, imageView: ImageView, defaultResId: Int) {
        if (photoData.isNullOrBlank()) {
            imageView.setImageResource(defaultResId)
            return
        }

        try {
            // If it's a Base64 string from Firebase
            if (photoData.length > 200 && !photoData.startsWith("/")) {
                val decodedBytes = Base64.decode(photoData, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    return
                }
            }

            // Legacy local file path fallback
            val file = File(photoData)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    return
                }
            }
        } catch (_: Throwable) {}

        imageView.setImageResource(defaultResId)
    }
}
