package com.zuramai.rencana.domain

private val MONTHS_EN = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val MONTHS_ID = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

/** Short month name for [month] 1..12 in [lang] (`en` or `id`). */
fun monthName(month: Int, lang: String): String = (if (lang == "id") MONTHS_ID else MONTHS_EN)[month - 1]

/**
 * Human period label: "Mar 2027", "2027", "Mar – Apr 2027", "Dec 2026 – Jan 2027", "2026 – 2027" ...
 * Null when there is no start year. Port of `formatPeriod` (shared/utils/period.ts).
 */
fun formatPeriod(startYear: Int?, startMonth: Int?, endYear: Int?, endMonth: Int?, lang: String): String? {
    if (startYear == null) return null
    fun single(year: Int, month: Int?) = if (month == null) year.toString() else "${monthName(month, lang)} $year"

    val start = single(startYear, startMonth)
    if (endYear == null) return start
    if (endYear == startYear && endMonth == startMonth) return start
    if (endYear == startYear && startMonth != null && endMonth != null) {
        return "${monthName(startMonth, lang)} – ${monthName(endMonth, lang)} $endYear"
    }
    return "$start – ${single(endYear, endMonth)}"
}
