package com.example.notesapp.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.R
import com.example.notesapp.data.Note
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private val onClick: (Note) -> Unit,
    private val onPin: (Note) -> Unit,
    private val onLock: (Note) -> Unit,
    private val onDelete: (Note) -> Unit,
    private val onDuplicate: (Note) -> Unit,
    private val onHide: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: android.widget.TextView = view.findViewById(R.id.tvTitle)
        val preview: android.widget.TextView = view.findViewById(R.id.tvPreview)
        val date: android.widget.TextView = view.findViewById(R.id.tvDate)
        val pinIcon: android.widget.ImageView = view.findViewById(R.id.ivPin)
        val lockIcon: android.widget.ImageView = view.findViewById(R.id.ivLock)
        val lockOverlay: View = view.findViewById(R.id.lockOverlay)
        val card: View = view.findViewById(R.id.cardRoot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val note = getItem(position)

        holder.title.text = note.title.ifBlank { "(Tanpa judul)" }

        val previewText = note.content.ifBlank { "Tidak ada isi" }
        holder.preview.text = previewText

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.date.text = sdf.format(Date(note.updatedAt))

        holder.pinIcon.visibility = if (note.isPinned) View.VISIBLE else View.GONE
        holder.lockIcon.visibility = if (note.isLocked) View.VISIBLE else View.GONE
        holder.lockOverlay.visibility = if (note.isLocked) View.VISIBLE else View.GONE

        try {
            if (!note.color.isNullOrBlank()) {
                holder.card.setBackgroundColor(Color.parseColor(note.color))
            }
        } catch (e: Exception) {
            // warna invalid, biarin default dari layout
        }

        holder.itemView.setOnClickListener {
            if (!note.isLocked) onClick(note) else onClick(note) // buka tetep lewat PIN check di activity
        }

        holder.itemView.setOnLongClickListener {
            val popup = PopupMenu(holder.itemView.context, holder.itemView)
            popup.menu.add(0, 1, 0, if (note.isPinned) "Unpin" else "Pin")
            popup.menu.add(0, 2, 1, if (note.isLocked) "Buka kunci" else "Kunci")
            popup.menu.add(0, 3, 2, "Duplikat")
            popup.menu.add(0, 4, 3, "Sembunyikan")
            popup.menu.add(0, 5, 4, "Hapus")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> onPin(note)
                    2 -> onLock(note)
                    3 -> onDuplicate(note)
                    4 -> onHide(note)
                    5 -> onDelete(note)
                }
                true
            }
            popup.show()
            true
        }
    }
}
