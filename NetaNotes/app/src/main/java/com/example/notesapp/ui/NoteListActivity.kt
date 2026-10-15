package com.example.notesapp.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.notesapp.R
import com.example.notesapp.data.Note
import com.example.notesapp.util.HomeCustomizer
import com.example.notesapp.util.PinManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File

class NoteListActivity : AppCompatActivity() {

    private lateinit var viewModel: NotesViewModel
    private lateinit var adapter: NoteAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var emptyState: View
    private lateinit var chipSemua: TextView
    private lateinit var chipPinned: TextView
    private lateinit var chipLocked: TextView
    private lateinit var chipKategori: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_list)

        viewModel = ViewModelProvider(this)[NotesViewModel::class.java]

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        recyclerView = findViewById(R.id.recyclerNotes)
        emptyState = findViewById(R.id.emptyState)
        chipSemua = findViewById(R.id.chipSemua)
        chipPinned = findViewById(R.id.chipPinned)
        chipLocked = findViewById(R.id.chipLocked)
        chipKategori = findViewById(R.id.chipKategori)

        setupRecycler()
        setupChips()
        setupSearch()
        setupFab()
        setupMenu()
        setupBottomNav()
        observeHomeCustomization()
        observeNotes()
    }

    private fun setupRecycler() {
        adapter = NoteAdapter(
            onClick = { note -> openNote(note) },
            onPin = { note -> viewModel.togglePin(note) },
            onLock = { note -> viewModel.toggleLock(note) },
            onDelete = { note -> confirmDelete(note) },
            onDuplicate = { note -> viewModel.duplicate(note) },
            onHide = { note -> Toast.makeText(this, "Disembunyikan", Toast.LENGTH_SHORT).show() }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val note = adapter.currentList[viewHolder.adapterPosition]
                if (direction == ItemTouchHelper.LEFT) {
                    viewModel.delete(note)
                } else {
                    viewModel.togglePin(note)
                    adapter.notifyItemChanged(viewHolder.adapterPosition)
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun setupChips() {
        val chips = listOf(chipSemua, chipPinned, chipLocked, chipKategori)
        fun select(selected: TextView, filterName: String) {
            chips.forEach { it.isSelected = false }
            selected.isSelected = true
            viewModel.setFilter(filterName)
        }
        chipSemua.setOnClickListener { select(chipSemua, "Semua") }
        chipPinned.setOnClickListener { select(chipPinned, "Pinned") }
        chipLocked.setOnClickListener { select(chipLocked, "Locked") }
        chipKategori.setOnClickListener {
            Toast.makeText(this, "Pilih kategori dari catatan masing-masing", Toast.LENGTH_SHORT).show()
        }
        chipSemua.isSelected = true
    }

    private fun setupSearch() {
        val searchBar = findViewById<EditText>(R.id.etSearch)
        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.setQuery(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupFab() {
        findViewById<View>(R.id.fabAdd).setOnClickListener {
            val intent = Intent(this, NoteEditActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupMenu() {
        findViewById<View>(R.id.btnMenu).setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("Pengaturan")
            popup.menu.add("Ekspor semua")
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Pengaturan" -> startActivity(Intent(this, SettingsActivity::class.java))
                    "Ekspor semua" -> startActivity(Intent(this, JsonTransferActivity::class.java).putExtra("mode", "export"))
                }
                true
            }
            popup.show()
        }
        findViewById<View>(R.id.btnSearchIcon).setOnClickListener {
            val searchBar = findViewById<EditText>(R.id.etSearch)
            searchBar.requestFocus()
        }
    }

    private fun setupBottomNav() {
        findViewById<View>(R.id.navNotes).setOnClickListener { }
        findViewById<View>(R.id.navSearch).setOnClickListener {
            findViewById<EditText>(R.id.etSearch).requestFocus()
        }
        findViewById<View>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<View>(R.id.navProfile).setOnClickListener {
            Toast.makeText(this, "Profile belum tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeHomeCustomization() {
        lifecycleScope.launch {
            try {
                HomeCustomizer.titleFlow(this@NoteListActivity).collect { title ->
                    tvHeaderTitle.text = title
                }
            } catch (e: Exception) {
                tvHeaderTitle.text = HomeCustomizer.DEFAULT_TITLE
            }
        }

        lifecycleScope.launch {
            try {
                HomeCustomizer.backgroundFlow(this@NoteListActivity).collect { (value, type) ->
                    val root = findViewById<View>(R.id.rootHome)
                    when (type) {
                        "solid" -> {
                            try {
                                root.setBackgroundColor(Color.parseColor(value ?: "#121212"))
                            } catch (e: Exception) {
                                root.setBackgroundColor(Color.parseColor("#121212"))
                            }
                        }
                        "image" -> {
                            if (!value.isNullOrBlank() && File(value).exists()) {
                                Glide.with(this@NoteListActivity).load(File(value)).into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                                    override fun onResourceReady(resource: android.graphics.drawable.Drawable, transition: com.bumptech.glide.request.transition.Transition<in android.graphics.drawable.Drawable>?) {
                                        root.background = resource
                                    }
                                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                                    override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                                        root.setBackgroundColor(Color.parseColor("#121212"))
                                    }
                                })
                            }
                        }
                        else -> root.setBackgroundColor(Color.parseColor("#121212"))
                    }
                }
            } catch (e: Exception) {
                // gagal apply background -> biarin default dari theme
            }
        }
    }

    private fun observeNotes() {
        viewModel.filteredNotes.observe(this) { notes ->
            adapter.submitList(notes)
            emptyState.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (notes.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun openNote(note: Note) {
        if (note.isLocked) {
            showPinDialog(note)
        } else {
            startActivity(Intent(this, NoteEditActivity::class.java).putExtra("noteId", note.id))
        }
    }

    private fun showPinDialog(note: Note) {
        if (!PinManager.isPinSet(this)) {
            Toast.makeText(this, "PIN belum diset, buka Pengaturan dulu", Toast.LENGTH_SHORT).show()
            return
        }
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input.hint = "Masukkan PIN"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Catatan terkunci")
            .setView(input)
            .setPositiveButton("Buka", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = input.text.toString()
                if (PinManager.verifyPin(this, pin)) {
                    dialog.dismiss()
                    startActivity(Intent(this, NoteEditActivity::class.java).putExtra("noteId", note.id))
                } else {
                    val fails = PinManager.getFailCount(this)
                    if (fails >= PinManager.MAX_ATTEMPTS) {
                        Toast.makeText(this, "PIN salah 5x, ditutup", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "PIN salah (${fails}/${PinManager.MAX_ATTEMPTS})", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun confirmDelete(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Hapus catatan?")
            .setMessage("Catatan ini bakal dihapus permanen.")
            .setPositiveButton("Hapus") { _, _ -> viewModel.delete(note) }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // refresh kalau ada perubahan dari Settings (title/bg) sambil balik ke home
    }
}
