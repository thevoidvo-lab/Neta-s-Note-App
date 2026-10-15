package com.example.notesapp.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

object CrashHandler {
    private const val TAG = "NetaNotesCrash"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                Log.e(TAG, sw.toString())

                val logDir = File(appContext.filesDir, "crash_logs")
                if (!logDir.exists()) logDir.mkdirs()
                val logFile = File(logDir, "crash_${System.currentTimeMillis()}.txt")
                logFile.writeText(sw.toString())
            } catch (e: Exception) {
                Log.e(TAG, "gagal nyimpen crash log", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
                exitProcess(1)
            }
        }
    }
}
