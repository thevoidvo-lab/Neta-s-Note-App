package com.example.notesapp.data

import org.json.JSONArray
import org.json.JSONObject

data class NoteElement(
    val id: String,
    val type: String, // "title" | "text" | "sticker" | "image" | "shape"
    val content: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val zIndex: Int = 0,
    val fontFamily: String? = null,
    val fontSize: Float? = 16f,
    val color: String? = "#FFFFFF",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val alignment: String = "left",
    val locked: Boolean = false,
    val hidden: Boolean = false
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("type", type)
        o.put("content", content)
        o.put("x", x)
        o.put("y", y)
        o.put("width", width)
        o.put("height", height)
        o.put("rotation", rotation)
        o.put("scale", scale)
        o.put("zIndex", zIndex)
        o.put("fontFamily", fontFamily ?: JSONObject.NULL)
        o.put("fontSize", fontSize ?: 16f)
        o.put("color", color ?: "#FFFFFF")
        o.put("bold", bold)
        o.put("italic", italic)
        o.put("underline", underline)
        o.put("alignment", alignment)
        o.put("locked", locked)
        o.put("hidden", hidden)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): NoteElement {
            return try {
                NoteElement(
                    id = o.optString("id", System.currentTimeMillis().toString()),
                    type = o.optString("type", "text"),
                    content = o.optString("content", ""),
                    x = o.optDouble("x", 0.0).toFloat(),
                    y = o.optDouble("y", 0.0).toFloat(),
                    width = o.optDouble("width", 200.0).toFloat(),
                    height = o.optDouble("height", 100.0).toFloat(),
                    rotation = o.optDouble("rotation", 0.0).toFloat(),
                    scale = o.optDouble("scale", 1.0).toFloat(),
                    zIndex = o.optInt("zIndex", 0),
                    fontFamily = if (o.isNull("fontFamily")) null else o.optString("fontFamily"),
                    fontSize = o.optDouble("fontSize", 16.0).toFloat(),
                    color = o.optString("color", "#FFFFFF"),
                    bold = o.optBoolean("bold", false),
                    italic = o.optBoolean("italic", false),
                    underline = o.optBoolean("underline", false),
                    alignment = o.optString("alignment", "left"),
                    locked = o.optBoolean("locked", false),
                    hidden = o.optBoolean("hidden", false)
                )
            } catch (e: Exception) {
                NoteElement(
                    id = System.currentTimeMillis().toString(),
                    type = "text", content = "", x = 0f, y = 0f, width = 200f, height = 100f
                )
            }
        }

        fun listToJson(list: List<NoteElement>): String {
            val arr = JSONArray()
            for (e in list) arr.put(e.toJson())
            return arr.toString()
        }

        fun listFromJson(json: String?): MutableList<NoteElement> {
            val result = mutableListOf<NoteElement>()
            if (json.isNullOrBlank()) return result
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    result.add(fromJson(arr.getJSONObject(i)))
                }
            } catch (e: Exception) {
                // JSON corrupt -> fallback elemen kosong
            }
            return result
        }
    }
}
