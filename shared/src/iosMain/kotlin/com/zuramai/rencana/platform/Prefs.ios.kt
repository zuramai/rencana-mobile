package com.zuramai.rencana.platform

import platform.Foundation.NSUserDefaults

private class UserDefaultsPrefs : Prefs {
    private val defaults = NSUserDefaults.standardUserDefaults
    override fun getString(key: String): String? = defaults.stringForKey(key)
    override fun putString(key: String, value: String) = defaults.setObject(value, forKey = key)
    override fun remove(key: String) = defaults.removeObjectForKey(key)
}

actual fun createPrefs(): Prefs = UserDefaultsPrefs()
