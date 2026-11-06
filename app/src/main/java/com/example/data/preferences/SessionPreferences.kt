package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "jio_pay_session")

class SessionPreferences(private val context: Context) {
    private val currentUserIdKey = longPreferencesKey("current_user_id")

    val currentUserIdFlow: Flow<Long?> = context.sessionDataStore.data.map { prefs ->
        prefs[currentUserIdKey]
    }

    suspend fun setCurrentUserId(userId: Long) {
        context.sessionDataStore.edit { prefs ->
            prefs[currentUserIdKey] = userId
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(currentUserIdKey)
        }
    }
}
