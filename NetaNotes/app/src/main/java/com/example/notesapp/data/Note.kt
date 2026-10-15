package com.example.notesapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val elements: String = "[]", // JSON array of NoteElement
    val backgroundPath: String? = null,
    val fontFamily: String? = null,
    val fontSize: Float? = null,
    val color: String? = null,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val category: String? = null,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val rotation: Float = 0f,
    val zIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
