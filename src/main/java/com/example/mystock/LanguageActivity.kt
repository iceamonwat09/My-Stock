package com.example.mystock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

// DataStore extension - must be at top level
private val Context.dataStore by preferencesDataStore(name = "settings")

class LanguageActivity : AppCompatActivity() {

    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
    }

    private lateinit var loadingLayout: View
    private lateinit var languageSelectionLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)

        // Initialize views
        loadingLayout = findViewById(R.id.loadingLayout)
        languageSelectionLayout = findViewById(R.id.languageSelectionLayout)

        // Check if language is already set
        lifecycleScope.launch {
            checkAndApplyLanguage()
        }

        // Setup button listeners
        findViewById<Button>(R.id.buttonEnglish).setOnClickListener {
            saveLanguageAndProceed("en")
        }

        findViewById<Button>(R.id.buttonThai).setOnClickListener {
            saveLanguageAndProceed("th")
        }
    }

    private suspend fun checkAndApplyLanguage() {
        try {
            // Show loading
            loadingLayout.visibility = View.VISIBLE
            languageSelectionLayout.visibility = View.GONE

            // Small delay to show loading (prevents flickering)
            delay(300)

            // Check saved language
            val prefs = dataStore.data.first()
            val savedLanguage = prefs[LANGUAGE_KEY]

            if (savedLanguage != null) {
                // Language already set, apply and navigate
                setAppLocale(savedLanguage)

                // Small delay to ensure locale is applied
                delay(200)

                navigateToMain()
            } else {
                // No language set, show selection
                loadingLayout.visibility = View.GONE
                languageSelectionLayout.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // On error, show language selection
            loadingLayout.visibility = View.GONE
            languageSelectionLayout.visibility = View.VISIBLE
        }
    }

    private fun saveLanguageAndProceed(languageCode: String) {
        // Disable buttons to prevent double-click
        findViewById<Button>(R.id.buttonEnglish).isEnabled = false
        findViewById<Button>(R.id.buttonThai).isEnabled = false

        // Show loading
        languageSelectionLayout.visibility = View.GONE
        loadingLayout.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Save language preference
                dataStore.edit { prefs ->
                    prefs[LANGUAGE_KEY] = languageCode
                }

                // Apply locale
                setAppLocale(languageCode)

                // Small delay to ensure locale is applied
                delay(300)

                // Navigate to main
                navigateToMain()
            } catch (e: Exception) {
                e.printStackTrace()
                // Re-enable buttons on error
                findViewById<Button>(R.id.buttonEnglish).isEnabled = true
                findViewById<Button>(R.id.buttonThai).isEnabled = true
                languageSelectionLayout.visibility = View.VISIBLE
                loadingLayout.visibility = View.GONE
            }
        }
    }

    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)

        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        // Also create configuration context
        createConfigurationContext(config)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivityInventory::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
