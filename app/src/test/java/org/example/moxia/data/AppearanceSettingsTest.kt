package org.example.moxia.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.gson.JsonObject
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.example.moxia.data.backup.BackupCategory
import org.example.moxia.data.backup.BackupManifest
import org.example.moxia.model.DefaultNavDestinations
import org.example.moxia.model.NavBarScrollMode
import org.example.moxia.model.ThemeStyle
import org.example.moxia.model.TopBarScrollMode
import org.example.moxia.ui.component.setting.enabledDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppearanceSettingsTest {
    @Test
    fun `theme style defaults to Miuix and survives backup restore`() = withSettings { settings ->
        assertEquals(ThemeStyle.MIUIX, settings.appearance.first().themeStyle)

        settings.saveThemeStyle(ThemeStyle.MD)
        assertEquals(ThemeStyle.MD, settings.appearance.first().themeStyle)

        val backup = settings.backupSettings()
        assertEquals(ThemeStyle.MD.name, backup.get(DataStoreManager.APPEARANCE_THEME_STYLE.name).asString)

        settings.saveThemeStyle(ThemeStyle.MIUIX)
        settings.applyBackup(
            BackupManifest(
                createdAt = 0L,
                categories = setOf(BackupCategory.SETTINGS),
                accounts = emptyList(),
                settings = backup
            )
        )
        assertEquals(ThemeStyle.MD, settings.appearance.first().themeStyle)
    }

    @Test
    fun `restoring an unknown theme style is rejected`() = withSettings { settings ->
        val settingsJson = JsonObject().apply {
            addProperty(DataStoreManager.APPEARANCE_THEME_STYLE.name, "MIUI_X")
        }
        assertThrows(IllegalArgumentException::class.java) { settings.validateBackupSettings(settingsJson) }
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
            org.example.moxia.ui.MainDestination.MEMOS,
            org.example.moxia.ui.MainDestination.ATTACHMENTS,
            org.example.moxia.ui.MainDestination.PROFILE
        )
        assertEquals(
            listOf(
                org.example.moxia.ui.MainDestination.MEMOS,
                org.example.moxia.ui.MainDestination.PROFILE
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
