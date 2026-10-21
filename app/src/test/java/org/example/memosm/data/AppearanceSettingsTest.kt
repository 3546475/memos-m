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
import org.example.memosm.model.ThemeStyle
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
