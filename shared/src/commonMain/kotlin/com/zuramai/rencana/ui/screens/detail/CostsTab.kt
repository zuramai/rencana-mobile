package com.zuramai.rencana.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.TriangleAlert
import com.composables.icons.lucide.X
import com.zuramai.rencana.data.CostDto
import com.zuramai.rencana.data.SummaryDto
import com.zuramai.rencana.domain.COST_PRESETS
import com.zuramai.rencana.domain.CostPreset
import com.zuramai.rencana.domain.ITEM_TYPES
import com.zuramai.rencana.domain.monthName
import com.zuramai.rencana.i18n.LocalLanguage
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.i18n.text
import com.zuramai.rencana.platform.formatMoney
import com.zuramai.rencana.ui.LocalAppContainer
import com.zuramai.rencana.ui.LocalSnackbar
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.RCard
import com.zuramai.rencana.ui.components.RencanaTextField
import com.zuramai.rencana.ui.components.apiErrorKey
import com.zuramai.rencana.ui.screens.ProgressBar
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/** Costs tab: budget summary, other costs and exchange rates (web BudgetSummaryCard, CostList, RatesCard). */
@Composable
internal fun CostsTab(
    planId: String,
    costs: List<CostDto>,
    summary: SummaryDto,
    onOpenCost: (CostDto?, CostPreset?) -> Unit,
    onRatesChanged: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard(summary)
        CostList(costs, summary, onOpenCost)
        RatesCard(planId, summary, onRatesChanged)
    }
}

@Composable
private fun SummaryCard(s: SummaryDto) {
    val lang = LocalLanguage.current
    val type = RencanaType.current
    val cur = s.currency
    val remaining = s.remaining
    val over = (remaining ?: 0.0) < 0
    val ratio = s.budgetAmount?.takeIf { it > 0 }?.let { s.grandTotal / it } ?: 0.0
    val accent = if (over) RColors.Coral else RColors.Primary
    RCard(Modifier.fillMaxWidth().testTag("budget_summary")) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (remaining != null) {
                Text(t(if (over) S.COSTS_OVER else S.COSTS_REMAINING), style = type.caption)
                Text(formatMoney(abs(remaining), cur, lang), style = type.display.copy(fontSize = 32.sp, lineHeight = 38.sp, color = accent))
                ProgressBar(ratio.coerceIn(0.0, 1.0).toFloat(), accent)
            } else {
                Text(t(S.COSTS_TOTAL_PLANNED), style = type.caption)
                Text(formatMoney(s.grandTotal, cur, lang), style = type.display.copy(fontSize = 32.sp, lineHeight = 38.sp, color = RColors.Primary))
            }
            Line(t(S.COSTS_ITINERARY), formatMoney(s.itineraryTotal, cur, lang), bold = true)
            for (ty in ITEM_TYPES) {
                val amt = s.byType[ty.value] ?: continue
                Row(Modifier.padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).background(ty.color, CircleShape))
                    Text(t(ty.label), Modifier.weight(1f), style = type.body.copy(fontSize = 14.sp, color = RColors.Muted))
                    Text(formatMoney(amt, cur, lang), style = type.body.copy(fontSize = 14.sp, color = RColors.Muted))
                }
            }
            Line(t(S.COSTS_OTHER_COSTS), formatMoney(s.otherCostsTotal, cur, lang), bold = true)
            Box(Modifier.fillMaxWidth().height(1.dp).background(RColors.Border))
            Line(t(S.COSTS_TOTAL_PLANNED), formatMoney(s.grandTotal, cur, lang), bold = true, strong = true)
            Line(
                t(S.COSTS_EXPECTED_BUDGET),
                s.budgetAmount?.let { formatMoney(it, cur, lang) } ?: t(S.COSTS_NO_BUDGET), bold = true
            )
            for (c in s.missingRates) Warning(t(S.COSTS_MISSING_RATE, "currency" to c))
        }
    }
}

