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
     * Reads the selected image Uri, scales it to compact avatar dimensions (default 180px),
     * compresses it as JPEG (70% quality), and returns a Base64 string (~10-20 KB).
     */
    fun convertUriToBase64(context: Context, uri: Uri, maxSize: Int = 180): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

                val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                val width: Int
                val height: Int
                if (ratio > 1) {
                    width = maxSize
                    height = (maxSize / ratio).toInt()
                } else {
                    height = maxSize
                    width = (maxSize * ratio).toInt()
                }

                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val byteArray = outputStream.toByteArray()

                Base64.encodeToString(byteArray, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Loads a profile photo into an ImageView.
     * Supports Base64 strings from Firebase RTDB and local file paths.
     */
    fun loadProfilePhoto(photoData: String?, imageView: ImageView, defaultResId: Int) {
        if (photoData.isNullOrBlank()) {
            imageView.setImageResource(defaultResId)
            return
        }

        try {
            // Check if it is a Base64 image string from Firebase
            if (photoData.length > 200 && !photoData.startsWith("/")) {
                val decodedBytes = Base64.decode(photoData, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    return
                }
            }

            // Fallback for legacy local file path
            val file = File(photoData)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        imageView.setImageResource(defaultResId)
    }
}
