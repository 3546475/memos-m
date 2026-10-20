package org.example.memosm.ui.component.setting

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.example.memosm.R
import org.example.memosm.data.DataStoreManager
import org.example.memosm.model.AppearancePreferences
import org.example.memosm.model.ThemeStyle
import org.koin.core.context.GlobalContext

/**
 * Switches the whole app between the original Material Design styling and the Miuix (HyperOS)
 * theme from the miuix library. The theme is applied by [org.example.memosm.ui.theme.MemosMTheme].
 */
@Composable
internal fun ThemeStyleRow(
    dataStore: DataStoreManager = remember { GlobalContext.get().get() }
) {
    val appearance by dataStore.appearance.collectAsStateWithLifecycle(initialValue = AppearancePreferences())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val saveError = stringResource(R.string.appearance_save_error)
    val miuix = appearance.themeStyle == ThemeStyle.MIUIX
    SettingToggleRow(
        label = stringResource(R.string.settings_theme_style),
        description = stringResource(
            if (miuix) R.string.settings_theme_style_miuix else R.string.settings_theme_style_md
        ),
        checked = miuix,
        onCheckedChange = { enabled ->
            scope.launch {
                try {
                    dataStore.saveThemeStyle(if (enabled) ThemeStyle.MIUIX else ThemeStyle.MD)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    Toast.makeText(context, saveError, Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
}
