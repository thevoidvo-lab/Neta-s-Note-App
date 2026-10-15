package com.example.notesapp.ui

import com.example.notesapp.data.NoteElement
import java.util.UUID

class ElementManager {

    private val elements = mutableListOf<NoteElement>()
    private var nextZ = 0

    fun setAll(list: List<NoteElement>) {
        elements.clear()
        elements.addAll(list)
        nextZ = (list.maxOfOrNull { it.zIndex } ?: 0) + 1
    }

    fun getAll(): List<NoteElement> = elements.toList()

    fun add(element: NoteElement): NoteElement {
        val withZ = element.copy(zIndex = nextZ++)
        elements.add(withZ)
        return withZ
    }

    fun newTextElement(x: Float, y: Float, content: String = "Teks baru"): NoteElement {
        return add(
            NoteElement(
                id = UUID.randomUUID().toString(),
                type = "text",
                content = content,
                x = x, y = y, width = 220f, height = 80f
            )
        )
    }

    fun newTitleElement(x: Float, y: Float): NoteElement {
        return add(
            NoteElement(
                id = UUID.randomUUID().toString(),
                type = "title",
                content = "Judul",
                x = x, y = y, width = 260f, height = 60f,
                fontSize = 24f, bold = true
            )
        )
    }

    fun newStickerElement(x: Float, y: Float, path: String): NoteElement {
        return add(
            NoteElement(
                id = UUID.randomUUID().toString(),
                type = "sticker",
                content = path,
                x = x, y = y, width = 120f, height = 120f
            )
        )
    }

    fun newImageElement(x: Float, y: Float, path: String): NoteElement {
        return add(
            NoteElement(
                id = UUID.randomUUID().toString(),
                type = "image",
                content = path,
                x = x, y = y, width = 200f, height = 200f
            )
        )
    }

    fun newShapeElement(x: Float, y: Float, shapeType: String = "rect"): NoteElement {
        return add(
            NoteElement(
                id = UUID.randomUUID().toString(),
                type = "shape",
                content = shapeType,
                x = x, y = y, width = 150f, height = 150f
            )
        )
    }

    fun update(id: String, transform: (NoteElement) -> NoteElement) {
        val idx = elements.indexOfFirst { it.id == id }
        if (idx != -1) elements[idx] = transform(elements[idx])
    }

    fun remove(id: String) {
        elements.removeAll { it.id == id }
    }

    fun duplicate(id: String): NoteElement? {
        val original = elements.find { it.id == id } ?: return null
        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            x = original.x + 24f,
            y = original.y + 24f,
            zIndex = nextZ++
        )
        elements.add(copy)
        return copy
    }

    fun bringToFront(id: String) {
        update(id) { it.copy(zIndex = nextZ++) }
    }
}
