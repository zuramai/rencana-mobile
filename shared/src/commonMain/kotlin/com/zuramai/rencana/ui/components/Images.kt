package com.zuramai.rencana.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.zuramai.rencana.data.PlanListItem
import com.zuramai.rencana.domain.coverEmojiGlyph
import com.zuramai.rencana.domain.parseHexColor
import com.zuramai.rencana.ui.LocalAppContainer

/** Coil loader that sends the bearer token; for backend images (`/api/files/...`). Provided by `App()`. */
val LocalAuthImageLoader: ProvidableCompositionLocal<ImageLoader?> = staticCompositionLocalOf { null }

/** Plain Coil loader without credentials; for external images such as Google avatars. Provided by `App()`. */
val LocalPublicImageLoader: ProvidableCompositionLocal<ImageLoader?> = staticCompositionLocalOf { null }

/** The public loader (falls back to Coil's singleton outside `App()`, e.g. previews). */
@Composable
fun publicImageLoader(): ImageLoader =
    LocalPublicImageLoader.current ?: SingletonImageLoader.get(LocalPlatformContext.current)

/**
 * Backend image (`GET api/files/<[fileId]>`; [thumb] requests the JPEG thumbnail). Authenticated, never disk-cached.
 */
@Composable
fun AuthImage(
    fileId: String,
    modifier: Modifier = Modifier,
    thumb: Boolean = true,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null
) {
    val container = LocalAppContainer.current
    val url = container.files.fileUrl(fileId, thumb)
    val loader = LocalAuthImageLoader.current ?: SingletonImageLoader.get(LocalPlatformContext.current)
    AsyncImage(model = url, contentDescription = contentDescription, imageLoader = loader, contentScale = contentScale, modifier = modifier)
}

/**
 * Plan cover tile: colour background with either the cover emoji (Unicode glyph) or the cover photo.
 * [previewBytes] overrides the stored image with a just-picked local image (form preview).
 */
@Composable
fun PlanCover(
    coverType: String,
    coverEmoji: String?,
    coverFileId: String?,
    coverColor: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    radius: Dp = 16.dp,
    previewBytes: ByteArray? = null
) {
    Box(
        modifier = modifier.size(size).clip(androidx.compose.foundation.shape.RoundedCornerShape(radius)).background(parseHexColor(coverColor)),
        contentAlignment = Alignment.Center
    ) {
        when {
            coverType == "image" && previewBytes != null ->
                AsyncImage(model = previewBytes, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size), imageLoader = publicImageLoader())
            coverType == "image" && coverFileId != null ->
                AuthImage(coverFileId, Modifier.size(size))
            else -> {
                Text(coverEmojiGlyph(coverEmoji), fontSize = with(LocalDensity.current) { (size * 0.5f).toSp() })
            }
        }
    }
}

/** [PlanCover] for a list item or plan DTO's cover fields. */
@Composable
fun PlanCover(plan: PlanListItem, modifier: Modifier = Modifier, size: Dp = 56.dp, radius: Dp = 16.dp) =
    PlanCover(plan.coverType, plan.coverEmoji, plan.coverFileId, plan.coverColor, modifier, size, radius)
