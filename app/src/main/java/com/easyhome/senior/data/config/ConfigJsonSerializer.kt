package com.easyhome.senior.data.config

import com.easyhome.senior.domain.model.ActionType
import com.easyhome.senior.domain.model.HomeButton
import com.easyhome.senior.domain.model.LauncherConfig
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

/** Serializzazione JSON (org.json nativo) usata sia per DataStore sia per backup/ripristino. */
object ConfigJsonSerializer {

    private const val APP_ID = "EasyHomeSenior"
    private const val SCHEMA_VERSION = 1

    fun toJson(config: LauncherConfig): String = JSONObject().apply {
        put("app", APP_ID)
        put("schemaVersion", SCHEMA_VERSION)
        put("theme", config.theme.name)
        put("fontSizeSp", config.fontSizeSp)
        put("iconSizeDp", config.iconSizeDp)
        put("soundEnabled", config.soundEnabled)
        put("vibration", config.vibration.name)
        put("overlayPosition", config.overlayPosition.name)
        put("buttons", buttonsToJsonArray(config.buttons))
    }.toString(2)

    /** @throws IllegalArgumentException con messaggio leggibile se il testo non è un backup valido. */
    fun fromJson(text: String): LauncherConfig {
        val root = try {
            JSONObject(text.trim())
        } catch (e: JSONException) {
            throw IllegalArgumentException("Il testo inserito non è un backup valido (JSON non leggibile).")
        }
        if (root.optString("app") != APP_ID) {
            throw IllegalArgumentException("Questo non è un backup di EasyHome Senior.")
        }
        val defaults = LauncherConfig()
        return try {
            LauncherConfig(
                buttons = parseButtons(root.optJSONArray("buttons") ?: JSONArray()),
                theme = enumOrDefault(root.optString("theme"), defaults.theme),
                fontSizeSp = root.optInt("fontSizeSp", defaults.fontSizeSp),
                iconSizeDp = root.optInt("iconSizeDp", defaults.iconSizeDp),
                soundEnabled = root.optBoolean("soundEnabled", defaults.soundEnabled),
                vibration = enumOrDefault(root.optString("vibration"), defaults.vibration),
                overlayPosition = enumOrDefault(root.optString("overlayPosition"), defaults.overlayPosition),
            ).sanitized()
        } catch (e: JSONException) {
            throw IllegalArgumentException("Il backup è danneggiato: ${e.message}")
        }
    }

    fun buttonsToJsonArray(buttons: List<HomeButton>): JSONArray = JSONArray().apply {
        buttons.forEach { b ->
            put(JSONObject().apply {
                put("id", b.id)
                put("type", b.type.name)
                put("label", b.label)
                put("color", b.colorArgb)
                put("packageName", b.packageName ?: JSONObject.NULL)
                put("contactName", b.contactName ?: JSONObject.NULL)
                put("phoneNumber", b.phoneNumber ?: JSONObject.NULL)
                put("whatsappDataId", b.whatsappDataId ?: JSONObject.NULL)
            })
        }
    }

    fun buttonsFromJson(text: String): List<HomeButton> =
        try {
            parseButtons(JSONArray(text))
        } catch (e: JSONException) {
            emptyList()
        }

    private fun parseButtons(array: JSONArray): List<HomeButton> {
        val result = mutableListOf<HomeButton>()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val type = enumOrNull<ActionType>(o.optString("type")) ?: continue
            val button = HomeButton(
                id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                type = type,
                label = o.optString("label"),
                colorArgb = o.optLong("color", 0xFF0D47A1),
                packageName = o.nullableString("packageName"),
                contactName = o.nullableString("contactName"),
                phoneNumber = o.nullableString("phoneNumber"),
                whatsappDataId = if (o.has("whatsappDataId") && !o.isNull("whatsappDataId")) {
                    o.optLong("whatsappDataId")
                } else {
                    null
                },
            )
            val valid = when (type) {
                ActionType.APP -> !button.packageName.isNullOrBlank()
                ActionType.PHONE_CALL, ActionType.WHATSAPP_VIDEO -> !button.phoneNumber.isNullOrBlank()
            }
            if (valid) result += button
        }
        return result
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        enumValues<T>().firstOrNull { it.name == name }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumOrNull<T>(name) ?: default

}
