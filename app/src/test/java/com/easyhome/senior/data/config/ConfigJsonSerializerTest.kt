package com.easyhome.senior.data.config

import com.easyhome.senior.domain.model.ActionType
import com.easyhome.senior.domain.model.AppTheme
import com.easyhome.senior.domain.model.HomeButton
import com.easyhome.senior.domain.model.LauncherConfig
import com.easyhome.senior.domain.model.OverlayPosition
import com.easyhome.senior.domain.model.VibrationIntensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigJsonSerializerTest {

    private val sample = LauncherConfig(
        buttons = listOf(
            HomeButton("1", ActionType.APP, "MESSAGGI", 0xFF1B5E20, packageName = "com.whatsapp"),
            HomeButton("2", ActionType.PHONE_CALL, "CHIAMA MARIO", 0xFF0D47A1, contactName = "Mario", phoneNumber = "+39 333 1234567"),
            HomeButton("3", ActionType.WHATSAPP_VIDEO, "VIDEO ANNA", 0xFFB71C1C, contactName = "Anna", phoneNumber = "3471112223", whatsappDataId = 42L),
        ),
        theme = AppTheme.HIGH_CONTRAST,
        fontSizeSp = 40,
        iconSizeDp = 120,
        soundEnabled = false,
        vibration = VibrationIntensity.MEDIUM,
        overlayPosition = OverlayPosition.TOP,
    )

    @Test
    fun `export e import producono la stessa configurazione`() {
        val restored = ConfigJsonSerializer.fromJson(ConfigJsonSerializer.toJson(sample))
        assertEquals(sample, restored)
    }

    @Test
    fun `i pulsanti sopravvivono al salvataggio in DataStore`() {
        val json = ConfigJsonSerializer.buttonsToJsonArray(sample.buttons).toString()
        assertEquals(sample.buttons, ConfigJsonSerializer.buttonsFromJson(json))
    }

    @Test
    fun `testo non JSON viene rifiutato con messaggio leggibile`() {
        val e = assertThrows(IllegalArgumentException::class.java) { ConfigJsonSerializer.fromJson("ciao") }
        assertTrue(e.message!!.contains("non è un backup valido"))
    }

    @Test
    fun `JSON di un'altra app viene rifiutato`() {
        assertThrows(IllegalArgumentException::class.java) { ConfigJsonSerializer.fromJson("""{"app":"Altro"}""") }
    }

    @Test
    fun `import con valori fuori limite viene normalizzato`() {
        val buttons = (1..6).joinToString(",") {
            """{"id":"$it","type":"APP","label":"app $it","color":4278190080,"packageName":"pkg.$it"}"""
        }
        val json = """{"app":"EasyHomeSenior","fontSizeSp":99,"iconSizeDp":1,"theme":"SCONOSCIUTO","buttons":[$buttons]}"""
        val config = ConfigJsonSerializer.fromJson(json)
        assertEquals(LauncherConfig.MAX_BUTTONS, config.buttons.size)
        assertEquals(LauncherConfig.MAX_FONT_SP, config.fontSizeSp)
        assertEquals(LauncherConfig.MIN_ICON_DP, config.iconSizeDp)
        assertEquals(AppTheme.LIGHT, config.theme)
        assertEquals("APP 1", config.buttons.first().label)
    }

    @Test
    fun `pulsanti incompleti o di tipo ignoto vengono scartati`() {
        val json = """[
            {"id":"a","type":"APP","label":"X","color":1},
            {"id":"b","type":"PHONE_CALL","label":"Y","color":1},
            {"id":"c","type":"TELETRASPORTO","label":"Z","color":1},
            {"id":"d","type":"PHONE_CALL","label":"OK","color":1,"phoneNumber":"123","whatsappDataId":null}
        ]"""
        val buttons = ConfigJsonSerializer.buttonsFromJson(json)
        assertEquals(listOf("d"), buttons.map { it.id })
        assertNull(buttons.single().whatsappDataId)
    }

    @Test
    fun `le etichette vengono rese maiuscole e troncate`() {
        val long = "videochiamata alla nonna giuseppina"
        assertEquals(LauncherConfig.MAX_LABEL_LENGTH, LauncherConfig.normalizeLabel(long).length)
        assertEquals("VIDEOCHIAMATA ALLA NONNA", LauncherConfig.normalizeLabel(long))
    }
}
