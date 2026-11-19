package com.zuramai.rencana.platform

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

actual fun Modifier.exposeTestTags(): Modifier = semantics { testTagsAsResourceId = true }
