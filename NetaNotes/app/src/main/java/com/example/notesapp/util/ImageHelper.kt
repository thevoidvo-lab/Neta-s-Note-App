package com.example.notesapp.util

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.io.File

object ImageHelper {
    private const val TAG = "ImageHelper"

    fun copyUriToInternal(context: Context, uri: Uri, subFolder: String, fileName: String): String? {
        return try {
            val dir = File(context.filesDir, subFolder)
            if (!dir.exists()) dir.mkdirs()
            val dest = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            dest.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "gagal copy gambar", e)
            null
        }
    }

    fun loadInto(imageView: ImageView, path: String?, fallbackColor: Int) {
        if (path.isNullOrBlank()) {
            imageView.setBackgroundColor(fallbackColor)
            return
        }
        Glide.with(imageView.context)
            .load(File(path))
            .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    imageView.setBackgroundColor(fallbackColor)
                    return false
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: Target<android.graphics.drawable.Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean
                ): Boolean = false
            })
            .into(imageView)
    }
}
