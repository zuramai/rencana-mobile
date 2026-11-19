package com.zuramai.rencana.data

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import com.zuramai.rencana.platform.openWithDefaultApp
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.write

/** Saves downloads to `cacheDir/exports/` and hands them to the system viewer. The directory is wiped at start and on logout. */
object FileOpener {
    private fun dir(): PlatformFile = FileKit.cacheDir / "exports"

    /** Removes every file saved by [save]. */
    suspend fun wipe() {
        runCatching {
            val d = dir()
            if (d.exists()) d.list().forEach { runCatching { it.delete() } }
        }
    }

    /** Characters outside `[A-Za-z0-9._-]` become `_`; never empty, never a dot-leading/path name. */
    fun sanitizeName(raw: String?, fallback: String): String {
        val cleaned = (raw ?: "").substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._-]"), "_").trimStart('.')
        return cleaned.ifEmpty { fallback }
    }

    /** Writes [download] into the export cache. [fallbackName] is used when the server sent no usable name. */
    suspend fun save(download: Download, fallbackName: String): PlatformFile {
        val d = dir()
        d.createDirectories()
        val file = d / sanitizeName(download.fileName, fallbackName)
        file.write(download.bytes)
        return file
    }

    /** Opens [file] with the default app (Android FileProvider / iOS document interaction). Throws on failure. */
    suspend fun open(file: PlatformFile) {
        openWithDefaultApp(file)
    }

    /** Convenience: [save] then [open]. Throws on failure; callers show `S.COMMON_OPEN_FAILED`. */
    suspend fun saveAndOpen(download: Download, fallbackName: String) = open(save(download, fallbackName))
}
