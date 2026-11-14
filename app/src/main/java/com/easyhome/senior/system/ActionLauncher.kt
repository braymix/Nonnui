package com.easyhome.senior.system

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.easyhome.senior.domain.model.ActionType
import com.easyhome.senior.domain.model.HomeButton

sealed interface LaunchResult {
    data object Success : LaunchResult
    data class Failure(val message: String) : LaunchResult
}

/** Traduce un [HomeButton] nell'Intent corretto e lo avvia. */
class ActionLauncher(
    private val appContext: Context,
    private val contactResolver: ContactResolver,
) {

    fun launch(context: Context, button: HomeButton): LaunchResult = when (button.type) {
        ActionType.APP -> launchApp(context, button)
        ActionType.PHONE_CALL -> launchPhoneCall(context, button)
        ActionType.WHATSAPP_VIDEO -> launchWhatsAppVideo(context, button)
    }

    private fun launchApp(context: Context, button: HomeButton): LaunchResult {
        val pkg = button.packageName ?: return LaunchResult.Failure("PULSANTE NON CONFIGURATO")
        val intent = appContext.packageManager.getLaunchIntentForPackage(pkg)
            ?: return LaunchResult.Failure("APP NON DISPONIBILE")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return start(context, intent, "IMPOSSIBILE APRIRE ${button.label}")
    }

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

    private fun launchWhatsAppVideo(context: Context, button: HomeButton): LaunchResult {
        if (appContext.packageManager.getLaunchIntentForPackage(ContactResolver.WHATSAPP_PACKAGE) == null) {
            return LaunchResult.Failure("WHATSAPP NON INSTALLATO")
        }
        val dataId = button.whatsappDataId?.takeIf { contactResolver.isWhatsAppDataRowValid(it) }
            ?: button.phoneNumber?.let { contactResolver.findWhatsAppVideoDataIdByNumber(it) }

        if (dataId != null) {
            val videoIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    ContactResolver.WHATSAPP_VIDEO_MIME,
                )
                setPackage(ContactResolver.WHATSAPP_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (start(context, videoIntent, "") is LaunchResult.Success) return LaunchResult.Success
        }

        // Ripiego: apre la chat WhatsApp del contatto, da cui la videochiamata è a un tocco.
        val digits = button.phoneNumber?.filter { it.isDigit() }.orEmpty()
        if (digits.isEmpty()) return LaunchResult.Failure("CONTATTO NON TROVATO SU WHATSAPP")
        val chatIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))
            .setPackage(ContactResolver.WHATSAPP_PACKAGE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context, chatIntent, "IMPOSSIBILE APRIRE WHATSAPP")
    }

    private fun start(context: Context, intent: Intent, errorMessage: String): LaunchResult = try {
        context.startActivity(intent)
        LaunchResult.Success
    } catch (_: ActivityNotFoundException) {
        LaunchResult.Failure(errorMessage)
    } catch (_: SecurityException) {
        LaunchResult.Failure(errorMessage)
    }
}
