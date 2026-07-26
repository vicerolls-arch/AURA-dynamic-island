package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "aura_prefs")

object AuraPreferences {
    private val KEY_OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
    private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    private val KEY_SNOOZED_UNTIL = longPreferencesKey("snoozed_until_millis")
    private val KEY_DISMISSED_UNTIL_RELAUNCH = booleanPreferencesKey("dismissed_until_relaunch")
    private val KEY_BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_notification_packages")

    fun getBlockedPackages(context: Context): Flow<Set<String>> =
        context.dataStore.data.map { it[KEY_BLOCKED_PACKAGES] ?: emptySet() }

    suspend fun setBlockedPackages(context: Context, packages: Set<String>) {
        context.dataStore.edit { it[KEY_BLOCKED_PACKAGES] = packages }
    }

    fun isOverlayEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_OVERLAY_ENABLED] ?: false }

    suspend fun setOverlayEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_OVERLAY_ENABLED] = enabled }
    }

    fun isOnboardingComplete(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_ONBOARDING_COMPLETE] ?: false }

    suspend fun setOnboardingComplete(context: Context, complete: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETE] = complete }
    }

    fun getSnoozedUntil(context: Context): Flow<Long> =
        context.dataStore.data.map { it[KEY_SNOOZED_UNTIL] ?: 0L }

    suspend fun setSnoozedUntil(context: Context, timestampMs: Long) {
        context.dataStore.edit { it[KEY_SNOOZED_UNTIL] = timestampMs }
    }

    fun getDismissedUntilRelaunch(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_DISMISSED_UNTIL_RELAUNCH] ?: false }

    suspend fun setDismissedUntilRelaunch(context: Context, dismissed: Boolean) {
        context.dataStore.edit { it[KEY_DISMISSED_UNTIL_RELAUNCH] = dismissed }
    }
}

