package com.zuramai.rencana.platform

import kotlin.io.encoding.Base64
import kotlinx.datetime.TimeZone

/**
 * Holds the single encrypted token blob (JSON). Android: AES-GCM key in AndroidKeyStore; iOS: Keychain.
 * Implementations never throw: unreadable data is wiped and reported as null.
 */
interface SecureStore {
    /** Returns the stored blob or null when nothing (or nothing decryptable) is stored. */
    fun get(): String?

    /** Replaces the stored blob. */
    fun put(value: String)

    /** Removes the stored blob. */
    fun clear()
}

/** Creates the platform's [SecureStore]. */
expect fun createSecureStore(): SecureStore

/** Non-secret settings only (`backend_url`, `language`). */
interface Prefs {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

/** Creates the platform's [Prefs] (SharedPreferences / NSUserDefaults). */
expect fun createPrefs(): Prefs

/** [n] cryptographically secure random bytes. */
expect fun secureRandomBytes(n: Int): ByteArray

/** SHA-256 digest of [bytes]. */
expect fun sha256(bytes: ByteArray): ByteArray

/** Base64url without padding (RFC 4648 section 5), as used by PKCE. */
fun base64Url(bytes: ByteArray): String = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(bytes)

/**
 * Opens [url] in the system's secure auth browser (Custom Tab / ASWebAuthenticationSession) and returns
 * the callback URL (`com.zuramai.rencana:/oauth/callback?...`), or null when the user cancelled.
 */
expect object OAuthBrowser {
    suspend fun authenticate(url: String): String?
}

/** Build/device facts. */
expect object PlatformInfo {
    /** True in debug builds: allows `http://` backends, enables request logging. */
    val isDebug: Boolean

    /** Prefilled backend URL in debug builds (emulator host alias), null in release. */
    val defaultBackendUrl: String?

    /** `"id"` when the system language is Indonesian, else `"en"`. */
    val deviceLanguage: String
}

/** IANA id of the device time zone. */
fun timeZoneId(): String = TimeZone.currentSystemDefault().id

/** Formats [amount] in [currency] for [lang] (`en` -> en-US, `id` -> id-ID), e.g. `Rp25.000.000`. */
expect fun formatMoney(amount: Double, currency: String, lang: String): String

/** All currencies as (ISO code, localised name) sorted by code. */
expect fun currencyOptions(lang: String): List<Pair<String, String>>

/**
 * Opens [file] with the system default viewer. Android passes the FileProvider authority
 * (`com.zuramai.rencana.fileprovider`); iOS uses the document interaction. Throws on failure.
 */
expect suspend fun openWithDefaultApp(file: io.github.vinceglb.filekit.PlatformFile)
