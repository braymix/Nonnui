package com.easyhome.senior.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.easyhome.senior.domain.model.PickedContact

/** Accesso alla rubrica: lettura del contatto scelto e ricerca delle righe WhatsApp. */
class ContactResolver(private val context: Context) {

    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /** Legge il numero scelto con ACTION_PICK (permesso temporaneo concesso sull'URI). */
    fun readPickedPhone(uri: Uri): PickedContact? = try {
        context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null,
            null,
            null,
        )?.use { c ->
            if (!c.moveToFirst()) return@use null
            val number = c.getString(2)?.trim().orEmpty()
            if (number.isEmpty()) return@use null
            PickedContact(
                contactId = c.getLong(0),
                name = c.getString(1)?.trim().orEmpty().ifEmpty { number },
                phoneNumber = number,
            )
        }
    } catch (_: SecurityException) {
        null
    }

    /** ID della riga Data "videochiamata WhatsApp" per un contatto, se esiste. */
    fun findWhatsAppVideoDataId(contactId: Long): Long? {
        if (!hasContactsPermission()) return null
        return try {
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(contactId.toString(), WHATSAPP_VIDEO_MIME),
                null,
            )?.use { c -> if (c.moveToFirst()) c.getLong(0) else null }
        } catch (_: SecurityException) {
            null
        }
    }

    /** Usato dopo un import da un altro telefono: ritrova la riga WhatsApp partendo dal numero. */
    fun findWhatsAppVideoDataIdByNumber(phoneNumber: String): Long? {
        if (!hasContactsPermission() || phoneNumber.isBlank()) return null
        return try {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber),
            )
            val contactIds = mutableListOf<Long>()
            context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null,
            )?.use { c -> while (c.moveToNext()) contactIds += c.getLong(0) }
            contactIds.firstNotNullOfOrNull { findWhatsAppVideoDataId(it) }
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** Verifica che l'ID salvato punti ancora ad una riga WhatsApp video valida. */
    fun isWhatsAppDataRowValid(dataId: Long): Boolean {
        if (!hasContactsPermission()) return true // Non verificabile: si tenta comunque.
        return try {
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                "${ContactsContract.Data._ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(dataId.toString(), WHATSAPP_VIDEO_MIME),
                null,
            )?.use { it.count > 0 } ?: false
        } catch (_: SecurityException) {
            true
        }
    }

    companion object {
        const val WHATSAPP_PACKAGE = "com.whatsapp"
        const val WHATSAPP_VIDEO_MIME = "vnd.android.cursor.item/vnd.com.whatsapp.video.call"
    }
}
