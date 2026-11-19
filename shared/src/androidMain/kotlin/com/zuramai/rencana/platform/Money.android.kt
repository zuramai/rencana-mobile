package com.zuramai.rencana.platform

import android.icu.number.NumberFormatter
import android.os.Build
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private fun localeFor(lang: String): Locale = Locale.forLanguageTag(if (lang == "id") "id-ID" else "en-US")

actual fun formatMoney(amount: Double, currency: String, lang: String): String {
    val locale = localeFor(lang)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return NumberFormatter.withLocale(locale)
            .unit(android.icu.util.Currency.getInstance(currency))
            .unitWidth(NumberFormatter.UnitWidth.NARROW)
            .format(amount)
            .toString()
    }
    return NumberFormat.getCurrencyInstance(locale).apply { this.currency = Currency.getInstance(currency) }.format(amount)
}

actual fun currencyOptions(lang: String): List<Pair<String, String>> {
    val locale = localeFor(lang)
    return Currency.getAvailableCurrencies()
        .map { it.currencyCode to it.getDisplayName(locale) }
        .sortedBy { it.first }
}
