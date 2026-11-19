package com.zuramai.rencana.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plane
import com.composables.icons.lucide.Wallet
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RShapes
import com.zuramai.rencana.ui.RencanaType

/**
 * Top bar with the "rencana" wordmark, the 32 dp happy mascot and the user's avatar (Google photo, else the
 * initial on a coral disc). Tapping the avatar calls [onAvatarClick] (navigate to Settings); the logo calls [onLogoClick].
 * Test tags: `top_bar_avatar`.
 */
@Composable
fun RencanaTopBar(
    displayName: String?,
    avatarUrl: String?,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLogoClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.let { if (onLogoClick != null) it.clickable(onClick = onLogoClick) else it },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Mascot(MascotPose.Happy, size = 32.dp)
            Text("rencana", style = RencanaType.current.title.copy(color = RColors.Primary, fontSize = 24.sp))
        }
        Box(Modifier.weight(1f))
        val initial = displayName?.trim()?.firstOrNull()?.uppercase() ?: "K"
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(RColors.Coral)
                .clickable(role = Role.Button, onClick = onAvatarClick).testTag("top_bar_avatar"),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl, contentDescription = null, imageLoader = publicImageLoader(),
                    contentScale = ContentScale.Crop, modifier = Modifier.size(36.dp)
                )
            } else {
                Text(initial, style = RencanaType.current.bodySemi.copy(color = Color.White))
            }
        }
    }
}

/** Bottom navigation tabs. */
enum class BottomTab { Travel, Budget }

/**
 * Bottom navigation with the Travel and Budget pills. Test tags: `nav_travel`, `nav_budget`.
 */
@Composable
fun RencanaBottomNav(selected: BottomTab, onSelect: (BottomTab) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = RColors.Surface, modifier = modifier.fillMaxWidth(), shadowElevation = 0.dp) {
        Column {
            Box(Modifier.fillMaxWidth().height(1.dp).background(RColors.Border))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NavPill(Lucide.Plane, t(S.NAV_TRAVEL), selected == BottomTab.Travel, { onSelect(BottomTab.Travel) }, Modifier.weight(1f).testTag("nav_travel"))
                NavPill(Lucide.Wallet, t(S.NAV_BUDGET), selected == BottomTab.Budget, { onSelect(BottomTab.Budget) }, Modifier.weight(1f).testTag("nav_budget"))
            }
        }
    }
}

@Composable
private fun NavPill(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val tint = if (active) Color.White else RColors.Muted
    Row(
        modifier = modifier.height(48.dp).clip(RShapes.Pill)
            .background(if (active) RColors.Primary else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = RencanaType.current.bodySemi.copy(color = tint))
    }
}

/**
 * EN / ID switch (globe + two pills). [language] is the active code; [onChange] receives `"en"` or `"id"`.
 * Test tags: `lang_en`, `lang_id`.
 */
@Composable
fun LangSwitch(language: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clip(RShapes.Pill).background(RColors.Surface)
            .border(BorderStroke(1.5.dp, RColors.Border), RShapes.Pill)
            .padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(Lucide.Globe, contentDescription = null, tint = RColors.Muted, modifier = Modifier.size(16.dp))
        for (code in listOf("en", "id")) {
            val active = language == code
            Box(
                modifier = Modifier.height(28.dp).clip(RShapes.Pill)
                    .background(if (active) RColors.Primary else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onChange(code) }
                    .padding(horizontal = 12.dp)
                    .semantics { contentDescription = code }
                    .testTag("lang_$code"),
                contentAlignment = Alignment.Center
            ) {
                Text(code.uppercase(), style = RencanaType.current.caption.copy(color = if (active) Color.White else RColors.Muted))
            }
        }
    }
}
