package com.easyhome.senior.data.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.easyhome.senior.domain.model.LauncherConfig
import com.easyhome.senior.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "easyhome_settings")

class DataStoreConfigRepository(context: Context) : ConfigRepository {

    private val dataStore = context.applicationContext.settingsDataStore

    private object Keys {
        val BUTTONS = stringPreferencesKey("buttons_json")
        val THEME = stringPreferencesKey("theme")
        val FONT_SP = intPreferencesKey("font_size_sp")
        val ICON_DP = intPreferencesKey("icon_size_dp")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val VIBRATION = stringPreferencesKey("vibration")
        val OVERLAY_POSITION = stringPreferencesKey("overlay_position")
    }

    override val config: Flow<LauncherConfig> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs.toConfig() }
        .distinctUntilChanged()

    override suspend fun update(transform: (LauncherConfig) -> LauncherConfig) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toConfig()).sanitized()
            prefs.write(updated)
        }
    }

    override suspend fun replace(config: LauncherConfig) {
        dataStore.edit { prefs -> prefs.write(config.sanitized()) }
    }

    private fun Preferences.toConfig(): LauncherConfig {
        val defaults = LauncherConfig()
        return LauncherConfig(
            buttons = this[Keys.BUTTONS]?.let { ConfigJsonSerializer.buttonsFromJson(it) } ?: emptyList(),
            theme = enumValueOrDefault(this[Keys.THEME], defaults.theme),
            fontSizeSp = this[Keys.FONT_SP] ?: defaults.fontSizeSp,
            iconSizeDp = this[Keys.ICON_DP] ?: defaults.iconSizeDp,
            soundEnabled = this[Keys.SOUND] ?: defaults.soundEnabled,
            vibration = enumValueOrDefault(this[Keys.VIBRATION], defaults.vibration),
            overlayPosition = enumValueOrDefault(this[Keys.OVERLAY_POSITION], defaults.overlayPosition),
        ).sanitized()
    }

    private fun MutablePreferences.write(config: LauncherConfig) {
        this[Keys.BUTTONS] = ConfigJsonSerializer.buttonsToJsonArray(config.buttons).toString()
        this[Keys.THEME] = config.theme.name
        this[Keys.FONT_SP] = config.fontSizeSp
        this[Keys.ICON_DP] = config.iconSizeDp
        this[Keys.SOUND] = config.soundEnabled
        this[Keys.VIBRATION] = config.vibration.name
        this[Keys.OVERLAY_POSITION] = config.overlayPosition.name
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

}
