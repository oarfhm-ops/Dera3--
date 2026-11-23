package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppPreferences
import com.example.localization.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dera3DataStore: DataStore<Preferences> by preferencesDataStore(name = "dera3_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val SOUND = booleanPreferencesKey("sound_enabled")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dera3DataStore.data.map { prefs ->
        val lang = if (prefs[Keys.LANGUAGE] == AppLanguage.EN.name) AppLanguage.EN else AppLanguage.AR
        AppPreferences(
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            language = lang,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            soundEnabled = prefs[Keys.SOUND] ?: true,
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dera3DataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dera3DataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dera3DataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dera3DataStore.edit { it[Keys.SOUND] = enabled }
    }
}
