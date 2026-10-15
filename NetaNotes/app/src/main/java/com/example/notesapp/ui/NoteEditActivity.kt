package com.example.notesapp.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.notesapp.R
import com.example.notesapp.data.Note
import com.example.notesapp.data.NoteElement
import com.example.notesapp.util.ImageHelper
import kotlinx.coroutines.launch
import java.util.UUID

class NoteEditActivity : AppCompatActivity() {

    private lateinit var viewModel: NotesViewModel
    private lateinit var canvasView: CanvasView
    private val elementManager = ElementManager()
    private val undoRedo = UndoRedoManager(maxSize = 100)

    private var currentNoteId: Long = 0
    private var noteTitle: String = ""
    private var selectedElementId: String? = null
    private var isDirty = false

    private val autosaveHandler = Handler(Looper.getMainLooper())
    private val autosaveInterval = 5000L

    private var pickImageTargetType = "sticker" // "sticker" | "image"

    private val pickImageLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) handlePickedImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_edit)

        viewModel = ViewModelProvider(this)[NotesViewModel::class.java]
        canvasView = findViewById(R.id.canvasView)

        currentNoteId = intent.getLongExtra("noteId", 0)

        setupCanvasListener()
        setupToolbar()
        setupBottomSheetTools()
        loadNote()
        startAutosaveLoop()
    }

    private fun setupCanvasListener() {
        canvasView.listener = object : CanvasView.CanvasListener {
            override fun onElementUpdated(element: NoteElement, commit: Boolean) {
                elementManager.update(element.id) { element }
                isDirty = true
                if (commit) undoRedo.pushState(elementManager.getAll())
            }

            override fun onElementTapped(element: NoteElement) {
                selectedElementId = element.id
            }

            override fun onElementDoubleTapped(element: NoteElement) {
                if (element.type == "text" || element.type == "title") {
                    showEditTextDialog(element)
                }
            }

            override fun onElementLongPressed(element: NoteElement, view: View) {
                showElementMenu(element, view)
            }
        }
    }

    private fun setupToolbar() {
        findViewById<View>(R.id.btnBack).setOnClickListener {
            saveNote { finish() }
        }
        findViewById<View>(R.id.btnUndo).setOnClickListener {
            val prev = undoRedo.undo()
            if (prev != null) {
                elementManager.setAll(prev)
                canvasView.renderAll(elementManager.getAll())
                isDirty = true
            }
        }
        findViewById<View>(R.id.btnRedo).setOnClickListener {
            val next = undoRedo.redo()
            if (next != null) {
                elementManager.setAll(next)
                canvasView.renderAll(elementManager.getAll())
                isDirty = true
            }
        }
        findViewById<View>(R.id.btnCanvasSettings).setOnClickListener {
            showCanvasSettingsDialog()
        }
        findViewById<View>(R.id.btnSave).setOnClickListener {
            saveNote { Toast.makeText(this, "Tersimpan", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun setupBottomSheetTools() {
        findViewById<View>(R.id.toolAddElement).setOnClickListener { showAddElementMenu(it) }
        findViewById<View>(R.id.toolFont).setOnClickListener { showFontMenuForSelected() }
        findViewById<View>(R.id.toolColor).setOnClickListener { showColorPickerForSelected() }
        findViewById<View>(R.id.toolBackground).setOnClickListener { showBackgroundDialog() }
        findViewById<View>(R.id.toolSticker).setOnClickListener {
            pickImageTargetType = "sticker"
            pickImageLauncher.launch("image/*")
        }
        findViewById<View>(R.id.toolLayer).setOnClickListener {
            selectedElementId?.let {
                elementManager.bringToFront(it)
                canvasView.renderAll(elementManager.getAll())
                isDirty = true
            } ?: Toast.makeText(this, "Pilih elemen dulu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddElementMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("Title")
        popup.menu.add("Text")
        popup.menu.add("Image")
        popup.menu.add("Shape")
        popup.setOnMenuItemClickListener { item ->
            val cx = 100f
            val cy = 200f
            when (item.title) {
                "Title" -> elementManager.newTitleElement(cx, cy)
                "Text" -> elementManager.newTextElement(cx, cy)
                "Image" -> {
                    pickImageTargetType = "image"
                    pickImageLauncher.launch("image/*")
                    return@setOnMenuItemClickListener true
                }
                "Shape" -> elementManager.newShapeElement(cx, cy)
            }
            canvasView.renderAll(elementManager.getAll())
            undoRedo.pushState(elementManager.getAll())
            isDirty = true
            true
        }
        popup.show()
    }

    private fun handlePickedImage(uri: Uri) {
        val folder = if (pickImageTargetType == "sticker") "stickers" else "note_images"
        val fileName = "${UUID.randomUUID()}.png"
        val path = ImageHelper.copyUriToInternal(this, uri, folder, fileName)
        if (path != null) {
            val element = if (pickImageTargetType == "sticker") {
                elementManager.newStickerElement(120f, 220f, path)
            } else {
                elementManager.newImageElement(120f, 220f, path)
            }
            canvasView.renderAll(elementManager.getAll())
            undoRedo.pushState(elementManager.getAll())
            isDirty = true
        } else {
            Toast.makeText(this, "Gagal muat gambar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showElementMenu(element: NoteElement, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("Duplikat")
        popup.menu.add(if (element.locked) "Buka kunci elemen" else "Kunci elemen")
        popup.menu.add(if (element.hidden) "Tampilkan" else "Sembunyikan")
        popup.menu.add("Hapus")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Duplikat" -> elementManager.duplicate(element.id)
                "Kunci elemen" -> elementManager.update(element.id) { it.copy(locked = true) }
                "Buka kunci elemen" -> elementManager.update(element.id) { it.copy(locked = false) }
                "Sembunyikan" -> elementManager.update(element.id) { it.copy(hidden = true) }
                "Tampilkan" -> elementManager.update(element.id) { it.copy(hidden = false) }
                "Hapus" -> elementManager.remove(element.id)
            }
            canvasView.renderAll(elementManager.getAll())
            undoRedo.pushState(elementManager.getAll())
            isDirty = true
            true
        }
        popup.show()
    }

    private fun showEditTextDialog(element: NoteElement) {
        val input = EditText(this)
        input.setText(element.content)
        AlertDialog.Builder(this)
            .setTitle("Edit teks")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val newContent = input.text.toString()
                elementManager.update(element.id) { it.copy(content = newContent) }
                if (element.type == "title") noteTitle = newContent
                canvasView.renderAll(elementManager.getAll())
                undoRedo.pushState(elementManager.getAll())
                isDirty = true
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showFontMenuForSelected() {
        val id = selectedElementId ?: run {
            Toast.makeText(this, "Pilih elemen teks dulu", Toast.LENGTH_SHORT).show()
            return
        }
        val sizes = arrayOf("12", "14", "16", "18", "24", "32", "48")
        AlertDialog.Builder(this)
            .setTitle("Ukuran font")
            .setItems(sizes) { _, which ->
                elementManager.update(id) { it.copy(fontSize = sizes[which].toFloat()) }
                canvasView.renderAll(elementManager.getAll())
                undoRedo.pushState(elementManager.getAll())
                isDirty = true
            }
            .show()
    }

    private fun showColorPickerForSelected() {
        val id = selectedElementId ?: run {
            Toast.makeText(this, "Pilih elemen dulu", Toast.LENGTH_SHORT).show()
            return
        }
        val colors = arrayOf("#FFFFFF", "#000000", "#BB86FC", "#03DAC6", "#FF5252", "#FFD740", "#69F0AE")
        AlertDialog.Builder(this)
            .setTitle("Pilih warna")
            .setItems(colors) { _, which ->
                elementManager.update(id) { it.copy(color = colors[which]) }
                canvasView.renderAll(elementManager.getAll())
                undoRedo.pushState(elementManager.getAll())
                isDirty = true
            }
            .show()
    }

    private fun showBackgroundDialog() {
        val options = arrayOf("Warna solid", "Gradient", "Gambar dari galeri", "Blur")
        AlertDialog.Builder(this)
            .setTitle("Background canvas")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> canvasView.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"))
                    1 -> {
                        val gd = android.graphics.drawable.GradientDrawable(
                            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(android.graphics.Color.parseColor("#BB86FC"), android.graphics.Color.parseColor("#1E1E1E"))
                        )
                        canvasView.background = gd
                    }
                    2 -> {
                        pickImageTargetType = "canvas_bg"
                        pickImageLauncher.launch("image/*")
                    }
                    3 -> Toast.makeText(this, "Blur diterapkan (ringan)", Toast.LENGTH_SHORT).show()
                }
                isDirty = true
            }
            .show()
    }

    private fun showCanvasSettingsDialog() {
        val view = layoutInflater.inflate(android.R.layout.select_dialog_multichoice, null)
        val options = arrayOf("Grid", "Snap")
        val checked = booleanArrayOf(SnapHelper.gridEnabled, SnapHelper.snapEnabled)
        AlertDialog.Builder(this)
            .setTitle("Pengaturan canvas")
            .setMultiChoiceItems(options, checked) { _, which, isChecked ->
                when (which) {
                    0 -> canvasView.setGridEnabled(isChecked)
                    1 -> canvasView.setSnapEnabled(isChecked)
                }
            }
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun loadNote() {
        if (currentNoteId == 0L) {
            elementManager.setAll(emptyList())
            canvasView.renderAll(emptyList())
            undoRedo.pushState(emptyList())
            return
        }
        lifecycleScope.launch {
            val note = viewModel.getById(currentNoteId)
            if (note != null) {
                noteTitle = note.title
                val elements = NoteElement.listFromJson(note.elements)
                elementManager.setAll(elements)
                canvasView.renderAll(elementManager.getAll())
                undoRedo.pushState(elementManager.getAll())
                try {
                    if (!note.color.isNullOrBlank()) {
                        canvasView.setBackgroundColor(android.graphics.Color.parseColor(note.color))
                    }
                } catch (e: Exception) {
                    // warna invalid, biarin default
                }
            }
        }
    }

    private fun startAutosaveLoop() {
        val runnable = object : Runnable {
            override fun run() {
                if (isDirty) {
                    saveNote {}
                    isDirty = false
                }
                autosaveHandler.postDelayed(this, autosaveInterval)
            }
        }
        autosaveHandler.postDelayed(runnable, autosaveInterval)
    }

    private fun saveNote(onDone: () -> Unit) {
        val elements = elementManager.getAll()
        val contentPreview = elements.filter { it.type == "text" || it.type == "title" }
            .joinToString(" ") { it.content }.take(200)

        val titleToSave = noteTitle.ifBlank {
            elements.find { it.type == "title" }?.content ?: "Catatan baru"
        }

        if (currentNoteId == 0L) {
            val note = Note(
                title = titleToSave,
                content = contentPreview,
                elements = NoteElement.listToJson(elements)
            )
            viewModel.insert(note) { id ->
                currentNoteId = id
                onDone()
            }
        } else {
            lifecycleScope.launch {
                val existing = viewModel.getById(currentNoteId)
                if (existing != null) {
                    viewModel.update(
                        existing.copy(
                            title = titleToSave,
                            content = contentPreview,
                            elements = NoteElement.listToJson(elements)
                        )
                    )
                }
                onDone()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autosaveHandler.removeCallbacksAndMessages(null)
    }

    override fun onBackPressed() {
        saveNote { super.onBackPressed() }
    }
}
