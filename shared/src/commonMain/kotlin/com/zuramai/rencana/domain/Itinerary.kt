package com.zuramai.rencana.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** One itinerary day: [date] is `YYYY-MM-DD`, [dayNumber] is 1-based from the first item's date. */
data class DayGroup<T>(val date: String, val dayNumber: Int, val items: List<T>)

/** Groups items (already sorted by `startsAt`) by the date part of `startsAt`. */
fun <T> groupByDay(items: List<T>, startsAt: (T) -> String): List<DayGroup<T>> {
    if (items.isEmpty()) return emptyList()
    val first = LocalDate.parse(startsAt(items.first()).take(10))
    val order = LinkedHashMap<String, MutableList<T>>()
    for (item in items) order.getOrPut(startsAt(item).take(10)) { mutableListOf() }.add(item)
    // Equal dates are contiguous when the input is sorted, so grouping by key preserves order.
    return order.map { (date, group) -> DayGroup(date, first.daysUntil(LocalDate.parse(date)) + 1, group) }
}

private val WEEKDAYS_EN = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val WEEKDAYS_ID = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

/**
 * "Wed, Mar 3" (en) / "Rab, 3 Mar" (id) for a `YYYY-MM-DD` date, independent of the device time zone.
 * Matches the TS `formatDayLabel` for `en` and `id-ID`.
 */
fun formatDayLabel(date: String, lang: String): String {
    val d = LocalDate.parse(date.take(10))
    val weekday = (if (lang == "id") WEEKDAYS_ID else WEEKDAYS_EN)[d.dayOfWeek.ordinal]
    val month = monthName(d.month.ordinal + 1, lang)
    return if (lang == "id") "$weekday, ${d.day} $month" else "$weekday, $month ${d.day}"
}
