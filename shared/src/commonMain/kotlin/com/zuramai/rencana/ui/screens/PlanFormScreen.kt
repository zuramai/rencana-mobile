package com.zuramai.rencana.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zuramai.rencana.AppContainer
import com.zuramai.rencana.data.PlanInput
import com.zuramai.rencana.data.SessionState
import com.zuramai.rencana.data.UploadFile
import com.zuramai.rencana.domain.DEFAULT_COVER_EMOJI
import com.zuramai.rencana.domain.amountToInput
import com.zuramai.rencana.domain.normalizeAmount
import com.zuramai.rencana.i18n.LocalLanguage
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.i18n.text
import com.zuramai.rencana.ui.LocalAppContainer
import com.zuramai.rencana.ui.LocalSnackbar
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.components.ErrorText
import com.zuramai.rencana.ui.components.FullPillButton
import com.zuramai.rencana.ui.components.MoneyInput
import com.zuramai.rencana.ui.components.PageHeader
import com.zuramai.rencana.ui.components.PillButton
import com.zuramai.rencana.ui.components.RencanaTextField
import com.zuramai.rencana.ui.components.apiErrorKey
import com.zuramai.rencana.ui.screens.planform.CoverPicker
import com.zuramai.rencana.ui.screens.planform.CoverValue
import com.zuramai.rencana.ui.screens.planform.PeriodInput
import com.zuramai.rencana.ui.screens.planform.PeriodMode
import com.zuramai.rencana.ui.screens.planform.PeriodValue
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.compressImage
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Create ([planId] null) or edit form state; ports web `PlanForm`. */
class PlanFormViewModel(val container: AppContainer, val planId: String?) : ViewModel() {
    var loading by mutableStateOf(planId != null)
    var loadError by mutableStateOf<S?>(null)
    var name by mutableStateOf("")
    var cover by mutableStateOf(CoverValue(coverEmoji = DEFAULT_COVER_EMOJI))
    var coverUpload by mutableStateOf<UploadFile?>(null)
    var period by mutableStateOf(PeriodValue())
    var currency by mutableStateOf(
        (container.session.state.value as? SessionState.LoggedIn)?.me?.defaultCurrency ?: "USD"
    )
    var amount by mutableStateOf("")
    var pending by mutableStateOf(false)
    var error by mutableStateOf<S?>(null)

    init {
        if (planId != null) load()
    }

    fun load() {
        val id = planId ?: return
        viewModelScope.launch {
            loading = true
            loadError = null
            try {
                val p = container.plans.get(id)
                name = p.name
                cover = CoverValue(
                    coverType = if (p.coverType == "image") "image" else "emoji",
                    coverEmoji = p.coverEmoji ?: DEFAULT_COVER_EMOJI,
                    coverColor = p.coverColor,
                    coverFileId = p.coverFileId
                )
                period = PeriodValue(
                    mode = if (p.endYear != null) PeriodMode.Range else if (p.startYear != null) PeriodMode.Month else PeriodMode.None,
                    startYear = p.startYear, startMonth = p.startMonth, endYear = p.endYear, endMonth = p.endMonth
                )
                currency = p.budgetCurrency
                amount = amountToInput(p.budgetAmount)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadError = apiErrorKey(e)
            }
            loading = false
        }
    }

    fun onPhotoPicked(file: PlatformFile) {
        viewModelScope.launch {
            try {
                val bytes = FileKit.compressImage(file.readBytes(), quality = 90, maxWidth = 2000, maxHeight = 2000, imageFormat = ImageFormat.JPEG)
                coverUpload = UploadFile("cover.jpg", "image/jpeg", bytes)
                cover = cover.copy(coverType = "image", previewBytes = bytes)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = S.COMMON_SOMETHING_WRONG
            }
        }
    }

    /** Saves; [onSaved] receives the plan id. */
    fun submit(onSaved: (String) -> Unit) {
        if (pending) return
        if (cover.coverType == "image" && coverUpload == null && cover.coverFileId == null) {
            error = S.ERRORS_COVER_REQUIRED
            return
        }
        val range = period.mode == PeriodMode.Range
        val input = PlanInput(
            name = name,
            coverType = cover.coverType,
            coverEmoji = cover.coverEmoji,
            coverColor = cover.coverColor,
            startYear = period.startYear,
            startMonth = period.startMonth,
            endYear = if (range) period.endYear else null,
            endMonth = if (range) period.endMonth else null,
            budgetCurrency = currency,
            budgetAmount = normalizeAmount(amount)
        )
        val upload = if (cover.coverType == "image") coverUpload else null
        viewModelScope.launch {
            pending = true
            error = null
            try {
                val res = if (planId != null) container.plans.update(planId, input, upload) else container.plans.create(input, upload)
                onSaved(res.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = apiErrorKey(e)
            }
            pending = false
        }
    }
}

/**
 * Create / edit plan (design 05). Tags: `plan_name`, `plan_budget_amount`, `plan_budget_currency`, `plan_submit`.
 * [onDone] receives the saved plan id; [onBack] cancels. (Web has no delete here; deleting lives on the list.)
 */
@Composable
fun PlanFormScreen(planId: String?, @Suppress("UNUSED_PARAMETER") fromList: Boolean, onDone: (String?) -> Unit, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel(key = planId ?: "new") { PlanFormViewModel(container, planId) }
    val snack = LocalSnackbar.current
    val lang = LocalLanguage.current
    val picker = rememberFilePickerLauncher(type = FileKitType.Image, mode = FileKitMode.Single) { file ->
        if (file != null) vm.onPhotoPicked(file)
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader(onBack, title = t(if (planId == null) S.PLAN_NEW_TRIP else S.PLAN_EDIT_TRIP))
        when {
            vm.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RColors.Primary) }
            vm.loadError != null -> Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ErrorText(t(vm.loadError!!))
                PillButton(t(S.COMMON_RETRY), vm::load)
            }
            else -> Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ErrorText(vm.error?.let { t(it) })
                    CoverPicker(vm.cover, { vm.cover = it }, picker::launch)
                    RencanaTextField(
                        vm.name, { if (it.length <= 80) vm.name = it }, Modifier.testTag("plan_name"),
                        label = t(S.PLAN_TRIP_NAME), placeholder = t(S.PLAN_TRIP_NAME_PLACEHOLDER)
                    )
                    PeriodInput(vm.period, { vm.period = it })
                    MoneyInput(
                        vm.currency, vm.amount, { vm.currency = it }, { vm.amount = it },
                        Modifier.fillMaxWidth(), label = t(S.PLAN_EXPECTED_BUDGET), tagPrefix = "plan_budget"
                    )
                }
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    FullPillButton(
                        t(if (planId == null) S.PLAN_CREATE else S.COMMON_SAVE),
                        {
                            if (vm.name.isBlank()) {
                                vm.error = S.ERRORS_VALIDATION
                            } else {
                                vm.submit { id ->
                                    container.scope.launch { snack.showSnackbar((if (planId == null) S.TRAVEL_TRIP_CREATED else S.TRAVEL_TRIP_SAVED).text(lang)) }
                                    onDone(id)
                                }
                            }
                        },
                        Modifier.testTag("plan_submit"), loading = vm.pending
                    )
                }
            }
        }
    }
}
