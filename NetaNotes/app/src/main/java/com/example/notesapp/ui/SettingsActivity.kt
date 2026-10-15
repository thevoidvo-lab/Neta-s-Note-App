package com.example.notesapp.ui

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.notesapp.R
import com.example.notesapp.util.FontManager
import com.example.notesapp.util.HomeCustomizer
import com.example.notesapp.util.ImageHelper
import com.example.notesapp.util.PinManager
import com.example.notesapp.util.ThemeManager
import kotlinx.coroutines.launch
import java.util.UUID

class SettingsActivity : AppCompatActivity() {

    private var pendingAction = ""

    private val pickImageLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) handlePickedBackgroundImage(uri)
    }

    private val pickFontLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) handlePickedFont(uri)
    }

    private val pickStickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) handlePickedSticker(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        setupThemeCard()
        setupHomeTitleCard()
        setupHomeBackgroundCard()
        setupHomeFontCard()
        setupHomeStickerCard()
        setupPinCard()
        setupIconCard()
        setupExportCard()
        setupImportCard()
        setupAboutCard()
    }

    private fun setupThemeCard() {
        findViewById<android.view.View>(R.id.cardTheme).setOnClickListener {
            val options = arrayOf("Dark", "Light")
            AlertDialog.Builder(this)
                .setTitle("Pilih tema")
                .setItems(options) { _, which ->
                    lifecycleScope.launch {
                        ThemeManager.setDarkMode(this@SettingsActivity, which == 0)
                        Toast.makeText(this@SettingsActivity, "Tema diubah, buka ulang app", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }
    }

    private fun setupHomeTitleCard() {
        findViewById<android.view.View>(R.id.cardHomeTitle).setOnClickListener {
            val input = EditText(this)
            input.hint = "Max 30 karakter"
            input.filters = arrayOf(android.text.InputFilter.LengthFilter(30))
            AlertDialog.Builder(this)
                .setTitle("Judul Home")
                .setView(input)
                .setPositiveButton("Simpan") { _, _ ->
                    lifecycleScope.launch {
                        HomeCustomizer.setTitle(this@SettingsActivity, input.text.toString())
                        Toast.makeText(this@SettingsActivity, "Judul diperbarui", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun setupHomeBackgroundCard() {
        findViewById<android.view.View>(R.id.cardHomeBg).setOnClickListener {
            val options = arrayOf("Warna solid", "Gradient", "Gambar", "Blur")
            AlertDialog.Builder(this)
                .setTitle("Background Home")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showSolidColorPicker()
                        1 -> lifecycleScope.launch {
                            HomeCustomizer.setBackground(this@SettingsActivity, "#BB86FC,#121212", "gradient")
                            Toast.makeText(this@SettingsActivity, "Gradient diterapkan", Toast.LENGTH_SHORT).show()
                        }
                        2 -> {
                            pendingAction = "bg_image"
                            pickImageLauncher.launch("image/*")
                        }
                        3 -> lifecycleScope.launch {
                            HomeCustomizer.setBackground(this@SettingsActivity, "", "blur")
                            Toast.makeText(this@SettingsActivity, "Blur diterapkan", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .show()
        }
    }

    private fun showSolidColorPicker() {
        val colors = arrayOf("#121212", "#1E1E1E", "#000000", "#2C2C54", "#302B63")
        AlertDialog.Builder(this)
            .setTitle("Pilih warna")
            .setItems(colors) { _, which ->
                lifecycleScope.launch {
                    HomeCustomizer.setBackground(this@SettingsActivity, colors[which], "solid")
                }
            }
            .show()
    }

    private fun handlePickedBackgroundImage(uri: Uri) {
        val fileName = "${UUID.randomUUID()}.png"
        val path = ImageHelper.copyUriToInternal(this, uri, "home_backgrounds", fileName)
        if (path != null) {
            lifecycleScope.launch {
                HomeCustomizer.setBackground(this@SettingsActivity, path, "image")
                Toast.makeText(this@SettingsActivity, "Background diterapkan", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Gagal upload gambar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupHomeFontCard() {
        findViewById<android.view.View>(R.id.cardHomeFont).setOnClickListener {
            pickFontLauncher.launch("*/*")
        }
    }

    private fun handlePickedFont(uri: Uri) {
        try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                val fileName = "${UUID.randomUUID()}.ttf"
                val path = FontManager.copyFontToInternal(this, bytes, fileName)
                if (path != null) {
                    lifecycleScope.launch {
                        HomeCustomizer.setFont(this@SettingsActivity, path)
                        Toast.makeText(this@SettingsActivity, "Font diterapkan", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Gagal simpan font, pakai default", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Font gagal dimuat, pakai default", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupHomeStickerCard() {
        findViewById<android.view.View>(R.id.cardHomeSticker).setOnClickListener {
            pickStickerLauncher.launch("image/*")
        }
    }

    private fun handlePickedSticker(uri: Uri) {
        val fileName = "${UUID.randomUUID()}.png"
        val path = ImageHelper.copyUriToInternal(this, uri, "home_stickers", fileName)
        if (path != null) {
            lifecycleScope.launch {
                val current = HomeCustomizer.stickersFlow(this@SettingsActivity)
                val list = mutableListOf<HomeCustomizer.StickerPos>()
                list.add(HomeCustomizer.StickerPos(path, 50f, 50f, 1f, 0f))
                HomeCustomizer.setStickers(this@SettingsActivity, list)
                Toast.makeText(this@SettingsActivity, "Sticker ditambahkan ke Home", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Gagal upload sticker", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPinCard() {
        findViewById<android.view.View>(R.id.cardPin).setOnClickListener {
            val input = EditText(this)
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            input.hint = "PIN baru (4-6 digit)"
            AlertDialog.Builder(this)
                .setTitle("Ganti PIN")
                .setView(input)
                .setPositiveButton("Simpan") { _, _ ->
                    val pin = input.text.toString()
                    if (pin.length in 4..6) {
                        PinManager.setPin(this, pin)
                        Toast.makeText(this, "PIN disimpan", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "PIN harus 4-6 digit", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun setupIconCard() {
        findViewById<android.view.View>(R.id.cardIcon).setOnClickListener {
            val icons = arrayOf("Default", "Dark", "Pastel", "Neon", "Minimal", "Gold")
            val aliases = arrayOf("IconDefault", "IconDark", "IconPastel", "IconNeon", "IconMinimal", "IconGold")
            AlertDialog.Builder(this)
                .setTitle("Pilih ikon aplikasi")
                .setItems(icons) { _, which ->
                    applyIcon(aliases[which], aliases)
                }
                .show()
        }
    }

    private fun applyIcon(selectedAlias: String, allAliases: Array<String>) {
        try {
            val pm = packageManager
            for (alias in allAliases) {
                val state = if (alias == selectedAlias) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                pm.setComponentEnabledSetting(
                    ComponentName(packageName, "com.example.notesapp.$alias"),
                    state,
                    PackageManager.DONT_KILL_APP
                )
            }
            lifecycleScope.launch {
                HomeCustomizer.setIcon(this@SettingsActivity, selectedAlias)
            }
            Toast.makeText(this, "Ikon diubah ke $selectedAlias", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal ganti ikon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupExportCard() {
        findViewById<android.view.View>(R.id.cardExport).setOnClickListener {
            startActivity(Intent(this, JsonTransferActivity::class.java).putExtra("mode", "export"))
        }
    }

    private fun setupImportCard() {
        findViewById<android.view.View>(R.id.cardImport).setOnClickListener {
            startActivity(Intent(this, JsonTransferActivity::class.java).putExtra("mode", "import"))
        }
    }

    private fun setupAboutCard() {
        findViewById<TextView>(R.id.tvVersion).text = "Versi 1.0"
    }
}
