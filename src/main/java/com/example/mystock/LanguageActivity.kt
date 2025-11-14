package com.example.mystock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

// DataStore extension - must be at top level with unique name
private val Context.languageDataStore by preferencesDataStore(name = "language_settings")

class LanguageActivity : AppCompatActivity() {

    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
        private const val TAG = "LanguageActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if language is already set with error handling
        lifecycleScope.launch {
            try {
                val prefs = languageDataStore.data.first()
                val savedLanguage = prefs[LANGUAGE_KEY]

                if (savedLanguage != null) {
                    // Language already set, go to main activity
                    Log.d(TAG, "Language already set: $savedLanguage")
                    setAppLocale(savedLanguage)
                    navigateToMain()
                    return@launch
                }

                // No saved language, show selection screen
                Log.d(TAG, "No saved language, showing selection screen")
            } catch (e: Exception) {
                Log.e(TAG, "Error reading language preference", e)
                // Continue to show language selection if error
            }
        }

        setContentView(R.layout.activity_language)

        findViewById<Button>(R.id.buttonEnglish).setOnClickListener {
            saveLanguageAndProceed("en")
        }

        findViewById<Button>(R.id.buttonThai).setOnClickListener {
            saveLanguageAndProceed("th")
        }
    }

    private fun saveLanguageAndProceed(languageCode: String) {
        lifecycleScope.launch {
            try {
                languageDataStore.edit { prefs ->
                    prefs[LANGUAGE_KEY] = languageCode
                }
                Log.d(TAG, "Language saved: $languageCode")
                setAppLocale(languageCode)
                navigateToMain()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving language preference", e)
                Toast.makeText(this@LanguageActivity, "Error saving language", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setAppLocale(languageCode: String) {
        try {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
            Log.d(TAG, "Locale set to: $languageCode")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting locale", e)
        }
    }

    private fun navigateToMain() {
        try {
            Log.d(TAG, "Navigating to MainActivityInventory")
            val intent = Intent(this, MainActivityInventory::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to main activity", e)
            Toast.makeText(this, "Error opening main screen", Toast.LENGTH_SHORT).show()
        }
    }
}
