package com.zuramai.rencana.platform

import java.security.MessageDigest
import java.security.SecureRandom

private val secureRandom = SecureRandom()

actual fun secureRandomBytes(n: Int): ByteArray = ByteArray(n).also { secureRandom.nextBytes(it) }

actual fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)
