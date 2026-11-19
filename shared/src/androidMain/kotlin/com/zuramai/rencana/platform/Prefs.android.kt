package com.zuramai.rencana.platform

import android.content.Context

private class AndroidPrefs(context: Context) : Prefs {
    private val prefs = context.getSharedPreferences("rencana_prefs", Context.MODE_PRIVATE)
    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun remove(key: String) { prefs.edit().remove(key).apply() }
}

actual fun createPrefs(): Prefs = AndroidPrefs(AndroidPlatform.appContext)
