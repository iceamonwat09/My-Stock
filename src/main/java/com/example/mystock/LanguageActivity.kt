package com.example.mystock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

// DataStore extension - must be at top level
private val Context.dataStore by preferencesDataStore(name = "settings")

class LanguageActivity : AppCompatActivity() {

    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if language is already set
        lifecycleScope.launch {
            val prefs = dataStore.data.first()
            val savedLanguage = prefs[LANGUAGE_KEY]

            if (savedLanguage != null) {
                // Language already set, go to main activity
                setAppLocale(savedLanguage)
                navigateToMain()
                return@launch
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
            dataStore.edit { prefs ->
                prefs[LANGUAGE_KEY] = languageCode
            }
            setAppLocale(languageCode)
            navigateToMain()
        }
    }

    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivityInventory::class.java)
        startActivity(intent)
        finish()
    }
}
