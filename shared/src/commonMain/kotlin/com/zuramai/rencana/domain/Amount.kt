package com.zuramai.rencana.domain

/**
 * Free-typed amount to a decimal string for the API ("25.000.000" -> "25000000", "1.234,5" -> "1234.5",
 * "12,50" -> "12.50"); null when empty. A lone separator followed by exactly three digits is read as a
 * thousands separator. Port of `normalizeAmount` (shared/utils/money.ts).
 */
fun normalizeAmount(input: String): String? {
    val s = input.filterNot { it.isWhitespace() || it == '\u00A0' }
    if (s.isEmpty()) return null
    val lastSep = maxOf(s.lastIndexOf('.'), s.lastIndexOf(','))
    if (lastSep == -1) return s
    val separatorKinds = s.filter { it == '.' || it == ',' }.toSet()
    if (s.length - lastSep - 1 == 3 && separatorKinds.size == 1) return s.filterNot { it == '.' || it == ',' }
    val head = s.substring(0, lastSep).filterNot { it == '.' || it == ',' }
    return "$head.${s.substring(lastSep + 1)}"
}

/** Stored decimal ("25000000.00") to editable text ("25000000"), mirroring `String(Number(value))`. */
fun amountToInput(value: String?): String {
    if (value == null) return ""
    val d = value.toDoubleOrNull() ?: return value
    return if (d % 1.0 == 0.0 && kotlin.math.abs(d) < 1e15) d.toLong().toString() else d.toString()
}
