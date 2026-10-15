package com.example.notesapp.ui

import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.notesapp.R
import com.example.notesapp.data.Note
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class JsonTransferActivity : AppCompatActivity() {

    private lateinit var viewModel: NotesViewModel
    private lateinit var statusText: TextView

    private val createDocLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) doExport(uri) else finish()
    }

    private val openDocLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) doImport(uri) else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_json_transfer)
        statusText = findViewById(R.id.tvTransferStatus)

        viewModel = ViewModelProvider(this)[NotesViewModel::class.java]

        val mode = intent.getStringExtra("mode") ?: "export"
        if (mode == "export") {
            statusText.text = "Menyiapkan ekspor..."
            createDocLauncher.launch("neta_notes_backup.json")
        } else {
            statusText.text = "Pilih file JSON buat diimpor..."
            openDocLauncher.launch("application/json")
        }
    }

    private fun doExport(uri: Uri) {
        lifecycleScope.launch {
            try {
                val notes = viewModel.getAllOnce()
                val arr = JSONArray()
                for (n in notes) {
                    val o = JSONObject()
                    o.put("title", n.title)
                    o.put("content", n.content)
                    o.put("elements", n.elements)
                    o.put("backgroundPath", n.backgroundPath ?: JSONObject.NULL)
                    o.put("fontFamily", n.fontFamily ?: JSONObject.NULL)
                    o.put("fontSize", n.fontSize ?: JSONObject.NULL)
                    o.put("color", n.color ?: JSONObject.NULL)
                    o.put("isPinned", n.isPinned)
                    o.put("isLocked", n.isLocked)
                    o.put("category", n.category ?: JSONObject.NULL)
                    o.put("createdAt", n.createdAt)
                    o.put("updatedAt", n.updatedAt)
                    arr.put(o)
                }
                contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(arr.toString(2).toByteArray())
                }
                statusText.text = "Ekspor selesai (${notes.size} catatan)"
                Toast.makeText(this@JsonTransferActivity, "Ekspor berhasil", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                statusText.text = "Ekspor gagal"
                Toast.makeText(this@JsonTransferActivity, "Ekspor gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                finish()
            }
        }
    }

    private fun doImport(uri: Uri) {
        lifecycleScope.launch {
            try {
                val text = contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: ""
                val arr = JSONArray(text)
                var count = 0
                for (i in 0 until arr.length()) {
                    try {
                        val o = arr.getJSONObject(i)
                        val note = Note(
                            title = o.optString("title", "Tanpa judul"),
                            content = o.optString("content", ""),
                            elements = o.optString("elements", "[]"),
                            backgroundPath = if (o.isNull("backgroundPath")) null else o.optString("backgroundPath"),
                            fontFamily = if (o.isNull("fontFamily")) null else o.optString("fontFamily"),
                            fontSize = if (o.isNull("fontSize")) null else o.optDouble("fontSize").toFloat(),
                            color = if (o.isNull("color")) null else o.optString("color"),
                            isPinned = o.optBoolean("isPinned", false),
                            isLocked = o.optBoolean("isLocked", false),
                            category = if (o.isNull("category")) null else o.optString("category"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                        viewModel.insert(note)
                        count++
                    } catch (inner: Exception) {
                        // satu entri korup, skip, lanjut yang lain
                    }
                }
                statusText.text = "Impor selesai ($count catatan)"
                Toast.makeText(this@JsonTransferActivity, "Impor berhasil", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                statusText.text = "Impor gagal, file JSON tidak valid"
                Toast.makeText(this@JsonTransferActivity, "Impor gagal", Toast.LENGTH_SHORT).show()
            } finally {
                finish()
            }
        }
    }
}
