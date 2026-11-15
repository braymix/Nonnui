package com.easyhome.senior.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.easyhome.senior.domain.model.ActionType
import com.easyhome.senior.domain.model.PickedContact
import java.util.Locale

/** Accesso alla rubrica: contatto scelto, righe WhatsApp e normalizzazione dei numeri. */
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

    /** ID della riga Data WhatsApp con il [mimeType] indicato per un contatto, se esiste. */
    fun findWhatsAppDataId(contactId: Long, mimeType: String): Long? {
        if (!hasContactsPermission()) return null
        return try {
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(contactId.toString(), mimeType),
                null,
            )?.use { c -> if (c.moveToFirst()) c.getLong(0) else null }
        } catch (_: SecurityException) {
            null
        }
    }

    /** Ritrova la riga WhatsApp partendo dal numero (es. dopo un import da un altro telefono). */
    fun findWhatsAppDataIdByNumber(phoneNumber: String, mimeType: String): Long? {
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
            contactIds.firstNotNullOfOrNull { findWhatsAppDataId(it, mimeType) }
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** Verifica che l'ID salvato punti ancora a una riga WhatsApp valida del tipo richiesto. */
    fun isWhatsAppDataRowValid(dataId: Long, mimeType: String): Boolean {
        if (!hasContactsPermission()) return true // Non verificabile: si tenta comunque.
        return try {
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                "${ContactsContract.Data._ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(dataId.toString(), mimeType),
                null,
            )?.use { it.count > 0 } ?: false
        } catch (_: SecurityException) {
            true
        }
    }

    /**
     * Numero in formato internazionale senza "+" (es. 393331234567), come richiesto da
     * wa.me e da Telegram. Usa il paese della SIM/rete per i numeri senza prefisso.
     */
    fun toInternationalDigits(phoneNumber: String): String {
        val trimmed = phoneNumber.trim()
        val e164 = try {
            PhoneNumberUtils.formatNumberToE164(trimmed, countryIso())
        } catch (_: RuntimeException) {
            null
        }
        if (!e164.isNullOrBlank()) return e164.filter { it.isDigit() }
        val digits = trimmed.filter { it.isDigit() }
        return when {
            trimmed.startsWith("+") -> digits
            digits.startsWith("00") -> digits.removePrefix("00")
            countryIso() == "IT" && digits.length in 9..10 -> "39$digits"
            else -> digits
        }
    }

    private fun countryIso(): String {
        val tm = context.getSystemService(TelephonyManager::class.java)
        val fromTelephony = listOfNotNull(tm?.simCountryIso, tm?.networkCountryIso)
            .firstOrNull { it.isNotBlank() }
        return (fromTelephony ?: Locale.getDefault().country.ifBlank { "IT" }).uppercase(Locale.ROOT)
    }

    companion object {
        const val WHATSAPP_PACKAGE = "com.whatsapp"
        const val WHATSAPP_VIDEO_MIME = "vnd.android.cursor.item/vnd.com.whatsapp.video.call"
        const val WHATSAPP_VOICE_MIME = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"
        const val WHATSAPP_PROFILE_MIME = "vnd.android.cursor.item/vnd.com.whatsapp.profile"

        /** Client Telegram supportati, in ordine di preferenza. */
        val TELEGRAM_PACKAGES = listOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram")

        fun whatsAppMimeFor(type: ActionType): String? = when (type) {
            ActionType.WHATSAPP_VIDEO -> WHATSAPP_VIDEO_MIME
            ActionType.WHATSAPP_VOICE -> WHATSAPP_VOICE_MIME
            ActionType.WHATSAPP_CHAT -> WHATSAPP_PROFILE_MIME
            else -> null
        }
    }
}
