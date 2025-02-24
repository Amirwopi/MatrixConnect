package com.matrixconnect.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager as AndroidXPreferenceManager

class PreferenceManager private constructor(context: Context) {
    private val prefs: SharedPreferences = AndroidXPreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        private const val KEY_SELECTED_SERVER = "selected_server"
        private const val KEY_AUTO_CONNECT = "auto_connect"
        private const val KEY_KILL_SWITCH = "kill_switch"
        private const val KEY_STEALTH_MODE = "stealth_mode"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_LAST_CONNECTED = "last_connected"
        private const val KEY_DATA_USAGE_RESET = "data_usage_reset"
        private const val KEY_TOTAL_BYTES_SENT = "total_bytes_sent"
        private const val KEY_TOTAL_BYTES_RECEIVED = "total_bytes_received"

        @Volatile
        private var INSTANCE: PreferenceManager? = null

        fun init(context: Context) {
            INSTANCE = PreferenceManager(context.applicationContext)
        }

        fun getInstance(): PreferenceManager {
            return INSTANCE ?: throw IllegalStateException("PreferenceManager must be initialized")
        }
    }

    var selectedServer: String?
        get() = prefs.getString(KEY_SELECTED_SERVER, null)
        set(value) = prefs.edit { putString(KEY_SELECTED_SERVER, value) }

    var autoConnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_CONNECT, value) }

    var killSwitch: Boolean
        get() = prefs.getBoolean(KEY_KILL_SWITCH, true)
        set(value) = prefs.edit { putBoolean(KEY_KILL_SWITCH, value) }

    var stealthMode: Boolean
        get() = prefs.getBoolean(KEY_STEALTH_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_STEALTH_MODE, value) }

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_DARK_MODE, value) }

    var notificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATION_ENABLED, value) }

    var lastConnected: Long
        get() = prefs.getLong(KEY_LAST_CONNECTED, 0)
        set(value) = prefs.edit { putLong(KEY_LAST_CONNECTED, value) }

    var dataUsageResetTime: Long
        get() = prefs.getLong(KEY_DATA_USAGE_RESET, 0)
        set(value) = prefs.edit { putLong(KEY_DATA_USAGE_RESET, value) }

    var totalBytesSent: Long
        get() = prefs.getLong(KEY_TOTAL_BYTES_SENT, 0)
        set(value) = prefs.edit { putLong(KEY_TOTAL_BYTES_SENT, value) }

    var totalBytesReceived: Long
        get() = prefs.getLong(KEY_TOTAL_BYTES_RECEIVED, 0)
        set(value) = prefs.edit { putLong(KEY_TOTAL_BYTES_RECEIVED, value) }

    fun resetDataUsage() {
        prefs.edit {
            putLong(KEY_DATA_USAGE_RESET, System.currentTimeMillis())
            putLong(KEY_TOTAL_BYTES_SENT, 0)
            putLong(KEY_TOTAL_BYTES_RECEIVED, 0)
        }
    }

    fun clearAll() {
        prefs.edit { clear() }
    }

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
