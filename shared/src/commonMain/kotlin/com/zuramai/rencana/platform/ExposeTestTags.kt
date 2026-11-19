package com.zuramai.rencana.platform

import androidx.compose.ui.Modifier

/**
 * Android: exposes `Modifier.testTag` values as resource ids for uiautomator. iOS: identity.
 * Applied once to the root of `App()`.
 */
expect fun Modifier.exposeTestTags(): Modifier
