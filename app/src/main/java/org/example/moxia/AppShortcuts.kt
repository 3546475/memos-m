package org.example.moxia

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Long-press launcher entries. Published dynamically rather than through res/xml/shortcuts.xml:
 * the entries need distinct actions for [MainActivity] to route, and the debug/canary builds ship
 * a suffixed applicationId that a static `targetPackage` would not match.
 */
object AppShortcuts {

    const val ACTION_NEW_MEMO = "org.example.moxia.action.NEW_MEMO"
    const val ACTION_OPEN_DRAFTS = "org.example.moxia.action.OPEN_DRAFTS"
    const val ACTION_OPEN_NOTIFICATIONS = "org.example.moxia.action.OPEN_NOTIFICATIONS"
    const val ACTION_OPEN_SEARCH = "org.example.moxia.action.OPEN_SEARCH"

    private data class Entry(
        val id: String,
        val action: String,
        val shortLabel: Int,
        val longLabel: Int,
        val icon: Int
    )

    private val entries = listOf(
        Entry(
            id = "new_memo",
            action = ACTION_NEW_MEMO,
            shortLabel = R.string.shortcut_new_memo_short,
            longLabel = R.string.memo_composer_fab_new_memo,
            icon = R.drawable.shortcut_new_memo
        ),
        Entry(
            id = "drafts",
            action = ACTION_OPEN_DRAFTS,
            shortLabel = R.string.drafts_title,
            longLabel = R.string.drafts_title,
            icon = R.drawable.shortcut_drafts
        ),
        Entry(
            id = "notifications",
            action = ACTION_OPEN_NOTIFICATIONS,
            shortLabel = R.string.shortcut_notifications_short,
            longLabel = R.string.profile_notifications,
            icon = R.drawable.shortcut_notifications
        ),
        Entry(
            id = "search",
            action = ACTION_OPEN_SEARCH,
            shortLabel = R.string.shortcut_search,
            longLabel = R.string.shortcut_search,
            icon = R.drawable.shortcut_search
        )
    )

    /** Idempotent: the launcher keeps whatever this call wrote last. */
    fun publish(context: Context) {
        runCatching {
            val shortcuts = entries.mapIndexed { index, entry ->
                ShortcutInfoCompat.Builder(context, entry.id)
                    .setShortLabel(context.getString(entry.shortLabel))
                    .setLongLabel(context.getString(entry.longLabel))
                    .setIcon(IconCompat.createWithResource(context, entry.icon))
                    .setIntent(
                        Intent(context, MainActivity::class.java)
                            .setAction(entry.action)
                            .addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            )
                    )
                    .setRank(index)
                    .build()
            }
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        }
    }
}
