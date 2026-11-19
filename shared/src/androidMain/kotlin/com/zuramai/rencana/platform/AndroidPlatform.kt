package com.zuramai.rencana.platform

import android.app.Activity
import android.content.Context
import java.lang.ref.WeakReference

/**
 * Android-only bootstrap state, set by `MainActivity.onCreate` before any shared code runs.
 * [appContext] is the application context; [activity] is the foreground activity (for Custom Tabs).
 */
object AndroidPlatform {
    lateinit var appContext: Context
        private set

    /** Mirrors the app's `BuildConfig.DEBUG`. */
    var isDebug: Boolean = false
        private set

    private var activityRef: WeakReference<Activity>? = null

    /** Foreground activity, or null when none. */
    val activity: Activity? get() = activityRef?.get()

    /** Call from `MainActivity.onCreate`. */
    fun init(activity: Activity, debug: Boolean) {
        appContext = activity.applicationContext
        isDebug = debug
        activityRef = WeakReference(activity)
    }

    /** Call from `MainActivity.onDestroy`. */
    fun detach(activity: Activity) {
        if (activityRef?.get() === activity) activityRef = null
    }
}
