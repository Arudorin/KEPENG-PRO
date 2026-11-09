package com.example.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kepeng_settings")

class PreferencesManager(private val context: Context) {
    companion object {
        val KEY_START_DAY = intPreferencesKey("start_day")
        val KEY_BALANCE_HIDDEN = booleanPreferencesKey("is_balance_hidden")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("is_onboarding_done")
        val KEY_APP_PIN = stringPreferencesKey("app_pin")
        val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("is_app_lock_enabled")
        val KEY_LAST_ACCOUNT_ID = stringPreferencesKey("last_used_account_id")
        val KEY_LAST_CATEGORY_ID = stringPreferencesKey("last_used_category_id")
    }

    val startDayFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_START_DAY] ?: 1
    }

    val isBalanceHiddenFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BALANCE_HIDDEN] ?: false
    }

    val isOnboardingDoneFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    val appPinFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_PIN]
    }

    val isAppLockEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LOCK_ENABLED] ?: false
    }

    val lastUsedAccountIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_ACCOUNT_ID]
    }

    val lastUsedCategoryIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_CATEGORY_ID]
    }

    suspend fun setStartDay(day: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_START_DAY] = day.coerceIn(1, 28)
        }
    }

    suspend fun setBalanceHidden(hidden: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BALANCE_HIDDEN] = hidden
        }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_DONE] = done
        }
    }

    suspend fun setAppPin(pin: String?) {
        context.dataStore.edit { prefs ->
            if (pin != null) {
                prefs[KEY_APP_PIN] = pin
                prefs[KEY_APP_LOCK_ENABLED] = true
            } else {
                prefs.remove(KEY_APP_PIN)
                prefs[KEY_APP_LOCK_ENABLED] = false
            }
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setLastUsed(accountId: String?, categoryId: String?) {
        context.dataStore.edit { prefs ->
            if (accountId != null) prefs[KEY_LAST_ACCOUNT_ID] = accountId
            if (categoryId != null) prefs[KEY_LAST_CATEGORY_ID] = categoryId
        }
    }
}
