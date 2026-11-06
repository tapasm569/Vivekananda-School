package com.vidyamandir.schoolmanager.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import java.io.File
import java.io.FileOutputStream

object ImageHelper {

    fun saveImageToInternalStorage(context: Context, uri: Uri, prefix: String): String? {
        return try {
            val directory = File(context.filesDir, "profiles").apply { if (!exists()) mkdirs() }
            val file = File(directory, "${prefix}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadProfilePhoto(filePath: String?, imageView: ImageView, defaultResId: Int) {
        if (filePath.isNullOrBlank()) {
            imageView.setImageResource(defaultResId)
            return
        }
        try {
            val file = File(filePath)
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
