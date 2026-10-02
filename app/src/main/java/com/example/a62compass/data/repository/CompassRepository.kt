package com.example.a62compass.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.a62compass.data.model.AppTheme
import com.example.a62compass.data.model.CompassSettings
import com.example.a62compass.data.model.HeadingFormat
import com.example.a62compass.data.model.SavedHeading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "compass_preferences")

class CompassRepository(private val context: Context) {

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_HEADING_FORMAT = stringPreferencesKey("heading_format")
        val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val KEY_SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
        val KEY_SHOW_DEGREE_TICKS = booleanPreferencesKey("show_degree_ticks")
        val KEY_SHOW_ACCURACY = booleanPreferencesKey("show_accuracy")
        val KEY_HISTORY = stringPreferencesKey("history_json")

        const val MAX_HISTORY_RECORDS = 50
    }

    val settingsFlow: Flow<CompassSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeStr = preferences[KEY_THEME] ?: AppTheme.SYSTEM.name
            val theme = try {
                AppTheme.valueOf(themeStr)
            } catch (e: Exception) {
                AppTheme.SYSTEM
            }

            val dynamicColor = preferences[KEY_DYNAMIC_COLOR] ?: true

            val formatStr = preferences[KEY_HEADING_FORMAT] ?: HeadingFormat.DEGREES_AND_DIRECTION.name
            val format = try {
                HeadingFormat.valueOf(formatStr)
            } catch (e: Exception) {
                HeadingFormat.DEGREES_AND_DIRECTION
            }

            val haptic = preferences[KEY_HAPTIC_FEEDBACK] ?: true
            val sound = preferences[KEY_SOUND_EFFECTS] ?: false
            val showTicks = preferences[KEY_SHOW_DEGREE_TICKS] ?: true
            val showAccuracy = preferences[KEY_SHOW_ACCURACY] ?: true

            CompassSettings(
                theme = theme,
                dynamicColor = dynamicColor,
                headingFormat = format,
                hapticFeedback = haptic,
                soundEffects = sound,
                showDegreeTicks = showTicks,
                showAccuracy = showAccuracy
            )
        }

    val historyFlow: Flow<List<SavedHeading>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val json = preferences[KEY_HISTORY] ?: ""
            SavedHeading.listFromJson(json)
        }

    suspend fun saveHeading(heading: SavedHeading) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[KEY_HISTORY] ?: ""
            val currentList = SavedHeading.listFromJson(currentJson).toMutableList()

            // Prepend new heading (most recent at top)
            currentList.add(0, heading)

            // Keep maximum 50 records (drop oldest when 51 is added)
            val trimmedList = if (currentList.size > MAX_HISTORY_RECORDS) {
                currentList.take(MAX_HISTORY_RECORDS)
            } else {
                currentList
            }

            preferences[KEY_HISTORY] = SavedHeading.listToJson(trimmedList)
        }
    }

    suspend fun deleteHeading(id: Long) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[KEY_HISTORY] ?: ""
            val currentList = SavedHeading.listFromJson(currentJson).filter { it.id != id }
            preferences[KEY_HISTORY] = SavedHeading.listToJson(currentList)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { preferences ->
            preferences[KEY_HISTORY] = "[]"
        }
    }

    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { it[KEY_THEME] = theme.name }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun updateHeadingFormat(format: HeadingFormat) {
        context.dataStore.edit { it[KEY_HEADING_FORMAT] = format.name }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun updateSoundEffects(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND_EFFECTS] = enabled }
    }

    suspend fun updateShowDegreeTicks(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_DEGREE_TICKS] = enabled }
    }

    suspend fun updateShowAccuracy(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_ACCURACY] = enabled }
    }

    suspend fun resetSettings() {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = AppTheme.SYSTEM.name
            preferences[KEY_DYNAMIC_COLOR] = true
            preferences[KEY_HEADING_FORMAT] = HeadingFormat.DEGREES_AND_DIRECTION.name
            preferences[KEY_HAPTIC_FEEDBACK] = true
            preferences[KEY_SOUND_EFFECTS] = false
            preferences[KEY_SHOW_DEGREE_TICKS] = true
            preferences[KEY_SHOW_ACCURACY] = true
        }
    }
}
