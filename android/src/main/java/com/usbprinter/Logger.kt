package com.usbprinter

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {
    private const val FILE_NAME = "usb_printer_errors.log"
    private const val TAG = "UsbPrinterLogger"

    @Synchronized
    fun logError(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }

        try {
            val logFile = File(context.filesDir, FILE_NAME)
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            
            FileWriter(logFile, true).use { writer ->
                writer.write("[$dateStr] [$tag] ERROR: $message\n")
                if (throwable != null) {
                    val pw = PrintWriter(writer)
                    throwable.printStackTrace(pw)
                    writer.write("\n")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write error to log file", e)
        }
    }

    @Synchronized
    fun getErrorLogs(context: Context): String {
        return try {
            val logFile = File(context.filesDir, FILE_NAME)
            if (!logFile.exists()) {
                return ""
            }
            logFile.readText()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read error logs", e)
            "Erro ao ler os logs: ${e.message}"
        }
    }

    @Synchronized
    fun clearErrorLogs(context: Context): Boolean {
        return try {
            val logFile = File(context.filesDir, FILE_NAME)
            if (logFile.exists()) {
                logFile.delete()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear error logs", e)
            false
        }
    }
}
