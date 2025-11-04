package com.chatting.ui.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    companion object {
        const val THEME_MODE = "theme_mode"
        const val LANGUAGE = "language"
        const val MESSAGE_BUBBLE_COLOR = "message_bubble_color"

        const val MESSAGE_FONT_SIZE = "message_font_size"
        const val CHAT_WALLPAPER = "chat_wallpaper"

        // Novas chaves para download automático
        const val AUTO_DOWNLOAD_IMAGES_MOBILE = "auto_download_images_mobile"
        const val AUTO_DOWNLOAD_AUDIO_MOBILE = "auto_download_audio_mobile"
        const val AUTO_DOWNLOAD_VIDEOS_MOBILE = "auto_download_videos_mobile"
        const val AUTO_DOWNLOAD_DOCS_MOBILE = "auto_download_docs_mobile"
        const val AUTO_DOWNLOAD_IMAGES_WIFI = "auto_download_images_wifi"
        const val AUTO_DOWNLOAD_AUDIO_WIFI = "auto_download_audio_wifi"
        const val AUTO_DOWNLOAD_VIDEOS_WIFI = "auto_download_videos_wifi"
        const val AUTO_DOWNLOAD_DOCS_WIFI = "auto_download_docs_wifi"

        // Theme modes
        const val THEME_LIGHT = 0
        const val THEME_DARK = 1
        const val THEME_SYSTEM = 2

        // Font sizes
        const val FONT_SIZE_SMALL = 0
        const val FONT_SIZE_NORMAL = 1
        const val FONT_SIZE_LARGE = 2
        const val FONT_SIZE_EXTRA_LARGE = 3
    }

    fun setThemeMode(mode: Int) {
        sharedPreferences.edit().putInt(THEME_MODE, mode).apply()
    }

    fun getThemeMode(): Int {
        return sharedPreferences.getInt(THEME_MODE, THEME_SYSTEM)
    }

    fun setLanguage(language: String) {
        sharedPreferences.edit().putString(LANGUAGE, language).apply()
    }

    fun getLanguage(): String {
        return sharedPreferences.getString(LANGUAGE, "pt") ?: "pt"
    }

    fun setMessageBubbleColor(color: Int) {
        sharedPreferences.edit().putInt(MESSAGE_BUBBLE_COLOR, color).apply()
    }

    fun getMessageBubbleColor(): Int {
        return sharedPreferences.getInt(MESSAGE_BUBBLE_COLOR, 0xFF0288D1.toInt())
    }

    fun setMessageFontSize(size: Int) {
        sharedPreferences.edit().putInt(MESSAGE_FONT_SIZE, size).apply()
    }

    fun getMessageFontSize(): Int {
        return sharedPreferences.getInt(MESSAGE_FONT_SIZE, FONT_SIZE_NORMAL)
    }

    fun setChatWallpaper(wallpaper: String) {
        sharedPreferences.edit().putString(CHAT_WALLPAPER, wallpaper).apply()
    }

    fun getChatWallpaper(): String {
        // ✅ CORREÇÃO DEFINITIVA: O padrão agora é uma string vazia ("").
        // Isso indica que nenhum papel de parede foi definido pelo usuário.
        return sharedPreferences.getString(CHAT_WALLPAPER, "") ?: ""
    }

    // --- Funções para Download Automático ---

    fun setAutoDownloadImagesOnMobile(enabled: Boolean) = sharedPreferences.edit().putBoolean(AUTO_DOWNLOAD_IMAGES_MOBILE, enabled).apply()
    fun getAutoDownloadImagesOnMobile(): Boolean = sharedPreferences.getBoolean(AUTO_DOWNLOAD_IMAGES_MOBILE, true)

    fun setAutoDownloadAudioOnMobile(enabled: Boolean) = sharedPreferences.edit().putBoolean(AUTO_DOWNLOAD_AUDIO_MOBILE, enabled).apply()
    fun getAutoDownloadAudioOnMobile(): Boolean = sharedPreferences.getBoolean(AUTO_DOWNLOAD_AUDIO_MOBILE, true)

    fun setAutoDownloadVideosOnMobile(enabled: Boolean) = sharedPreferences.edit().putBoolean(AUTO_DOWNLOAD_VIDEOS_MOBILE, enabled).apply()
    fun getAutoDownloadVideosOnMobile(): Boolean = sharedPreferences.getBoolean(AUTO_DOWNLOAD_VIDEOS_MOBILE, false)

    fun setAutoDownloadDocsOnMobile(enabled: Boolean) = sharedPreferences.edit().putBoolean(AUTO_DOWNLOAD_DOCS_MOBILE, enabled).apply()
    fun getAutoDownloadDocsOnMobile(): Boolean = sharedPreferences.getBoolean(AUTO_DOWNLOAD_DOCS_MOBILE, false)

    fun setAutoDownloadImagesOnWifi(enabled: Boolean) = sharedPreferences.edit().putBoolean(AUTO_DOWNLOAD_IMAGES_WIFI, enabled).apply()
    fun getAutoDownloadImagesOnWifi(): Boolean = sharedPreferences.getBoolean(AUTO_DOWNLOAD_IMAGES_WIFI, true)

    fun setAutoDownloadAudioOnWifi(enabled: Boolean) = sharedPreferences.edit().putBoolean(AUTO_DOWNLOAD_AUDIO_WIFI, enabled).apply()
    fun getAutoDownloadAudioOnWifi(): Boolean = sharedPreferences.getBoolean(AUTO_DOWNLOAD_AUDIO_WIFI, true)

    fun setAutoDownloadVideosOnWifi(enabled: Boolean) = sharedPreferences.edit().putBoolean(AUTO_DOWNLOAD_VIDEOS_WIFI, enabled).apply()
    fun getAutoDownloadVideosOnWifi(): Boolean = sharedPreferences.getBoolean(AUTO_DOWNLOAD_VIDEOS_WIFI, true)

    fun setAutoDownloadDocsOnWifi(enabled: Boolean) = sharedPreferences.edit().putBoolean(AUTO_DOWNLOAD_DOCS_WIFI, enabled).apply()
    fun getAutoDownloadDocsOnWifi(): Boolean = sharedPreferences.getBoolean(AUTO_DOWNLOAD_DOCS_WIFI, true)
}