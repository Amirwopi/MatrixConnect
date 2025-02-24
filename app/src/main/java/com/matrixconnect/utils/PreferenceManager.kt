package com.matrixconnect.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

class PreferenceManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "matrix_connect_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_AUTO_CONNECT = "auto_connect"
        private const val KEY_KILL_SWITCH = "kill_switch"
        private const val KEY_STEALTH_MODE = "stealth_mode"
        private const val KEY_SELECTED_SERVER = "selected_server"
        private const val KEY_AUTO_SERVER_SELECTION = "auto_server_selection"

        @Volatile
        private var instance: PreferenceManager? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = PreferenceManager(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(): PreferenceManager {
            return instance ?: throw IllegalStateException("PreferenceManager must be initialized first")
        }
    }

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = prefs.edit { putInt(KEY_THEME_MODE, value) }

    var autoConnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_CONNECT, value) }

    var killSwitch: Boolean
        get() = prefs.getBoolean(KEY_KILL_SWITCH, true)
        set(value) = prefs.edit { putBoolean(KEY_KILL_SWITCH, value) }

    var stealthMode: Boolean
        get() = prefs.getBoolean(KEY_STEALTH_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_STEALTH_MODE, value) }

    var selectedServer: String?
        get() = prefs.getString(KEY_SELECTED_SERVER, null)
        set(value) = prefs.edit { putString(KEY_SELECTED_SERVER, value) }

    var autoServerSelection: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SERVER_SELECTION, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_SERVER_SELECTION, value) }

    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    fun clear() {
        prefs.edit { clear() }
    }
}
