package com.zuramai.rencana.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AmountTest {
    @Test
    fun normalizeAmount() {
        val cases = listOf(
            "" to null, "  " to null, "20000" to "20000", "25.000.000" to "25000000", "1,000" to "1000",
            "12.5" to "12.5", "12,50" to "12.50", "1.234,56" to "1234.56", "1,234.56" to "1234.56", "300 000" to "300000"
        )
        for ((input, expected) in cases) assertEquals(expected, normalizeAmount(input), "normalizeAmount($input)")
    }

    @Test
    fun amountToInput() {
        assertEquals("25000000", amountToInput("25000000.00"))
        assertEquals("12.5", amountToInput("12.50"))
        assertEquals("", amountToInput(null))
    }
}

class PeriodTest {
    private fun p(sy: Int?, sm: Int?, ey: Int?, em: Int?, lang: String = "en") = formatPeriod(sy, sm, ey, em, lang)

    @Test
    fun formats() {
        assertEquals("Mar 2027", p(2027, 3, null, null))
        assertEquals("2027", p(2027, null, null, null))
        assertEquals("Mar – Apr 2027", p(2027, 3, 2027, 4))
        assertEquals("Dec 2026 – Jan 2027", p(2026, 12, 2027, 1))
        assertEquals("2026 – 2027", p(2026, null, 2027, null))
        assertEquals("Mar 2026 – 2027", p(2026, 3, 2027, null))
        assertEquals("2026 – Jan 2027", p(2026, null, 2027, 1))
        assertEquals("Mar 2027", p(2027, 3, 2027, 3))
        assertEquals("2027", p(2027, null, 2027, null))
    }

    @Test
    fun nullWithoutStart() = assertNull(p(null, null, null, null))

    @Test
    fun localizesMonthNames() = assertEquals("Mei 2027", p(2027, 5, null, null, "id"))
}

class ItineraryTest {
    // Expected strings come from the TS formatDayLabel under Node 24:
    // new Intl.DateTimeFormat(locale,{weekday:'short',day:'numeric',month:'short',timeZone:'UTC'}).
    @Test
    fun dayLabelMatchesTs() {
        assertEquals("Wed, Mar 3", formatDayLabel("2027-03-03", "en"))
        assertEquals("Rab, 3 Mar", formatDayLabel("2027-03-03", "id"))
        assertEquals("Sat, Dec 25", formatDayLabel("2027-12-25", "en"))
        assertEquals("Sab, 25 Des", formatDayLabel("2027-12-25", "id"))
    }

    @Test
    fun groupsByDay() {
        val items = listOf("2027-03-03T09:00", "2027-03-03T12:00", "2027-03-05T08:00")
        val groups = groupByDay(items) { it }
        assertEquals(2, groups.size)
        assertEquals(1, groups[0].dayNumber)
        assertEquals(2, groups[0].items.size)
        assertEquals("2027-03-05", groups[1].date)
        assertEquals(3, groups[1].dayNumber)
        assertEquals(emptyList(), groupByDay(emptyList<String>()) { it })
    }
}
