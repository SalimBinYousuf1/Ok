package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object InternalStorageImageManager {

    private const val DIRECTORY_NAME = "salim_attachments"

    /**
     * Persistently saves an image from a picked Uri to the app's internal private storage.
     * Returns the permanent local file URI string (file://...).
     */
    suspend fun saveImageToInternalStorage(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, DIRECTORY_NAME).apply {
                if (!exists()) mkdirs()
            }
            val fileName = "img_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            val destFile = File(dir, fileName)

            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                inputStream = context.contentResolver.openInputStream(sourceUri) ?: return@withContext null
                // Decode bounds first to check size, then compress appropriately
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext null
                outputStream = FileOutputStream(destFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
                return@withContext Uri.fromFile(destFile).toString()
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Checks if a persistent file exists on internal storage.
     */
    fun fileExists(filePathOrUri: String?): Boolean {
        if (filePathOrUri.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(filePathOrUri)
            val path = uri.path ?: filePathOrUri
            File(path).exists()
        } catch (e: Exception) {
            false
        }
    }
}
