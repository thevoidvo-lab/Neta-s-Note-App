package com.example.notesapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.notesapp.ui.NoteListActivity
import com.example.notesapp.util.ThemeManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val dark = try {
                ThemeManager.isDarkModeFlow(this@MainActivity).first()
            } catch (e: Exception) {
                true
            }
            ThemeManager.applyTheme(dark)
            startActivity(Intent(this@MainActivity, NoteListActivity::class.java))
            finish()
        }
    }
}
