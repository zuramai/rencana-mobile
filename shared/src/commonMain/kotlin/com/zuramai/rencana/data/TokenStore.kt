package com.zuramai.rencana.data

import com.zuramai.rencana.platform.SecureStore
import kotlinx.serialization.Serializable

/** Persisted token blob; bound to the backend it was issued by. */
@Serializable
data class StoredTokens(
    val baseUrl: String,
    val accessToken: String,
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String
)

/**
 * Token persistence over [SecureStore] for one backend ([baseUrl]). A blob issued by another backend is
 * wiped and ignored, so tokens never leak to a different server after the user changes the address.
 */
class TokenStore(private val store: SecureStore, private val baseUrl: String) {
    /** Current tokens for [baseUrl], or null. */
    fun load(): StoredTokens? {
        val raw = store.get() ?: return null
        val tokens = runCatching { ApiJson.decodeFromString(StoredTokens.serializer(), raw) }.getOrNull()
        if (tokens == null || tokens.baseUrl != baseUrl) {
            store.clear()
            return null
        }
        return tokens
    }

    /**
     * Persists a freshly issued [pair]. Throws when the platform store did not keep it (e.g. an iOS build
     * without keychain entitlements), so sign-in fails visibly instead of silently losing the session.
     */
    fun save(pair: TokenPair) {
        val blob = StoredTokens(baseUrl, pair.accessToken, pair.accessTokenExpiresAt, pair.refreshToken, pair.refreshTokenExpiresAt)
        val encoded = ApiJson.encodeToString(StoredTokens.serializer(), blob)
        store.put(encoded)
        check(store.get() == encoded) { "Secure storage did not keep the session tokens" }
    }

    /** Forgets the tokens. */
    fun clear() = store.clear()
}
