package ru.sansara.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class SansaraRole { CLIENT, ADMIN, PRODUCTION }

data class SansaraSession(
    val userId: String,
    val clientId: String?,
    val role: SansaraRole,
    val firstName: String
)

data class SansaraAuthResult(
    val session: SansaraSession?,
    val message: String
) {
    val ok: Boolean get() = session != null
}

interface AuthProvider {
    suspend fun signInWithAccessCode(code:String):SansaraAuthResult
    fun currentSession():SansaraSession?
    fun signOut()
}

class LocalAuthProvider(
    private val repository:SansaraRepository,
    private val sessionStore:SecureSessionStore
):AuthProvider {
    override suspend fun signInWithAccessCode(code:String):SansaraAuthResult {
        val session=repository.authenticate(code)
            ?: return SansaraAuthResult(null,"Код доступа не найден или отозван")
        sessionStore.save(session)
        return SansaraAuthResult(session,"Вход выполнен")
    }

    override fun currentSession():SansaraSession?=sessionStore.load()
    override fun signOut()=sessionStore.clear()
}

class SecureSessionStore(context:Context) {
    private val prefs=context.getSharedPreferences("sansara_secure_session",Context.MODE_PRIVATE)
    private val vault=SansaraVault(context.applicationContext)

    fun save(session:SansaraSession) {
        val json=JSONObject().apply {
            put("userId",session.userId)
            put("clientId",session.clientId ?: JSONObject.NULL)
            put("role",session.role.name)
            put("firstName",session.firstName)
        }.toString()
        prefs.edit().putString("session",vault.encrypt(json)).apply()
    }

    fun load():SansaraSession? {
        val encrypted=prefs.getString("session",null) ?: return null
        return runCatching {
            val o=JSONObject(vault.decrypt(encrypted))
            SansaraSession(
                userId=o.getString("userId"),
                clientId=if(o.isNull("clientId")) null else o.getString("clientId"),
                role=SansaraRole.valueOf(o.getString("role")),
                firstName=o.optString("firstName","")
            )
        }.getOrNull()
    }

    fun clear() { prefs.edit().remove("session").apply() }
}

class SansaraVault(context:Context) {
    private val alias="sansara_master_aes_v1"
    private val keyStore=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun key():SecretKey {
        val existing=keyStore.getKey(alias,null) as? SecretKey
        if(existing!=null) return existing
        val generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(plain:String):String {
        if(plain.isEmpty()) return ""
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key())
        val encrypted=cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8))
        val payload=ByteArray(cipher.iv.size+encrypted.size)
        System.arraycopy(cipher.iv,0,payload,0,cipher.iv.size)
        System.arraycopy(encrypted,0,payload,cipher.iv.size,encrypted.size)
        return Base64.encodeToString(payload,Base64.NO_WRAP)
    }

    fun decrypt(encoded:String):String {
        if(encoded.isEmpty()) return ""
        return runCatching {
            val payload=Base64.decode(encoded,Base64.NO_WRAP)
            val iv=payload.copyOfRange(0,12)
            val encrypted=payload.copyOfRange(12,payload.size)
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,iv))
            String(cipher.doFinal(encrypted),StandardCharsets.UTF_8)
        }.getOrDefault("")
    }
}
