package com.zuramai.rencana.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.zuramai.rencana.AppContainer
import com.zuramai.rencana.data.MeInput
import com.zuramai.rencana.data.SessionState
import com.zuramai.rencana.i18n.LocalLanguage
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.i18n.text
import com.zuramai.rencana.platform.currencyOptions
import com.zuramai.rencana.ui.LocalAppContainer
import com.zuramai.rencana.ui.LocalSnackbar
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RShapes
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.ConfirmDialog
import com.zuramai.rencana.ui.components.CurrencyPickerSheet
import com.zuramai.rencana.ui.components.ErrorText
import com.zuramai.rencana.ui.components.FullPillButton
import com.zuramai.rencana.ui.components.PageHeader
import com.zuramai.rencana.ui.components.PillStyle
import com.zuramai.rencana.ui.components.RCard
import com.zuramai.rencana.ui.components.RencanaTextField
import com.zuramai.rencana.ui.components.apiErrorKey
import com.zuramai.rencana.ui.components.publicImageLoader
import com.zuramai.rencana.ui.screens.planform.DropdownField
import com.zuramai.rencana.ui.screens.planform.LabeledColumn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class SettingsViewModel(val container: AppContainer) : ViewModel() {
    private val me = (container.session.state.value as? SessionState.LoggedIn)?.me
    var displayName by mutableStateOf(me?.displayName ?: "")
    var locale by mutableStateOf(me?.locale ?: "en")
    var currency by mutableStateOf(me?.defaultCurrency ?: "USD")
    var pending by mutableStateOf(false)
    var error by mutableStateOf<S?>(null)

    /** Saves the profile; [onSaved] runs on success. */
    fun save(onSaved: () -> Unit) {
        if (pending) return
        if (displayName.trim().isEmpty()) {
            error = S.ERRORS_VALIDATION
            return
        }
        viewModelScope.launch {
            pending = true
            error = null
            try {
                container.auth.updateMe(MeInput(displayName.trim(), locale, currency))
                container.session.refreshMe()
                container.session.setLanguagePref(locale)
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = apiErrorKey(e)
            }
            pending = false
        }
    }

    fun logout() {
        container.scope.launch { container.session.logout() }
    }
}

/**
 * Settings (design 17). Tags: `settings_name`, `settings_language`, `settings_currency`, `settings_save`,
 * `settings_server`, `settings_logout`. [onOpenServer] navigates to the Server screen.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenServer: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { SettingsViewModel(container) }
    val state by container.session.state.collectAsStateWithLifecycle()
    val me = (state as? SessionState.LoggedIn)?.me
    val url by container.session.backendUrl.collectAsStateWithLifecycle()
    val snack = LocalSnackbar.current
    val lang = LocalLanguage.current
    var pickingCurrency by remember { mutableStateOf(false) }
    var confirmServer by remember { mutableStateOf(false) }
    val currencyName = remember(vm.currency, lang) { currencyOptions(lang).firstOrNull { it.first == vm.currency }?.second }

    Column(Modifier.fillMaxSize()) {
        PageHeader(onBack, title = t(S.SETTINGS_TITLE))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (me?.avatarUrl != null) {
                    AsyncImage(
                        model = me.avatarUrl, contentDescription = null, imageLoader = publicImageLoader(),
                        contentScale = ContentScale.Crop, modifier = Modifier.size(88.dp).clip(CircleShape)
                    )
                } else {
                    Box(Modifier.size(88.dp).background(RColors.Coral, CircleShape), contentAlignment = Alignment.Center) {
                        Text(
                            vm.displayName.trim().take(1).uppercase().ifEmpty { "K" },
                            style = RencanaType.current.display.copy(color = androidx.compose.ui.graphics.Color.White, fontSize = 28.sp)
                        )
                    }
                }
                val handle = me?.username?.let { "@$it" } ?: me?.email
                if (handle != null) Text(handle, style = RencanaType.current.caption.copy(color = RColors.Muted), modifier = Modifier.testTag("settings_handle"))
            }
            ErrorText(vm.error?.let { t(it) })
            RCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    RencanaTextField(
                        vm.displayName, { if (it.length <= 60) vm.displayName = it }, Modifier.testTag("settings_name"),
                        label = t(S.AUTH_DISPLAY_NAME)
                    )
                    LabeledColumn(t(S.SETTINGS_LANGUAGE)) {
                        DropdownField(listOf("en" to "English", "id" to "Bahasa Indonesia"), vm.locale, { vm.locale = it }, tag = "settings_language")
                    }
                    LabeledColumn(t(S.SETTINGS_DEFAULT_CURRENCY)) {
                        Surface(
                            onClick = { pickingCurrency = true },
                            shape = RShapes.Field, color = RColors.Surface, border = BorderStroke(1.5.dp, RColors.Border),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("settings_currency")
                        ) {
                            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(vm.currency, style = RencanaType.current.bodySemi)
                                Text(currencyName ?: "", style = RencanaType.current.body.copy(color = RColors.Muted), maxLines = 1, modifier = Modifier.weight(1f))
                                Icon(Lucide.ChevronDown, contentDescription = null, tint = RColors.Muted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            FullPillButton(
                t(S.COMMON_SAVE),
                { vm.save { container.scope.launch { snack.showSnackbar(S.SETTINGS_SAVED.text(vm.locale)) } } },
                Modifier.testTag("settings_save"), loading = vm.pending
            )
            val shown = url ?: container.session.suggestedUrl
            Text(
                t(S.SERVER_CURRENT, "url" to shown),
                style = RencanaType.current.caption.copy(color = RColors.Muted, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().clip(RShapes.Field).clickable { confirmServer = true }.padding(12.dp).testTag("settings_server")
            )
            FullPillButton(t(S.AUTH_LOGOUT), vm::logout, Modifier.testTag("settings_logout"), style = PillStyle.Secondary)
        }
    }

    if (pickingCurrency) CurrencyPickerSheet(vm.currency, { vm.currency = it }, { pickingCurrency = false })
    if (confirmServer) {
        ConfirmDialog(
            text = t(S.SERVER_CHANGE_WARNING),
            onConfirm = { confirmServer = false; onOpenServer() },
            onDismiss = { confirmServer = false },
            confirmLabel = t(S.SERVER_TITLE)
        )
    }
}