@Composable
private fun Line(label: String, value: String, bold: Boolean = false, strong: Boolean = false) {
    val type = RencanaType.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f), style = if (strong) type.bodySemi.copy(fontSize = 14.sp) else type.body.copy(fontSize = 14.sp, color = RColors.Muted))
        Text(value, style = (if (bold) type.bodySemi else type.body).copy(fontSize = 14.sp))
    }
}

@Composable
private fun Warning(text: String) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFFFEF3C7), RoundedCornerShape(12.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Lucide.TriangleAlert, null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
        Text(text, Modifier.weight(1f), style = RencanaType.current.caption.copy(color = Color(0xFFB45309)))
    }
}

@Composable
private fun CostList(costs: List<CostDto>, summary: SummaryDto, onOpen: (CostDto?, CostPreset?) -> Unit) {
    val lang = LocalLanguage.current
    val type = RencanaType.current
    val totals = summary.costs.associateBy { it.id }
    val used = costs.map { it.name }.toSet()
    val presets = COST_PRESETS.filter { it.label.text(lang) !in used }
    RCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(t(S.COSTS_OTHER_COSTS), Modifier.weight(1f), style = type.title)
            Row(
                Modifier.height(34.dp).background(Tint, CircleShape).clickable { onOpen(null, null) }.padding(horizontal = 12.dp).testTag("cost_add"),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Lucide.Plus, null, tint = RColors.PrimaryDark, modifier = Modifier.size(16.dp))
                Text(t(S.COSTS_ADD_COST), style = type.bodySemi.copy(fontSize = 14.sp, color = RColors.PrimaryDark))
            }
        }
        if (costs.isEmpty()) Text(t(S.COSTS_EMPTY), style = type.body.copy(fontSize = 14.sp, color = RColors.Muted), modifier = Modifier.padding(vertical = 8.dp))
        for (c in costs) {
            val total = totals[c.id]
            val sub = when {
                c.frequency == "one_time" -> t(S.COSTS_ONE_TIME)
                else -> {
                    val amount = formatMoney(c.amount.toDoubleOrNull() ?: 0.0, c.currency, lang)
                    if (c.days == null) t(S.COSTS_PER_DAY_TRIP_LENGTH, "amount" to amount, "days" to summary.tripDays)
                    else t(S.COSTS_PER_DAY, "amount" to amount, "days" to c.days)
                }
            }
            Row(Modifier.fillMaxWidth().clickable { onOpen(c, null) }.padding(vertical = 10.dp).testTag("cost_row"), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(c.name, style = type.bodySemi)
                    Text(sub, style = type.caption)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatMoney(total?.total ?: c.amount.toDoubleOrNull() ?: 0.0, c.currency, lang), style = type.bodySemi.copy(fontSize = 14.sp))
                    val conv = total?.converted
                    if (c.currency != summary.currency && conv != null) {
                        Text("≈ " + formatMoney(conv, summary.currency, lang), style = type.caption.copy(fontSize = 12.sp))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(RColors.Border))
        }
        if (presets.isNotEmpty()) {
            Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (p in presets) {
                    Box(
                        Modifier.height(36.dp).background(RColors.Surface, CircleShape)
                            .border15().clickable { onOpen(null, p) }.padding(horizontal = 14.dp).testTag("cost_preset_${p.key}"),
                        contentAlignment = Alignment.Center
                    ) { Text("+ " + t(p.label), style = type.bodySemi.copy(fontSize = 14.sp)) }
                }
            }
        }
    }
}

private fun Modifier.border15(): Modifier = this.border(1.5.dp, RColors.Border, CircleShape)

private fun fmtRate(n: Double, lang: String): String {
    val digits = if (n < 1) 6 else 2
    val pow = 10.0.pow(digits).toLong()
    val scaled = (n * pow).roundToLong()
    val int = (scaled / pow).toString().reversed().chunked(3).joinToString(if (lang == "id") "." else ",").reversed()
    val frac = (scaled % pow).toString().padStart(digits, '0').trimEnd('0')
    return if (frac.isEmpty()) int else int + (if (lang == "id") "," else ".") + frac
}

