package com.zuramai.rencana.domain

import com.zuramai.rencana.data.ItemDto
import io.ktor.http.encodeURLParameter
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.plus

/** Port of `rencana-backend/src/shared/utils/calendar.ts`; keep the two in sync. */
data class CalendarItem(
    val description: String,
    val allDay: Boolean,
    /** `YYYY-MM-DDTHH:mm`, destination wall clock */
    val startsAt: String,
    val endsAt: String?,
    val note: String?,
    val link: String?
)

fun ItemDto.toCalendarItem() = CalendarItem(description, allDay, startsAt, endsAt, note, link)

data class CalendarSpan(val allDay: Boolean, val start: String, val end: String)

const val DEFAULT_EVENT_MINUTES = 60

private fun pad(n: Int, width: Int = 2) = n.toString().padStart(width, '0')

private fun formatDate(d: LocalDate) = "${pad(d.year, 4)}-${pad(d.month.number)}-${pad(d.day)}"

/** `YYYY-MM-DD` shifted by whole days. */
fun addDays(date: String, days: Int): String = formatDate(LocalDate.parse(date.take(10)).plus(days, DateTimeUnit.DAY))

/** `YYYY-MM-DDTHH:mm` shifted by minutes (wall-clock arithmetic, no time zone). */
fun addMinutes(wallClock: String, minutes: Int): String {
    val date = LocalDate.parse(wallClock.take(10))
    val h = wallClock.substring(11, 13).toInt()
    val m = wallClock.substring(14, 16).toInt()
    val total = h * 60 + m + minutes
    val dayShift = total.floorDiv(1440)
    val rest = total - dayShift * 1440
    return "${formatDate(date.plus(dayShift, DateTimeUnit.DAY))}T${pad(rest / 60)}:${pad(rest % 60)}"
}

fun calendarSpan(item: CalendarItem): CalendarSpan {
    if (item.allDay) {
        val start = item.startsAt.take(10)
        val last = (item.endsAt ?: item.startsAt).take(10)
        return CalendarSpan(true, start, addDays(if (last < start) start else last, 1))
    }
    return CalendarSpan(false, item.startsAt, item.endsAt ?: addMinutes(item.startsAt, DEFAULT_EVENT_MINUTES))
}

/** `2027-03-03` -> `20270303`; `2027-03-03T15:00` -> `20270303T150000`. */
fun compactDateTime(value: String): String {
    val date = value.take(10).replace("-", "")
    return if (value.length > 10) "${date}T${value.substring(11, 13)}${value.substring(14, 16)}00" else date
}

fun calendarDetails(item: CalendarItem): String =
    listOfNotNull(item.note, item.link).filter { it.isNotEmpty() }.joinToString("\n\n")

/** Google Calendar "create event" link, prefilled with the item (same rules and param order as the backend). */
fun googleCalendarUrl(item: CalendarItem, timeZone: String?): String {
    val span = calendarSpan(item)
    val params = mutableListOf(
        "action" to "TEMPLATE",
        "text" to item.description,
        "dates" to "${compactDateTime(span.start)}/${compactDateTime(span.end)}"
    )
    val details = calendarDetails(item)
    if (details.isNotEmpty()) params += "details" to details
    if (!timeZone.isNullOrEmpty() && !span.allDay) params += "ctz" to timeZone
    val query = params.joinToString("&") { (k, v) -> "$k=${v.encodeURLParameter(spaceToPlus = true)}" }
    return "https://calendar.google.com/calendar/render?$query"
}
