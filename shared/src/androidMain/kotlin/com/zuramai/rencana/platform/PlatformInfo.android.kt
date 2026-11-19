package com.zuramai.rencana.platform

import java.util.Locale

actual object PlatformInfo {
    actual val isDebug: Boolean get() = AndroidPlatform.isDebug
    actual val defaultBackendUrl: String? get() = if (isDebug) "http://10.0.2.2:8787" else null
    actual val deviceLanguage: String
        get() = Locale.getDefault().language.let { if (it == "id" || it == "in") "id" else "en" }
}
