package com.example.mystock

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// Single source of truth for DataStore
val Context.dataStore by preferencesDataStore(name = "settings")

object DataStoreKeys {
    val LANGUAGE_KEY = stringPreferencesKey("language")
    val PRO_VERSION_KEY = booleanPreferencesKey("pro_version")
}
