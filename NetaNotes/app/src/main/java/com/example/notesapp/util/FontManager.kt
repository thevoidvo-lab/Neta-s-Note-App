package com.example.notesapp.util

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import java.io.File

object FontManager {
    private const val TAG = "FontManager"

    fun fontsDir(context: Context): File {
        val dir = File(context.filesDir, "home_fonts")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun loadTypeface(context: Context, path: String?): Typeface {
        if (path.isNullOrBlank()) return Typeface.DEFAULT
        return try {
            val file = File(path)
            if (file.exists()) {
                Typeface.createFromFile(file)
            } else {
                Typeface.DEFAULT
            }
        } catch (e: Exception) {
            Log.e(TAG, "font gagal dimuat, fallback default", e)
            Typeface.DEFAULT
        }
    }

    fun copyFontToInternal(context: Context, sourceBytes: ByteArray, fileName: String): String? {
        return try {
            val dir = fontsDir(context)
            val dest = File(dir, fileName)
            dest.writeBytes(sourceBytes)
            dest.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "gagal copy font", e)
            null
        }
    }
}
