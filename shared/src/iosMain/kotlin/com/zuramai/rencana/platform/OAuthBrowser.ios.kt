@file:OptIn(ExperimentalForeignApi::class)

package com.zuramai.rencana.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.AuthenticationServices.ASWebAuthenticationSessionCompletionHandler
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.darwin.NSObject

private class PresentationProvider : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): ASPresentationAnchor =
        UIApplication.sharedApplication.keyWindow

}

actual object OAuthBrowser {
    // Strong references: ASWebAuthenticationSession keeps its provider only weakly.
    private var provider: PresentationProvider? = null

    actual suspend fun authenticate(url: String): String? = suspendCancellableCoroutine { cont ->
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        val handler: ASWebAuthenticationSessionCompletionHandler = { callback, _ ->
            provider = null
            if (cont.isActive) cont.resume(callback?.absoluteString)
        }
        val session = ASWebAuthenticationSession(
            uRL = nsUrl,
            callbackURLScheme = "com.zuramai.rencana",
            completionHandler = handler
        )
        val p = PresentationProvider()
        provider = p
        session.presentationContextProvider = p
        session.prefersEphemeralWebBrowserSession = false
        cont.invokeOnCancellation { session.cancel() }
        if (!session.start()) {
            provider = null
            if (cont.isActive) cont.resume(null)
        }
    }
}
