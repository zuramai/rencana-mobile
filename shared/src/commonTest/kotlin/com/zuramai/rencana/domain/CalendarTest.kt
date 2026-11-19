package com.zuramai.rencana.domain

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CalendarTest {
    private val base = CalendarItem("Ramen dinner", false, "2027-03-03T19:00", null, null, null)
    private fun query(url: String) = Url(url).parameters

    @Test
    fun defaultsTimedEventsToOneHourInDestinationZone() {
        val url = googleCalendarUrl(base, "Asia/Tokyo")
        assertEquals("https://calendar.google.com/calendar/render", url.substringBefore('?'))
        val q = query(url)
        assertEquals("TEMPLATE", q["action"])
        assertEquals("Ramen dinner", q["text"])
        assertEquals("20270303T190000/20270303T200000", q["dates"])
        assertEquals("Asia/Tokyo", q["ctz"])
        assertFalse(q.contains("details"))
    }

    @Test
    fun rollsDefaultEndOverMidnight() {
        assertEquals("20271231T233000/20280101T003000", query(googleCalendarUrl(base.copy(startsAt = "2027-12-31T23:30"), null))["dates"])
    }

    @Test
    fun allDayUsesDatesWithExclusiveEndAndNoCtz() {
        val q = query(googleCalendarUrl(base.copy(allDay = true, startsAt = "2027-03-03T00:00", endsAt = "2027-03-05T00:00"), "Asia/Tokyo"))
        assertEquals("20270303/20270306", q["dates"])
        assertFalse(q.contains("ctz"))
    }

    @Test
    fun putsNoteAndLinkInDetails() {
        val q = query(googleCalendarUrl(base.copy(endsAt = "2027-03-03T21:15", note = "Book ahead", link = "https://ichiran.com"), null))
        assertEquals("20270303T190000/20270303T211500", q["dates"])
        assertEquals("Book ahead\n\nhttps://ichiran.com", q["details"])
        assertFalse(q.contains("ctz"))
    }
}
