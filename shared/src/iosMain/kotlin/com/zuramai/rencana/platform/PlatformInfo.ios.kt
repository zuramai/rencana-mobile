package com.zuramai.rencana.platform

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

@OptIn(ExperimentalNativeApi::class)
actual object PlatformInfo {
    actual val isDebug: Boolean get() = Platform.isDebugBinary
    actual val defaultBackendUrl: String? get() = if (isDebug) "http://localhost:8787" else null
    actual val deviceLanguage: String
        get() {
            val first = (NSLocale.preferredLanguages.firstOrNull() as? String).orEmpty()
            return if (first.startsWith("id") || first.startsWith("in")) "id" else "en"
        }
}
