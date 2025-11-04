package com.chatting.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.chatting.ui.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {

    private val _themeMode = MutableStateFlow(PreferencesManager.THEME_SYSTEM)
    val themeMode = _themeMode.asStateFlow()

    fun init(context: Context) {
        val preferencesManager = PreferencesManager(context)
        val savedMode = preferencesManager.getThemeMode()
        _themeMode.value = savedMode
        applyThemeToSystem(savedMode)
    }

    fun setTheme(context: Context, mode: Int) {
        val preferencesManager = PreferencesManager(context)
        preferencesManager.setThemeMode(mode)
        _themeMode.value = mode
        applyThemeToSystem(mode)
    }

    private fun applyThemeToSystem(mode: Int) {
        val systemMode = when (mode) {
            PreferencesManager.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            PreferencesManager.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(systemMode)
    }
}