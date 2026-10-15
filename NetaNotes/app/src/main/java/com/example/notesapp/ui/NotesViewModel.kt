package com.example.notesapp.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.notesapp.data.Note
import com.example.notesapp.data.NoteDatabase
import kotlinx.coroutines.launch

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = NoteDatabase.getInstance(application).noteDao()

    private val _filter = MutableLiveData("Semua")
    private val _query = MutableLiveData("")

    val allNotes: LiveData<List<Note>> = dao.getAll()

    val filteredNotes: LiveData<List<Note>> = MediatorLiveData<List<Note>>().apply {
        var lastAll: List<Note> = emptyList()
        var lastFilter = "Semua"
        var lastQuery = ""

        fun recompute() {
            var list = lastAll
            if (lastQuery.isNotBlank()) {
                list = list.filter {
                    it.title.contains(lastQuery, true) || it.content.contains(lastQuery, true)
                }
            }
            list = when (lastFilter) {
                "Pinned" -> list.filter { it.isPinned }
                "Locked" -> list.filter { it.isLocked }
                else -> list
            }
            value = list
        }

        addSource(allNotes) { lastAll = it; recompute() }
        addSource(_filter) { lastFilter = it; recompute() }
        addSource(_query) { lastQuery = it; recompute() }
    }

    fun setFilter(filter: String) { _filter.value = filter }
    fun setQuery(query: String) { _query.value = query }

    fun insert(note: Note, onDone: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = dao.insert(note)
        onDone(id)
    }

    fun update(note: Note) = viewModelScope.launch {
        dao.update(note.copy(updatedAt = System.currentTimeMillis()))
    }

    fun delete(note: Note) = viewModelScope.launch {
        dao.delete(note)
    }

    fun togglePin(note: Note) = viewModelScope.launch {
        dao.update(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }

    fun toggleLock(note: Note) = viewModelScope.launch {
        dao.update(note.copy(isLocked = !note.isLocked, updatedAt = System.currentTimeMillis()))
    }

    fun duplicate(note: Note) = viewModelScope.launch {
        dao.insert(
            note.copy(
                id = 0,
                title = "${note.title} (copy)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getById(id: Long): Note? = dao.getById(id)

    suspend fun getAllOnce(): List<Note> = dao.getAllOnce()
}
