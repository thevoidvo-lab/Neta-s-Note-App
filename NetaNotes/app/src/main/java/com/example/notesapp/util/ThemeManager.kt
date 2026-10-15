package com.example.notesapp.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "neta_settings")

object ThemeManager {
    private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")

    fun isDarkModeFlow(context: Context): Flow<Boolean> {
        return context.dataStore.data
            .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
            .map { it[KEY_DARK_MODE] ?: true }
    }

    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        try {
            context.dataStore.edit { it[KEY_DARK_MODE] = enabled }
            applyTheme(enabled)
        } catch (e: Exception) {
            applyTheme(true) // fallback ke Light gagal apply -> paksa default aman
        }
    }

    fun applyTheme(dark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
