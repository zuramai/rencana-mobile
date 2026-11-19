package com.zuramai.rencana.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zuramai.rencana.data.SessionManager
import com.zuramai.rencana.data.SessionState
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.LocalAppContainer
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.ConfirmDialog
import com.zuramai.rencana.ui.components.ErrorText
import com.zuramai.rencana.ui.components.FullPillButton
import com.zuramai.rencana.ui.components.Mascot
import com.zuramai.rencana.ui.components.MascotPose
import com.zuramai.rencana.ui.components.PageHeader
import com.zuramai.rencana.ui.components.RencanaTextField
import kotlinx.coroutines.launch

class ServerViewModel(private val session: SessionManager) : ViewModel() {
    var url by mutableStateOf(session.suggestedUrl)
    var checking by mutableStateOf(false)
    var error by mutableStateOf<S?>(null)
    var confirming by mutableStateOf(false)

    /** True when saving signs the user out, so the UI asks first. */
    val needsConfirm: Boolean get() = session.state.value is SessionState.LoggedIn

    /** Validates, health-checks and saves; calls [onSaved] on success. */
    fun save(onSaved: () -> Unit) {
        if (checking) return
        val parsed = session.parseUrl(url).getOrNull()
        if (parsed == null) {
            error = S.SERVER_INVALID
            return
        }
        viewModelScope.launch {
            checking = true
            error = null
            if (!session.checkUrl(parsed)) {
                error = S.SERVER_UNREACHABLE
            } else {
                session.changeServer(parsed)
                onSaved()
            }
            checking = false
        }
    }
}

/**
 * Backend address (plan 3.6). Test tags: `server_url`, `server_save`. [onBack] null hides the back button
 * (first-run). [onSaved] runs after a successful save (the root navigation also reacts to the session state).
 */
@Composable
fun ServerScreen(onSaved: () -> Unit, onBack: (() -> Unit)?) {
    val container = LocalAppContainer.current
    val vm = viewModel { ServerViewModel(container.session) }

    Column(Modifier.fillMaxSize().imePadding()) {
        if (onBack != null) PageHeader(onBack, title = t(S.SERVER_TITLE))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Mascot(MascotPose.Map, size = 140.dp)
            if (onBack == null) Text(t(S.SERVER_TITLE), style = RencanaType.current.display)
            vm.error?.let { ErrorText(t(it)) }
            RencanaTextField(
                vm.url, { vm.url = it; vm.error = null }, Modifier.testTag("server_url"),
                label = t(S.SERVER_LABEL), placeholder = t(S.SERVER_HINT),
                keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done,
                onImeAction = { if (vm.needsConfirm) vm.confirming = true else vm.save(onSaved) }
            )
            FullPillButton(
                if (vm.checking) t(S.SERVER_CHECKING) else t(S.SERVER_SAVE),
                { if (vm.needsConfirm) vm.confirming = true else vm.save(onSaved) },
                Modifier.testTag("server_save"),
                loading = vm.checking
            )
            if (vm.needsConfirm) Text(t(S.SERVER_CHANGE_WARNING), style = RencanaType.current.caption.copy(color = RColors.Muted))
        }
    }
    if (vm.confirming) {
        ConfirmDialog(
            text = t(S.SERVER_CHANGE_WARNING),
            onConfirm = { vm.confirming = false; vm.save(onSaved) },
            onDismiss = { vm.confirming = false }
        )
    }
}
