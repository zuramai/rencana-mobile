package com.zuramai.rencana.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.resources.Res
import com.zuramai.rencana.resources.google_logo
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import org.jetbrains.compose.resources.painterResource

/**
 * Sub-screen header (60 dp): white round back button, optional [title], and [actions] pushed to the end.
 * Test tag of the back button: `header_back`.
 */
@Composable
fun PageHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().height(60.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(RColors.Surface)
                .clickable(role = Role.Button, onClick = onBack).testTag("header_back"),
            contentAlignment = Alignment.Center
        ) {
            Icon(Lucide.ArrowLeft, contentDescription = t(S.COMMON_BACK), modifier = Modifier.size(20.dp))
        }
        if (title != null) Text(title, style = RencanaType.current.title)
        Box(Modifier.weight(1f))
        actions()
    }
}

/** "or" separator between a primary action and an alternative. */
@Composable
fun OrDivider(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.5.dp).background(RColors.Border))
        Text(t(S.COMMON_OR), style = RencanaType.current.caption)
        Box(Modifier.weight(1f).height(1.5.dp).background(RColors.Border))
    }
}

/** "Continue with Google" pill (secondary style with the coloured G). Test tag: pass via [modifier]. */
@Composable
fun GoogleButton(onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false) {
    PillButton(
        text = t(S.AUTH_CONTINUE_WITH_GOOGLE),
        onClick = onClick,
        modifier = modifier,
        style = PillStyle.Secondary,
        loading = loading,
        leading = { Image(painterResource(Res.drawable.google_logo), contentDescription = null, modifier = Modifier.size(20.dp)) }
    )
}
