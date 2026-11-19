package com.zuramai.rencana

import android.app.Activity
import android.os.Bundle
import com.zuramai.rencana.platform.OAuthCallbackBus

/** Receives `com.zuramai.rencana:/oauth/callback?...` from the Custom Tab and hands it to the waiting sign-in. */
class OAuthRedirectActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OAuthCallbackBus.complete(intent?.data?.toString())
        finish()
    }
}
