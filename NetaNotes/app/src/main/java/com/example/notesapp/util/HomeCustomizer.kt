package com.example.notesapp.util

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray

object HomeCustomizer {
    private val KEY_TITLE = stringPreferencesKey("home_title")
    private val KEY_BG = stringPreferencesKey("home_bg")
    private val KEY_BG_TYPE = stringPreferencesKey("home_bg_type") // solid|gradient|image|blur
    private val KEY_FONT = stringPreferencesKey("home_font")
    private val KEY_STICKERS = stringPreferencesKey("home_stickers")
    private val KEY_ICON = stringPreferencesKey("app_icon")

    const val DEFAULT_TITLE = "Neta's Notes"

    fun titleFlow(context: Context): Flow<String> =
        context.dataStore.data
            .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
            .map { it[KEY_TITLE]?.takeIf { t -> t.isNotBlank() } ?: DEFAULT_TITLE }

    suspend fun setTitle(context: Context, title: String) {
        val safe = if (title.isBlank()) DEFAULT_TITLE else title.take(30)
        context.dataStore.edit { it[KEY_TITLE] = safe }
    }

    fun backgroundFlow(context: Context): Flow<Pair<String?, String>> =
        context.dataStore.data
            .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
            .map { prefs -> Pair(prefs[KEY_BG], prefs[KEY_BG_TYPE] ?: "solid") }

    suspend fun setBackground(context: Context, value: String, type: String) {
        context.dataStore.edit {
            it[KEY_BG] = value
            it[KEY_BG_TYPE] = type
        }
    }

    fun fontFlow(context: Context): Flow<String?> =
        context.dataStore.data
            .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
            .map { it[KEY_FONT] }

    suspend fun setFont(context: Context, path: String) {
        context.dataStore.edit { it[KEY_FONT] = path }
    }

    fun iconFlow(context: Context): Flow<String> =
        context.dataStore.data
            .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
            .map { it[KEY_ICON] ?: "IconDefault" }

    suspend fun setIcon(context: Context, aliasName: String) {
        context.dataStore.edit { it[KEY_ICON] = aliasName }
    }

    data class StickerPos(val path: String, val x: Float, val y: Float, val scale: Float, val rotation: Float)

    fun stickersFlow(context: Context): Flow<List<StickerPos>> =
        context.dataStore.data
            .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
            .map { prefs -> parseStickers(prefs[KEY_STICKERS]) }

    private fun parseStickers(json: String?): List<StickerPos> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<StickerPos>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    StickerPos(
                        path = o.optString("path"),
                        x = o.optDouble("x", 0.0).toFloat(),
                        y = o.optDouble("y", 0.0).toFloat(),
                        scale = o.optDouble("scale", 1.0).toFloat(),
                        rotation = o.optDouble("rotation", 0.0).toFloat()
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun setStickers(context: Context, stickers: List<StickerPos>) {
        val arr = JSONArray()
        for (s in stickers) {
            val o = org.json.JSONObject()
            o.put("path", s.path)
            o.put("x", s.x)
            o.put("y", s.y)
            o.put("scale", s.scale)
            o.put("rotation", s.rotation)
            arr.put(o)
        }
        context.dataStore.edit { it[KEY_STICKERS] = arr.toString() }
    }

    fun stickersDir(context: Context): java.io.File {
        val dir = java.io.File(context.filesDir, "home_stickers")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun backgroundsDir(context: Context): java.io.File {
        val dir = java.io.File(context.filesDir, "home_backgrounds")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
