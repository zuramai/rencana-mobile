package com.zuramai.rencana.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.Lucide
import com.zuramai.rencana.data.ApiException
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.errorKey
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RShapes
import com.zuramai.rencana.ui.RencanaType

/**
 * Maps a failure to its message key (plan 3.6 error rule): `errors.<code>` when the app knows the code,
 * `errors.validation` for any other 400, otherwise `common.somethingWrong`.
 */
fun apiErrorKey(e: Throwable): S {
    if (e is ApiException) {
        errorKey(e.code)?.let { return it }
        if (e.status == 400) return S.ERRORS_VALIDATION
    }
    return S.COMMON_SOMETHING_WRONG
}

/** Localised message for [e] (see [apiErrorKey]). */
@Composable
fun errorMessage(e: Throwable): String = t(apiErrorKey(e))

/** Subtle red alert box with an icon. Renders nothing when [message] is null. */
@Composable
fun ErrorText(message: String?, modifier: Modifier = Modifier) {
    if (message == null) return
    Row(
        modifier = modifier.fillMaxWidth()
            .background(RColors.DangerSoft, RShapes.Field)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Lucide.CircleAlert, contentDescription = null, tint = RColors.Danger, modifier = Modifier.size(20.dp))
        Text(message, style = RencanaType.current.bodySemi.copy(color = RColors.Danger))
    }
}
