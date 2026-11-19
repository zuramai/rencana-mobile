package com.zuramai.rencana.platform

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.CompletableDeferred

/**
 * Rendezvous between the Custom Tab flow and `OAuthRedirectActivity`.
 * The redirect activity calls [complete] with the callback URL; `MainActivity.onResume` calls [cancelPending].
 */
object OAuthCallbackBus {
    @Volatile
    private var pending: CompletableDeferred<String?>? = null

    internal fun register(deferred: CompletableDeferred<String?>) {
        pending?.complete(null)
        pending = deferred
    }

    internal fun unregister(deferred: CompletableDeferred<String?>) {
        if (pending === deferred) pending = null
    }

    /** Delivers the callback [url] to the waiting sign-in, if any. */
    fun complete(url: String?) {
        pending?.complete(url)
    }

    /** Treats a still-pending sign-in as cancelled (the user came back without a callback). */
    fun cancelPending() {
        pending?.complete(null)
    }
}

actual object OAuthBrowser {
    actual suspend fun authenticate(url: String): String? {
        val activity = AndroidPlatform.activity ?: return null
        val deferred = CompletableDeferred<String?>()
        OAuthCallbackBus.register(deferred)
        return try {
            CustomTabsIntent.Builder().build().launchUrl(activity, Uri.parse(url))
            deferred.await()
        } finally {
            OAuthCallbackBus.unregister(deferred)
        }
    }
}
