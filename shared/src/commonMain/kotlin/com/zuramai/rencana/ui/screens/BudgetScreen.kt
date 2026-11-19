package com.zuramai.rencana.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zuramai.rencana.data.SessionState
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.LocalAppContainer
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.EmptyState
import com.zuramai.rencana.ui.components.MascotPose
import com.zuramai.rencana.ui.components.RencanaTopBar

/** Budget (design 16): "coming soon" page, as on web. */
@Composable
fun BudgetScreen(onOpenSettings: () -> Unit) {
    val container = LocalAppContainer.current
    val state by container.session.state.collectAsStateWithLifecycle()
    val me = (state as? SessionState.LoggedIn)?.me
    Column(Modifier.fillMaxSize()) {
        RencanaTopBar(me?.displayName, me?.avatarUrl, onOpenSettings)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(48.dp)) {
            Text(t(S.BUDGET_TITLE), style = RencanaType.current.display)
            EmptyState(MascotPose.Construction, t(S.BUDGET_COMING_SOON), t(S.BUDGET_COMING_SOON_TEXT))
        }
    }
}
