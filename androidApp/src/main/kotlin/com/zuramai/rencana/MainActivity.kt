package com.zuramai.rencana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zuramai.rencana.platform.AndroidPlatform
import com.zuramai.rencana.platform.OAuthCallbackBus
import com.zuramai.rencana.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidPlatform.init(this, BuildConfig.DEBUG)
        setContent { App() }
    }

    override fun onResume() {
        super.onResume()
        // Back from the Custom Tab without a callback means the user cancelled.
        OAuthCallbackBus.cancelPending()
    }

    override fun onDestroy() {
        AndroidPlatform.detach(this)
        super.onDestroy()
    }
}
