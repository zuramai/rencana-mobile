@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.zuramai.rencana.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

private const val SERVICE = "com.zuramai.rencana"
private const val ACCOUNT = "tokens"

/** Keychain generic-password item, accessible only while unlocked and never migrated to another device. */
private class KeychainSecureStore : SecureStore {
    private fun newDict(): CFMutableDictionaryRef = CFDictionaryCreateMutable(
        null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr
    )!!

    /** Base query identifying our item. Values are retained by the dictionary; the bridged copies are released here. */
    private fun baseQuery(): CFMutableDictionaryRef {
        val d = newDict()
        CFDictionaryAddValue(d, kSecClass, kSecClassGenericPassword)
        val service = CFBridgingRetain(SERVICE)
        val account = CFBridgingRetain(ACCOUNT)
        CFDictionaryAddValue(d, kSecAttrService, service)
        CFDictionaryAddValue(d, kSecAttrAccount, account)
        CFRelease(service)
        CFRelease(account)
        return d
    }

    private fun dataOf(value: String): NSData {
        val bytes = value.encodeToByteArray()
        return if (bytes.isEmpty()) NSData() else bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.convert()) }
    }

    override fun get(): String? = memScoped {
        val q = baseQuery()
        CFDictionaryAddValue(q, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(q, kSecMatchLimit, kSecMatchLimitOne)
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(q as CFDictionaryRef, result.ptr)
        CFRelease(q)
        if (status != errSecSuccess) return@memScoped null
        val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
        val bytes = ByteArray(data.length.toInt())
        if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        bytes.decodeToString()
    }

    override fun put(value: String) {
        val data = CFBridgingRetain(dataOf(value))
        val query = baseQuery()
        val attrs = newDict()
        CFDictionaryAddValue(attrs, kSecValueData, data)
        val updated = SecItemUpdate(query as CFDictionaryRef, attrs as CFDictionaryRef)
        if (updated != errSecSuccess) {
            CFDictionaryAddValue(query, kSecValueData, data)
            CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
            SecItemAdd(query as CFDictionaryRef, null)
        }
        CFRelease(attrs)
        CFRelease(query)
        CFRelease(data)
    }

    override fun clear() {
        val q = baseQuery()
        SecItemDelete(q as CFDictionaryRef)
        CFRelease(q)
    }
}

actual fun createSecureStore(): SecureStore = KeychainSecureStore()
