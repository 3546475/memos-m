package org.example.memosm.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.gson.JsonObject
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.example.memosm.data.backup.BackupCategory
import org.example.memosm.data.backup.BackupManifest
import org.example.memosm.model.AppFont
import org.example.memosm.model.DefaultNavDestinations
import org.example.memosm.model.NavBarScrollMode
import org.example.memosm.model.TopBarScrollMode
import org.example.memosm.ui.component.setting.enabledDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppearanceSettingsTest {
    @Test
    fun `a backup from the Material Design build restores its appearance settings`() = withSettings { settings ->
        // The style switch is gone, but an older backup still carries its key: restoring must not
        // fail on it, and the rest of the appearance settings must land unchanged.
        val settingsJson = JsonObject().apply {
            addProperty(DataStoreManager.LEGACY_APPEARANCE_THEME_STYLE, "MIUIX")
            addProperty(DataStoreManager.APPEARANCE_FONT.name, AppFont.SERIF.name)
            addProperty(DataStoreManager.APPEARANCE_TOP_SCROLL_MODE.name, TopBarScrollMode.ALWAYS.name)
        }
        settings.applyBackup(
            BackupManifest(
                createdAt = 0L,
                categories = setOf(BackupCategory.SETTINGS),
                accounts = emptyList(),
                settings = settingsJson
            )
        )
        val appearance = settings.appearance.first()
        assertEquals(AppFont.SERIF, appearance.font)
        assertEquals(TopBarScrollMode.ALWAYS, appearance.topBarScrollMode)
    }

    @Test
    fun `frosting is stored per bar`() = withSettings { settings ->
        // Only the bottom bar ships frosted; the header is a thin row.
        assertEquals(false, settings.appearance.first().topBarBlur)
        assertEquals(true, settings.appearance.first().bottomBarBlur)

        settings.saveTopBarBlur(true)
        assertEquals(true, settings.appearance.first().topBarBlur)
        assertEquals(true, settings.appearance.first().bottomBarBlur)

        settings.saveBottomBarBlur(false)
        assertEquals(true, settings.appearance.first().topBarBlur)
        assertEquals(false, settings.appearance.first().bottomBarBlur)
    }

    @Test
    fun `a fresh install starts with the floating two-item bar`() = withSettings { settings ->
        val appearance = settings.appearance.first()
        assertEquals(NavBarScrollMode.ALWAYS_VISIBLE, appearance.navBarScrollMode)
        assertEquals(TopBarScrollMode.ON_SCROLL, appearance.topBarScrollMode)
        assertEquals(true, appearance.floatingNavBar)
        assertEquals(DefaultNavDestinations, appearance.navDestinations)

        // Only notes and profile are in the bar, and in that order.
        val available = listOf(
            org.example.memosm.ui.MainDestination.MEMOS,
            org.example.memosm.ui.MainDestination.ATTACHMENTS,
            org.example.memosm.ui.MainDestination.PROFILE
        )
        assertEquals(
            listOf(
                org.example.memosm.ui.MainDestination.MEMOS,
                org.example.memosm.ui.MainDestination.PROFILE
            ),
            enabledDestinations(appearance.navDestinations, available)
        )
    }

    @Test
    fun `the old single frosting switch seeds both bars`() = withSettings { settings ->
        // Backups from the build with one switch still carry that key; it has to keep working
        // without overriding a per-bar choice made afterwards.
        val settingsJson = JsonObject().apply {
            addProperty(DataStoreManager.APPEARANCE_BLUR_ENABLED.name, false)
        }
        settings.applyBackup(
            BackupManifest(
                createdAt = 0L,
                categories = setOf(BackupCategory.SETTINGS),
                accounts = emptyList(),
                settings = settingsJson
            )
        )
        assertEquals(false, settings.appearance.first().topBarBlur)
        assertEquals(false, settings.appearance.first().bottomBarBlur)

        settings.saveTopBarBlur(true)
        assertEquals(true, settings.appearance.first().topBarBlur)
        assertEquals(false, settings.appearance.first().bottomBarBlur)
    }

    private fun withSettings(check: suspend (DataStoreManager) -> Unit) = runBlocking {
        val directory = Files.createTempDirectory("appearance-settings").toFile()
        val job = SupervisorJob()
        try {
            val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) {
                directory.resolve("settings.preferences_pb")
            }
            check(DataStoreManager(store))
        } finally {
            job.cancelAndJoin()
            directory.deleteRecursively()
        }
    }
}
