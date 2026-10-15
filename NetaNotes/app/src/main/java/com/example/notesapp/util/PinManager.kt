package com.example.notesapp.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

object PinManager {
    private const val PREFS_NAME = "secure_pin_prefs"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_FAIL_COUNT = "fail_count"
    const val MAX_ATTEMPTS = 5

    private fun getPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // fallback ke prefs biasa kalau EncryptedSharedPreferences gagal (jarang terjadi)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun hash(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isPinSet(context: Context): Boolean {
        return getPrefs(context).contains(KEY_PIN_HASH)
    }

    fun setPin(context: Context, pin: String) {
        getPrefs(context).edit()
            .putString(KEY_PIN_HASH, hash(pin))
            .putInt(KEY_FAIL_COUNT, 0)
            .apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = getPrefs(context)
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val match = stored == hash(pin)
        if (match) {
            prefs.edit().putInt(KEY_FAIL_COUNT, 0).apply()
        } else {
            val fails = prefs.getInt(KEY_FAIL_COUNT, 0) + 1
            prefs.edit().putInt(KEY_FAIL_COUNT, fails).apply()
        }
        return match
    }

    fun getFailCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_FAIL_COUNT, 0)
    }

    fun resetFailCount(context: Context) {
        getPrefs(context).edit().putInt(KEY_FAIL_COUNT, 0).apply()
    }
}
