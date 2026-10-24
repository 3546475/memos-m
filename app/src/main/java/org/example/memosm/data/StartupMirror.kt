package org.example.memosm.data

import android.content.Context
import android.content.SharedPreferences

/**
 * A tiny mirror of "the last run reached the feed with an active account", kept outside DataStore.
 *
 * DataStore stays the single source of truth; this file is only a hint for the launch placeholder,
 * so the very first frame can already tell whether the app is heading for the memo feed or for
 * setup/login. It is read synchronously in [org.example.memosm.MemosApplication.onCreate] and
 * rewritten whenever the real account/setup values change.
 */
object StartupMirror {
    private const val PREFS_NAME = "startup_mirror"
    private const val KEY_ONBOARDED = "onboarded"

    @Volatile
    private var onboardedWithAccount = false

    /** Reads the mirror synchronously: one small prefs file, no coroutine, no DataStore. */
    fun load(context: Context) {
        onboardedWithAccount = prefs(context).getBoolean(KEY_ONBOARDED, false)
    }

    /**
     * True when the last recorded state had setup completed and an active account. The launch
     * placeholder then draws the home shell; every real routing decision still waits for DataStore.
     */
    val showsHomeShell: Boolean
        get() = onboardedWithAccount

    /** Mirrors the latest DataStore state. A no-op when nothing changed, so it costs no IO. */
    fun update(context: Context, onboarded: Boolean) {
        if (onboardedWithAccount == onboarded) return
        onboardedWithAccount = onboarded
        prefs(context).edit().putBoolean(KEY_ONBOARDED, onboarded).apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
