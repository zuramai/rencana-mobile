package com.zuramai.rencana.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** Current UI language, `"en"` or `"id"`. Provided once by `App()`; never read prefs directly in screens. */
val LocalLanguage = compositionLocalOf { "en" }

/** Replaces `{name}` placeholders with the given args. */
private fun String.fill(args: Array<out Pair<String, Any>>): String {
    var out = this
    for ((name, value) in args) out = out.replace("{$name}", value.toString())
    return out
}

/**
 * Localised text for [key] in the current [LocalLanguage].
 * Example: `t(S.TRAVEL_DELETE_TITLE, "name" to plan.name)`.
 */
@Composable
fun t(key: S, vararg args: Pair<String, Any>): String = key.text(LocalLanguage.current, *args)

/** Non-composable overload: [lang] is `"en"` or `"id"`. */
fun S.text(lang: String, vararg args: Pair<String, Any>): String =
    (if (lang == "id") id else en).fill(args)

/**
 * Picks `<base>_ZERO` / `<base>_ONE` / `<base>_OTHER` by [count] and fills `{count}`.
 * [zero], [one], [other] are the three generated entries (e.g. `S.TRAVEL_TRIP_COUNT_*`).
 */
@Composable
fun tPlural(zero: S, one: S, other: S, count: Int, vararg args: Pair<String, Any>): String =
    pluralKey(zero, one, other, count).text(LocalLanguage.current, "count" to count, *args)

/** Selects the plural form entry for [count]. */
fun pluralKey(zero: S, one: S, other: S, count: Int): S = when (count) {
    0 -> zero
    1 -> one
    else -> other
}

/** Entry for an API error code (`statusMessage`), or null if the app has no translation for it. */
fun errorKey(code: String): S? = S.entries.firstOrNull { it.name == "ERRORS_" + code.uppercase() }
