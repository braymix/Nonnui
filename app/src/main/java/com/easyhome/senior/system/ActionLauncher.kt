package com.easyhome.senior.system

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import androidx.core.content.ContextCompat
import com.easyhome.senior.domain.model.ActionType
import com.easyhome.senior.domain.model.HomeButton

sealed interface LaunchResult {
    data object Success : LaunchResult
    data class Failure(val message: String) : LaunchResult
}

/** Traduce un [HomeButton] nell'Intent corretto e lo avvia, con ripieghi sensati. */
class ActionLauncher(
    private val appContext: Context,
    private val contactResolver: ContactResolver,
) {

    private val packageManager = appContext.packageManager

    fun launch(context: Context, button: HomeButton): LaunchResult = when (button.type) {
        ActionType.APP -> launchApp(context, button.packageName, button.label)
        ActionType.PHONE_CALL -> launchPhoneCall(context, button)
        ActionType.WHATSAPP_VIDEO, ActionType.WHATSAPP_VOICE, ActionType.WHATSAPP_CHAT ->
            launchWhatsApp(context, button)
        ActionType.TELEGRAM_CHAT -> launchTelegramChat(context, button)
        ActionType.LINK -> launchLink(context, button)
        ActionType.SHORTCUT -> launchShortcut(context, button)
    }

    // ---- App -------------------------------------------------------------------------------

    private fun launchApp(context: Context, pkg: String?, label: String): LaunchResult {
        if (pkg == null) return LaunchResult.Failure("PULSANTE NON CONFIGURATO")
        val intent = packageManager.getLaunchIntentForPackage(pkg)
            ?: return LaunchResult.Failure("APP NON DISPONIBILE")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return start(context, intent, "IMPOSSIBILE APRIRE $label")
    }

    // ---- Telefono --------------------------------------------------------------------------

    private fun launchPhoneCall(context: Context, button: HomeButton): LaunchResult {
        val number = button.phoneNumber ?: return LaunchResult.Failure("NUMERO MANCANTE")
        val uri = Uri.fromParts("tel", number, null)
        val canCallDirectly = ContextCompat.checkSelfPermission(appContext, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        val primary = Intent(if (canCallDirectly) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(primary)
            LaunchResult.Success
        } catch (_: SecurityException) {
            start(context, Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "TELEFONO NON DISPONIBILE")
        } catch (_: ActivityNotFoundException) {
            LaunchResult.Failure("TELEFONO NON DISPONIBILE")
        }
    }

    // ---- WhatsApp --------------------------------------------------------------------------

    private fun launchWhatsApp(context: Context, button: HomeButton): LaunchResult {
        if (!isInstalled(ContactResolver.WHATSAPP_PACKAGE)) return LaunchResult.Failure("WHATSAPP NON INSTALLATO")
        val mime = ContactResolver.whatsAppMimeFor(button.type) ?: return LaunchResult.Failure("AZIONE NON VALIDA")

        // 1) Riga di rubrica WhatsApp: avvia direttamente videochiamata / chiamata / chat.
        val dataId = button.whatsappDataId?.takeIf { contactResolver.isWhatsAppDataRowValid(it, mime) }
            ?: button.phoneNumber?.let { contactResolver.findWhatsAppDataIdByNumber(it, mime) }
        if (dataId != null) {
            val direct = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse("content://com.android.contacts/data/$dataId"), mime)
                setPackage(ContactResolver.WHATSAPP_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (start(context, direct, "") is LaunchResult.Success) return LaunchResult.Success
        }

        // 2) Ripiego: chat del contatto via wa.me (da lì chiamata/videochiamata è a un tocco).
        val digits = button.phoneNumber?.let { contactResolver.toInternationalDigits(it) }.orEmpty()
        if (digits.isEmpty()) return LaunchResult.Failure("CONTATTO NON TROVATO SU WHATSAPP")
        val chatIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))
            .setPackage(ContactResolver.WHATSAPP_PACKAGE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context, chatIntent, "IMPOSSIBILE APRIRE WHATSAPP")
    }

    // ---- Telegram --------------------------------------------------------------------------

    private fun launchTelegramChat(context: Context, button: HomeButton): LaunchResult {
        val telegram = telegramPackage() ?: return LaunchResult.Failure("TELEGRAM NON INSTALLATO")
        val digits = button.phoneNumber?.let { contactResolver.toInternationalDigits(it) }.orEmpty()
        if (digits.isEmpty()) return LaunchResult.Failure("NUMERO MANCANTE")
        val resolve = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?phone=$digits"))
            .setPackage(telegram)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (start(context, resolve, "") is LaunchResult.Success) return LaunchResult.Success
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+$digits"))
            .setPackage(telegram)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context, web, "IMPOSSIBILE APRIRE TELEGRAM")
    }

    // ---- Link (gruppi WhatsApp/Telegram, siti) ---------------------------------------------

    private fun launchLink(context: Context, button: HomeButton): LaunchResult {
        val raw = button.link?.trim().orEmpty()
        if (raw.isEmpty()) return LaunchResult.Failure("LINK MANCANTE")
        val uri = Uri.parse(normalizeLink(raw))
        val preferred = (button.packageName ?: packageForLink(raw))?.takeIf { isInstalled(it) }
        if (preferred != null) {
            val inApp = Intent(Intent.ACTION_VIEW, uri).setPackage(preferred).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (start(context, inApp, "") is LaunchResult.Success) return LaunchResult.Success
        }
        return start(
            context,
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            "IMPOSSIBILE APRIRE IL COLLEGAMENTO",
        )
    }

    // ---- Scorciatoie app (chat/gruppi "Aggiungi a Home") -----------------------------------

    private fun launchShortcut(context: Context, button: HomeButton): LaunchResult {
        val pkg = button.packageName ?: return LaunchResult.Failure("PULSANTE NON CONFIGURATO")
        val id = button.shortcutId ?: return launchApp(context, pkg, button.label)
        val launcherApps = appContext.getSystemService(LauncherApps::class.java)
        return try {
            launcherApps.startShortcut(pkg, id, null, null, Process.myUserHandle())
            LaunchResult.Success
        } catch (_: RuntimeException) {
            // Non siamo il launcher predefinito o la scorciatoia è stata rimossa: apriamo l'app.
            when (val fallback = launchApp(context, pkg, button.label)) {
                is LaunchResult.Success -> fallback
                is LaunchResult.Failure -> LaunchResult.Failure("SCORCIATOIA NON PIÙ DISPONIBILE")
            }
        }
    }

    // ---- Utilità ---------------------------------------------------------------------------

    private fun isInstalled(pkg: String): Boolean = packageManager.getLaunchIntentForPackage(pkg) != null

    fun telegramPackage(): String? = ContactResolver.TELEGRAM_PACKAGES.firstOrNull { isInstalled(it) }

    private fun start(context: Context, intent: Intent, errorMessage: String): LaunchResult = try {
        context.startActivity(intent)
        LaunchResult.Success
    } catch (_: ActivityNotFoundException) {
        LaunchResult.Failure(errorMessage)
    } catch (_: SecurityException) {
        LaunchResult.Failure(errorMessage)
    }

    companion object {
        /** Aggiunge https:// ai link incollati senza schema (es. "chat.whatsapp.com/ABC"). */
        fun normalizeLink(raw: String): String {
            val trimmed = raw.trim()
            return if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(trimmed)) trimmed else "https://$trimmed"
        }

        /** App più adatta ad aprire un link, in base al dominio. */
        fun packageForLink(raw: String): String? {
            val link = raw.lowercase()
            return when {
                "chat.whatsapp.com" in link || "wa.me" in link || link.startsWith("whatsapp:") ->
                    ContactResolver.WHATSAPP_PACKAGE
                "t.me/" in link || "telegram.me" in link || link.startsWith("tg:") ->
                    ContactResolver.TELEGRAM_PACKAGES.first()
                else -> null
            }
        }
    }
}
