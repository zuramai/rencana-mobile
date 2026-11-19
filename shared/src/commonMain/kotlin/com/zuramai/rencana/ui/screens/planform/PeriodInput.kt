package com.zuramai.rencana.ui.screens.planform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zuramai.rencana.i18n.LocalLanguage
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.RencanaType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

enum class PeriodMode { None, Month, Range }

/** Editable period (web `PeriodValue`). */
data class PeriodValue(
    val mode: PeriodMode = PeriodMode.None,
    val startYear: Int? = null,
    val startMonth: Int? = null,
    val endYear: Int? = null,
    val endMonth: Int? = null
)

private val MONTHS_LONG_EN = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
private val MONTHS_LONG_ID = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")

private fun currentYear(): Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year

/** Mode change rules of web `PeriodInput`. */
private fun PeriodValue.withMode(mode: PeriodMode, thisYear: Int): PeriodValue {
    var n = copy(mode = mode)
    if (mode == PeriodMode.None) n = n.copy(startYear = null, startMonth = null, endYear = null, endMonth = null)
    if (mode != PeriodMode.None && n.startYear == null) n = n.copy(startYear = thisYear)
    if (mode == PeriodMode.Month) n = n.copy(endYear = null, endMonth = null)
    if (mode == PeriodMode.Range && n.endYear == null) n = n.copy(endYear = n.startYear)
    return n
}

/** "When?" segmented control with month and year dropdowns. Tags: `period_<mode>`, `period_start_month` etc. */
@Composable
fun PeriodInput(value: PeriodValue, onChange: (PeriodValue) -> Unit, modifier: Modifier = Modifier) {
    val lang = LocalLanguage.current
    val thisYear = remember { currentYear() }
    val years = remember(value.startYear, value.endYear) {
        val list = (thisYear - 1..thisYear + 5).toMutableList()
        for (y in listOf(value.startYear, value.endYear)) if (y != null && y !in list) list.add(y)
        list.sorted().map { it to it.toString() }
    }
    val anyMonth = t(S.COMMON_ANY_MONTH)
    val months = remember(lang, anyMonth) {
        listOf(0 to anyMonth) + (if (lang == "id") MONTHS_LONG_ID else MONTHS_LONG_EN).mapIndexed { i, n -> (i + 1) to n }
    }
    val rowModifier = Modifier
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t(S.PLAN_WHEN), style = RencanaType.current.caption)
        SegmentedControl(
            listOf(
                PeriodMode.None to t(S.PLAN_NO_DATE),
                PeriodMode.Month to t(S.PLAN_MONTH),
                PeriodMode.Range to t(S.PLAN_RANGE)
            ),
            value.mode, { onChange(value.withMode(it, thisYear)) }, tag = "period"
        )
        if (value.mode != PeriodMode.None) {
            Row(rowModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (value.mode == PeriodMode.Range) Text(t(S.PLAN_FROM), style = RencanaType.current.bodySemi, modifier = Modifier.width(52.dp))
                DropdownField(months, value.startMonth ?: 0, { onChange(value.copy(startMonth = it.takeIf { m -> m != 0 })) }, Modifier.weight(1f), "period_start_month")
                DropdownField(years, value.startYear ?: thisYear, { onChange(value.copy(startYear = it)) }, Modifier.weight(1f), "period_start_year")
            }
        }
        if (value.mode == PeriodMode.Range) {
            Row(rowModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t(S.PLAN_TO), style = RencanaType.current.bodySemi, modifier = Modifier.width(52.dp))
                DropdownField(months, value.endMonth ?: 0, { onChange(value.copy(endMonth = it.takeIf { m -> m != 0 })) }, Modifier.weight(1f), "period_end_month")
                DropdownField(years, value.endYear ?: thisYear, { onChange(value.copy(endYear = it)) }, Modifier.weight(1f), "period_end_year")
            }
        }
    }
}
