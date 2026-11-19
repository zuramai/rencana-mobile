package com.zuramai.rencana.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zuramai.rencana.data.CostDto
import com.zuramai.rencana.data.CostInput
import com.zuramai.rencana.domain.COST_PRESETS
import com.zuramai.rencana.domain.CostPreset
import com.zuramai.rencana.domain.amountToInput
import com.zuramai.rencana.domain.normalizeAmount
import com.zuramai.rencana.i18n.LocalLanguage
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.i18n.text
import com.zuramai.rencana.platform.formatMoney
import com.zuramai.rencana.ui.LocalAppContainer
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.ConfirmDeleteDialog
import com.zuramai.rencana.ui.components.ErrorText
import com.zuramai.rencana.ui.components.FullPillButton
import com.zuramai.rencana.ui.components.PillStyle
import com.zuramai.rencana.ui.components.RencanaTextField
import com.zuramai.rencana.ui.components.apiErrorKey
import kotlinx.coroutines.launch

/** Create/edit/delete an "other cost" (web CostForm). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CostFormSheet(
    planId: String,
    budgetCurrency: String,
    tripDays: Int,
    cost: CostDto?,
    preset: CostPreset?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val container = LocalAppContainer.current
    val lang = LocalLanguage.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(cost?.name ?: preset?.label?.text(lang) ?: "") }
    var frequency by remember { mutableStateOf(cost?.frequency ?: preset?.frequency ?: "one_time") }
    var selectedPreset by remember { mutableStateOf(if (cost == null) preset?.key else null) }
    var currency by remember { mutableStateOf(cost?.currency ?: budgetCurrency) }
    var amount by remember { mutableStateOf(amountToInput(cost?.amount)) }
    var useTripLength by remember { mutableStateOf(cost?.days == null) }
    var days by remember { mutableStateOf((cost?.days ?: tripDays).toString()) }
    var pending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<S?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val normalized = normalizeAmount(amount)
    val dayCount = if (useTripLength) tripDays else days.toIntOrNull() ?: 0
    val total = normalized?.toDoubleOrNull()?.let { if (frequency == "daily") it * dayCount else it }

    fun save() {
        submitted = true
        val daysOk = frequency != "daily" || useTripLength || dayCount in 1..365
        if (name.isBlank() || normalized == null || (normalized.toDoubleOrNull() ?: 0.0) <= 0 || !daysOk) {
            error = S.ERRORS_VALIDATION
            return
        }
        error = null
        val input = CostInput(name.trim(), frequency, currency, normalized, if (frequency == "daily" && !useTripLength) dayCount else null)
        pending = true
        scope.launch {
            try {
                if (cost != null) container.plans.updateCost(planId, cost.id, input) else container.plans.createCost(planId, input)
                onSaved()
                onDismiss()
            } catch (e: Exception) {
                error = apiErrorKey(e)
                pending = false
            }
        }
    }

    fun delete() {
        if (cost == null) return
        pending = true
        scope.launch {
            try {
                container.plans.deleteCost(planId, cost.id)
                confirmDelete = false
                onSaved()
                onDismiss()
            } catch (e: Exception) {
                confirmDelete = false
                error = apiErrorKey(e)
                pending = false
            }
        }
    }

    FormSheet(
        title = t(if (cost != null) S.COSTS_EDIT_COST else S.COSTS_NEW_COST),
        onDismiss = { if (!pending) onDismiss() },
        footer = {
            FullPillButton(t(S.COMMON_SAVE), ::save, Modifier.testTag("cost_save"), loading = pending)
            if (cost != null) FullPillButton(t(S.COMMON_DELETE), { confirmDelete = true }, Modifier.testTag("cost_delete"), style = PillStyle.Secondary, enabled = !pending)
        }
    ) {
        error?.let { ErrorText(t(it)) }
        Labeled(t(S.COSTS_PRESETS)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (p in COST_PRESETS) {
                    ChoicePill(t(p.label), selectedPreset == p.key, {
                        selectedPreset = p.key
                        name = p.label.text(lang)
                        frequency = p.frequency
                    })
                }
            }
        }
        RencanaTextField(
            name, { name = it.take(80) }, Modifier.testTag("cost_name"),
            label = t(S.COSTS_NAME), isError = submitted && name.isBlank(), capitalization = KeyboardCapitalization.Sentences
        )
        Segmented(
            listOf("one_time" to t(S.COSTS_ONE_TIME), "daily" to t(S.COSTS_DAILY)), frequency, { frequency = it },
            tag = { "cost_freq_$it" }
        )
        Labeled(t(if (frequency == "daily") S.COSTS_AMOUNT_PER_DAY else S.COSTS_AMOUNT)) {
            MoneyField(
                currency, amount, { currency = it }, { amount = it }, "cost_amount",
                isError = submitted && (normalized == null || (normalized.toDoubleOrNull() ?: 0.0) <= 0)
            )
        }
        if (frequency == "daily") {
            SwitchRow(t(S.COSTS_USE_TRIP_LENGTH), useTripLength, { useTripLength = it }, sub = t(S.COSTS_TRIP_LENGTH_DAYS, "n" to tripDays))
            if (!useTripLength) {
                RencanaTextField(
                    days, { days = it.filter(Char::isDigit).take(3) }, Modifier.testTag("cost_days"),
                    label = t(S.COSTS_DAYS), keyboardType = KeyboardType.Number, isError = submitted && dayCount !in 1..365
                )
            }
        }
        if (total != null && amount.isNotBlank()) {
            Row(
                Modifier.fillMaxWidth().background(SoftBg, androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text(t(S.COSTS_TOTAL), style = RencanaType.current.bodySemi.copy(color = RColors.Muted))
                Text(formatMoney(total, currency, lang), style = RencanaType.current.title)
            }
        }
    }

    if (confirmDelete && cost != null) {
        ConfirmDeleteDialog(
            t(S.COSTS_DELETE_TITLE), t(S.COSTS_DELETE_TEXT, "name" to cost.name),
            onConfirm = ::delete, onDismiss = { confirmDelete = false }, loading = pending
        )
    }
}
