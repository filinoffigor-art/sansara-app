package ru.sansara.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("sansara_secure_session", Context.MODE_PRIVATE)
    private val alias = "sansara_session_aes_v1"

    fun save(session: SansaraSession) {
        val json = JSONObject().apply {
            put("userId",session.userId)
            put("clientId",session.clientId)
            put("role",session.role.name)
            put("phone",session.phone)
            put("displayName",session.displayName)
            put("createdAtEpochMs",session.createdAtEpochMs)
        }.toString()

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("payload", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun load(): SansaraSession? = runCatching {
        val iv = prefs.getString("iv", null) ?: return@runCatching null
        val payload = prefs.getString("payload", null) ?: return@runCatching null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
        )
        val raw = String(cipher.doFinal(Base64.decode(payload, Base64.NO_WRAP)), Charsets.UTF_8)
        val o = JSONObject(raw)
        SansaraSession(
            userId=o.getString("userId"),
            clientId=o.optString("clientId").takeIf { it.isNotBlank() && it != "null" },
            role=SansaraRole.valueOf(o.getString("role")),
            phone=o.optString("phone"),
            displayName=o.optString("displayName"),
            createdAtEpochMs=o.optLong("createdAtEpochMs",System.currentTimeMillis())
        )
    }.getOrNull()

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias,null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generateKey()
        }
    }
}
