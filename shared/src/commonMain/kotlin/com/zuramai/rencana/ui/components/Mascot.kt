package com.zuramai.rencana.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zuramai.rencana.resources.Res
import com.zuramai.rencana.resources.mascot_construction
import com.zuramai.rencana.resources.mascot_happy
import com.zuramai.rencana.resources.mascot_map
import com.zuramai.rencana.resources.mascot_wave
import org.jetbrains.compose.resources.painterResource

/** The Koper mascot poses (vector drawables converted from the web SVGs). */
enum class MascotPose { Wave, Happy, Map, Construction }

/** Mascot illustration of the given [pose] at [size] x [size]. */
@Composable
fun Mascot(pose: MascotPose, modifier: Modifier = Modifier, size: Dp = 160.dp) {
    val res = when (pose) {
        MascotPose.Wave -> Res.drawable.mascot_wave
        MascotPose.Happy -> Res.drawable.mascot_happy
        MascotPose.Map -> Res.drawable.mascot_map
        MascotPose.Construction -> Res.drawable.mascot_construction
    }
    Image(painterResource(res), contentDescription = null, modifier = modifier.size(size))
}
