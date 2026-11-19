package com.zuramai.rencana.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.Mascot
import com.zuramai.rencana.ui.components.MascotPose
import com.zuramai.rencana.ui.components.PillButton

/** Shown while the session is [com.zuramai.rencana.data.SessionState.Loading] or errored; [failed] shows Retry. */
@Composable
fun StartupScreen(failed: Boolean, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Mascot(if (failed) MascotPose.Construction else MascotPose.Happy, size = 140.dp)
        if (failed) {
            Text(t(S.COMMON_SOMETHING_WRONG), style = RencanaType.current.body.copy(color = RColors.Muted))
            PillButton(t(S.COMMON_RETRY), onRetry, Modifier.testTag("startup_retry"))
        } else {
            CircularProgressIndicator(color = RColors.Primary)
        }
    }
}
