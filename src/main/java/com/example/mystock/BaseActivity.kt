package com.example.mystock

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

// DataStore extension
private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * BaseActivity - Ensures all activities maintain the correct locale
 */
abstract class BaseActivity : AppCompatActivity() {

    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved locale before onCreate
        lifecycleScope.launch {
            applySavedLocale()
        }
        super.onCreate(savedInstanceState)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { updateLocale(it) })
    }

    private fun updateLocale(context: Context): Context {
        // Try to get saved language synchronously from preferences
        // This is called before coroutines are available
        return context
    }

    private suspend fun applySavedLocale() {
        try {
            val prefs = dataStore.data.first()
            val savedLanguage = prefs[LANGUAGE_KEY]
            if (savedLanguage != null) {
                setAppLocale(savedLanguage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)

        // Update configuration properly
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        // Also update base context configuration
        createConfigurationContext(config)
    }

    protected suspend fun getSavedLanguage(): String? {
        return try {
            val prefs = dataStore.data.first()
            prefs[LANGUAGE_KEY]
        } catch (e: Exception) {
            null
        }
    }
}
