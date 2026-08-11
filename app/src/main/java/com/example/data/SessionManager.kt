package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("foundry_session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_USER_ID = "active_user_id"
        private const val KEY_PREFIX_EMAIL = "email_for_uid_"
    }

    fun getActiveUserId(): String? {
        return prefs.getString(KEY_ACTIVE_USER_ID, null)
    }

    fun setActiveUserId(userId: String?) {
        if (userId == null) {
            prefs.edit().remove(KEY_ACTIVE_USER_ID).apply()
        } else {
            prefs.edit().putString(KEY_ACTIVE_USER_ID, userId).apply()
        }
    }

    fun saveEmailForUid(uid: String, email: String) {
        prefs.edit().putString("$KEY_PREFIX_EMAIL$uid", email.trim().lowercase()).apply()
    }

    fun getEmailForUid(uid: String): String? {
        return prefs.getString("$KEY_PREFIX_EMAIL$uid", null)
    }

    fun clearSession() {
        prefs.edit().remove(KEY_ACTIVE_USER_ID).apply()
    }
}

