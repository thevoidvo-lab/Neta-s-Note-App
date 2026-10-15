package com.example.notesapp.ui

import com.example.notesapp.data.NoteElement

class UndoRedoManager(private val maxSize: Int = 100) {

    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    fun pushState(elements: List<NoteElement>) {
        val snapshot = NoteElement.listToJson(elements)
        if (undoStack.isNotEmpty() && undoStack.last() == snapshot) return
        undoStack.addLast(snapshot)
        if (undoStack.size > maxSize) undoStack.removeFirst()
        redoStack.clear()
    }

    fun canUndo() = undoStack.size > 1
    fun canRedo() = redoStack.isNotEmpty()

    fun undo(): List<NoteElement>? {
        if (!canUndo()) return null
        val current = undoStack.removeLast()
        redoStack.addLast(current)
        if (redoStack.size > maxSize) redoStack.removeFirst()
        val previous = undoStack.last()
        return NoteElement.listFromJson(previous)
    }

    fun redo(): List<NoteElement>? {
        if (!canRedo()) return null
        val next = redoStack.removeLast()
        undoStack.addLast(next)
        if (undoStack.size > maxSize) undoStack.removeFirst()
        return NoteElement.listFromJson(next)
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
