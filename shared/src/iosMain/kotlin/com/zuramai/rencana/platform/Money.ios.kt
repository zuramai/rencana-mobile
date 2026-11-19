package com.zuramai.rencana.platform

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCurrencyCode
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.ISOCurrencyCodes
import platform.Foundation.localeWithLocaleIdentifier

private fun localeFor(lang: String): NSLocale = NSLocale.localeWithLocaleIdentifier(if (lang == "id") "id_ID" else "en_US")

actual fun formatMoney(amount: Double, currency: String, lang: String): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterCurrencyStyle
        currencyCode = currency
        locale = localeFor(lang)
    }
    return formatter.stringFromNumber(NSNumber(double = amount)) ?: "$currency $amount"
}

actual fun currencyOptions(lang: String): List<Pair<String, String>> {
    val locale = localeFor(lang)
    return NSLocale.ISOCurrencyCodes.filterIsInstance<String>()
        .map { code -> code to (locale.displayNameForKey(NSLocaleCurrencyCode, code) ?: code) }
        .sortedBy { it.first }
}
