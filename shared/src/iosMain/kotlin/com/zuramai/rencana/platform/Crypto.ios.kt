@file:OptIn(ExperimentalForeignApi::class)

package com.zuramai.rencana.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault

actual fun secureRandomBytes(n: Int): ByteArray {
    val out = ByteArray(n)
    if (n > 0) {
        val status = out.usePinned { SecRandomCopyBytes(kSecRandomDefault, n.convert(), it.addressOf(0)) }
        check(status == errSecSuccess) { "SecRandomCopyBytes failed: $status" }
    }
    return out
}

actual fun sha256(bytes: ByteArray): ByteArray {
    val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
    // An empty array has no addressOf(0); hash a one-byte buffer with length 0 instead.
    val src = if (bytes.isEmpty()) ByteArray(1) else bytes
    src.usePinned { s ->
        digest.usePinned { d ->
            CC_SHA256(s.addressOf(0), bytes.size.convert(), d.addressOf(0).reinterpret())
        }
    }
    return digest
}
