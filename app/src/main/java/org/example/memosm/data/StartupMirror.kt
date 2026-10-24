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
    private const val KEY_FEED_TOP_INSET = "feed_top_inset_dp"
    private const val KEY_FEED_PANE_TOP = "feed_pane_top_dp"

    @Volatile
    private var onboardedWithAccount = false

    @Volatile
    private var feedTopInset = 64f

    @Volatile
    private var feedPaneTop = 0f

    /** Reads the mirror synchronously: one small prefs file, no coroutine, no DataStore. */
    fun load(context: Context) {
        val prefs = prefs(context)
        onboardedWithAccount = prefs.getBoolean(KEY_ONBOARDED, false)
        feedTopInset = prefs.getFloat(KEY_FEED_TOP_INSET, 64f)
        feedPaneTop = prefs.getFloat(KEY_FEED_PANE_TOP, 0f)
    }

    /**
     * Where the feed puts its first card, in dp, as the real feed measured it on the last run.
     * The launch shell places its placeholder cards at the same offset, so revealing the feed
     * does not shift them.
     */
    val feedTopInsetDp: Float
        get() = feedTopInset

    /**
     * Where the list pane itself starts, in dp: the top bar and its insets sit above the feed, so
     * the first card is this plus [feedTopInsetDp]. Mirrored by the real pane on layout.
     */
    val feedPaneTopDp: Float
        get() = feedPaneTop

    /** Mirrors the list pane's top offset. A no-op when nothing changed. */
    fun updateFeedPaneTop(context: Context, dp: Float) {
        if (feedPaneTop == dp) return
        feedPaneTop = dp
        prefs(context).edit().putFloat(KEY_FEED_PANE_TOP, dp).apply()
    }

    /** Mirrors the feed's first-card offset. A no-op when nothing changed. */
    fun updateFeedTopInset(context: Context, dp: Float) {
        if (feedTopInset == dp) return
        feedTopInset = dp
        prefs(context).edit().putFloat(KEY_FEED_TOP_INSET, dp).apply()
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
