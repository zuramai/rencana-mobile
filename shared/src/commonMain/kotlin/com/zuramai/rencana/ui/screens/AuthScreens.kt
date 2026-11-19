package com.zuramai.rencana.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zuramai.rencana.data.ApiException
import com.zuramai.rencana.data.GoogleOutcome
import com.zuramai.rencana.data.SessionManager
import com.zuramai.rencana.i18n.LocalLanguage
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.LocalAppContainer
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.ErrorText
import com.zuramai.rencana.ui.components.FullPillButton
import com.zuramai.rencana.ui.components.GoogleButton
import com.zuramai.rencana.ui.components.LangSwitch
import com.zuramai.rencana.ui.components.Mascot
import com.zuramai.rencana.ui.components.MascotPose
import com.zuramai.rencana.ui.components.OrDivider
import com.zuramai.rencana.ui.components.RencanaTextField
import com.zuramai.rencana.ui.components.apiErrorKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val USERNAME_RE = Regex("^[A-Za-z0-9_.]{3,32}$")

/** Runs [block], mapping any failure to [onError] (never swallowing cancellation). */
private suspend fun guarded(onError: (S) -> Unit, block: suspend () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onError(apiErrorKey(e))
    }
}

class LoginViewModel(private val session: SessionManager) : ViewModel() {
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var pending by mutableStateOf(false)
    var error by mutableStateOf<S?>(null)

    fun submit() {
        if (pending) return
        viewModelScope.launch {
            pending = true
            error = null
            guarded({ error = it }) { session.login(username.trim(), password) }
            pending = false
        }
    }

    fun google() {
        if (pending) return
        viewModelScope.launch {
            pending = true
            error = null
            try {
                if (session.googleSignIn() == GoogleOutcome.Failed) error = S.ERRORS_GOOGLE
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = S.ERRORS_GOOGLE
            }
            pending = false
        }
    }
}

/**
 * Login (design 01). Test tags: `login_username`, `login_password`, `login_submit`, `login_google`,
 * `login_register`, `login_server`.
 */
@Composable
fun LoginScreen(onRegister: () -> Unit, onServer: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { LoginViewModel(container.session) }
    val lang = LocalLanguage.current
    val url by container.session.backendUrl.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            LangSwitch(lang, container.session::setLanguagePref)
        }
        Mascot(MascotPose.Wave, size = 160.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t(S.AUTH_WELCOME_BACK), style = RencanaType.current.display, textAlign = TextAlign.Center)
            Text(t(S.AUTH_LOGIN_SUBTITLE), style = RencanaType.current.body.copy(color = RColors.Muted), textAlign = TextAlign.Center)
        }
        vm.error?.let { ErrorText(t(it)) }
        GoogleButton(vm::google, Modifier.fillMaxWidth().testTag("login_google"))
        OrDivider()
        RencanaTextField(
            vm.username, { vm.username = it }, Modifier.testTag("login_username"),
            label = t(S.AUTH_USERNAME), capitalization = KeyboardCapitalization.None
        )
        RencanaTextField(
            vm.password, { vm.password = it }, Modifier.testTag("login_password"),
            label = t(S.AUTH_PASSWORD), password = true, imeAction = ImeAction.Done, onImeAction = vm::submit
        )
        FullPillButton(t(S.AUTH_LOGIN), vm::submit, Modifier.testTag("login_submit"), loading = vm.pending)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t(S.AUTH_NEW_HERE), style = RencanaType.current.caption.copy(color = RColors.Muted))
            Text(
                t(S.AUTH_CREATE_AN_ACCOUNT),
                style = RencanaType.current.caption.copy(color = RColors.Primary),
                modifier = Modifier.clickable(onClick = onRegister).testTag("login_register")
            )
        }
        val shownUrl = url ?: container.session.suggestedUrl
        Text(
            if (shownUrl.isEmpty()) t(S.SERVER_TITLE) else t(S.SERVER_CURRENT, "url" to shownUrl),
            style = RencanaType.current.caption,
            modifier = Modifier.clickable(onClick = onServer).testTag("login_server")
        )
    }
}

class RegisterViewModel(private val session: SessionManager) : ViewModel() {
    var displayName by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var pending by mutableStateOf(false)
    var error by mutableStateOf<S?>(null)

    fun submit() {
        if (pending) return
        if (displayName.trim().isEmpty() || displayName.length > 60 || !USERNAME_RE.matches(username.trim()) || password.length !in 8..128) {
            error = S.ERRORS_VALIDATION
            return
        }
        viewModelScope.launch {
            pending = true
            error = null
            guarded({ error = it }) { session.register(username.trim(), displayName.trim(), password) }
            pending = false
        }
    }

    fun google() {
        if (pending) return
        viewModelScope.launch {
            pending = true
            error = null
            try {
                if (session.googleSignIn() == GoogleOutcome.Failed) error = S.ERRORS_GOOGLE
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = S.ERRORS_GOOGLE
            }
            pending = false
        }
    }
}

/**
 * Register (design 02). Sends the device time zone and the current language. Test tags:
 * `register_displayName`, `register_username`, `register_password`, `register_submit`, `register_google`, `register_login`.
 */
@Composable
fun RegisterScreen(onLogin: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { RegisterViewModel(container.session) }
    val lang = LocalLanguage.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            LangSwitch(lang, container.session::setLanguagePref)
        }
        Mascot(MascotPose.Wave, size = 120.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t(S.AUTH_CREATE_ACCOUNT_TITLE), style = RencanaType.current.display, textAlign = TextAlign.Center)
            Text(t(S.AUTH_REGISTER_SUBTITLE), style = RencanaType.current.body.copy(color = RColors.Muted), textAlign = TextAlign.Center)
        }
        vm.error?.let { ErrorText(t(it)) }
        RencanaTextField(
            vm.displayName, { if (it.length <= 60) vm.displayName = it }, Modifier.testTag("register_displayName"),
            label = t(S.AUTH_DISPLAY_NAME), capitalization = KeyboardCapitalization.Words
        )
        RencanaTextField(
            vm.username, { if (it.length <= 32) vm.username = it }, Modifier.testTag("register_username"),
            label = t(S.AUTH_USERNAME), supportingText = t(S.AUTH_USERNAME_HINT)
        )
        RencanaTextField(
            vm.password, { if (it.length <= 128) vm.password = it }, Modifier.testTag("register_password"),
            label = t(S.AUTH_PASSWORD), supportingText = t(S.AUTH_PASSWORD_HINT), password = true,
            imeAction = ImeAction.Done, onImeAction = vm::submit
        )
        FullPillButton(t(S.AUTH_CREATE_ACCOUNT), vm::submit, Modifier.testTag("register_submit"), loading = vm.pending)
        OrDivider()
        GoogleButton(vm::google, Modifier.fillMaxWidth().testTag("register_google"))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t(S.AUTH_HAVE_ACCOUNT), style = RencanaType.current.caption.copy(color = RColors.Muted))
            Text(
                t(S.AUTH_LOGIN),
                style = RencanaType.current.caption.copy(color = RColors.Primary),
                modifier = Modifier.clickable(onClick = onLogin).testTag("register_login")
            )
        }
    }
}
