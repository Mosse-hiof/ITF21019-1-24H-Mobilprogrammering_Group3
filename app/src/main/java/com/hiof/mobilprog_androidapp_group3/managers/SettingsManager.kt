package com.hiof.mobilprog_androidapp_group3.managers

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

    fun saveSettings(key: String, value: Any) {
        with(sharedPreferences.edit()) {
            when (value) {
                is String -> putString(key, value)
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Float -> putFloat(key, value)
                else -> error("Unsupported data type")
            }
            apply()
        }
    }

    fun getSettings(key: String, default: Any): Any {
        return when (default) {
            is String -> sharedPreferences.getString(key, default) ?: default
            is Boolean -> sharedPreferences.getBoolean(key, default)
            is Int -> sharedPreferences.getInt(key, default)
            is Float -> sharedPreferences.getFloat(key, default)
            else -> error("Unsupported data type")
        }
    }

    fun updateBluetoothSettings(enableBluetooth: Boolean) {
        // Code to enable/disable Bluetooth connectivity for earphones
    }
}