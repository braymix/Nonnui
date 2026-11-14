package com.easyhome.senior.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Condivisione del backup come testo o come file .json tramite ACTION_SEND. */
object BackupSharer {

    private const val FILE_NAME = "easyhome_backup.json"
    private const val MAX_IMPORT_BYTES = 512 * 1024

    fun shareAsText(context: Context, json: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Backup EasyHome Senior")
            putExtra(Intent.EXTRA_TEXT, json)
        }
        context.safeStartActivity(Intent.createChooser(send, "Condividi configurazione"))
    }

    fun shareAsFile(context: Context, json: String): Boolean {
        val dir = File(context.cacheDir, "backup").apply { mkdirs() }
        val file = File(dir, FILE_NAME)
        file.writeText(json, Charsets.UTF_8)
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "Backup EasyHome Senior")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return context.safeStartActivity(Intent.createChooser(send, "Condividi file di backup"))
    }

    /** Legge un file scelto con il selettore documenti; limita la dimensione per sicurezza. */
    fun readText(context: Context, uri: Uri): String {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Impossibile aprire il file selezionato.")
        return stream.use { input ->
            val bytes = input.readBytes()
            if (bytes.size > MAX_IMPORT_BYTES) throw IllegalArgumentException("Il file è troppo grande per essere un backup.")
            String(bytes, Charsets.UTF_8)
        }
    }
}
