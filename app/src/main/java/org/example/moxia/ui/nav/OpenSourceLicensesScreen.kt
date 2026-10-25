package org.example.moxia.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import org.example.moxia.R
import org.example.moxia.ui.theme.AppCircularProgressIndicator
import org.example.moxia.ui.theme.navigationBarBottom

@Composable
internal fun OpenSourceLicensesScreen(onBack: () -> Unit) {
    val libraries by produceLibraries()
    SettingsPageScaffold(
        title = stringResource(R.string.profile_about_licenses),
        onBack = onBack,
        collapsingHeader = true
    ) { padding ->
        val loaded = libraries
        if (loaded == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                AppCircularProgressIndicator()
            }
        } else {
            // The licence list runs under the gesture handle, so the last row keeps that space
            // as its own content padding.
            LibrariesContainer(
                libraries = loaded,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = navigationBarBottom())
            )
        }
    }
}
