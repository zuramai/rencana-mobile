package com.zuramai.rencana.platform

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication

actual suspend fun openWithDefaultApp(file: PlatformFile) {
    FileKit.openFileWithDefaultApplication(file)
}
