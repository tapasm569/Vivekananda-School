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

    fun convertUriToBase64(context: Context, uri: Uri, maxSize: Int = 180): String? {
        return try {
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
            // Base64 strings are long (> 200 characters) regardless of starting character
            if (photoData.length > 200) {
                val cleanData = if (photoData.contains(",")) {
                    photoData.substringAfter(",")
                } else {
                    photoData
                }.trim()

                val decodedBytes = Base64.decode(cleanData, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    return
                }
            }

            // Fallback for local disk file paths
            val file = File(photoData)
            if (file.exists() && file.isFile) {
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
