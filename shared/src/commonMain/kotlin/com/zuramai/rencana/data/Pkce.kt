package com.zuramai.rencana.data

import com.zuramai.rencana.platform.base64Url
import com.zuramai.rencana.platform.secureRandomBytes
import com.zuramai.rencana.platform.sha256

/** PKCE (RFC 7636, S256) helpers. */
object Pkce {
    /** Random 43-char base64url string (32 bytes), used for `state` and the code verifier. */
    fun randomToken(): String = base64Url(secureRandomBytes(32))

    /** `BASE64URL(SHA256(ASCII(verifier)))` */
    fun challenge(verifier: String): String = base64Url(sha256(verifier.encodeToByteArray()))
}
