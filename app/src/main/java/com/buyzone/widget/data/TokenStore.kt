package com.buyzone.widget.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "secure_credentials",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun read(): String? = preferences.getString(TIINGO_TOKEN, null)

    fun save(token: String) {
        preferences.edit().putString(TIINGO_TOKEN, token.trim()).apply()
    }

    fun clear() {
        preferences.edit().remove(TIINGO_TOKEN).apply()
    }

    private companion object {
        const val TIINGO_TOKEN = "tiingo_token"
    }
}
