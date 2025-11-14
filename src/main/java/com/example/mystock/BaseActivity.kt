package com.example.mystock

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * BaseActivity - Ensures all activities maintain the correct locale
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Locale is already applied in attachBaseContext
    }

    override fun attachBaseContext(newBase: Context?) {
        // Apply saved locale before creating the activity
        val context = newBase ?: return super.attachBaseContext(newBase)
        val updatedContext = updateLocaleContext(context)
        super.attachBaseContext(updatedContext)
    }

    private fun updateLocaleContext(context: Context): Context {
        // Get saved language synchronously using runBlocking
        // This is safe here because attachBaseContext is called before onCreate
        val savedLanguage = try {
            runBlocking {
                val prefs = context.dataStore.data.first()
                prefs[DataStoreKeys.LANGUAGE_KEY]
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (savedLanguage != null) {
            val locale = Locale(savedLanguage)
            Locale.setDefault(locale)

            val config = context.resources.configuration
            config.setLocale(locale)

            return context.createConfigurationContext(config)
        }

        return context
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
}
