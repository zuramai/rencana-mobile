package com.zuramai.rencana.platform

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.encoding.Base64

private const val KEY_ALIAS = "rencana_tokens_v1"
private const val IV_LENGTH = 12

/** AES-256-GCM with a non-exportable AndroidKeyStore key; blob is `base64(iv + ciphertext)` in SharedPreferences. */
private class AndroidSecureStore(context: Context) : SecureStore {
    private val prefs = context.getSharedPreferences("rencana_secure", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        fun generate(strongBox: Boolean): SecretKey {
            val builder = KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
            if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) builder.setIsStrongBoxBacked(true)
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                .apply { init(builder.build()) }
                .generateKey()
        }
        // StrongBoxUnavailableException extends ProviderException.
        return try { generate(strongBox = true) } catch (_: ProviderException) { generate(strongBox = false) }
    }

    override fun get(): String? {
        val blob = prefs.getString("t", null) ?: return null
        return try {
            val raw = Base64.decode(blob)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw, 0, IV_LENGTH))
            String(cipher.doFinal(raw, IV_LENGTH, raw.size - IV_LENGTH), Charsets.UTF_8)
        } catch (_: Exception) {
            clear()
            null
        }
    }

    override fun put(value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val out = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit().putString("t", Base64.encode(out)).apply()
    }

    override fun clear() {
        prefs.edit().remove("t").apply()
    }
}

actual fun createSecureStore(): SecureStore = AndroidSecureStore(AndroidPlatform.appContext)