private fun fmtDate(iso: String, lang: String): String {
    val d = runCatching { LocalDate.parse(iso.take(10)) }.getOrNull() ?: return iso.take(10)
    val m = monthName(d.month.ordinal + 1, lang)
    return if (lang == "id") "${d.day} $m" else "$m ${d.day}"
}

@Composable
private fun RatesCard(planId: String, summary: SummaryDto, onChanged: () -> Unit) {
    val container = LocalAppContainer.current
    val lang = LocalLanguage.current
    val snack = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val type = RencanaType.current
    var editing by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf(false) }

    data class RateRow(val currency: String, val rate: com.zuramai.rencana.data.FxRateDto?)
    val rows = (summary.rates.map { RateRow(it.currency, it) } + summary.missingRates.map { RateRow(it, null) }).sortedBy { it.currency }

    fun put(currency: String, rate: Double?) {
        pending = true
        scope.launch {
            try {
                container.plans.setRate(planId, currency, rate)
                editing = null
                onChanged()
            } catch (e: Exception) {
                snack.showSnackbar(apiErrorKey(e).text(lang))
            } finally {
                pending = false
            }
        }
    }

    fun saveDraft(currency: String) {
        val rate = draft.trim().replace(',', '.').toDoubleOrNull()
        if (rate != null && rate.isFinite() && rate > 0) put(currency, rate)
        else scope.launch { snack.showSnackbar(S.ERRORS_VALIDATION.text(lang)) }
    }

    RCard(Modifier.fillMaxWidth().testTag("rates_card")) {
        Text(t(S.RATES_TITLE), style = type.title)
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (rows.isEmpty()) Text(t(S.RATES_NONE), style = type.body.copy(fontSize = 14.sp, color = RColors.Muted))
            for (row in rows) {
                val rate = row.rate
                when {
                    editing == row.currency -> androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(row.currency, Modifier.width(40.dp).padding(top = 14.dp), style = type.bodySemi.copy(fontSize = 14.sp))
                        RencanaTextField(
                            draft, { draft = it }, Modifier.testTag("rate_input"), containerModifier = Modifier.weight(1f),
                            placeholder = t(S.RATES_SET_RATE, "currency" to summary.currency), keyboardType = KeyboardType.Decimal
                        )
                        IconBtn(Lucide.Check, t(S.COMMON_SAVE), Modifier.testTag("rate_save")) { if (!pending) saveDraft(row.currency) }
                        IconBtn(Lucide.X, t(S.COMMON_CANCEL)) { editing = null }
                    }
                    rate != null -> androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(row.currency, Modifier.width(40.dp), style = type.bodySemi.copy(fontSize = 14.sp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                t(S.RATES_LINE, "from" to row.currency, "rate" to fmtRate(rate.rate, lang), "to" to summary.currency) +
                                    " · " + t(if (rate.source == "manual") S.RATES_MANUAL else S.RATES_AUTO) + " · " + fmtDate(rate.fetchedAt, lang),
                                style = type.caption
                            )
                            if (rate.source == "manual") {
                                Text(
                                    t(S.RATES_RESET_TO_AUTO), style = type.bodySemi.copy(fontSize = 13.sp, color = RColors.Primary),
                                    modifier = Modifier.clickable(enabled = !pending) { put(row.currency, null) }.testTag("rate_reset_${row.currency}")
                                )
                            }
                        }
                        IconBtn(Lucide.Pencil, t(S.COMMON_EDIT), Modifier.testTag("rate_edit_${row.currency}")) { editing = row.currency; draft = rate.rate.toString() }
                    }
                    else -> androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth().background(Color(0xFFFEF3C7), RoundedCornerShape(12.dp)).padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Lucide.TriangleAlert, null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                        Text(t(S.RATES_MISSING, "currency" to row.currency), Modifier.weight(1f), style = type.caption.copy(color = Color(0xFFB45309)))
                        IconBtn(Lucide.Pencil, t(S.COMMON_EDIT), Modifier.testTag("rate_edit_${row.currency}")) { editing = row.currency; draft = "" }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.size(44.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = RColors.Muted, modifier = Modifier.size(20.dp))
    }
}
