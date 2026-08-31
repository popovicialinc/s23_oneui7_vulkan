package com.popovicialinc.gama

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

/** Capability token required by exported Tasker broadcasts. */
object TaskerAuth {
    private const val PREFS = "tasker_auth"
    private const val PREF_TOKEN = "tasker_access_token"

    @Synchronized
    fun getOrCreateToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(PREF_TOKEN, null)?.let { return it }

        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        val token = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        prefs.edit().putString(PREF_TOKEN, token).commit()
        return token
    }

    fun isValid(context: Context, provided: String?): Boolean {
        if (provided.isNullOrBlank()) return false
        val expected = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(PREF_TOKEN, null) ?: return false
        return MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.UTF_8),
            provided.toByteArray(StandardCharsets.UTF_8)
        )
    }
}
